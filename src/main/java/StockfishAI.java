import java.io.*;

public class StockfishAI {
    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;

    // Résultat d'une analyse : meilleur coup + évaluation (du point de vue du joueur au trait)
    public static class Analyse {
        public String coup;        // ex: "e2e4", "e7e8q"
        public Integer centipions; // ex: 35 = +0.35 (null si mat trouvé)
        public Integer mat;        // ex: 3 = mat en 3, -2 = se fait mater en 2 (null sinon)
        public int profondeur;
    }

    public StockfishAI(String cheminExecutable) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cheminExecutable);
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            // Initialisation du protocole UCI
            sendCommand("uci");
            waitFor("uciok");
            sendCommand("isready");
            waitFor("readyok");
        } catch (IOException e) {
            System.err.println("Impossible de lancer Stockfish (" + cheminExecutable + ") : " + e.getMessage());
            writer = null;
            reader = null;
        }
    }

    public boolean estDisponible() {
        return writer != null && process != null && process.isAlive();
    }

    public synchronized void sendCommand(String command) {
        if (writer == null) return;
        try {
            writer.write(command + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized String waitFor(String token) {
        if (reader == null) return null;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(token)) return line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Définir le niveau de difficulté (Elo ou Skill Level)
    public synchronized void setNiveau(int elo) {
        // Active la limitation de force (disponible sur les versions modernes de Stockfish)
        sendCommand("setoption name UCI_LimitStrength value true");
        sendCommand("setoption name UCI_Elo value " + elo);
    }

    // Demande au moteur de calculer le meilleur coup pour une position FEN donnée
    public synchronized String trouverMeilleurCoup(String fen, int tempsMs) {
        if (!estDisponible()) return null;
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + tempsMs); // Temps de réflexion en millisecondes

        String line = waitFor("bestmove");
        if (line != null) {
            // La réponse de Stockfish ressemble à : "bestmove e2e4 ponder e7e5"
            String[] parts = line.split(" ");
            return parts.length > 1 && !parts[1].equals("(none)") ? parts[1] : null;
        }
        return null;
    }

    // Analyse à pleine force (sans limite d'Elo) : meilleur coup + évaluation.
    // Utilisé par le bouton "Montrer meilleur coup".
    public synchronized Analyse analyser(String fen, int tempsMs) {
        if (!estDisponible()) return null;
        sendCommand("setoption name UCI_LimitStrength value false");
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + tempsMs);

        Analyse a = new Analyse();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("bestmove")) {
                    String[] parts = line.split(" ");
                    if (parts.length < 2 || parts[1].equals("(none)")) return null;
                    a.coup = parts[1];
                    return a;
                }
                // ex: "info depth 18 seldepth 25 multipv 1 score cp 35 nodes ... pv e2e4 e7e5 ..."
                if (line.startsWith("info") && line.contains(" score ")) {
                    String[] t = line.split(" ");
                    for (int i = 0; i < t.length - 1; i++) {
                        if (t[i].equals("depth")) a.profondeur = Integer.parseInt(t[i + 1]);
                        if (t[i].equals("score")) {
                            if (t[i + 1].equals("cp") && i + 2 < t.length) {
                                a.centipions = Integer.parseInt(t[i + 2]); a.mat = null;
                            } else if (t[i + 1].equals("mate") && i + 2 < t.length) {
                                a.mat = Integer.parseInt(t[i + 2]); a.centipions = null;
                            }
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void fermer() {
        sendCommand("quit");
        if (process != null) process.destroy();
    }
}
