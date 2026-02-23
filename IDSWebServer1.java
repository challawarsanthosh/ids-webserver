import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class IDSWebServer1 {

    static Map<String, Integer> loginAttempts = new HashMap<>();
    static Map<String, Integer> scamCounts = new HashMap<>();

    static Set<String> scamBlacklist = new HashSet<>(Arrays.asList(
            "8889988999",
            "9998887776",
            "7777770000",
            "8888888888",
            "9999999999"
    ));

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new RootHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/scam", new ScamHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server running...");
    }

    // ================= HOME PAGE =================
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            String html =
                    "<html><body style='text-align:center;font-family:Arial'>" +

                    "<h2>Login Detection</h2>" +
                    "<form action='/login'>" +
                    "Username: <input name='user'><br><br>" +
                    "Password: <input name='pass'><br><br>" +
                    "IP: <input name='ip'><br><br>" +
                    "<button type='submit'>Login</button>" +
                    "</form><br><hr>" +

                    "<h2>Phone Scam Detection</h2>" +
                    "<form action='/scam'>" +
                    "Phone Number: <input name='number'><br><br>" +
                    "IP: <input name='ip'><br><br>" +
                    "<button type='submit'>Check</button>" +
                    "</form>" +

                    "</body></html>";

            exchange.sendResponseHeaders(200, html.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
        }
    }

    // ================= LOGIN HANDLER =================
    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String pass = params.get("pass");
            String ip = params.get("ip");
            String time = LocalDateTime.now().toString();

            String response;

            if (!"admin123".equals(pass)) {

                loginAttempts.put(ip, loginAttempts.getOrDefault(ip, 0) + 1);
                int count = loginAttempts.get(ip);

                response =
                        "<html><body style='color:red;font-family:Arial'>" +
                        "<h2>LOGIN INTRUSION</h2>" +
                        "<h3>IP: " + ip + "</h3>" +
                        "<h3>Time: " + time + "</h3>" +
                        "<h3>Attempts: " + count + "</h3>" +
                        "</body></html>";

            } else {

                response =
                        "<html><body style='color:green;font-family:Arial'>" +
                        "<h2>LOGIN SAFE</h2>" +
                        "<h3>IP: " + ip + "</h3>" +
                        "<h3>Time: " + time + "</h3>" +
                        "</body></html>";
            }

            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // ================= SCAM HANDLER =================
    static class ScamHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String number = params.get("number");
            String ip = params.get("ip");
            String time = LocalDateTime.now().toString();

            String response;

            if (isScamNumber(number)) {

                scamCounts.put(number, scamCounts.getOrDefault(number, 0) + 1);
                int count = scamCounts.get(number);

                response =
                        "<html><body style='color:red;font-family:Arial'>" +
                        "<h2>SCAM DETECTED</h2>" +
                        "<h3>IP: " + ip + "</h3>" +
                        "<h3>Time: " + time + "</h3>" +
                        "<h3>Occurrences: " + count + "</h3>" +
                        "</body></html>";

            } else {

                response =
                        "<html><body style='color:green;font-family:Arial'>" +
                        "<h2>SAFE NUMBER</h2>" +
                        "<h3>IP: " + ip + "</h3>" +
                        "<h3>Time: " + time + "</h3>" +
                        "</body></html>";
            }

            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // ================= SCAM CHECK =================
    static boolean isScamNumber(String number) {

        if (number == null || !number.matches("\\d{10}"))
            return true;

        if (scamBlacklist.contains(number))
            return true;

        if (number.chars().distinct().count() == 1)
            return true;

        if (number.equals("1234567890") ||
                number.equals("0987654321"))
            return true;

        int[] count = new int[10];
        for (char c : number.toCharArray())
            count[c - '0']++;

        for (int c : count)
            if (c >= 4)
                return true;

        return false;
    }

    // ================= PARSE QUERY =================
    static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length == 2)
                map.put(parts[0],
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return map;
    }
}
