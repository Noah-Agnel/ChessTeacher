import java.io.*;

public class StockfishAI {
    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;

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
            e.printStackTrace();
        }
    }

    public void sendCommand(String command) {
        try {
            writer.write(command + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String waitFor(String token) {
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
    public void setNiveau(int elo) {
        // Active la limitation de force (disponible sur les versions modernes de Stockfish)
        sendCommand("setoption name UCI_LimitStrength value true");
        sendCommand("setoption name UCI_Elo value " + elo);
    }

    // Demande au moteur de calculer le meilleur coup pour une position FEN donnée
    public String trouverMeilleurCoup(String fen, int tempsMs) {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + tempsMs); // Temps de réflexion en millisecondes
        
        String line = waitFor("bestmove");
        if (line != null) {
            // La réponse de Stockfish ressemble à : "bestmove e2e4 ponder e7e5"
            String[] parts = line.split(" ");
            return parts[1]; // Retourne "e2e4"
        }
        return null;
    }

    public void fermer() {
        sendCommand("quit");
        process.destroy();
    }
}
