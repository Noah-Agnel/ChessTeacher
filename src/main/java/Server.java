import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;
import java.util.List;

public class Server {
    static Partie partie = new Partie();
    
    static String os = System.getProperty("os.name").toLowerCase();
    static String nomFichier = os.contains("win") ? "stockfish-windows.exe" : "./stockfish-macos-universal";
    
    static StockfishAI ai = new StockfishAI(nomFichier);
    static int niveauIA = 1500; // Niveau par défaut

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

        // Route pour faire jouer l'IA 
        server.createContext("/api/ai-move", ex -> {
            // On configure le niveau
            ai.setNiveau(niveauIA);

            // On récupère la position actuelle au format FEN générée par ton code Partie
            String fenActuelle = partie.fen();

            // On demande à Stockfish de trouver le coup (réflexion de 500 ms)
            String meilleurCoup = ai.trouverMeilleurCoup(fenActuelle, 500); // ex: "e7e5"

            if (meilleurCoup != null && meilleurCoup.length() >= 4) {
                String from = meilleurCoup.substring(0, 2);
                String to = meilleurCoup.substring(2, 4);
                String promo = meilleurCoup.length() > 4 ? String.valueOf(meilleurCoup.charAt(4)) : null;

                // On joue le coup dans ton moteur Partie existant !
                partie.jouer(from, to, promo);
            }
            send(ex, 200, "application/json", etat());
        });

        // Nouvelle route pour changer le niveau
        server.createContext("/api/level", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("\"elo\"\\s*:\\s*(\\d+)").matcher(body);
            if (m.find()) {
                niveauIA = Integer.parseInt(m.group(1));
            }
            send(ex, 200, "application/json", "{\"status\":\"ok\"}");
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