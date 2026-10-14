import java.util.ArrayList;
import java.util.List;

public class Partie {
    private Plateau plateau = new Plateau();
    private final List<String> historique = new ArrayList<>(); // "e2e4", "e7e8Q"
    private final List<String> notations = new ArrayList<>();   // "e4", "Nf3", "O-O"...
    private int curseur = 0;                                    // nombre de coups affichés
    private String statut = "ok";                               // ok, echec, mat, pat

    public String getTrait()     { return curseur % 2 == 0 ? "Blanc" : "Noir"; }
    public String getStatut()    { return statut; }
    public int getCurseur()      { return curseur; }
    public int getTotal()        { return historique.size(); }
    public boolean estEnDirect() { return curseur == historique.size(); }
    public List<String> getNotations() { return notations; }

    // ---------- Navigation ----------

    public void reculer()  { if (curseur > 0) { curseur--; reconstruire(); } }
    public void avancer()  { if (curseur < historique.size()) { curseur++; reconstruire(); } }

    public void recommencer() {
        historique.clear();
        notations.clear();
        curseur = 0;
        reconstruire();
    }

    // Coups joués jusqu'à la position affichée
    private List<String> joues() { return historique.subList(0, curseur); }

    private void reconstruire() {
        plateau = new Plateau();
        for (String m : joues()) {
            appliquer(plateau, m.charAt(0) - 'a', m.charAt(1) - '1',
                               m.charAt(2) - 'a', m.charAt(3) - '1',
                               m.length() > 4 ? m.charAt(4) : (char) 0);
        }
        majStatut();
    }

    // ---------- Jouer un coup ----------

    public boolean jouer(String from, String to, String promotion) {
        if (!estEnDirect() || statut.equals("mat") || statut.equals("pat")) return false;

        int c1 = from.charAt(0) - 'a', l1 = from.charAt(1) - '1';
        int c2 = to.charAt(0) - 'a',   l2 = to.charAt(1) - '1';

        Piece p = plateau.getPiece(c1, l1);
        if (p == null || !p.getCouleur().equals(getTrait())) return false;
        if (!coupLegal(c1, l1, c2, l2)) return false;

        char promo = 0;
        if (p instanceof Pion && l2 == (p.getCouleur().equals("Blanc") ? 7 : 0)) {
            promo = (promotion != null && promotion.length() == 1 && "QRBN".contains(promotion))
                    ? promotion.charAt(0) : 'Q';
        }

        String san = notation(p, c1, l1, c2, l2, promo); // calculée AVANT d'appliquer le coup

        historique.add(from + to + (promo != 0 ? String.valueOf(promo) : ""));
        curseur++;
        appliquer(plateau, c1, l1, c2, l2, promo);
        majStatut();

        notations.add(san + (statut.equals("mat") ? "#" : statut.equals("echec") ? "+" : ""));
        return true;
    }

    // Cases où la pièce en `from` peut aller (coups légaux uniquement)
    public List<String> coupsLegaux(String from) {
        List<String> res = new ArrayList<>();
        if (!estEnDirect() || statut.equals("mat") || statut.equals("pat")) return res;

        int c1 = from.charAt(0) - 'a', l1 = from.charAt(1) - '1';
        Piece p = plateau.getPiece(c1, l1);
        if (p == null || !p.getCouleur().equals(getTrait())) return res;

        for (int l2 = 0; l2 < 8; l2++) {
            for (int c2 = 0; c2 < 8; c2++) {
                if (coupLegal(c1, l1, c2, l2)) res.add("" + (char) ('a' + c2) + (l2 + 1));
            }
        }
        return res;
    }

