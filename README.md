# ChronoArena
Networks Game Project

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

