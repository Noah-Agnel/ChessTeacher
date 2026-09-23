import java.util.Scanner;
import java.util.ArrayList;

public abstract class Piece {	
	private String couleur;
	private int LigneCase;
	private int ColonneCase;
	
	public Piece(String couleur, String type, int colonne, int ligne) {
      		this.couleur = couleur;
        	this.type = type;
        	this.colonne = colonne;
        	this.ligne = ligne;
        }
        
        public String getCouleur() {
		return couleur;
	}

	public int getColonne() {
		return ColonneCase;
	}

	public int getLigne() {
		return LigneCase;
	}
    
        public void deplacer(int nouvelleColonne, int nouvelleLigne) {
        	this.colonne = nouvelleColonne;
        	this.ligne = nouvelleLigne;
    	}
	
	public abstract boolean DeplacementValide(int cibleCol, int cibleLigne);
	
	public class Tour extends Piece {
		public boolean DeplacementValide(int cibleCol, int cibleLigne){
			return (this.colonne == cibleCol) || (this.ligne == cibleLigne);
		}	
	}
	public class Cavalier extends Piece {
		public boolean DeplacementValide(int cibleCol, int cibleLigne){
			int diffCol = Math.abs(cibleCol - this.colonne);
       	 		int diffLigne = Math.abs(cibleLigne - this.ligne);
			return (diffCol == 2 && diffLigne == 1) || (diffCol == 1 && diffLigne == 2);
		}
	}
	public class Fou extends Piece {
		public boolean DeplacementValide(int cibleCol, int cibleLigne){
			if (this.colonne == cibleCol && this.ligne == cibleLigne) {
           			return false;
        		}
			int diffCol = Math.abs(cibleCol - this.colonne);
       			int diffLigne = Math.abs(cibleLigne - this.ligne);	
       			return diffCol == diffLigne;
       		}
       	}
	
}
	