    // Notation algébrique standard : e4, Nf3, Bxc6, exd5, Nbd2, O-O, e8=Q
    private String notation(Piece p, int c1, int l1, int c2, int l2, char promo) {
        if (p instanceof Roi && Math.abs(c2 - c1) == 2) {
            return c2 > c1 ? "O-O" : "O-O-O";
        }

        boolean prise = plateau.getPiece(c2, l2) != null
                     || (p instanceof Pion && c1 != c2);      // en passant inclus
        String dest = "" + (char) ('a' + c2) + (l2 + 1);
        StringBuilder sb = new StringBuilder();

        if (p instanceof Pion) {
            if (prise) sb.append((char) ('a' + c1)).append('x');
            sb.append(dest);
            if (promo != 0) sb.append('=').append(promo);
            return sb.toString();
        }

        sb.append(Plateau.type(p));

        // désambiguïsation : une autre pièce identique peut-elle aller sur la même case ?
        boolean ambigu = false, memeColonne = false, memeLigne = false;
        for (int l = 0; l < 8; l++) {
            for (int c = 0; c < 8; c++) {
                if (c == c1 && l == l1) continue;
                Piece q = plateau.getPiece(c, l);
                if (q != null && Plateau.type(q) == Plateau.type(p)
                        && q.getCouleur().equals(p.getCouleur())
                        && coupLegal(c, l, c2, l2)) {
                    ambigu = true;
                    if (c == c1) memeColonne = true;
                    if (l == l1) memeLigne = true;
                }
            }
        }
        if (ambigu) {
            if (!memeColonne)    sb.append((char) ('a' + c1));
            else if (!memeLigne) sb.append(l1 + 1);
            else                 sb.append((char) ('a' + c1)).append(l1 + 1);
        }

        if (prise) sb.append('x');
        return sb.append(dest).toString();
    }

    // Exécute un coup (supposé légal) : gère prise en passant, roque, promotion
    private void appliquer(Plateau pl, int c1, int l1, int c2, int l2, char promo) {
        Piece p = pl.getPiece(c1, l1);
        Piece cible = pl.getPiece(c2, l2);

        // prise en passant : pion en diagonale vers une case vide
        if (p instanceof Pion && c1 != c2 && cible == null) {
            pl.poserPiece(null, c2, l1);
        }
        // roque : le roi se déplace de deux colonnes, la tour suit
        if (p instanceof Roi && Math.abs(c2 - c1) == 2) {
            int cTour = c2 > c1 ? 7 : 0;
            int cTourDest = c2 > c1 ? c2 - 1 : c2 + 1;
            Piece tour = pl.getPiece(cTour, l1);
            pl.poserPiece(null, cTour, l1);
            pl.poserPiece(tour, cTourDest, l1);
        }

        pl.poserPiece(null, c1, l1);
        if (p instanceof Pion && promo != 0) {
            pl.poserPiece(Plateau.creer(promo, p.getCouleur(), c2, l2), c2, l2);
        } else {
            pl.poserPiece(p, c2, l2);
        }
    }

    private void majStatut() {
        String t = getTrait();
        boolean echec = roiEnEchec(plateau, t);
        boolean coups = aDesCoupsLegaux(t);
        if (!coups) statut = echec ? "mat" : "pat";
        else        statut = echec ? "echec" : "ok";
    }

    // ---------- Règles ----------

    // Mouvement de la pièce, chemin libre, captures, roque, en passant (sans regarder l'échec)
    private boolean coupPseudoLegal(int c1, int l1, int c2, int l2) {
        Piece p = plateau.getPiece(c1, l1);
        if (p == null) return false;

        if (p instanceof Roi && l1 == l2 && Math.abs(c2 - c1) == 2) {
            return roquePossible(c1, l1, c2, l2);
        }
        if (!p.deplacementValide(c2, l2)) return false;

        Piece cible = plateau.getPiece(c2, l2);
        if (cible != null && cible.getCouleur().equals(p.getCouleur())) return false;

        if (p instanceof Pion) {
            boolean droit = c1 == c2;
            if (droit && cible != null) return false;
            if (!droit && cible == null && !enPassantPossible(c1, l1, c2)) return false;
        }
        return cheminLibre(plateau, p, c2, l2);
    }

    // Pseudo-légal ET le roi n'est pas en échec après le coup
    private boolean coupLegal(int c1, int l1, int c2, int l2) {
        if (!coupPseudoLegal(c1, l1, c2, l2)) return false;
        String couleur = plateau.getPiece(c1, l1).getCouleur();
        Plateau essai = plateau.copie();
        appliquer(essai, c1, l1, c2, l2, (char) 0);
        return !roiEnEchec(essai, couleur);
    }

    // En passant : le dernier coup est une avance de 2 d'un pion adverse, juste à côté de nous
    private boolean enPassantPossible(int c1, int l1, int c2) {
        List<String> h = joues();
        if (h.isEmpty()) return false;
        String m = h.get(h.size() - 1);
        int mc1 = m.charAt(0) - 'a', ml1 = m.charAt(1) - '1';
        int mc2 = m.charAt(2) - 'a', ml2 = m.charAt(3) - '1';

        Piece p = plateau.getPiece(c1, l1);
        Piece adv = plateau.getPiece(c2, l1);
        return adv instanceof Pion && !adv.getCouleur().equals(p.getCouleur())
            && mc1 == c2 && mc2 == c2 && ml2 == l1 && Math.abs(ml2 - ml1) == 2;
    }

