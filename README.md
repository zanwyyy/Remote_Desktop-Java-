# Remote Desktop Application

This project is a **Java-based Remote Desktop Application** using a **Client-Server** architecture. It allows a server to connect to multiple clients, select specific clients, and remotely view and control their screens. The project handles screen sharing, as well as mouse and keyboard events, making it an efficient tool for remote management and support.

## Features

- **Multiple Client Connections**: The server can connect to multiple clients, displaying each client’s IP address for easy identification.
- **Remote Screen Streaming**: Allows continuous streaming of the client’s screen to the server, controlled by `start` and `stop` commands from the server.
- **Real-time Remote Control**:
  - **Mouse Control**: Server can control the mouse on the client’s side in real-time.
  - **Keyboard Control**: Server can send keyboard inputs to the client.
- **Dynamic Client Management**:
  - The server UI updates to reflect currently connected clients.
  - Automatically removes disconnected clients from the UI, ensuring only active connections are displayed.

## Architecture

- **Client-Server Model**: 
  - The **Server** component connects to clients via sockets and initiates remote control sessions.
  - The **Client** component listens for commands from the server and streams its screen, mouse, and keyboard events back.
- **Sockets**:
  - The main communication socket connects the server and client.
  - Three additional sockets handle **screen**, **mouse**, and **keyboard** data to allow synchronized control.

## Requirements

- **Java Development Kit (JDK) 8 or higher**
- **IntelliJ IDEA** or another Java IDE
- **Network Configuration**: Ensure both the client and server machines are on the same network or configure port forwarding for remote access.

## Setup and Installation

1. **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/RemoteDesktopApplication.git
    cd RemoteDesktopApplication
    ```

2. **Build the Project**: Open the project in IntelliJ or your preferred IDE and build the project to resolve dependencies.

3. **Run the Server**:
    - Open the `Server` directory.
    - Run the `RemoteDesktopServer.java` file to start the server.

4. **Run the Client**:
    - Open the `Client` directory.
    - Run the `RemoteDesktopClient.java` file on the client machine.
    - The client will attempt to connect to the server based on the predefined server IP.

## Usage

1. **Starting the Server**:
   - The server application provides a simple UI displaying all connected clients by IP address.
   - Click on a client’s IP address to select them, then press "Select Client" to initiate a remote session.

2. **Starting Screen Streaming**:
   - After selecting a client, the server sends a `start` command to initiate screen streaming.
   - The client then continuously sends screen data, which the server displays in real-time.

3. **Controlling Mouse and Keyboard**:
   - Once connected, the server captures mouse and keyboard inputs and sends them to the client, allowing real-time remote control.

4. **Ending a Session**:
   - To stop a session, use the `stop` command, which halts screen streaming and closes the additional sockets for mouse and keyboard control.

## Code Overview

- **Server Side**:
  - `RemoteDesktopServer.java`: Main server class that handles client connections, displays connected clients, and sends control commands.
  - `ClientHandler.java`: Manages individual client sessions and socket communication.

- **Client Side**:
  - `RemoteDesktopClient.java`: Main client class, manages server connection, and listens for commands.
  - **Event Listeners**:
    - **Screen**: Streams screen data to the server on command.
    - **Mouse**: Listens to server commands to move and click the client’s mouse.
    - **Keyboard**: Receives keyboard inputs from the server.

## Important Notes

- **Network Requirements**: Ensure firewall permissions are configured for the specified ports.
- **Security Considerations**: The current version does not include encryption, so use this application only within trusted networks.
- **Error Handling**: Automatic reconnection attempts are implemented for clients that lose connection to the server.

## Future Improvements

- **Encryption** for secure data transmission.
- **Cross-Platform Support** for different OS environments.
- **User Authentication** to restrict access.
- **Video Compression** to improve streaming efficiency.


