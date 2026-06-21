# Jetris — Client

The Jetris client is a JavaFX 21 desktop application. It connects to the Jetris server over TCP (control) and UDP (game input), renders the Tetris boards in real time, and manages user authentication, matchmaking, spectating, chat, and profiles through animated screens.

---

## How to Run

### Prerequisites

- Java 21+
- Maven 3.8+

### Configuration

Create `src/main/resources/config/.env` (resolved from the working directory at runtime under `/config`):

```env
SERVER_HOST=127.0.0.1
SERVER_PORT_TCP=9090
SERVER_PORT_UDP=9091
```

### Run

```bash
mvn exec:java
# or with the JavaFX Maven plugin:
mvn javafx:run
```

### Build Native Package (Windows)

```bash
mvn package
# Produces target/dist/JetrisOnline/
```

---

## Screen Flow

```mermaid
stateDiagram-v2
    [*] --> LoadingScreen : app start
    LoadingScreen --> LoginScreen : TCP + UDP connected
    LoginScreen --> MainScreen : login or register success
    MainScreen --> GameScreen : quick match or private match starts
    MainScreen --> SpectatorScreen : spectate button clicked
    MainScreen --> ProfileScreen : profile menu item
    MainScreen --> UserSearchScreen : Users button
    MainScreen --> SettingsScreen : settings menu item
    GameScreen --> MatchResultScreen : MATCHRESULT received
    GameScreen --> MainScreen : leave match / forfeit
    GameScreen --> LoadingScreen : connection drop detected
    SpectatorScreen --> MainScreen : leave spectate
    MatchResultScreen --> MainScreen : back button
    ProfileScreen --> MainScreen : back / ESC
    UserSearchScreen --> ProfileScreen : user selected
    SettingsScreen --> MainScreen : back / ESC
    LoadingScreen --> LoadingScreen : reconnect retry loop
```

---

## Architecture Overview

```mermaid
graph TD
    subgraph Entry["Entry"]
        CM["ClientMain\nApplication.launch()"]
        SM["ScreenManager\nSingleton stage manager"]
    end

    subgraph Network["Network Layer (virtual threads)"]
        NM["NetworkManager\nwires all threads"]
        TC["TCPClient\npersistent reader loop + ping"]
        UC["UDPClient\ndatagram sender + reader loop"]
        PP["PacketParserTCP\nrawQueueTCP -> packetQueueTCP"]
        DT["DispatcherTCP\npacketQueueTCP -> callbacks / listeners"]
        NC["NetworkContext\nshared queues, state, listeners"]
    end

    subgraph UI["UI Layer"]
        LS["LoginScreen"]
        MS["MainScreen"]
        GS["GameScreen"]
        SS["SpectatorScreen"]
        MRS["MatchResultScreen"]
        PS["ProfileScreen"]
    end

    subgraph Services["UI Services"]
        SVC_L["LoginService"]
        SVC_MM["MatchMakingService"]
        SVC_ML["MatchListService"]
        SVC_SP["SpectateMatchService"]
        SVC_CH["MatchChatService"]
    end

    subgraph Session["Client State"]
        US["UserSession\ntoken + username + pfp bytes"]
    end

    CM --> SM
    CM --> NM
    NM --> TC
    NM --> UC
    NM --> PP
    NM --> DT
    TC --> NC
    UC --> NC
    PP --> NC
    DT --> NC
    SM --> LS
    LS --> SVC_L
    SM --> MS
    MS --> SVC_MM
    MS --> SVC_ML
    MS --> SVC_SP
    SM --> GS
    GS --> SVC_MM
    GS --> SVC_ML
    GS --> SVC_CH
    SVC_L --> NM
    SVC_MM --> NM
    SVC_ML --> NM
    GS -->|"sendUDP MOVE"| NM
    US --> GS
    US --> MS
```

---

## Network Pipeline Detail

