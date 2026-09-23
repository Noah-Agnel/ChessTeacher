import java.util.ArrayList;

public abstract class Piece {   
    private String couleur; // "Blanc" ou "Noir"
    protected int colonne;
    protected int ligne;
    
    public Piece(String couleur, int colonne, int ligne) {
        this.couleur = couleur;
        this.colonne = colonne;
        this.ligne = ligne;
    }
        
    public String getCouleur() {
        return couleur;
    }

    public int getColonne() {
        return colonne;
    }

    public int getLigne() {
        return ligne;
    }
    
    public void deplacer(int nouvelleColonne, int nouvelleLigne) {
        this.colonne = nouvelleColonne;
        this.ligne = nouvelleLigne;
    }
    
    // Représentation textuelle de la pièce (ex: P_B pour Pion Blanc)
    public abstract String getSymbole();
    
    // Vérifie si le déplacement respecte la marche théorique de la pièce
    public abstract boolean deplacementValide(int cibleCol, int cibleLigne);
}

// --- SOUS-CLASSES DE PIÈCES ---

class Tour extends Piece {
    public Tour(String couleur, int colonne, int ligne) {
        super(couleur, colonne, ligne);
    }

    @Override
    public String getSymbole() {
        return "T_" + getCouleur().charAt(0);
    }

    @Override
    public boolean deplacementValide(int cibleCol, int cibleLigne) {
        if (this.colonne == cibleCol && this.ligne == cibleLigne) return false;
        return (this.colonne == cibleCol) || (this.ligne == cibleLigne);
    }   
}

class Cavalier extends Piece {
    public Cavalier(String couleur, int colonne, int ligne) {
        super(couleur, colonne, ligne);
    }

    @Override
    public String getSymbole() {
        return "C_" + getCouleur().charAt(0);
    }

    @Override
    public boolean deplacementValide(int cibleCol, int cibleLigne) {
        int diffCol = Math.abs(cibleCol - this.colonne);
        int diffLigne = Math.abs(cibleLigne - this.ligne);
        return (diffCol == 2 && diffLigne == 1) || (diffCol == 1 && diffLigne == 2);
    }
}

class Fou extends Piece {
    public Fou(String couleur, int colonne, int ligne) {
        super(couleur, colonne, ligne);
    }

    @Override
    public String getSymbole() {
        return "F_" + getCouleur().charAt(0);
    }

    @Override
    public boolean deplacementValide(int cibleCol, int cibleLigne) {
        if (this.colonne == cibleCol && this.ligne == cibleLigne) return false;
        int diffCol = Math.abs(cibleCol - this.colonne);
        int diffLigne = Math.abs(cibleLigne - this.ligne);  
        return diffCol == diffLigne;
    }
}

class Reine extends Piece {
    public Reine(String couleur, int colonne, int ligne) {
        super(couleur, colonne, ligne);
    }

    @Override
    public String getSymbole() {
        return "Q_" + getCouleur().charAt(0);
    }

    @Override
    public boolean deplacementValide(int cibleCol, int cibleLigne) {
        if (this.colonne == cibleCol && this.ligne == cibleLigne) return false;
        int diffCol = Math.abs(cibleCol - this.colonne);
        int diffLigne = Math.abs(cibleLigne - this.ligne);
        // La reine combine la Tour (mouvement rectiligne) et le Fou (mouvement diagonal)
        return (this.colonne == cibleCol) || (this.ligne == cibleLigne) || (diffCol == diffLigne);
    }
}

class Roi extends Piece {
    public Roi(String couleur, int colonne, int ligne) {
        super(couleur, colonne, ligne);
    }

    @Override
    public String getSymbole() {
        return "R_" + getCouleur().charAt(0);
    }

    @Override
    public boolean deplacementValide(int cibleCol, int cibleLigne) {
        if (this.colonne == cibleCol && this.ligne == cibleLigne) return false;
        int diffCol = Math.abs(cibleCol - this.colonne);
        int diffLigne = Math.abs(cibleLigne - this.ligne);
        // Le roi se déplace d'une seule case dans toutes les directions
        return diffCol <= 1 && diffLigne <= 1;
    }
}

class Pion extends Piece {
    public Pion(String couleur, int colonne, int ligne) {
        super(couleur, colonne, ligne);
    }

    @Override
    public String getSymbole() {
        return "P_" + getCouleur().charAt(0);
    }

    @Override
    public boolean deplacementValide(int cibleCol, int cibleLigne) {
        if (this.colonne == cibleCol && this.ligne == cibleLigne) return false;
        
        int direction = getCouleur().equalsIgnoreCase("Blanc") ? 1 : -1;
        int diffCol = Math.abs(cibleCol - this.colonne);
        int diffLigne = cibleLigne - this.ligne;

        // Déplacement tout droit
        if (diffCol == 0) {
            // Avancer d'une case
            if (diffLigne == direction) return true;
            // Premier coup : avancer de 2 cases
            if ((getCouleur().equalsIgnoreCase("Blanc") && this.ligne == 1 && diffLigne == 2) ||
                (getCouleur().equalsIgnoreCase("Noir") && this.ligne == 6 && diffLigne == -2)) {
                return true;
            }
        }
        // Capture en diagonale (1 case)
        else if (diffCol == 1 && diffLigne == direction) {
            return true;
        }

        return false;
    }
}