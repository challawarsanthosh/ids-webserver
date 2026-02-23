import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

public class IDSWebServer1 {

    static boolean loggedIn = false;

    static Map<String,Integer> ipAttempts = new HashMap<>();
    static Set<String> blockedIPs = new HashSet<>();
    static List<String> alerts = new ArrayList<>();

    static int totalIntrusions = 0;
    static int totalScams = 0;

    static final String USERNAME = "admin";
    static final String PASSWORD = "admin123";
    static final int MAX_ATTEMPTS = 3;

    // 🔥 ADD YOUR TELEGRAM DETAILS
    static final String BOT_TOKEN = "YOUR_BOT_TOKEN";
    static final String CHAT_ID = "YOUR_CHAT_ID";

    public static void main(String[] args) throws Exception {

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT","8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port),0);

        server.createContext("/", new LoginPage());
        server.createContext("/login", new LoginHandler());
        server.createContext("/dashboard", new Dashboard());
        server.createContext("/monitor", new Monitor());
        server.createContext("/logout", new Logout());

        server.setExecutor(null);
        server.start();
        System.out.println("🔥 Advanced IDS Running...");
    }

    // ================= TELEGRAM ALERT =================
    static void sendTelegramAlert(String message){
        try{
            String urlString = "https://api.telegram.org/bot"+BOT_TOKEN+
                    "/sendMessage?chat_id="+CHAT_ID+
                    "&text="+URLEncoder.encode(message,"UTF-8");

            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.getInputStream().close();
        }catch(Exception e){
            System.out.println("Telegram alert failed");
        }
    }

    // ================= LOGIN PAGE =================
    static class LoginPage implements HttpHandler{
        public void handle(HttpExchange ex) throws IOException{
            String html =
                    "<html><body style='background:#141e30;color:white;text-align:center;font-family:Arial'>" +
                    "<h1>🔐 IDS Login</h1>" +
                    "<form action='/login'>" +
                    "Username:<input name='user'><br><br>" +
                    "Password:<input type='password' name='pass'><br><br>" +
                    "<button>Login</button></form></body></html>";
            send(ex,html);
        }
    }

    // ================= LOGIN =================
    static class LoginHandler implements HttpHandler{
        public void handle(HttpExchange ex) throws IOException{
            Map<String,String> p = parseQuery(ex.getRequestURI().getQuery());
            if(USERNAME.equals(p.get("user")) && PASSWORD.equals(p.get("pass"))){
                loggedIn = true;
                send(ex,"<h2>Login Successful</h2><a href='/dashboard'>Go to Dashboard</a>");
            }else{
                send(ex,"<h2 style='color:red'>Invalid Credentials</h2><a href='/'>Try Again</a>");
            }
        }
    }

    // ================= DASHBOARD =================
    static class Dashboard implements HttpHandler{
        public void handle(HttpExchange ex) throws IOException{
            if(!loggedIn){
                send(ex,"Access Denied <a href='/'>Login</a>");
                return;
            }

            String status = blockedIPs.isEmpty() ?
                    "🟢 SYSTEM SAFE" : "🔴 ATTACK DETECTED";

            String html =
                    "<html><body style='background:black;color:#0f0;text-align:center;font-family:Arial'>" +
                    "<h1>🛡 Cyber Security Dashboard</h1>" +
                    "<h2>Status: "+status+"</h2>" +
                    "<h3>Total Intrusions: "+totalIntrusions+"</h3>" +
                    "<h3>Total Scam Detections: "+totalScams+"</h3>" +
                    "<h3>Blocked IPs: "+blockedIPs.size()+"</h3>" +
                    "<br><a href='/monitor' style='color:cyan'>Open Monitoring</a><br><br>" +
                    "<a href='/logout'>Logout</a></body></html>";

            send(ex,html);
        }
    }

    // ================= MONITOR =================
    static class Monitor implements HttpHandler{
        public void handle(HttpExchange ex) throws IOException{

            if(!loggedIn){
                send(ex,"Access Denied <a href='/'>Login</a>");
                return;
            }

            Map<String,String> p = parseQuery(ex.getRequestURI().getQuery());
            String ip = p.get("ip");
            String number = p.get("number");
            String result = "";

            if(ip != null && !ip.isEmpty())
                result = detectIntrusion(ip);

            if(number != null && !number.isEmpty())
                result = detectScam(number);

            String html =
                    "<html><body style='background:#1c1c1c;color:white;text-align:center;font-family:Arial'>" +
                    "<h1>🚨 IDS Monitoring</h1>" +

                    "<h3>IP Detection</h3>" +
                    "<form action='/monitor'>" +
                    "IP:<input name='ip'><br><br>" +
                    "<button>Check IP</button></form><hr>" +

                    "<h3>Phone Detection</h3>" +
                    "<form action='/monitor'>" +
                    "Number:<input name='number'><br><br>" +
                    "<button>Check Number</button></form><hr>" +

                    "<h3>Result:</h3><p style='color:yellow'>"+result+"</p>" +

                    "<h3>Alerts</h3><ul>";

            for(String a:alerts)
                html+="<li>"+a+"</li>";

            html+="</ul><br><a href='/dashboard'>Back</a></body></html>";

            send(ex,html);
        }
    }

    // ================= INTRUSION =================
    static String detectIntrusion(String ip){

        totalIntrusions++;

        if(!ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}$")){
            String msg="⚠ INVALID IP: "+ip;
            alerts.add(msg+" | "+LocalDateTime.now());
            sendTelegramAlert(msg);
            return msg;
        }

        ipAttempts.put(ip,ipAttempts.getOrDefault(ip,0)+1);

        if(ipAttempts.get(ip)>=MAX_ATTEMPTS){
            blockedIPs.add(ip);
            String msg="🚫 BRUTE FORCE BLOCKED IP: "+ip;
            alerts.add(msg+" | "+LocalDateTime.now());
            sendTelegramAlert(msg);
            return msg;
        }

        String msg="⚠ Suspicious IP: "+ip;
        alerts.add(msg+" | "+LocalDateTime.now());
        sendTelegramAlert(msg);
        return msg;
    }

    // ================= SCAM =================
    static String detectScam(String number){

        totalScams++;

        if(!number.matches("\\d{10}") ||
                number.chars().distinct().count()==1){
            String msg="🚨 SCAM NUMBER DETECTED: "+number;
            alerts.add(msg+" | "+LocalDateTime.now());
            sendTelegramAlert(msg);
            return msg;
        }

        return "✅ SAFE NUMBER";
    }

    // ================= LOGOUT =================
    static class Logout implements HttpHandler{
        public void handle(HttpExchange ex) throws IOException{
            loggedIn=false;
            send(ex,"<h2>Logged Out</h2><a href='/'>Login Again</a>");
        }
    }

    // ================= UTIL =================
    static Map<String,String> parseQuery(String query){
        Map<String,String> map=new HashMap<>();
        if(query==null) return map;
        for(String pair:query.split("&")){
            String[] parts=pair.split("=");
            if(parts.length==2)
                map.put(parts[0],URLDecoder.decode(parts[1],StandardCharsets.UTF_8));
        }
        return map;
    }

    static void send(HttpExchange ex,String response)throws IOException{
        ex.getResponseHeaders().set("Content-Type","text/html; charset=UTF-8");
        byte[] data=response.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200,data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }
}
