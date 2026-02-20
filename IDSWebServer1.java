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

    // 🔥 Now using Map to count occurrences
    static Map<String, Integer> blockedIPs = new HashMap<>();
    static Map<String, Integer> blockedNumbers = new HashMap<>();
    static List<String> recentAlerts = new ArrayList<>();

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

        System.out.println("Website running at http://localhost:8080");
    }

    // ================= HOME PAGE =================
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            StringBuilder blockedIPList = new StringBuilder();
            for (Map.Entry<String, Integer> entry : blockedIPs.entrySet()) {
                blockedIPList.append("<li>")
                        .append(entry.getKey())
                        .append(" (")
                        .append(entry.getValue())
                        .append(" times)")
                        .append("</li>");
            }

            StringBuilder blockedNumberList = new StringBuilder();
            for (Map.Entry<String, Integer> entry : blockedNumbers.entrySet()) {
                blockedNumberList.append("<li>")
                        .append(entry.getKey())
                        .append(" (")
                        .append(entry.getValue())
                        .append(" times)")
                        .append("</li>");
            }

            StringBuilder alertList = new StringBuilder();
            for (String alert : recentAlerts) {
                alertList.append("<li>").append(alert).append("</li>");
            }

            String html =
                    "<html><body style='text-align:center;font-family:Arial'>" +
                    "<h1>Cyber Security IDS Website</h1>" +

                    "<h2>Login Detection</h2>" +
                    "<form action='/login'>" +
                    "Username: <input name='user'><br><br>" +
                    "Password: <input name='pass'><br><br>" +
                    "IP: <input name='ip'><br><br>" +
                    "<button type='submit'>Login</button>" +
                    "</form><br><hr>" +

                    "<h2>Scam Call Detection</h2>" +
                    "<form action='/scam'>" +
                    "Phone Number: <input name='number'><br><br>" +
                    "IP: <input name='ip'><br><br>" +
                    "<button type='submit'>Check</button>" +
                    "</form><br><hr>" +

                    "<h3>Blocked IPs</h3><ul>" +
                    blockedIPList +
                    "</ul>" +

                    "<h3>Blocked Scam Numbers</h3><ul>" +
                    blockedNumberList +
                    "</ul>" +

                    "<h3>Recent Alerts</h3><ul>" +
                    alertList +
                    "</ul>" +

                    "</body></html>";

            exchange.sendResponseHeaders(200, html.length());
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
        }
    }

    // ================= LOGIN HANDLER =================
    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            String pass = params.get("pass");
            String ip = params.get("ip");

            String response;

            if (!"admin123".equals(pass)) {

                blockedIPs.put(ip, blockedIPs.getOrDefault(ip, 0) + 1);

                String alert = "LOGIN INTRUSION from IP: " + ip +
                        " at " + LocalDateTime.now();

                recentAlerts.add(alert);
                saveLog(alert);

                response = "<h2 style='color:red'>" + alert + "</h2>";
            }
            else {
                response = "<h2 style='color:green'>Login Successful</h2>";
            }

            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // ================= SCAM HANDLER =================
    static class ScamHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {

            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            String number = params.get("number");
            String ip = params.get("ip");

            String response;

            if (isScamNumber(number)) {

                blockedNumbers.put(number,
                        blockedNumbers.getOrDefault(number, 0) + 1);

                blockedIPs.put(ip,
                        blockedIPs.getOrDefault(ip, 0) + 1);

                String alert = "SCAM DETECTED: " + number +
                        " from IP: " + ip +
                        " at " + LocalDateTime.now();

                recentAlerts.add(alert);
                saveLog(alert);

                response = "<h2 style='color:red'>" + alert + "</h2>";
            }
            else {
                response = "<h2 style='color:green'>Safe Number</h2>";
            }

            exchange.sendResponseHeaders(200, response.length());
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

    // ================= SAVE LOG =================
    static void saveLog(String message) {
        try {
            FileWriter fw = new FileWriter("ids_logs.txt", true);
            fw.write(message + "\n");
            fw.close();
        } catch (IOException e) {
            System.out.println("Log error");
        }
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