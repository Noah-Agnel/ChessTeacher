import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;
import java.util.List;

public class Server {
    static Partie partie = new Partie();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", ex -> {
            byte[] html = Files.readAllBytes(Path.of("src/main/resources/public/index.html"));
            send(ex, 200, "text/html; charset=utf-8", html);
        });

        server.createContext("/api/state", ex -> send(ex, 200, "application/json", etat()));

        server.createContext("/api/move", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean ok = false;
            Matcher f = Pattern.compile("\"from\"\\s*:\\s*\"([a-h][1-8])\"").matcher(body);
            Matcher t = Pattern.compile("\"to\"\\s*:\\s*\"([a-h][1-8])\"").matcher(body);
            Matcher p = Pattern.compile("\"promotion\"\\s*:\\s*\"([QRBN])\"").matcher(body);
            if (f.find() && t.find()) {
                ok = partie.jouer(f.group(1), t.group(1), p.find() ? p.group(1) : null);
            }
            send(ex, ok ? 200 : 400, "application/json", etat());
        });

        server.createContext("/api/undo", ex -> { partie.reculer();     send(ex, 200, "application/json", etat()); });
        server.createContext("/api/redo", ex -> { partie.avancer();     send(ex, 200, "application/json", etat()); });
        server.createContext("/api/restart", ex -> { partie.recommencer(); send(ex, 200, "application/json", etat()); });
        server.createContext("/api/legal", ex -> {
            String q = ex.getRequestURI().getQuery();   // ex: from=e2
            StringBuilder sb = new StringBuilder("[");
            if (q != null && q.matches("from=[a-h][1-8]")) {
                List<String> cases = partie.coupsLegaux(q.substring(5));
                for (int i = 0; i < cases.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append('"').append(cases.get(i)).append('"');
                }
            }
            send(ex, 200, "application/json", sb.append("]").toString());
        });

        server.start();
        System.out.println("http://localhost:8080");
    }

    static String etat() {
        StringBuilder coups = new StringBuilder("[");
        for (int i = 0; i < partie.getNotations().size(); i++) {
            if (i > 0) coups.append(',');
            coups.append('"').append(partie.getNotations().get(i)).append('"');
        }
        coups.append(']');

        return "{\"fen\":\"" + partie.fen() + "\","
             + "\"statut\":\"" + partie.getStatut() + "\","
             + "\"trait\":\"" + partie.getTrait() + "\","
             + "\"live\":" + partie.estEnDirect() + ","
             + "\"coup\":" + partie.getCurseur() + ","
             + "\"total\":" + partie.getTotal() + ","
             + "\"notation\":" + coups + "}";
    }

    static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
    static void send(HttpExchange ex, int code, String type, String s) throws IOException {
        send(ex, code, type, s.getBytes(StandardCharsets.UTF_8));
    }
}