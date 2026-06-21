# Jetris Protocol — Custom TCP Text Protocol

## Overview

Jetris uses a custom application-layer protocol over TCP that the developer deliberately designed to resemble HTTP's structural conventions. It is **not** HTTP, but its shape mirrors HTTP's request/response model closely enough that understanding HTTP makes understanding this protocol immediate.

The protocol is called the **Jetris Protocol** throughout this document.

---

## Why Build a Custom Protocol?

Most real-time games use either:

1. **Raw binary protocols** — compact but tedious to debug and extend.
2. **WebSockets + JSON** — convenient but heavyweight for a game where latency matters.
3. **gRPC / Protobuf** — powerful but complex to set up.

The Jetris Protocol was built with these goals:

- **Human-readable** — a `telnet` session or Wireshark dump is instantly legible.
- **Request/response correlation over a persistent connection** — a single TCP socket is reused for hundreds of different operations without HTTP's one-request-per-connection overhead.
- **Push notifications without polling** — the server can send unsolicited messages at any time (game state, chat, results) without the client having to ask.
- **Async-safe callbacks** — multiple requests can be in-flight simultaneously; responses are matched to their caller by ID.
- **Minimal parsing** — `String.split(" ", 4)` is the entire parser.

---

## Protocol Grammar (EBNF)

```ebnf
message      = type " " code " " callbackCode " " body newline ;
type         = UPPERCASE_WORD ;
code         = non_negative_integer ;
callbackCode = non_negative_integer ;
body         = any_printable_characters_including_spaces ;
newline      = "\n" ;

UPPERCASE_WORD       = letter { letter | "_" } ;
non_negative_integer = "0" | digit_nonzero { digit } ;
```

**Key constraint:** The parser splits on exactly **3** space tokens (using `split(" ", 4)`), so `body` is everything after the third space — including any embedded spaces or special characters. This allows base64-encoded images, long messages, and multi-field payloads to appear naturally in the body without escaping.

---

## Comparison with HTTP

| HTTP concept | Jetris Protocol equivalent |
|---|---|
| Method (`GET`, `POST`, ...) | `TYPE` (`LOGIN`, `MATCH`, `MOVE`, ...) |
| Path (`/api/profile/42`) | `code` (integer sub-operation) |
| `X-Request-ID` header | `callbackCode` (correlation integer) |
| Body | `body` (rest of line) |
| Status line (`200 OK`, `404 Not Found`) | First word(s) of `body` (`SUCCESS`, `FAIL reason`) |
| Connection: keep-alive | Implicit — single persistent socket for entire session |
| Server-sent events / WebSocket push | callbackCode = 0 push messages |
| Middleware (`express.use(...)`) | Middleware chain (`AddressMiddleware` → ... → `SessionMiddleware`) |

### HTTP Request vs Jetris Request

```
HTTP:
POST /auth/login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=gabriel&password=secret

Jetris:
LOGIN 1 42 gabriel secret
```

### HTTP Response vs Jetris Response

```
HTTP:
HTTP/1.1 200 OK
Content-Type: application/json

{"token":"550e84...","pfp":"iVBORw0..."}

Jetris:
LOGIN 1 42 SUCCESS 550e84... iVBORw0...
```

The Jetris response echoes the same `TYPE` and `code` fields back (just like HTTP echoes the same verb context in the response body), and uses the `callbackCode` to identify which pending request this response belongs to.

---

## Request/Response Correlation

Because Jetris uses a single persistent TCP connection for all operations, multiple requests can be outstanding simultaneously. The `callbackCode` field solves this:

```
Client side:
  callbackCode = atomicInteger.incrementAndGet()   // e.g. 42
  mapCallbacks.put(42, callback)
  tcp.send("GETPROFILE 1 42 token gabriel")

Server side:
  // processes request...
  send("GETPROFILE 1 42 gabriel iVBOR... 15 3 10 2 1 5 ...")
                     ^^
                     echoed callbackCode

Client's DispatcherTCP:
  packet = packetQueueTCP.take()
  callbackCode = packet.callbackCode  // 42
  callback = mapCallbacks.remove(42)
  callback.onSuccess(data)
```

This is structurally identical to how an HTTP/2 stream ID works, or how a request ID header is used in microservices for async correlation — but implemented with three lines of Java.

---

## Push Notification Model

The server can send messages to clients at any time without a corresponding client request. These **push messages** always use `callbackCode = 0`.

```
Server -> Client:  MATCH_STATE 0 0 a1b2c3d4 IN_PROGRESS 1|0|0|...
Server -> Client:  MATCH_COUNTDOWN 0 0 a1b2c3d4 3
Server -> Client:  CHAT 0 0 a1b2c3d4 12345 gabriel Hello!
Server -> Client:  MATCHRESULT 0 0 SUCCESS a1b2c3d4|WIN|Topout|1719000000000|1719003600000
```

Because `callbackCode = 0` is never placed in `mapCallbacks` (the client only stores codes it generated, starting from 1), the `DispatcherTCP` handles these packets through a separate path: it checks the packet type, finds the registered listener in `NetworkContext`, and calls it.

