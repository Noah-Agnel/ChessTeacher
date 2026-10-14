public class Plateau { 
    private Piece[][] grille; // Grille 8x8
    
    public Plateau() {
        this.grille = new Piece[8][8];
        initialiserPlateau();
    }

    public Piece getPiece(int colonne, int ligne) {
        if (colonne >= 0 && colonne < 8 && ligne >= 0 && ligne < 8) {
            return grille[ligne][colonne];
        }
        return null;
    }

    public void poserPiece(Piece piece, int colonne, int ligne) {
        if (colonne >= 0 && colonne < 8 && ligne >= 0 && ligne < 8) {
            grille[ligne][colonne] = piece;
            if (piece != null) {
                piece.deplacer(colonne, ligne);
            }
        }
    }

    public int[] trouverRoi(String couleur) {
        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                Piece p = grille[l][c];
                if (p instanceof Roi && p.getCouleur().equals(couleur)) {
                    return new int[] { c, l };
                }
            }
        }
        return null;
    }

    public static char type(Piece p) {
        return p instanceof Tour ? 'R' : p instanceof Cavalier ? 'N'
             : p instanceof Fou ? 'B' : p instanceof Reine ? 'Q'
             : p instanceof Roi ? 'K' : 'P';
    }

    public static Piece creer(char t, String couleur, int c, int l) {
        switch (t) {
            case 'R': return new Tour(couleur, c, l);
            case 'N': return new Cavalier(couleur, c, l);
            case 'B': return new Fou(couleur, c, l);
            case 'Q': return new Reine(couleur, c, l);
            case 'K': return new Roi(couleur, c, l);
            default:  return new Pion(couleur, c, l);
        }
    }

    public Plateau copie() {
        Plateau n = new Plateau();
        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                Piece p = grille[l][c];
                n.grille[l][c] = (p == null) ? null : creer(type(p), p.getCouleur(), c, l);
            }
        }
        return n;
    }

    public void initialiserPlateau() {
        // Vider le plateau
        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                grille[l][c] = null;
            }
        }

        // --- PIÈCES BLANCHES (Lignes 0 et 1) ---
        grille[0][0] = new Tour("Blanc", 0, 0);
        grille[0][1] = new Cavalier("Blanc", 1, 0);
        grille[0][2] = new Fou("Blanc", 2, 0);
        grille[0][3] = new Reine("Blanc", 3, 0);
        grille[0][4] = new Roi("Blanc", 4, 0);
        grille[0][5] = new Fou("Blanc", 5, 0);
        grille[0][6] = new Cavalier("Blanc", 6, 0);
        grille[0][7] = new Tour("Blanc", 7, 0);

        for (int c = 0; c < 8; c++) {
            grille[1][c] = new Pion("Blanc", c, 1);
        }

        // --- PIÈCES NOIRES (Lignes 6 et 7) ---
        for (int c = 0; c < 8; c++) {
            grille[6][c] = new Pion("Noir", c, 6);
        }

        grille[7][0] = new Tour("Noir", 0, 7);
        grille[7][1] = new Cavalier("Noir", 1, 7);
        grille[7][2] = new Fou("Noir", 2, 7);
        grille[7][3] = new Reine("Noir", 3, 7);
        grille[7][4] = new Roi("Noir", 4, 7);
        grille[7][5] = new Fou("Noir", 5, 7);
        grille[7][6] = new Cavalier("Noir", 6, 7);
        grille[7][7] = new Tour("Noir", 7, 7);
    }

    // Méthode pour afficher le plateau dans la console
    public void afficher() {
        System.out.println("  0   1   2   3   4   5   6   7");
        for (int l = 7; l >= 0; l--) { // Affiche de haut en bas (vue classique des échecs)
            System.out.print(l + " ");
            for (int c = 0; c < 8; c++) {
                if (grille[l][c] == null) {
                    System.out.print(".   ");
                } else {
                    System.out.print(grille[l][c].getSymbole() + " ");
                }
            }
            System.out.println();
        }
    }

    // Exemple de test pour vérifier l'initialisation
    public static void main(String[] args) {
        Plateau p = new Plateau();
        p.afficher();
    }
}