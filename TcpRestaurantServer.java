package io.grpc.examples.helloworld;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TcpRestaurantServer {
    private static final int PORT = 50052;
    private static final double TAX_RATE = 0.08;

    static class MenuItem {
        String name;
        String category;
        double price;
        int stock;
        boolean available;

        MenuItem(String name, String category, double price, int stock) {
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
            this.available = stock > 0;
        }
    }

    static class OrderLine {
        String itemName;
        double price;
        int quantity;

        OrderLine(String itemName, double price, int quantity) {
            this.itemName = itemName;
            this.price = price;
            this.quantity = quantity;
        }

        double total() {
            return price * quantity;
        }
    }

    static class Order {
        String orderId;
        String guestName;
        String orderType;
        String tableNumber;
        String status = "OPEN";
        CopyOnWriteArrayList<OrderLine> lines = new CopyOnWriteArrayList<>();

        Order(String orderId, String guestName, String orderType) {
            this.orderId = orderId;
            this.guestName = guestName;
            this.orderType = orderType;
        }

        double subtotal() {
            double total = 0;
            for (OrderLine line : lines) {
                total += line.total();
            }
            return total;
        }
    }

    static class Ticket {
        String ticketId;
        String orderId;
        String status;
        String summary;

        Ticket(String ticketId, String orderId, String status, String summary) {
            this.ticketId = ticketId;
            this.orderId = orderId;
            this.status = status;
            this.summary = summary;
        }
    }

    static class Reservation {
        String reservationId;
        String customerName;
        String timeSlot;
        String tableNumber;
        int partySize;
        String status = "CONFIRMED";

        Reservation(String reservationId, String customerName, String timeSlot, String tableNumber, int partySize) {
            this.reservationId = reservationId;
            this.customerName = customerName;
            this.timeSlot = timeSlot;
            this.tableNumber = tableNumber;
            this.partySize = partySize;
        }
    }

    static class Session {
        String token;
        RestaurantProto.ProtocolMessage.Role role;
        Queue<String> notifications = new ConcurrentLinkedQueue<>();

        Session(String token, RestaurantProto.ProtocolMessage.Role role) {
            this.token = token;
            this.role = role;
        }
    }

    private static final ConcurrentHashMap<String, MenuItem> MENU = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Order> ORDERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Ticket> TICKETS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> TABLES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Reservation> RESERVATIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private static final AtomicInteger orderCounter = new AtomicInteger(1000);
    private static final AtomicInteger ticketCounter = new AtomicInteger(2000);
    private static final AtomicInteger reservationCounter = new AtomicInteger(3000);

    static {
        addInitialItem("Wings", "starter", 8.50, 40);
        addInitialItem("Nachos", "starter", 9.00, 40);
        addInitialItem("Caesar Salad", "starter", 7.00, 35);
        addInitialItem("Burger", "main", 9.99, 50);
        addInitialItem("Chicken Sandwich", "main", 10.49, 50);
        addInitialItem("Ribeye Steak", "main", 26.00, 25);
        addInitialItem("BBQ Ribs", "main", 21.99, 25);
        addInitialItem("Cheesecake", "dessert", 6.50, 30);
        addInitialItem("Chocolate Cake", "dessert", 6.25, 30);
        addInitialItem("Soda", "drink", 2.50, 100);
        addInitialItem("Water", "drink", 0.00, 100);
        addInitialItem("Coffee", "drink", 3.25, 80);
    }

    private static void addInitialItem(String name, String category, double price, int stock) {
        MENU.put(name.toLowerCase(), new MenuItem(name, category, price, stock));
    }

    public static void main(String[] args) throws Exception {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("TCP Restaurant Server started on port " + PORT);

            while (true) {
                Socket socket = server.accept();
                System.out.println("Accepted connection from " + socket.getRemoteSocketAddress());
                new Thread(() -> handleClient(socket)).start();
            }
        }
    }

    private static void handleClient(Socket socket) {
        try (Socket s = socket) {
            while (true) {
                RestaurantProto.ProtocolMessage request = TcpWire.readMessage(s.getInputStream());
                if (request == null) {
                    break;
                }

                RestaurantProto.ProtocolMessage response = process(request);
                TcpWire.writeMessage(s.getOutputStream(), response);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected");
        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private static RestaurantProto.ProtocolMessage process(RestaurantProto.ProtocolMessage req) {
        System.out.println("Operation: " + req.getOperation() + " | Role: " + req.getRole());

        switch (req.getOperation()) {
            case LOGIN:
                return login(req);
            case GET_MENU:
                return getMenu(req);
            case ADD_MENU_ITEM:
                return addMenuItem(req);
            case REMOVE_MENU_ITEM:
                return removeMenuItem(req);
            case CREATE_ORDER:
                return createOrder(req);
            case ADD_ITEM:
                return addItem(req);
            case SUBMIT_ORDER:
                return submitOrder(req);
            case GET_PENDING_TICKETS:
                return getPendingTickets(req);
            case UPDATE_TICKET_STATUS:
                return updateTicketStatus(req);
            case GET_SERVER_NOTIFICATIONS:
                return getServerNotifications(req);
            case ASSIGN_TABLE:
                return assignTable(req);
            case RELEASE_TABLE:
                return releaseTable(req);
            case VIEW_ALL_ORDERS:
                return viewAllOrders(req);
            case GET_BILL:
                return getBill(req);
            case RETURN_ORDER:
                return returnOrder(req);
            case MAKE_RESERVATION:
                return makeReservation(req);
            case GET_RESERVATIONS:
                return getReservations(req);
            case CANCEL_RESERVATION:
                return cancelReservation(req);
            case GET_STOCK:
                return getStock(req);
            case UPDATE_STOCK:
                return updateStock(req);
            case CANCEL_ORDER:
                return cancelOrder(req);
            default:
                return error(req, "Unknown operation");
        }
    }

    private static RestaurantProto.ProtocolMessage login(RestaurantProto.ProtocolMessage req) {
        String token = UUID.randomUUID().toString();
        Session session = new Session(token, req.getRole());
        SESSIONS.put(token, session);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("role", req.getRole().name());
        data.put("activeSessions", String.valueOf(SESSIONS.size()));

        return ok(req, "Login successful", data);
    }

    private static RestaurantProto.ProtocolMessage getMenu(RestaurantProto.ProtocolMessage req) {
        StringBuilder sb = new StringBuilder();

        for (MenuItem item : MENU.values()) {
            if (item.available) {
                sb.append(item.category)
                        .append(" | ")
                        .append(item.name)
                        .append(" | $")
                        .append(String.format("%.2f", item.price))
                        .append(" | stock=")
                        .append(item.stock)
                        .append("\n");
            }
        }

        return ok(req, "Menu returned", Map.of("menu", sb.toString()));
    }

    private static RestaurantProto.ProtocolMessage addMenuItem(RestaurantProto.ProtocolMessage req) {
        if (!isManager(req)) return error(req, "Manager role required");

        String name = req.getDataMap().getOrDefault("name", "").trim();
        String category = req.getDataMap().getOrDefault("category", "").trim();
        String priceText = req.getDataMap().getOrDefault("price", "").trim();
        String stockText = req.getDataMap().getOrDefault("stock", "50").trim();

        if (name.isEmpty() || category.isEmpty() || priceText.isEmpty()) {
            return error(req, "name, category, and price are required");
        }

        try {
            double price = Double.parseDouble(priceText);
            int stock = Integer.parseInt(stockText);
            MENU.put(name.toLowerCase(), new MenuItem(name, category, price, stock));
            return ok(req, "Menu item added: " + name, Map.of("name", name));
        } catch (NumberFormatException e) {
            return error(req, "Invalid price or stock");
        }
    }

    private static RestaurantProto.ProtocolMessage removeMenuItem(RestaurantProto.ProtocolMessage req) {
        if (!isManager(req)) return error(req, "Manager role required");

        String name = req.getDataMap().getOrDefault("name", "").trim().toLowerCase();

        if (MENU.remove(name) == null) {
            return error(req, "Item not found");
        }

        return ok(req, "Menu item removed", Map.of("name", name));
    }

    private static RestaurantProto.ProtocolMessage createOrder(RestaurantProto.ProtocolMessage req) {
        String guestName = req.getDataMap().getOrDefault("guestName", "Guest");
        String orderType = req.getDataMap().getOrDefault("orderType", "dine-in");
        String orderId = "ORD-" + orderCounter.getAndIncrement();

        Order order = new Order(orderId, guestName, orderType);
        ORDERS.put(orderId, order);

        return ok(req, "Order created", Map.of("orderId", orderId));
    }

    private static RestaurantProto.ProtocolMessage addItem(RestaurantProto.ProtocolMessage req) {
        String orderId = req.getDataMap().getOrDefault("orderId", "");
        String itemName = req.getDataMap().getOrDefault("itemName", "").trim();
        String qtyText = req.getDataMap().getOrDefault("quantity", "1");

        Order order = ORDERS.get(orderId);
        if (order == null) return error(req, "Order not found");

        MenuItem item = MENU.get(itemName.toLowerCase());
        if (item == null || !item.available) return error(req, "Item unavailable");

        try {
            int quantity = Integer.parseInt(qtyText);

            synchronized (item) {
                if (item.stock < quantity) {
                    return error(req, "Insufficient stock");
                }

                item.stock -= quantity;
                item.available = item.stock > 0;
            }

            order.lines.add(new OrderLine(item.name, item.price, quantity));

            return ok(req, "Item added", Map.of(
                    "orderId", orderId,
                    "itemName", item.name,
                    "subtotal", String.format("%.2f", order.subtotal())
            ));
        } catch (NumberFormatException e) {
            return error(req, "Invalid quantity");
        }
    }

    private static RestaurantProto.ProtocolMessage submitOrder(RestaurantProto.ProtocolMessage req) {
        String orderId = req.getDataMap().getOrDefault("orderId", "");
        Order order = ORDERS.get(orderId);

        if (order == null) return error(req, "Order not found");
        if (order.lines.isEmpty()) return error(req, "Order has no items");

        String ticketId = "TCK-" + ticketCounter.getAndIncrement();
        StringBuilder summary = new StringBuilder();

        for (OrderLine line : order.lines) {
            summary.append(line.quantity)
                    .append("x ")
                    .append(line.itemName)
                    .append("\n");
        }

        Ticket ticket = new Ticket(ticketId, orderId, "PENDING", summary.toString());
        TICKETS.put(ticketId, ticket);
        order.status = "SUBMITTED";

        return ok(req, "Order submitted to kitchen", Map.of(
                "orderId", orderId,
                "ticketId", ticketId
        ));
    }

    private static RestaurantProto.ProtocolMessage getPendingTickets(RestaurantProto.ProtocolMessage req) {
        if (!isChef(req)) return error(req, "Chef role required");

        StringBuilder sb = new StringBuilder();

        for (Ticket ticket : TICKETS.values()) {
            if (!ticket.status.equals("READY")) {
                sb.append(ticket.ticketId)
                        .append(" | ")
                        .append(ticket.status)
                        .append(" | order=")
                        .append(ticket.orderId)
                        .append("\n")
                        .append(ticket.summary)
                        .append("\n");
            }
        }

        if (sb.length() == 0) sb.append("No pending tickets");

        return ok(req, "Tickets returned", Map.of("tickets", sb.toString()));
    }

    private static RestaurantProto.ProtocolMessage updateTicketStatus(RestaurantProto.ProtocolMessage req) {
        if (!isChef(req)) return error(req, "Chef role required");

        String ticketId = req.getDataMap().getOrDefault("ticketId", "");
        String status = req.getDataMap().getOrDefault("status", "READY");

        Ticket ticket = TICKETS.get(ticketId);
        if (ticket == null) return error(req, "Ticket not found");

        ticket.status = status;

        Order order = ORDERS.get(ticket.orderId);
        if (order != null && status.equalsIgnoreCase("READY")) {
            order.status = "READY";
        }

        broadcastToServers("ORDER_READY | ticketId=" + ticket.ticketId + " | orderId=" + ticket.orderId);

        return ok(req, "Ticket updated", Map.of("ticketId", ticketId, "status", status));
    }

    private static RestaurantProto.ProtocolMessage getServerNotifications(RestaurantProto.ProtocolMessage req) {
        if (!isServer(req) && !isManager(req)) return error(req, "Server or Manager role required");

        String token = req.getDataMap().getOrDefault("token", "");
        Session session = SESSIONS.get(token);

        if (session == null) return error(req, "Session not found");

        StringBuilder sb = new StringBuilder();
        String note;

        while ((note = session.notifications.poll()) != null) {
            sb.append(note).append("\n");
        }

        if (sb.length() == 0) sb.append("No notifications");

        return ok(req, "Notifications returned", Map.of("notifications", sb.toString()));
    }

    private static RestaurantProto.ProtocolMessage assignTable(RestaurantProto.ProtocolMessage req) {
        if (!isServer(req) && !isManager(req)) return error(req, "Server or Manager role required");

        String orderId = req.getDataMap().getOrDefault("orderId", "");
        String tableNumber = req.getDataMap().getOrDefault("tableNumber", "");

        Order order = ORDERS.get(orderId);
        if (order == null) return error(req, "Order not found");

        synchronized (TABLES) {
            if (TABLES.containsKey(tableNumber)) {
                return error(req, "Table is already occupied");
            }

            TABLES.put(tableNumber, orderId);
            order.tableNumber = tableNumber;
        }

        return ok(req, "Table assigned", Map.of("tableNumber", tableNumber, "orderId", orderId));
    }

    private static RestaurantProto.ProtocolMessage releaseTable(RestaurantProto.ProtocolMessage req) {
        if (!isServer(req) && !isManager(req)) return error(req, "Server or Manager role required");

        String tableNumber = req.getDataMap().getOrDefault("tableNumber", "");

        TABLES.remove(tableNumber);
        broadcastToServers("TABLE_FREE | table=" + tableNumber);

        return ok(req, "Table released", Map.of("tableNumber", tableNumber));
    }

    private static RestaurantProto.ProtocolMessage viewAllOrders(RestaurantProto.ProtocolMessage req) {
        if (!isServer(req) && !isManager(req)) return error(req, "Server or Manager role required");

        StringBuilder sb = new StringBuilder();

        for (Order order : ORDERS.values()) {
            sb.append(order.orderId)
                    .append(" | guest=")
                    .append(order.guestName)
                    .append(" | status=")
                    .append(order.status)
                    .append(" | table=")
                    .append(order.tableNumber == null ? "none" : order.tableNumber)
                    .append(" | subtotal=$")
                    .append(String.format("%.2f", order.subtotal()))
                    .append("\n");
        }

        return ok(req, "Orders returned", Map.of("orders", sb.toString()));
    }

    private static RestaurantProto.ProtocolMessage getBill(RestaurantProto.ProtocolMessage req) {
        String orderId = req.getDataMap().getOrDefault("orderId", "");
        Order order = ORDERS.get(orderId);

        if (order == null) return error(req, "Order not found");

        double subtotal = order.subtotal();
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        StringBuilder sb = new StringBuilder();
        sb.append("Bill for ").append(order.guestName).append("\n");

        for (OrderLine line : order.lines) {
            sb.append(line.quantity)
                    .append("x ")
                    .append(line.itemName)
                    .append(" = $")
                    .append(String.format("%.2f", line.total()))
                    .append("\n");
        }

        sb.append("Subtotal: $").append(String.format("%.2f", subtotal)).append("\n");
        sb.append("Tax: $").append(String.format("%.2f", tax)).append("\n");
        sb.append("Total before tip: $").append(String.format("%.2f", total)).append("\n");

        order.status = "COMPLETED";

        if (order.tableNumber != null) {
            TABLES.remove(order.tableNumber);
            broadcastToServers("TABLE_FREE | table=" + order.tableNumber);
        }

        return ok(req, "Bill returned", Map.of("bill", sb.toString()));
    }

    private static RestaurantProto.ProtocolMessage returnOrder(RestaurantProto.ProtocolMessage req) {
        String orderId = req.getDataMap().getOrDefault("orderId", "");
        String itemName = req.getDataMap().getOrDefault("itemName", "");
        String reason = req.getDataMap().getOrDefault("reason", "Customer dissatisfaction");

        Order order = ORDERS.get(orderId);
        if (order == null) return error(req, "Order not found");

        String ticketId = "TCK-" + ticketCounter.getAndIncrement();

        Ticket ticket = new Ticket(ticketId, orderId, "RETURNED",
                "Returned item: " + itemName + "\nReason: " + reason);

        TICKETS.put(ticketId, ticket);
        broadcastToServers("ORDER_RETURNED | orderId=" + orderId + " | item=" + itemName);

        return ok(req, "Return ticket created", Map.of("ticketId", ticketId));
    }

    private static RestaurantProto.ProtocolMessage makeReservation(RestaurantProto.ProtocolMessage req) {
        if (!isServer(req) && !isManager(req)) return error(req, "Server or Manager role required");

        String customerName = req.getDataMap().getOrDefault("customerName", "");
        String timeSlot = req.getDataMap().getOrDefault("timeSlot", "");
        String tableNumber = req.getDataMap().getOrDefault("tableNumber", "");
        String partySizeText = req.getDataMap().getOrDefault("partySize", "1");

        try {
            int partySize = Integer.parseInt(partySizeText);

            for (Reservation r : RESERVATIONS.values()) {
                if (r.tableNumber.equals(tableNumber) && r.timeSlot.equals(timeSlot) && r.status.equals("CONFIRMED")) {
                    return error(req, "Reservation collision for table and time");
                }
            }

            String reservationId = "RES-" + reservationCounter.getAndIncrement();
            Reservation reservation = new Reservation(reservationId, customerName, timeSlot, tableNumber, partySize);
            RESERVATIONS.put(reservationId, reservation);

            return ok(req, "Reservation confirmed", Map.of("reservationId", reservationId));
        } catch (NumberFormatException e) {
            return error(req, "Invalid party size");
        }
    }

    private static RestaurantProto.ProtocolMessage getReservations(RestaurantProto.ProtocolMessage req) {
        if (!isServer(req) && !isManager(req)) return error(req, "Server or Manager role required");

        StringBuilder sb = new StringBuilder();

        for (Reservation r : RESERVATIONS.values()) {
            sb.append(r.reservationId)
                    .append(" | ")
                    .append(r.customerName)
                    .append(" | table=")
                    .append(r.tableNumber)
                    .append(" | time=")
                    .append(r.timeSlot)
                    .append(" | party=")
                    .append(r.partySize)
                    .append(" | ")
                    .append(r.status)
                    .append("\n");
        }

        if (sb.length() == 0) sb.append("No reservations");

        return ok(req, "Reservations returned", Map.of("reservations", sb.toString()));
    }

    private static RestaurantProto.ProtocolMessage cancelReservation(RestaurantProto.ProtocolMessage req) {
        if (!isServer(req) && !isManager(req)) return error(req, "Server or Manager role required");

        String reservationId = req.getDataMap().getOrDefault("reservationId", "");
        Reservation reservation = RESERVATIONS.get(reservationId);

        if (reservation == null) return error(req, "Reservation not found");

        reservation.status = "CANCELLED";

        return ok(req, "Reservation cancelled", Map.of("reservationId", reservationId));
    }

    private static RestaurantProto.ProtocolMessage getStock(RestaurantProto.ProtocolMessage req) {
        if (!isManager(req)) return error(req, "Manager role required");

        StringBuilder sb = new StringBuilder();

        for (MenuItem item : MENU.values()) {
            sb.append(item.name)
                    .append(" | stock=")
                    .append(item.stock)
                    .append(" | available=")
                    .append(item.available)
                    .append("\n");
        }

        return ok(req, "Stock returned", Map.of("stock", sb.toString()));
    }

    private static RestaurantProto.ProtocolMessage updateStock(RestaurantProto.ProtocolMessage req) {
        if (!isManager(req)) return error(req, "Manager role required");

        String name = req.getDataMap().getOrDefault("name", "").trim().toLowerCase();
        String stockText = req.getDataMap().getOrDefault("stock", "");

        MenuItem item = MENU.get(name);
        if (item == null) return error(req, "Item not found");

        try {
            int stock = Integer.parseInt(stockText);
            item.stock = stock;
            item.available = stock > 0;

            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            return ok(req, "Stock updated", Map.of(
                    "name", item.name,
                    "stock", String.valueOf(item.stock),
                    "date", date
            ));
        } catch (NumberFormatException e) {
            return error(req, "Invalid stock value");
        }
    }

    private static RestaurantProto.ProtocolMessage cancelOrder(RestaurantProto.ProtocolMessage req) {
        String orderId = req.getDataMap().getOrDefault("orderId", "");
        Order order = ORDERS.remove(orderId);

        if (order == null) return error(req, "Order not found");

        if (order.tableNumber != null) {
            TABLES.remove(order.tableNumber);
        }

        return ok(req, "Order cancelled", Map.of("orderId", orderId));
    }

    private static void broadcastToServers(String notification) {
        Collection<Session> sessions = SESSIONS.values();

        for (Session session : sessions) {
            if (session.role == RestaurantProto.ProtocolMessage.Role.SERVER ||
                    session.role == RestaurantProto.ProtocolMessage.Role.MANAGER) {
                session.notifications.add(notification);
            }
        }
    }

    private static boolean isManager(RestaurantProto.ProtocolMessage req) {
        return req.getRole() == RestaurantProto.ProtocolMessage.Role.MANAGER;
    }

    private static boolean isServer(RestaurantProto.ProtocolMessage req) {
        return req.getRole() == RestaurantProto.ProtocolMessage.Role.SERVER;
    }

    private static boolean isChef(RestaurantProto.ProtocolMessage req) {
        return req.getRole() == RestaurantProto.ProtocolMessage.Role.CHEF;
    }

    private static RestaurantProto.ProtocolMessage ok(
            RestaurantProto.ProtocolMessage req,
            String message,
            Map<String, String> data
    ) {
        return RestaurantProto.ProtocolMessage.newBuilder()
                .setRequestId(req.getRequestId())
                .setMessageType(RestaurantProto.ProtocolMessage.MessageType.RESPONSE)
                .setRole(req.getRole())
                .setOperation(req.getOperation())
                .setStatus(RestaurantProto.ProtocolMessage.Status.OK)
                .setMessage(message)
                .putAllData(data)
                .build();
    }

    private static RestaurantProto.ProtocolMessage error(RestaurantProto.ProtocolMessage req, String message) {
        return RestaurantProto.ProtocolMessage.newBuilder()
                .setRequestId(req.getRequestId())
                .setMessageType(RestaurantProto.ProtocolMessage.MessageType.RESPONSE)
                .setRole(req.getRole())
                .setOperation(req.getOperation())
                .setStatus(RestaurantProto.ProtocolMessage.Status.ERROR)
                .setMessage(message)
                .build();
    }
}