This creates two distinct message flows in the same TCP stream:

```
+-----------+---------------------+-----------------------------------------+
| Direction | callbackCode        | Routing in client DispatcherTCP         |
+-----------+---------------------+-----------------------------------------+
| Response  | > 0 (echoed)        | mapCallbacks.remove(code).onSuccess/Fail|
| Push      | 0                   | matchEventListener / matchStateListener |
|           |                     | chatListener / matchResultListener      |
+-----------+---------------------+-----------------------------------------+
```

---

## Middleware as HTTP Middleware

HTTP frameworks (Express, Spring, Django) use middleware stacks where each layer can inspect or reject a request before it reaches the handler. The Jetris server implements exactly this pattern using a chain-of-responsibility:

```java
private static final Middleware chain = Middleware.link(
    new AddressMiddleware(),       // IP blacklist check
    new RateLimitMiddleware(200),  // 200ms per client for TCP
    new AuthMiddleware(),          // token validation
    new SessionMiddleware()        // active session check
);
```

Compare to Express.js:

```javascript
app.use(checkIpBlacklist);
app.use(rateLimit({ windowMs: 200 }));
app.use(verifyJwt);
app.use(requireSession);
```

Each `Middleware.check()` call either:
- Returns `false` (drops the packet — analogous to `res.status(403).end()`)
- Calls `checkNext(...)` to pass control down the chain (analogous to `next()`)

The `LOGIN` and `REGISTER` routes bypass `AuthMiddleware` and `SessionMiddleware` (analogous to `app.use('/auth', publicRouter)`).

---

## Full Annotated Examples

### Annotated Login (credential flow)

```
# Client sends:
LOGIN 1 42 gabriel secret
^     ^ ^  ^^^^^^^^^^^^^^^
|     | |  body = "gabriel secret"
|     | callbackCode = 42 (client-generated correlation ID)
|     code = 1 (LOGIN with credentials, not token)
TYPE = LOGIN

# Server validates credentials with BCrypt, generates UUID token
# Server sends:
LOGIN 1 42 SUCCESS 550e8400-e29b-41d4-a716-446655440000 iVBORw0KGgoAAAANS...
^     ^ ^  ^^^^^^^                                        ^^^^^^^^^^^^^^^^^^^
|     | |  |  body starts with "SUCCESS" = status word   base64-encoded PNG avatar
|     | |  token UUID
|     | echoed callbackCode = 42
|     code = 1
TYPE = LOGIN
```

### Annotated Game State Push

```
# Server pushes during gravity tick (no client request):
MATCH_STATE 0 0 a1b2c3d4 IN_PROGRESS 1|0|0|100|0|..........1.........|..........| 2|3|0|0
^           ^ ^  ^^^^^^^^ ^^^^^^^^^^^  ^^^^^^^^^ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
|           | |  |        |            |          payload = "1|0|0|100|0|board1|board2|..."
|           | |  |        state        round|w1|w2|score1|score2|...
|           | |  matchCode
|           | callbackCode = 0 → PUSH (no callback to invoke)
|           code = 0
TYPE = MATCH_STATE
```

### Annotated UDP Move

```
# Client sends over UDP (no response expected):
MOVE 0 0 550e8400-e29b-41d4-a716-446655440000 a1b2c3d4 LEFT
^    ^ ^  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  ^^^^^^^^  ^^^^
|    | |  token                                 matchCode action
|    | callbackCode = 0 (UDP moves are fire-and-forget)
|    code = 0
TYPE = MOVE
```

UDP uses the same format but has no persistent connection and no response. The `callbackCode` is always 0 because moves are fire-and-forget: the server processes the move and broadcasts an updated `MATCH_STATE` to all participants, which is how the client learns the move was applied.

---

## Special Cases

### PING / PONG

The keepalive exchange is the only exception to the standard format:

```
Client -> Server:  PING
Server -> Client:  PONG
```

These are plain strings, not `TYPE code callbackCode body`. Both parsers detect this before trying to split:

```java
if (cleanData.equalsIgnoreCase("PING")) {
    TCPConnectionManager.send(clientIp, "PONG");
    return;
}
```

### Duplicate Match Starts

When two players join simultaneously, the second player's response carries their actual `callbackCode`, while the push to the first player uses `callbackCode = 0`:

```
Server -> Player1:  MATCH 0 0 SUCCESS START a1b2c3d4      # push (callbackCode=0)
Server -> Player2:  MATCH 0 11 SUCCESS START a1b2c3d4     # response (callbackCode=11)
```

Both players receive the same information but through different routing paths on the client.

---

## Protocol Extensibility

Adding a new operation requires only:

1. Pick a `TYPE` string and an unused `code` integer.
2. Define the `body` format.
3. Add a `case` in `TCPPacketParser.parse()` on the server.
4. Add a `case` in `PacketParserTCP.run()` on the client.
5. Create a `Packet` subclass if the new message has typed fields.

No schema files, no code generation, no breaking changes to existing messages.
