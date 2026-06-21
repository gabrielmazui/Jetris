# Jetris

Real-time multiplayer Tetris in Java 21 + JavaFX 21. Two players compete in a best-of-5 format over a TCP + UDP dual-protocol network, with live spectating, private rooms, in-match chat, and user profiles.

---

## Features

- 1v1 real-time Tetris — best-of-5 rounds (first to 3 wins), 5-second countdown between rounds
- Public matchmaking queue and private rooms (6-character code)
- Live spectating — any ongoing match, full board view, shared chat
- Garbage line system — clearing lines sends garbage rows to the opponent
- DAS/ARR input — 100 ms delay, 33 ms repeat rate
- Disconnect grace period — 30 seconds before forfeit
- User profiles — PNG avatar, win/loss record, paginated match history

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

- **TCP** carries authentication, control, and game state updates.
- **UDP** carries real-time player inputs (LEFT / RIGHT / DOWN / ROTATE / DROP).
- The server is the single source of truth — it applies every input and broadcasts the updated board to all participants (players + spectators) on every change.

---

## Quick Start

### Prerequisites

- Java 21+, Maven 3.8+, MySQL 8.x

### Database

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

### Server

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

### Client

Create `client/src/main/resources/config/.env`:

```env
SERVER_HOST=127.0.0.1
SERVER_PORT_TCP=9090
SERVER_PORT_UDP=9091
```

```bash
cd client
mvn exec:java
```

---

## Documentation

| File | Description |
|---|---|
| [server/README.md](server/README.md) | Server architecture and class diagram |
| [client/README.md](client/README.md) | Client architecture and class diagram |
| [docs/PROTOCOL.md](docs/PROTOCOL.md) | Full network protocol reference |
