import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;
import java.util.List;
import java.util.Locale;

public class Server {
    static Partie partie = new Partie();
    
    static String os = System.getProperty("os.name").toLowerCase();
    static String nomFichier = os.contains("win") ? "stockfish-windows.exe" : "./stockfish-macos-universal";
    
    static StockfishAI ai = new StockfishAI(nomFichier);
    static int niveauIA = 1500; // Niveau par défaut (Elo, de ELO_MIN à ELO_MAX)
    static final int ELO_MIN = 200, ELO_MAX = 3000;
    // Stockfish n'accepte pas d'Elo en dessous de 1320 (voir stockfish/src/search.h)
    static final int ELO_MIN_STOCKFISH = 1320;

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
            // On configure le niveau (Stockfish ne descend pas sous 1320)
            ai.setNiveau(Math.max(niveauIA, ELO_MIN_STOCKFISH));

            // En dessous de 1320, on affaiblit le bot en jouant parfois un coup légal au hasard :
            // 0 % de coups au hasard à 1320 Elo, jusqu'à 90 % à 200 Elo.
            double probaHasard = niveauIA < ELO_MIN_STOCKFISH
                ? 0.9 * (ELO_MIN_STOCKFISH - niveauIA) / (double) (ELO_MIN_STOCKFISH - ELO_MIN)
                : 0;

            String meilleurCoup;
            List<String> coups = partie.tousLesCoupsLegaux();
            if (!coups.isEmpty() && Math.random() < probaHasard) {
                meilleurCoup = coups.get((int) (Math.random() * coups.size()));
            } else {
                // On demande à Stockfish de trouver le coup (réflexion de 500 ms)
                meilleurCoup = ai.trouverMeilleurCoup(partie.fen(), 500); // ex: "e7e5"
            }

            if (meilleurCoup != null && meilleurCoup.length() >= 4) {
                String from = meilleurCoup.substring(0, 2);
                String to = meilleurCoup.substring(2, 4);
                // Stockfish renvoie la promotion en minuscule ("e7e8q"), Partie attend "Q"
                String promo = meilleurCoup.length() > 4 ? String.valueOf(meilleurCoup.charAt(4)).toUpperCase() : null;

                // On joue le coup dans ton moteur Partie existant !
                partie.jouer(from, to, promo);
            }
            send(ex, 200, "application/json", etat());
        });

        // Meilleur coup (Stockfish à pleine force) pour la position actuellement affichée,
        // y compris quand on revient en arrière avec les flèches. Le coup n'est pas joué.
        server.createContext("/api/best-move", ex -> {
            int coup = partie.getCurseur();
            String fen = partie.fen();
            String base = "{\"coup\":" + coup + ",\"fen\":\"" + fen + "\"";

            String statut = partie.getStatut();
            if (statut.equals("mat") || statut.equals("pat")) {
                send(ex, 200, "application/json", base + ",\"fin\":true}");
                return;
            }

            StockfishAI.Analyse a = ai.analyser(fen, 1000); // 1 s de réflexion
            if (a == null) {
                send(ex, 503, "application/json", base + ",\"erreur\":\"Stockfish indisponible\"}");
                return;
            }

            // Stockfish donne le score du point de vue du joueur au trait ; on l'affiche
            // du point de vue des Blancs, comme chess.com (+ = avantage Blanc).
            int signe = partie.getTrait().equals("Blanc") ? 1 : -1;
            String eval;
            if (a.mat != null) {
                int m = signe * a.mat;
                eval = (m > 0 ? "M" : "-M") + Math.abs(m);
            } else {
                int cp = signe * (a.centipions == null ? 0 : a.centipions);
                eval = String.format(Locale.US, "%+.2f", cp / 100.0);
            }

            send(ex, 200, "application/json", base
                + ",\"uci\":\"" + a.coup + "\""
                + ",\"san\":\"" + partie.notationCoup(a.coup) + "\""
                + ",\"eval\":\"" + eval + "\""
                + ",\"profondeur\":" + a.profondeur + "}");
        });

        // Nouvelle route pour changer le niveau
        server.createContext("/api/level", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("\"elo\"\\s*:\\s*(\\d+)").matcher(body);
            if (m.find()) {
                niveauIA = Math.max(ELO_MIN, Math.min(ELO_MAX, Integer.parseInt(m.group(1))));
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
             + "\"elo\":" + niveauIA + ","
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