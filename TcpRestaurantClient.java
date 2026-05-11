package io.grpc.examples.helloworld;

import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class TcpRestaurantClient {
    private static final String HOST = "localhost";
    private static final int PORT = 50052;

    private static RestaurantProto.ProtocolMessage.Role currentRole =
            RestaurantProto.ProtocolMessage.Role.CUSTOMER;

    private static String token = "";
    private static String activeOrderId = "";
    private static String activeTicketId = "";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket(HOST, PORT)) {
            System.out.println("Connected to restaurant server");

            while (true) {
                printMenu();

                System.out.print("Choose option: ");
                String choice = scanner.nextLine().trim();

                if (choice.equals("0")) {
                    System.out.println("Goodbye");
                    break;
                }

                RestaurantProto.ProtocolMessage request = buildRequest(choice, scanner);

                if (request == null) {
                    System.out.println("Invalid option");
                    continue;
                }

                TcpWire.writeMessage(socket.getOutputStream(), request);

                RestaurantProto.ProtocolMessage response =
                        TcpWire.readMessage(socket.getInputStream());

                printResponse(response);
                updateSession(response);
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("======================================");
        System.out.println("Restaurant Middleware Client");
        System.out.println("Role: " + currentRole);
        System.out.println("Token: " + token);
        System.out.println("Active Order: " + activeOrderId);
        System.out.println("Active Ticket: " + activeTicketId);
        System.out.println("======================================");
        System.out.println("1. Login as Customer");
        System.out.println("2. Login as Server");
        System.out.println("3. Login as Manager");
        System.out.println("4. Login as Chef");
        System.out.println("5. Get Menu");
        System.out.println("6. Create Order");
        System.out.println("7. Add Item to Order");
        System.out.println("8. Submit Order");
        System.out.println("9. View Kitchen Tickets");
        System.out.println("10. Mark Ticket Ready");
        System.out.println("11. Check Server Notifications");
        System.out.println("12. Assign Table");
        System.out.println("13. View All Orders");
        System.out.println("14. Get Bill");
        System.out.println("15. Return Order Item");
        System.out.println("16. Add Menu Item");
        System.out.println("17. Remove Menu Item");
        System.out.println("18. Make Reservation");
        System.out.println("19. View Reservations");
        System.out.println("20. Cancel Reservation");
        System.out.println("21. View Stock");
        System.out.println("22. Update Stock");
        System.out.println("23. Release Table");
        System.out.println("24. Cancel Order");
        System.out.println("0. Exit");
        System.out.println("======================================");
    }

    private static RestaurantProto.ProtocolMessage buildRequest(String choice, Scanner scanner) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("token", token);

        RestaurantProto.ProtocolMessage.Operation op;

        switch (choice) {
            case "1":
                currentRole = RestaurantProto.ProtocolMessage.Role.CUSTOMER;
                op = RestaurantProto.ProtocolMessage.Operation.LOGIN;
                break;

            case "2":
                if (!checkPassword(scanner, "Server1")) return null;
                currentRole = RestaurantProto.ProtocolMessage.Role.SERVER;
                op = RestaurantProto.ProtocolMessage.Operation.LOGIN;
                break;

            case "3":
                if (!checkPassword(scanner, "Manager1")) return null;
                currentRole = RestaurantProto.ProtocolMessage.Role.MANAGER;
                op = RestaurantProto.ProtocolMessage.Operation.LOGIN;
                break;

            case "4":
                if (!checkPassword(scanner, "Chef1")) return null;
                currentRole = RestaurantProto.ProtocolMessage.Role.CHEF;
                op = RestaurantProto.ProtocolMessage.Operation.LOGIN;
                break;

            case "5":
                op = RestaurantProto.ProtocolMessage.Operation.GET_MENU;
                break;

            case "6":
                op = RestaurantProto.ProtocolMessage.Operation.CREATE_ORDER;
                System.out.print("Guest name: ");
                data.put("guestName", scanner.nextLine().trim());
                System.out.print("Order type dine-in/takeout: ");
                data.put("orderType", scanner.nextLine().trim());
                break;

            case "7":
                op = RestaurantProto.ProtocolMessage.Operation.ADD_ITEM;
                data.put("orderId", ask(scanner, "Order ID", activeOrderId));
                System.out.print("Item name: ");
                data.put("itemName", scanner.nextLine().trim());
                System.out.print("Quantity: ");
                data.put("quantity", scanner.nextLine().trim());
                break;

            case "8":
                op = RestaurantProto.ProtocolMessage.Operation.SUBMIT_ORDER;
                data.put("orderId", ask(scanner, "Order ID", activeOrderId));
                break;

            case "9":
                op = RestaurantProto.ProtocolMessage.Operation.GET_PENDING_TICKETS;
                break;

            case "10":
                op = RestaurantProto.ProtocolMessage.Operation.UPDATE_TICKET_STATUS;
                data.put("ticketId", ask(scanner, "Ticket ID", activeTicketId));
                data.put("status", "READY");
                break;

            case "11":
                op = RestaurantProto.ProtocolMessage.Operation.GET_SERVER_NOTIFICATIONS;
                break;

            case "12":
                op = RestaurantProto.ProtocolMessage.Operation.ASSIGN_TABLE;
                data.put("orderId", ask(scanner, "Order ID", activeOrderId));
                System.out.print("Table number: ");
                data.put("tableNumber", scanner.nextLine().trim());
                break;

            case "13":
                op = RestaurantProto.ProtocolMessage.Operation.VIEW_ALL_ORDERS;
                break;

            case "14":
                op = RestaurantProto.ProtocolMessage.Operation.GET_BILL;
                data.put("orderId", ask(scanner, "Order ID", activeOrderId));
                break;

            case "15":
                op = RestaurantProto.ProtocolMessage.Operation.RETURN_ORDER;
                data.put("orderId", ask(scanner, "Order ID", activeOrderId));
                System.out.print("Returned item name: ");
                data.put("itemName", scanner.nextLine().trim());
                System.out.print("Reason: ");
                data.put("reason", scanner.nextLine().trim());
                break;

            case "16":
                op = RestaurantProto.ProtocolMessage.Operation.ADD_MENU_ITEM;
                System.out.print("Item name: ");
                data.put("name", scanner.nextLine().trim());
                System.out.print("Category: ");
                data.put("category", scanner.nextLine().trim());
                System.out.print("Price: ");
                data.put("price", scanner.nextLine().trim());
                System.out.print("Starting stock: ");
                data.put("stock", scanner.nextLine().trim());
                break;

            case "17":
                op = RestaurantProto.ProtocolMessage.Operation.REMOVE_MENU_ITEM;
                System.out.print("Item name: ");
                data.put("name", scanner.nextLine().trim());
                break;

            case "18":
                op = RestaurantProto.ProtocolMessage.Operation.MAKE_RESERVATION;
                System.out.print("Customer name: ");
                data.put("customerName", scanner.nextLine().trim());
                System.out.print("Time slot: ");
                data.put("timeSlot", scanner.nextLine().trim());
                System.out.print("Table number: ");
                data.put("tableNumber", scanner.nextLine().trim());
                System.out.print("Party size: ");
                data.put("partySize", scanner.nextLine().trim());
                break;

            case "19":
                op = RestaurantProto.ProtocolMessage.Operation.GET_RESERVATIONS;
                break;

            case "20":
                op = RestaurantProto.ProtocolMessage.Operation.CANCEL_RESERVATION;
                System.out.print("Reservation ID: ");
                data.put("reservationId", scanner.nextLine().trim());
                break;

            case "21":
                op = RestaurantProto.ProtocolMessage.Operation.GET_STOCK;
                break;

            case "22":
                op = RestaurantProto.ProtocolMessage.Operation.UPDATE_STOCK;
                System.out.print("Item name: ");
                data.put("name", scanner.nextLine().trim());
                System.out.print("New stock amount: ");
                data.put("stock", scanner.nextLine().trim());
                break;

            case "23":
                op = RestaurantProto.ProtocolMessage.Operation.RELEASE_TABLE;
                System.out.print("Table number: ");
                data.put("tableNumber", scanner.nextLine().trim());
                break;

            case "24":
                op = RestaurantProto.ProtocolMessage.Operation.CANCEL_ORDER;
                data.put("orderId", ask(scanner, "Order ID", activeOrderId));
                break;

            default:
                return null;
        }

        return RestaurantProto.ProtocolMessage.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setMessageType(RestaurantProto.ProtocolMessage.MessageType.REQUEST)
                .setRole(currentRole)
                .setOperation(op)
                .putAllData(data)
                .build();
    }

    private static boolean checkPassword(Scanner scanner, String expected) {
        System.out.print("Password: ");
        String entered = scanner.nextLine().trim();

        if (!entered.equals(expected)) {
            System.out.println("Incorrect password");
            return false;
        }

        return true;
    }

    private static String ask(Scanner scanner, String label, String defaultValue) {
        if (defaultValue != null && !defaultValue.isEmpty()) {
            System.out.print(label + " [" + defaultValue + "]: ");
            String entered = scanner.nextLine().trim();

            if (entered.isEmpty()) {
                return defaultValue;
            }

            return entered;
        }

        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private static void printResponse(RestaurantProto.ProtocolMessage response) {
        System.out.println();
        System.out.println("Status: " + response.getStatus());
        System.out.println("Message: " + response.getMessage());

        for (Map.Entry<String, String> entry : response.getDataMap().entrySet()) {
            System.out.println(entry.getKey() + ":");
            System.out.println(entry.getValue());
        }
    }

    private static void updateSession(RestaurantProto.ProtocolMessage response) {
        if (response.getStatus() != RestaurantProto.ProtocolMessage.Status.OK) {
            return;
        }

        Map<String, String> data = response.getDataMap();

        if (data.containsKey("token")) {
            token = data.get("token");
        }

        if (data.containsKey("orderId")) {
            activeOrderId = data.get("orderId");
        }

        if (data.containsKey("ticketId")) {
            activeTicketId = data.get("ticketId");
        }
    }
}
