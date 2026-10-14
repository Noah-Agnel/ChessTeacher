import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;

public class Server {
    static Plateau plateau = new Plateau();
    static String trait = "Blanc";

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", ex -> {
            byte[] html = Files.readAllBytes(Path.of("src/main/resources/public/index.html"));
            send(ex, 200, "text/html; charset=utf-8", html);
        });

        server.createContext("/api/state", ex ->
            send(ex, 200, "application/json", json(fen())));

        server.createContext("/api/move", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            boolean ok = false;
            Matcher f = Pattern.compile("\"from\"\\s*:\\s*\"([a-h][1-8])\"").matcher(body);
            Matcher t = Pattern.compile("\"to\"\\s*:\\s*\"([a-h][1-8])\"").matcher(body);
            if (f.find() && t.find()) ok = jouer(f.group(1), t.group(1));
            send(ex, ok ? 200 : 400, "application/json", json(fen()));
        });

        server.start();
        System.out.println("http://localhost:8080");
    }

    static boolean jouer(String from, String to) {
        int c1 = from.charAt(0) - 'a', l1 = from.charAt(1) - '1';
        int c2 = to.charAt(0) - 'a',   l2 = to.charAt(1) - '1';

        Piece p = plateau.getPiece(c1, l1);
        if (p == null || !p.getCouleur().equals(trait)) return false;
        if (!p.deplacementValide(c2, l2)) return false;

        Piece cible = plateau.getPiece(c2, l2);
        if (cible != null && cible.getCouleur().equals(p.getCouleur())) return false;

        if (p instanceof Pion) {
            boolean droit = c1 == c2;
            if (droit && cible != null) return false;   // un pion ne capture pas tout droit
            if (!droit && cible == null) return false;  // diagonale = capture uniquement
        }

        if (!(p instanceof Cavalier)) {                 // chemin libre
            int dc = Integer.signum(c2 - c1), dl = Integer.signum(l2 - l1);
            int c = c1 + dc, l = l1 + dl;
            while (c != c2 || l != l2) {
                if (plateau.getPiece(c, l) != null) return false;
                c += dc; l += dl;
            }
        }

        plateau.poserPiece(null, c1, l1);
        plateau.poserPiece(p, c2, l2);
        trait = trait.equals("Blanc") ? "Noir" : "Blanc";
        return true;
    }

    static String fen() {
        StringBuilder sb = new StringBuilder();
        for (int l = 7; l >= 0; l--) {
            int vides = 0;
            for (int c = 0; c < 8; c++) {
                Piece p = plateau.getPiece(c, l);
                if (p == null) { vides++; continue; }
                if (vides > 0) { sb.append(vides); vides = 0; }
                char ch = p instanceof Tour ? 'R' : p instanceof Cavalier ? 'N'
                        : p instanceof Fou ? 'B' : p instanceof Reine ? 'Q'
                        : p instanceof Roi ? 'K' : 'P';
                sb.append(p.getCouleur().equals("Blanc") ? ch : Character.toLowerCase(ch));
            }
            if (vides > 0) sb.append(vides);
            if (l > 0) sb.append('/');
        }
        return sb + (trait.equals("Blanc") ? " w" : " b");
    }

    static String json(String s) { return "\"" + s + "\""; }

    static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
    static void send(HttpExchange ex, int code, String type, String s) throws IOException {
        send(ex, code, type, s.getBytes(StandardCharsets.UTF_8));
    }
}