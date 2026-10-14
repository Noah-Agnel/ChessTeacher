# ChessTeacher

Un jeu d'échecs jouable dans le navigateur, géré par un serveur Java. Le serveur garde l'état de la partie et applique les règles ; la page web affiche l'échiquier.

## Fonctionnalités

- Validation complète des coups légaux : déplacement des pièces, chemins bloqués, prises
- Détection de l'échec, de l'échec et mat et du pat
- Roque, prise en passant et promotion du pion
- Des points sur toutes les cases légales quand on sélectionne une pièce
- Liste des coups à droite en notation algébrique standard (`e4`, `Nf3`, `exd5`, `O-O`, `e8=Q`, `+` et `#`)
- Navigation dans la partie avec les flèches précédent / suivant ; le coup affiché est surligné dans la liste
- Bouton pour recommencer la partie

## Prérequis

- **JDK 11 ou plus récent** (vérifier avec `java -version` et `javac -version`)
- Un navigateur web

## Lancer le projet

Si le projet n'est pas déjà cloné:

```bash
git clone https://github.com/Noah-Agnel/ChessTeacher.git
cd ChessTeacher

javac -d out src/main/java/*.java
java -cp out Server
```

Sinon, il suffit de se rendre dans le répertoire, récupérer des potentiels modifications, puis lancer le code:

```bash
cd chemin/vers/ChessTeacher #attention il faut mettre le bon chemin par exemple bureau/ChessTeacher
git pull

javac -d out src/main/java/*.java
java -cp out Server
```


Le terminal affiche `http://localhost:8080`. Ouvrez cette adresse dans votre navigateur.

> Attention: si les commandes sont lancés depuis un autre dossier que la racine du projet (le dossier `ChessTeacher`). Une erreur sera provoqué car les fichiers seront cherchés au mauvais endroit.

Pour arrêter le serveur, appuyez sur `Ctrl+C` dans le terminal.

## Comment jouer

- **Déplacer une pièce :** cliquez sur la pièce, puis sur une case indiquée. Un petit point marque une case libre, un anneau marque une pièce que vous pouvez capturer. Les Blancs jouent en premier.
- **Changer d'avis :** cliquez de nouveau sur la pièce sélectionnée pour la désélectionner, ou cliquez sur une autre de vos pièces pour changer de sélection.
- **Roque :** cliquez sur votre roi, puis sur la case située deux colonnes plus loin (par exemple `g1` ou `c1` pour les Blancs).
- **Prise en passant :** cliquez sur votre pion, puis sur la case vide en diagonale, derrière le pion adverse qui vient d'avancer de deux cases.
- **Promotion :** quand un pion atteint la dernière rangée, choisissez la nouvelle pièce (dame, tour, fou ou cavalier).
- **Revoir la partie :** utilisez les boutons ◀ et ▶ ou les flèches gauche / droite du clavier. Le coup correspondant est surligné dans la liste. Quand vous regardez une position passée, l'échiquier est en lecture seule ; revenez au dernier coup pour continuer à jouer.
- **Recommencer :** le bouton Restart remet l'échiquier et la liste des coups à zéro.

## Structure du projet

```
src/main/java/
  Piece.java       Classes des pièces et leurs règles de déplacement
  Plateau.java     L'échiquier 8x8
  Partie.java      Logique de jeu : légalité des coups, échec, roque,
                   prise en passant, promotion, historique, notation
  Server.java      Serveur HTTP et API JSON (point d'entrée)
src/main/resources/public/
  index.html       Échiquier, liste des coups et boutons (HTML + JavaScript)
```
