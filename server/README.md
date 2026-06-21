# Jetris — Server

Headless Java 21 server. Runs a TCP listener for control messages and a UDP listener for game inputs. All match logic executes server-side; the server broadcasts board state to players and spectators every time it changes.

Every incoming TCP connection and every UDP datagram gets its own Java 21 virtual thread. Packets pass through a four-step middleware chain (address block → rate limit → auth → session) before reaching the service layer.

---

## How to Run

Create `src/main/resources/config/.env`:

```env
DB_HOST=jdbc:mysql://localhost:3306/jetris
DB_USER=root
DB_PASSWORD=secret
TCP_PORT=9090
UDP_PORT=9091
```

```bash
mvn exec:java
# or build a fat JAR:
mvn package && java -jar target/libs/JetrisServer.jar
```

---

## Class Diagram

```mermaid
classDiagram
    class ServerMain
    class NetworkManager
    class TCPServer
    class UDPServer
    class GameLoopManager {
        -16ms tick scheduler
    }

    class TCPPacketParser
    class UDPPacketParser
    class Middleware {
        <<abstract>>
    }
    class AddressMiddleware
    class RateLimitMiddleware
    class AuthMiddleware
    class SessionMiddleware

    class LoginService
    class RegisterService
    class LogoutService
    class ProfileService
    class MatchmakingService
    class SpectateService

    class MatchManager {
        -queue
        -activeMatches
        -pendingPrivateMatches
        -finishedMatches
        -pendingDisconnects
    }
    class MatchSession {
        -matchCode
        -player1
        -player2
        -state
        -spectators
        -game
    }
    class TetrisGame {
        -board 10x20
        -gravity 650ms
        -pieces I O T S Z J L
    }
    class MatchState {
        <<enumeration>>
        WAITING_FOR_PLAYERS
        STARTING
        IN_PROGRESS
        FINISHED
    }

    class SessionManager
    class TCPConnectionManager
    class DatabaseManager

    ServerMain --> NetworkManager
    ServerMain --> DatabaseManager
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
    TCPPacketParser --> LoginService
    TCPPacketParser --> RegisterService
    TCPPacketParser --> LogoutService
    TCPPacketParser --> ProfileService
    TCPPacketParser --> MatchmakingService
    TCPPacketParser --> SpectateService
    MatchmakingService --> MatchManager
    SpectateService --> MatchManager
    MatchManager --> MatchSession
    MatchSession --> TetrisGame
    MatchSession --> MatchState
    GameLoopManager --> MatchSession
    MatchManager --> SessionManager
    MatchManager --> TCPConnectionManager
    MatchManager --> DatabaseManager
```
