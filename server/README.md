# Jetris — Server

The Jetris server is a headless Java 21 application that manages authentication, matchmaking, real-time Tetris game logic, spectating, and user profiles. It runs two network listeners in parallel: a persistent TCP server for control messages and a stateless UDP server for high-frequency game input.

---

## How to Run

### Prerequisites

- Java 21+
- Maven 3.8+
- MySQL 8.x

### Configuration

Create `src/main/resources/config/.env`:

```env
DB_HOST=jdbc:mysql://localhost:3306/jetris
DB_USER=root
DB_PASSWORD=secret
TCP_PORT=9090
UDP_PORT=9091
```

### Run

```bash
mvn exec:java
```

### Build Fat JAR

```bash
mvn package
java -jar target/libs/JetrisServer.jar
```

---

## Architecture Overview

```
ServerMain
  ├── DatabaseManager.connect()     — fail-fast: exits if DB unreachable
  └── NetworkManager.init()
        ├── TCPServer (virtual thread)
        ├── UDPServer (virtual thread)
        └── GameLoopManager.start()   — 16 ms scheduled heartbeat
```

Every incoming TCP connection gets its own Java 21 virtual thread (`TCPClientHandler`). Every UDP datagram also gets its own virtual thread (`UDPClientHandler`). Virtual threads are extremely cheap so the server can handle many concurrent connections with no thread-pool bottleneck.

---

## Middleware Chain

Both the TCP and UDP parsers run every packet through an identical chain-of-responsibility before it reaches business logic.

```
Incoming packet
      |
      v
AddressMiddleware        — reject blacklisted IPs
      |
      v
RateLimitMiddleware      — reject if < minInterval since last packet
      |  (TCP: 200 ms,  UDP: 50 ms)
      v
AuthMiddleware           — reject if token not in SessionManager
      |  (LOGIN and REGISTER bypass this step)
      v
SessionMiddleware        — reject if no active IP session
      |  (LOGIN, REGISTER, PING bypass)
      v
Packet Parser / Handler
```

```mermaid
flowchart LR
    P([Packet]) --> A[AddressMiddleware]
    A -->|allowed| R[RateLimitMiddleware]
    A -->|blocked| X1([DROP])
    R -->|ok| Au[AuthMiddleware]
    R -->|too fast| X2([DROP])
    Au -->|valid token| S[SessionMiddleware]
    Au -->|no token\nLOGIN/REGISTER bypass| S
    S -->|session valid| H([Handler])
    S -->|no session| X3([DROP])
```

The chain is implemented as an abstract `Middleware` class with a linked-list of `next` nodes. Each node calls `checkNext(...)` to continue, or returns `false` to abort.

---

## Services Layer

| Service | Responsibilities |
|---|---|
| `LoginService` | Validates credentials with BCrypt, generates UUID token, registers session, sends avatar |
| `RegisterService` | Validates input format, checks uniqueness, BCrypt-hashes password, inserts user |
| `LogoutService` | Removes session, triggers immediate forfeit if in match |
| `DeleteService` | Deletes user row, cascades to match history |
| `ProfileService` | Username search (prefix), paginated profile + match history, avatar upload (PNG, max 5 MB) |
| `MatchmakingService` | Routes MATCH 0-8 codes to `MatchManager` |
| `SpectateService` | Routes SPECTATE codes to `SpectateManager` |

---

## Match Lifecycle

```mermaid
sequenceDiagram
    participant P1 as Player 1
    participant P2 as Player 2
    participant MM as MatchManager
    participant MS as MatchSession
    participant TG as TetrisGame
    participant GL as GameLoopManager
    participant DB as DatabaseManager

    P1->>MM: MATCH 0 (join queue)
    MM-->>P1: MATCH 0 SUCCESS QUEUED

    P2->>MM: MATCH 0 (join queue)
    MM->>MS: new MatchSession(matchCode, p1)
    MM->>MS: setPlayer2(p2)
    MM->>MS: startMatch()
    MS->>TG: startFirstRound()
    TG->>MS: setState(STARTING)

    MM-->>P1: MATCH 0 0 SUCCESS START matchCode
    MM-->>P2: MATCH 0 callbackCode SUCCESS START matchCode

    loop Every 16 ms
        GL->>MS: tick()
        MS->>TG: tick()
        TG->>MS: broadcastToParticipants(MATCH_COUNTDOWN / MATCH_STATE)
        MS-->>P1: MATCH_COUNTDOWN 0 0 matchCode secondsLeft
        MS-->>P2: MATCH_COUNTDOWN 0 0 matchCode secondsLeft
    end

    TG->>MS: setState(IN_PROGRESS)
    MS-->>P1: MATCH_STATE 0 0 matchCode IN_PROGRESS payload
    MS-->>P2: MATCH_STATE 0 0 matchCode IN_PROGRESS payload

    loop Game Input (UDP)
        P1->>TG: MOVE 0 0 token matchCode LEFT/RIGHT/DOWN/ROTATE/DROP
        TG->>MS: broadcastToParticipants(MATCH_STATE IN_PROGRESS)
    end

    TG->>MS: endRound(winnerId, loserId, reason)
    MS-->>P1: MATCH_STATE 0 0 matchCode ROUND_END round|winner|reason|w1|w2
    MS-->>P2: MATCH_STATE 0 0 matchCode ROUND_END round|winner|reason|w1|w2

    Note over TG: If winner.wins >= 3 or round >= 5
    TG->>MM: notifyMatchResult(matchCode, winnerId, loserId, reason)
    MM->>DB: saveMatchResult(...)
    MM-->>P1: MATCHRESULT 0 0 SUCCESS matchCode|WIN|reason|startMs|endMs
    MM-->>P2: MATCHRESULT 0 0 SUCCESS matchCode|LOSE|reason|startMs|endMs
    MM->>MS: stopMatch()
```