```mermaid
sequenceDiagram
    participant S as Server
    participant TC as TCPClient
    participant RQ as rawQueueTCP
    participant PP as PacketParserTCP
    participant PQ as packetQueueTCP
    participant DT as DispatcherTCP
    participant CB as NetworkCallback / Listener
    participant UI as UI Screen

    S->>TC: "LOGIN 1 42 SUCCESS token pfp"
    TC->>RQ: rawQueueTCP.put(line)
    PP->>RQ: take()
    PP->>PQ: packetQueueTCP.add(loginPacket)
    DT->>PQ: take()
    DT->>CB: mapCallbacks.remove(42).onSuccess("token pfp")
    CB->>UI: Platform.runLater(() -> navigate to MainScreen)
```

The three consumer threads (TCPClient reader, PacketParserTCP, DispatcherTCP) each run in their own virtual thread, connected by `LinkedBlockingQueue` instances. This keeps the JavaFX thread free at all times — all UI updates go through `Platform.runLater()`.

Push packets (those the server sends without a prior client request, such as `MATCH_COUNTDOWN`, `MATCH_STATE`, `MATCH_ABORT`, `MATCHRESULT`, and `CHAT`) are routed by `DispatcherTCP` to volatile listener interfaces stored in `NetworkContext`. The active screen registers itself as a listener on construction and clears the listener on departure.

---

## Key Client Classes

```mermaid
classDiagram
    class ClientMain {
        +start(stage)
        +main(args)
    }

    class ScreenManager {
        +CurrScreen: Screen
        -stage: Stage
        -scene: Scene
        +init(stage, firstScreen)$
        +setScreen(screen)$
        +tornarArrastavel(node)$
        +minimizar()$
        +alternarTelaCheia()$
    }

    class NetworkManager {
        -tcp: TCPClient
        -udp: UDPClient
        +start()$
        +sendTCP(msg, callback)$
        +sendTCP(msg)$
        +sendUDP(msg, callback)$
        +sendUDP(msg)$
        +notifyConnectionDrop()$
        +retryConnection()$
    }

    class NetworkContext {
        +HOST: String
        +PORT_TCP: int
        +PORT_UDP: int
        +ping: int
        +tcpState: ConnectionState
        +udpState: ConnectionState
        +rawQueueTCP: BlockingQueue
        +packetQueueTCP: BlockingQueue
        +mapCallbacks: Map~Integer, NetworkCallback~
        +requestCallbackID: AtomicInteger
        +matchEventListener: MatchEventListener
        +matchStateListener: MatchStateListener
        +chatListener: ChatListener
        +matchResultListener: MatchResultListener
    }

    class PacketParserTCP {
        +run()
        -parseLongSafe(s) long
    }

    class DispatcherTCP {
        +run()
    }

    class NetworkCallback {
        +code: int
        +onSuccess(data)
        +onFailure(reason)
    }

    class UserSession {
        +logged: boolean
        -token: String
        -username: String
        -pfpBytes: byte[]
        +iniciarESalvarSessao(token, username)$
        +setPfp(bytes)$
        +getPfp() byte[]$
        +carregarDoArquivo()$
        +limparSessao()$
        +getToken() String$
        +getUsername() String$
    }

    class GameScreen {
        -matchCode: String
        -spectatorMode: boolean
        -DAS_MS: int = 150
        -ARR_MS: int = 50
        -pressedKeys: Set~KeyCode~
        -dasTimer: Timeline
        -arrTimer: Timeline
        +leaveMatch()
        -onKeyDown(key)
        -onKeyUp(key)
        -startDAS(key)
        -startARR(key)
        -handleGameKey(key)
        -sendGameAction(action)
        -applyProgressPayload(payload)
        -renderBoard(cells, boardStr)
        -wireCountdownListener()
        -wireMatchStateListener()
        -wireChatListener()
        -wireMatchResultListener()
    }

    class MatchMakingService {
        +findMatch(callback)$
        +cancelQueue(callback)$
        +createPrivateMatch(callback)$
        +joinPrivateMatch(code, callback)$
        +cancelPrivateMatch(code, callback)$
        +leaveCurrentMatch(callback)$
        +listenForCountdown(listener)$
        +stopListeningForCountdown()$
    }

    class MatchListService {
        +fetchLiveMatches(query, callback)$
        +fetchMatchInfo(code, callback)$
    }

    ClientMain --> ScreenManager
    ClientMain --> NetworkManager
    NetworkManager --> NetworkContext
    NetworkManager --> PacketParserTCP
    NetworkManager --> DispatcherTCP
    DispatcherTCP --> NetworkCallback
    DispatcherTCP --> NetworkContext
    PacketParserTCP --> NetworkContext
    GameScreen --> NetworkManager
    GameScreen --> UserSession
    GameScreen --> MatchMakingService
    GameScreen --> MatchListService
    MatchMakingService --> NetworkManager
    MatchListService --> NetworkManager
```

