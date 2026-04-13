# ChronoArena
Networks Game Project

## How to Play

ChronoArena is a real-time multiplayer arena game where players compete to capture zones and collect items on a shared map. The objective is to score the most points before the match ends.

### Game Mechanics

- **Players**: Move your character around the map using keyboard controls.
- **Zones**: Capture colored zones by standing in them. Captured zones award points over time (5 points per second).
- **Items**:
  - **Energy Items**: Collect for 10 points each. They spawn randomly on the map.
  - **Freeze Items**: Pick up to gain freeze attack ability. Freeze opponents within range for 2 seconds (cooldown: 5 seconds).
- **Match Duration**: 3 minutes (180 seconds), but players can vote to extend or shorten.
- **Winning**: The player with the highest score at the end wins.

### Controls

- **Movement**: Arrow keys (Up, Down, Left, Right) or WASD.
- **Freeze Attack**: Spacebar (when you have a freeze item).
- **Stop**: Release movement keys to stop.

### Match Phases

1. **Lobby**: Players join and wait. Match starts when at least 2 players are connected after 10 seconds.
2. **Running**: Active gameplay. Capture zones, collect items, use abilities.
3. **Ending**: Match concludes, scores are displayed.
4. **Waiting**: Between matches, players can vote for next match settings.

### Multiplayer Setup

- One player runs the server.
- Other players run clients and connect to the server.
- Supports multiple clients connecting to one server.

### Tips

- Stay in zones to build points passively.
- Use freeze strategically to disrupt opponents.
- Collect energy items for quick score boosts.
- Coordinate with teammates if playing in groups.

---

## Running the Game

To run the game locally:

```bash
chmod +x server_build.sh client_build.sh
./server_build.sh
```

```
chmod +x client_build.sh
./client_build.sh
```

---

## Run using JAR files (Recommended)

If you have the JAR files, no setup or compilation is needed.

Start the server:

```bash
java -jar ChronoArenaServer.jar
```

In a second terminal, start the client:

```bash
java -jar ChronoArenaClient.jar
```

When the client starts, enter your player name and server details.

---

## Run on macOS / Linux

Open a terminal in the project root.

Start the server:

```bash
chmod +x server_build.sh client_build.sh
./server_build.sh
```

In a second terminal, start the client:

```bash
./client_build.sh
```

## Run on Windows PowerShell

Open PowerShell in the project root.

Start the server:

```powershell
java -cp .\src\server;.\src\common server.ServerMain
```

In a second PowerShell window, start the client:

```powershell
java -cp .\src\client;.\src\common client.ClientMain
```

> Note: the `server_build.sh` and `client_build.sh` scripts on macOS/Linux compile the code before launching. The Windows PowerShell `java` commands shown here assume the Java classes have already been compiled. If you run on Windows and see errors, compile first with `javac` or add a Windows build script.

---

## Notes

- Game starts when at least 2 players join
- Players can vote for match duration
- For demo, only these files are needed:
    - ChronoArenaServer.jar
    - ChronoArenaClient.jar  