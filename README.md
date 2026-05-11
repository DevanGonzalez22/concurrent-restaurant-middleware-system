Concurrent Restaurant Middleware System: 
- A concurrent multi-client restaurant ordering middleware system built in Java using TCP sockets and Protocol Buffers.
- This project simulates a real restaurant workflow with multiple connected users operating at the same time as Customers, Servers, Chefs, and Managers. The middleware server manages shared restaurant state, routes structured protocol messages, enforces role-based access control, and coordinates real-time restaurant operations.

Features
- Multi-client TCP server with concurrent session handling
- Protocol Buffers request/response messaging
- Role-based access for Customer, Server, Chef, and Manager users
- Dynamic menu management with live updates
- Order creation, submission, billing, tax, tip, and return workflows
- Kitchen ticket queue and ready-status notifications
- Table assignment with collision prevention
- Reservation management
- Inventory and stock tracking
- Thread-safe shared state using concurrent data structures

Technologies
- Java
- TCP/IP Sockets
- Protocol Buffers
- Multithreading
- ConcurrentHashMap
- Client-server architecture
- Middleware design

Core Files
- `TcpRestaurantServer.java` — concurrent middleware server
- `TcpRestaurantClient.java` — terminal-based client application
- `TcpWire.java` — TCP message framing utility
- `restaurant.proto` — Protocol Buffer message definition

Project Summary
- The system uses a central Java middleware server that accepts multiple TCP client connections. Each client sends structured Protocol Buffer requests over TCP. The server processes the request, validates permissions, updates shared restaurant state, and returns a structured response.
- The project demonstrates distributed systems concepts including concurrency, synchronization, application-layer protocol design, role-based access control, shared-state management, and real-time event coordination.