### Match States

```mermaid
stateDiagram-v2
    [*] --> WAITING_FOR_PLAYERS : MatchSession created
    WAITING_FOR_PLAYERS --> STARTING : second player joins
    STARTING --> IN_PROGRESS : countdown reaches 0
    IN_PROGRESS --> STARTING : round ends, more rounds to play
    IN_PROGRESS --> FINISHED : winner reached 3 round wins
    STARTING --> FINISHED : match aborted / forfeit
    IN_PROGRESS --> FINISHED : match aborted / forfeit
    FINISHED --> [*]
```

---

## Game Loop

`GameLoopManager` runs a single `ScheduledExecutorService` heartbeat at 16 ms intervals (approximately 62 ticks per second). For up to 10 concurrent matches, all sessions are ticked inline on the heartbeat thread. For more than 10 matches, ticks are parallelized across a fixed thread pool sized to the number of CPU cores.

Inside each `TetrisGame.tick()`:

- **STARTING state** — broadcasts `MATCH_COUNTDOWN` every tick until the 5-second countdown expires, then transitions to `IN_PROGRESS`.
- **IN_PROGRESS state** — applies gravity every 650 ms (GRAVITY_NANOS). On each gravity step: attempts to move the piece down; if it cannot move, locks the piece, clears full lines, sends garbage to the opponent, and spawns the next piece. If spawning fails, the player tops out and the round ends.

---

## TetrisGame Engine

| Constant | Value |
|---|---|
| Board width | 10 columns |
| Board height | 20 rows |
| Gravity interval | 650 ms |
| Round countdown | 5 seconds |
| Pieces | I, O, T, S, Z, J, L (indices 0-6) |
| Garbage formula | `max(0, linesCleared - 1)` rows sent to opponent |

Rotation uses a simple wall-kick with offsets `[0, -1, 1, -2, 2]`. The board is encoded as a flat 200-character string (`'.' = empty`, `'1'-'7' = piece type`, `'8' = garbage`) for efficient transmission.

---

## Disconnect Grace Period

When a TCP connection drops, `SessionManager.removeSession()` calls `MatchManager.handleUserDeparture()`, which schedules a 30-second forfeit timer via a `ScheduledExecutorService`. If the player reconnects within 30 seconds and re-authenticates, `handleUserReconnected()` cancels the timer. If the timer fires and the player is still offline, the opponent wins by `Player_timeout`.

---

## Main Class Diagram

