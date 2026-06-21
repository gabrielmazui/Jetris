# Jetris

Real-time multiplayer Tetris in Java 21 + JavaFX 21. Two players compete in a best-of-5 format over a TCP + UDP dual-protocol network, with live spectating, private rooms, in-match chat, and user profiles.

---

## Features

- 1v1 real-time Tetris — best-of-5 rounds (first to 3 wins), 5-second countdown between rounds
- Next-piece preview for both players (visible to each player and spectators)
- Public matchmaking queue and private rooms (6-character code)
- Live spectating — any ongoing match, full board view, shared chat
- Garbage line system — clearing lines sends garbage rows to the opponent
- DAS/ARR input — 100 ms delay, 33 ms repeat rate
- Disconnect grace period — 30 seconds before forfeit
- User profiles — PNG avatar (up to 5 MB), win/loss record, paginated match history

---

## Architecture

```
Client (JavaFX)                     Server (headless Java 21)
──────────────────────              ──────────────────────────
LoginScreen                         TCPServer :9090
MainScreen         TCP ──────────▶  UDPServer :9091
GameScreen      ◀─────              Middleware chain (address/rate/auth/session)
SpectatorScreen    UDP ──────────▶  Services (login, matchmaking, spectate, profile)
                ◀─────              MatchManager → MatchSession → TetrisGame
                                    GameLoopManager (16 ms tick)
                                    DatabaseManager (MySQL)
```

- **TCP** carries authentication, control, and game state updates (board, scores, chat).
- **UDP** carries real-time player inputs (LEFT / RIGHT / DOWN / ROTATE / DROP).
- The server is the single source of truth — it applies every input and broadcasts the full board state to players and spectators on every change.

---

## Libraries

### Server (`server/pom.xml`)

| Library | Version | Purpose |
|---|---|---|
| MySQL Connector/J | 9.7.0 | JDBC driver for MySQL 8 |
| jBCrypt | 0.4 | BCrypt password hashing |
| dotenv-java | 3.2.0 | `.env` config file loading |
| JNA Platform | 5.14.0 | Native OS integration |

### Client (`client/pom.xml`)

| Library | Version | Purpose |
|---|---|---|
| JavaFX Controls | 21.0.4 | UI controls (Label, Button, GridPane…) |
| JavaFX FXML | 21.0.4 | JavaFX runtime support |
| dotenv-java | 3.2.0 | `.env` config file loading |
| JNA Platform | 5.14.0 | Windows dark mode titlebar |

---

## Quick Start

### Prerequisites

- Java 21+, Maven 3.8+, MySQL 8.x

### 1. Database setup

```sql
CREATE DATABASE jetris;
USE jetris;

CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(25) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    pfp           LONGBLOB,
    matches_won   INT DEFAULT 0,
    matches_lost  INT DEFAULT 0
);

CREATE TABLE match_history (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    user1_id         INT NOT NULL,
    user2_id         INT NOT NULL,
    duration_seconds INT DEFAULT 0,
    score_user1      INT DEFAULT 0,
    score_user2      INT DEFAULT 0,
    winner_id        INT NOT NULL,
    match_date       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 2. Configure and run the server

Create `server/src/main/resources/config/.env`:

```env
DB_HOST=jdbc:mysql://localhost:3306/jetris
DB_USER=root
DB_PASSWORD=secret
TCP_PORT=9090
UDP_PORT=9091
```

```bash
cd server
mvn exec:java
```

The server exits immediately if the database is unreachable. Check the `.env` values if you see a connection error on startup.

To build a standalone fat JAR:

```bash
mvn package
java -jar target/libs/JetrisServer.jar
```

### 3. Configure and run the client

Create `client/src/main/resources/config/.env`:

```env
SERVER_HOST=127.0.0.1
SERVER_PORT_TCP=9090
SERVER_PORT_UDP=9091
```

```bash
cd client
mvn javafx:run
```

> **Note:** Use `mvn javafx:run` (not `mvn exec:java`) for the client — the JavaFX Maven plugin correctly sets up the module path for JavaFX 21.

To build a native Windows package:

```bash
mvn package
# Output: target/dist/JetrisOnline/
```

---

## Server overview

The server runs three long-lived threads: a TCP listener, a UDP listener, and a game loop scheduler. Every client connection runs in its own Java 21 virtual thread, which keeps the code simple without thread-pool limits.

Each incoming packet passes through four middleware steps before reaching the service layer: IP block check, rate limiting (TCP 200 ms / UDP 50 ms), token authentication, and session validation. LOGIN and REGISTER bypass auth; PING bypasses session.

Match logic lives entirely in `TetrisGame`. Gravity ticks every 650 ms. When a player locks a piece, cleared lines are counted and `max(0, lines - 1)` garbage rows are sent to the opponent. Rotation uses wall-kick offsets `[0, -1, 1, -2, 2]`; the O-piece skips rotation. A match runs until one player wins 3 rounds or 5 rounds are played.

---

## Client overview

The client is a pure JavaFX application with no FXML — all layouts are built programmatically. Three virtual threads handle networking in the background (TCP reader, packet parser, packet dispatcher) and communicate through `LinkedBlockingQueue` instances so the JavaFX thread is never blocked.

Server-pushed packets (board updates, chat, countdown, match result) are routed to volatile listener slots on `NetworkContext`. The active screen registers itself as a listener on construction and clears it when navigating away, so only one screen listens at a time.

Input handling uses client-side DAS/ARR (100 ms / 33 ms) implemented with JavaFX `Timeline`. Key presses send UDP datagrams (`MOVE 0 0 <token> <matchCode> <action>`); the server applies the move and broadcasts the updated board back to all participants.

---

## Documentation

| File | Description |
|---|---|
| [server/README.md](server/README.md) | Server architecture and class diagram |
| [client/README.md](client/README.md) | Client architecture and class diagram |
| [docs/PROTOCOL.md](docs/PROTOCOL.md) | Full network protocol reference |