---

## DAS / ARR — Input System

Tetris players hold keys to move pieces. Without DAS/ARR, holding a key would fire one move per OS key-repeat interval, which is too slow and inconsistent for competitive play. Jetris implements its own timing entirely in JavaFX:

| Term | Value | Meaning |
|---|---|---|
| DAS (Delayed Auto Shift) | 150 ms | Time from first key press until auto-repeat begins |
| ARR (Auto Repeat Rate) | 50 ms | Interval between repeated moves during auto-repeat |

**Flow:**

```
KeyDown(LEFT / RIGHT / DOWN)
  -> handleGameKey(key)   (immediate first move)
  -> startDAS(key)
       -> after 150 ms: startARR(key)
            -> every 50 ms: handleGameKey(key) while key is held
KeyUp(key)
  -> stopDAS()   (cancels dasTimer and arrTimer)
```

DOWN bypasses DAS and goes directly to ARR (no initial delay for soft drop). ROTATE and DROP (hard drop via SPACE) are single-fire with no repeat.

Each key action calls `sendGameAction(action)` which sends:

```
MOVE 0 0 <token> <matchCode> <action>
```

over UDP.

---

## Listener Model for Push Packets

`NetworkContext` exposes four volatile listener slots:

| Listener Interface | Used By | Triggered By |
|---|---|---|
| `MatchEventListener` | `GameScreen`, `MainScreen` | `MATCH_COUNTDOWN`, `MATCH_ABORT` |
| `MatchStateListener` | `GameScreen`, `SpectatorScreen` | `MATCH_STATE` |
| `ChatListener` | `GameScreen`, `SpectatorScreen` | `CHAT` |
| `MatchResultListener` | `GameScreen` | `MATCHRESULT` |

Screens register their listener on construction and set it to `null` (or replace it) when navigating away. Because each listener is a single volatile reference, only one screen can listen at a time — which is the correct behavior since only one screen is active.

---

## Screens Reference

| Screen | Description |
|---|---|
| `LoadingScreen` | Shown while TCP/UDP connect or reconnect. Auto-advances to `LoginScreen`. |
| `LoginScreen` | Username/password login and new account registration. Animated slide between forms. |
| `MainScreen` | Match lobby: quick match, private match creation/join, live match list with spectate, ping display. |
| `GameScreen` | 10x20 board for the local player + smaller opponent board, chat panel, countdown overlay, DAS/ARR input. Also used as a base for `SpectatorScreen`. |
| `SpectatorScreen` | Extends `GameScreen` in spectator mode — shows both full-size boards side by side, no input. |
| `MatchResultScreen` | Win/loss badge, match code, start/end times, duration, back button. |
| `ProfileScreen` | Avatar, username, win/loss stats, paginated match history. |
| `UserSearchScreen` | Prefix-based user search, click to open profile. |
| `SettingsScreen` | App settings (avatar upload, account deletion). |

---

## Connection State Machine

```mermaid
stateDiagram-v2
    [*] --> CONNECTING : app start
    CONNECTING --> CONNECTED : socket established
    CONNECTED --> CONNECTING : socket dropped / error
    CONNECTING --> CONNECTING : retry loop (back-off)
```

`NetworkManager.downHandlerLoop()` checks TCP and UDP state every second. If either is not `CONNECTED`, it calls `UpdatePing(-1)` on the current screen and, if not already on `LoadingScreen`, calls `notifyConnectionDrop()` to navigate back there so the reconnect loop can re-establish both connections.