```mermaid
classDiagram
    class ServerMain {
        +main(args)
    }

    class NetworkManager {
        +init()
    }

    class TCPServer {
        -running: boolean
        +start()
        +stop()
    }

    class UDPServer {
        -running: boolean
        -MAX_PACKET_SIZE: int
        +start()
        +stop()
    }

    class TCPPacketParser {
        -chain: Middleware
        +parse(rawData, clientIp)
    }

    class UDPPacketParser {
        -chain: Middleware
        +parse(rawData, clientIp, port)
    }

    class Middleware {
        <<abstract>>
        -next: Middleware
        +link(first, chain)$
        +check(type, code, cbCode, body, ip) bool
        #checkNext(type, code, cbCode, body, ip) bool
    }

    class AddressMiddleware
    class RateLimitMiddleware {
        -ipTimestamps: ConcurrentHashMap
        -minMillis: long
    }
    class AuthMiddleware
    class SessionMiddleware

    class MatchManager {
        -queue: ConcurrentLinkedQueue
        -activeMatches: ConcurrentHashMap
        -pendingPrivateMatches: ConcurrentHashMap
        -finishedMatches: ConcurrentHashMap
        -pendingDisconnects: ConcurrentHashMap
        +joinQueue(userId, ip, cb)$
        +leaveQueue(userId, ip, cb)$
        +createPrivateMatch(userId, ip, cb)$
        +joinPrivateMatch(userId, code, ip, cb)$
        +cancelPrivateMatch(userId, code, ip, cb)$
        +leaveActiveMatch(userId, ip, cb)$
        +handleUserDeparture(userId, ip)$
        +notifyMatchResult(code, winner, loser, reason)$
        +endMatch(code)$
        +buildMatchListPayload(query) String$
        +buildMatchInfoPayload(code) String$
    }

    class MatchSession {
        -matchCode: String
        -player1: Integer
        -player2: Integer
        -state: MatchState
        -spectators: Set~Integer~
        -game: TetrisGame
        +startMatch()
        +stopMatch()
        +forfeit(loserId, reason)
        +tick()
        +handleInput(userId, action)
        +broadcastToParticipants(packet)
    }

    class TetrisGame {
        -WIDTH: int = 10
        -HEIGHT: int = 20
        -GRAVITY_NANOS: long = 650ms
        -player1: PlayerState
        -player2: PlayerState
        +startFirstRound()
        +tick()
        +handleInput(userId, action)
        -stepGravity(self, opponent)
        -clearLines(state) int
        -applyPendingGarbage(state)
        -endRound(winnerId, loserId, reason)
    }

    class GameLoopManager {
        -TICK_RATE_MS: int = 16
        -heartbeat: ScheduledExecutorService
        -workers: ExecutorService
        +start()$
        +shutdown()$
    }

    class SpectateManager {
        +joinSpectate(userId, matchCode, ip, cb)$
        +handleChat(senderId, matchCode, msg, ip)$
    }

    class SessionManager {
        -sessionsByUserId: ConcurrentHashMap
        -ipToUserId: ConcurrentHashMap
        -tokenToUserId: ConcurrentHashMap
        +registerSession(ip, userId)$
        +removeSession(ip)$
        +generateToken(userId) String$
        +getUserIdByToken(token) Integer$
        +getUserId(ip) Integer$
        +getIpByUserId(userId) String$
        +logout(userId)$
    }

    class DatabaseManager {
        -connection: Connection
        +connect() bool$
        +createUser(username, password) bool$
        +validateCredentials(username, password) int$
        +getUserStats(userId) int[]$
        +saveMatchResult(u1, u2, duration, s1, s2, winner)$
        +getUserMatchHistoryPaged(userId, page, size) List~Match~$
        +updateUserPfp(userId, bytes) bool$
        +searchUsernamesByPrefix(prefix) List~String[]~$
    }

    class MatchState {
        <<enumeration>>
        WAITING_FOR_PLAYERS
        STARTING
        IN_PROGRESS
        FINISHED
    }

    ServerMain --> NetworkManager
    NetworkManager --> TCPServer
    NetworkManager --> UDPServer
    NetworkManager --> GameLoopManager
    TCPServer --> TCPPacketParser
    UDPServer --> UDPPacketParser
    TCPPacketParser --> Middleware
    UDPPacketParser --> Middleware
    Middleware <|-- AddressMiddleware
    Middleware <|-- RateLimitMiddleware
    Middleware <|-- AuthMiddleware
    Middleware <|-- SessionMiddleware
    TCPPacketParser --> MatchManager
    TCPPacketParser --> SessionManager
    UDPPacketParser --> MatchManager
    GameLoopManager --> MatchSession
    MatchManager --> MatchSession
    MatchSession --> TetrisGame
    MatchSession --> MatchState
    MatchManager --> DatabaseManager
    MatchManager --> SessionManager
    SpectateManager --> MatchManager
    SpectateManager --> SessionManager
```

---

## Database Schema

### `users`

| Column | Type | Notes |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | |
| `username` | VARCHAR(25) UNIQUE | 5-25 chars, alphanumeric + underscore |
| `password` | VARCHAR(255) | BCrypt hash |
| `pfp` | LONGBLOB | PNG bytes, max 5 MB |
| `matches_won` | INT DEFAULT 0 | Incremented atomically on match result |
| `matches_lost` | INT DEFAULT 0 | Incremented atomically on match result |

### `match_history`

| Column | Type | Notes |
|---|---|---|
| `id` | INT PK AUTO_INCREMENT | |
| `user1_id` | INT FK → users.id | CASCADE DELETE |
| `user2_id` | INT FK → users.id | CASCADE DELETE |
| `duration_seconds` | INT | Wall-clock duration |
| `score_user1` | INT | Reserved (currently 0) |
| `score_user2` | INT | Reserved (currently 0) |
| `winner_id` | INT | References users.id |
| `match_date` | TIMESTAMP DEFAULT NOW() | |

Match results are written inside a transaction that also increments `matches_won` for the winner and `matches_lost` for the loser atomically.

---

## Environment Variables

| Variable | Example | Description |
|---|---|---|
| `DB_HOST` | `jdbc:mysql://localhost:3306/jetris` | Full JDBC URL |
| `DB_USER` | `root` | MySQL user |
| `DB_PASSWORD` | `secret` | MySQL password |
| `TCP_PORT` | `9090` | TCP listen port |
| `UDP_PORT` | `9091` | UDP listen port |