    // Roque : décidé à partir de l'historique, sans variable "déjà bougé"
    private boolean roquePossible(int c1, int l1, int c2, int l2) {
        Piece roi = plateau.getPiece(c1, l1);
        String couleur = roi.getCouleur();
        int ligneBase = couleur.equals("Blanc") ? 0 : 7;
        if (c1 != 4 || l1 != ligneBase) return false;

        boolean petit = c2 == 6;
        if (!petit && c2 != 2) return false;
        int cTour = petit ? 7 : 0;

        Piece tour = plateau.getPiece(cTour, l1);
        if (!(tour instanceof Tour) || !tour.getCouleur().equals(couleur)) return false;

        // ni le roi ni cette tour n'ont bougé, et la tour n'a pas été capturée
        String caseRoi = "" + (char) ('a' + c1) + (l1 + 1);
        String caseTour = "" + (char) ('a' + cTour) + (l1 + 1);
        for (String m : joues()) {
            String de = m.substring(0, 2), vers = m.substring(2, 4);
            if (de.equals(caseRoi) || de.equals(caseTour) || vers.equals(caseTour)) return false;
        }

        // cases entre le roi et la tour vides
        for (int c = Math.min(c1, cTour) + 1; c < Math.max(c1, cTour); c++) {
            if (plateau.getPiece(c, l1) != null) return false;
        }

        // roi pas en échec, et ne traverse / n'arrive pas sur une case attaquée
        int milieu = (c1 + c2) / 2;
        return !estAttaquee(plateau, c1, l1, couleur)
            && !estAttaquee(plateau, milieu, l1, couleur)
            && !estAttaquee(plateau, c2, l1, couleur);
    }

    private boolean roiEnEchec(Plateau pl, String couleur) {
        int[] roi = pl.trouverRoi(couleur);
        return roi != null && estAttaquee(pl, roi[0], roi[1], couleur);
    }

    // La case (c, l) est-elle attaquée par l'adversaire de `couleur` ?
    private boolean estAttaquee(Plateau pl, int c, int l, String couleur) {
        for (int l2 = 0; l2 < 8; l2++) {
            for (int c2 = 0; c2 < 8; c2++) {
                Piece p = pl.getPiece(c2, l2);
                if (p != null && !p.getCouleur().equals(couleur) && attaque(pl, p, c, l)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attaque(Plateau pl, Piece p, int c, int l) {
        if (p instanceof Pion) { // un pion n'attaque qu'en diagonale
            int dir = p.getCouleur().equals("Blanc") ? 1 : -1;
            return Math.abs(c - p.getColonne()) == 1 && l - p.getLigne() == dir;
        }
        return p.deplacementValide(c, l) && cheminLibre(pl, p, c, l);
    }

    private boolean aDesCoupsLegaux(String couleur) {
        for (int l1 = 0; l1 < 8; l1++) {
            for (int c1 = 0; c1 < 8; c1++) {
                Piece p = plateau.getPiece(c1, l1);
                if (p == null || !p.getCouleur().equals(couleur)) continue;
                for (int l2 = 0; l2 < 8; l2++) {
                    for (int c2 = 0; c2 < 8; c2++) {
                        if (coupLegal(c1, l1, c2, l2)) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean cheminLibre(Plateau pl, Piece p, int c2, int l2) {
        if (p instanceof Cavalier) return true;
        int c1 = p.getColonne(), l1 = p.getLigne();
        int dc = Integer.signum(c2 - c1), dl = Integer.signum(l2 - l1);
        int c = c1 + dc, l = l1 + dl;
        while (c != c2 || l != l2) {
            if (pl.getPiece(c, l) != null) return false;
            c += dc; l += dl;
        }
        return true;
    }

    // ---------- FEN pour le front ----------

    public String fen() {
        StringBuilder sb = new StringBuilder();
        for (int l = 7; l >= 0; l--) {
            int vides = 0;
            for (int c = 0; c < 8; c++) {
                Piece p = plateau.getPiece(c, l);
                if (p == null) { vides++; continue; }
                if (vides > 0) { sb.append(vides); vides = 0; }
                char ch = Plateau.type(p);
                sb.append(p.getCouleur().equals("Blanc") ? ch : Character.toLowerCase(ch));
            }
            if (vides > 0) sb.append(vides);
            if (l > 0) sb.append('/');
        }
        return sb + (getTrait().equals("Blanc") ? " w" : " b");
    }
}