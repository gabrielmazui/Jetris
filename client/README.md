# Jetris — Client

JavaFX 21 desktop application. Connects to the server over TCP (control/state) and UDP (game input). All game logic runs on the server — the client renders board state as it arrives and sends key actions over UDP.

Three virtual threads run in the background: a TCP reader, a packet parser, and a dispatcher. The dispatcher routes parsed packets to either a `NetworkCallback` (for request/response flows) or a push listener on `NetworkContext` (for server-initiated packets like board updates and chat).

---

## How to Run

Create `src/main/resources/config/.env`:

```env
SERVER_HOST=127.0.0.1
SERVER_PORT_TCP=9090
SERVER_PORT_UDP=9091
```

```bash
mvn exec:java
# or build a native package (Windows):
mvn package
```

---

## Class Diagram

```mermaid
classDiagram
    class ClientMain
    class ScreenManager {
        -stage
        -scene
    }
    class NetworkManager
    class NetworkContext {
        -rawQueueTCP
        -packetQueueTCP
        -mapCallbacks
        -matchEventListener
        -matchStateListener
        -chatListener
        -matchResultListener
    }
    class TCPClient
    class UDPClient
    class PacketParserTCP
    class DispatcherTCP
    class NetworkCallback

    class UserSession {
        -token
        -username
        -pfpBytes
    }

    class LoadingScreen
    class LoginScreen
    class MainScreen
    class GameScreen {
        -matchCode
        -spectatorMode
        -mainBoardCells
        -opponentBoardCells
        -DAS 100ms / ARR 33ms
    }
    class SpectatorScreen
    class MatchResultScreen
    class ProfileScreen
    class UserSearchScreen
    class SettingsScreen

    class LoginService
    class MatchMakingService
    class MatchListService
    class SpectateMatchService
    class MatchChatService

    ClientMain --> ScreenManager
    ClientMain --> NetworkManager
    NetworkManager --> TCPClient
    NetworkManager --> UDPClient
    NetworkManager --> PacketParserTCP
    NetworkManager --> DispatcherTCP
    NetworkManager --> NetworkContext
    PacketParserTCP --> NetworkContext
    DispatcherTCP --> NetworkContext
    DispatcherTCP --> NetworkCallback
    ScreenManager --> LoadingScreen
    ScreenManager --> LoginScreen
    ScreenManager --> MainScreen
    ScreenManager --> GameScreen
    ScreenManager --> MatchResultScreen
    ScreenManager --> ProfileScreen
    SpectatorScreen --|> GameScreen
    GameScreen --> MatchMakingService
    GameScreen --> MatchListService
    GameScreen --> MatchChatService
    MainScreen --> MatchMakingService
    MainScreen --> MatchListService
    MainScreen --> SpectateMatchService
    LoginScreen --> LoginService
    LoginService --> NetworkManager
    MatchMakingService --> NetworkManager
    MatchListService --> NetworkManager
    SpectateMatchService --> NetworkManager
    GameScreen --> NetworkManager
    GameScreen --> UserSession
    MainScreen --> UserSession
```
