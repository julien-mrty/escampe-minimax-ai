package escampe;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class JoueurSuperFort implements IJoueur {
    private int couleur;
    private EscampeBoard escampeBoard;
    private boolean phaseInitiale = true;
    private final String initPosBottom = "F6/E6/F5/A5/C5/E5";
    private final String initPosTop = "A1/B1/A2/B2/C2/F2";
    private final String[] logNames = {"[SuperFort]", "[Ennemi]"};
    private final int minMaxDepth = 10;

    @Override
    public void initJoueur(int mycolour) {
        this.couleur = mycolour;
        this.escampeBoard = new EscampeBoard();
    }

    @Override
    public int getNumJoueur() {
        return couleur;
    }

    @Override
    public String choixMouvement() {
        this.printLogsBeforeMove(this.logNames[0]);

        if (phaseInitiale) return initialPhaseMovement();
        else return inGameMovement();
    }

    public String inGameMovement() {
        List<String> possibleMoves = this.escampeBoard.possiblesMoves(this.getCouleurString());
        System.out.println("[SuperFort] Movements possibles : " + possibleMoves);

        String chosenMovement = "E";
        if (!possibleMoves.isEmpty()) {
            chosenMovement = this.findBestMove(minMaxDepth);
        }
        System.out.println("[SuperFort] log mouvement : " + chosenMovement);

        this.escampeBoard.play(chosenMovement, this.getCouleurString());
        this.printLogsAfterMove(this.logNames[0], chosenMovement);

        return chosenMovement;
    }

    private String findBestMove(int depth) {
        List<String> moves = this.escampeBoard.possiblesMoves(getCouleurString());
        int bestValue = Integer.MIN_VALUE;
        int randomIndex = ThreadLocalRandom.current().nextInt(moves.size());
        // We randomly chose a move from the possible moves in case no move is advantageous
        String bestMove = moves.get(randomIndex);
        boolean randomMove = true;

        for (String move : moves) {
            EscampeBoard simulated = this.escampeBoard.clone();
            simulated.play(move, getCouleurString());

            int value = minMax(simulated, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
                randomMove = false;
            }
        }

        if (randomMove) System.out.println("=== Random move, algorithm should be upgraded ===");

        return bestMove;
    }

    private int minMax(EscampeBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth == 0 || board.gameOver()) {
            return evaluate(board, getCouleurString());
        }

        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;

            for (String move : board.possiblesMoves(getCouleurString())) {
                EscampeBoard simulated = board.clone();
                simulated.play(move, getCouleurString());

                int eval = minMax(simulated, depth - 1, alpha, beta, false);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }

            return maxEval;
        }
        else {
            int minEval = Integer.MAX_VALUE;

            for (String move : board.possiblesMoves(getCouleurEnnemiString())) {
                EscampeBoard simulated = board.clone();
                simulated.play(move, getCouleurEnnemiString());

                int eval = minMax(simulated, depth - 1, alpha, beta, true);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }

            return minEval;
        }
    }

    private int evaluate(EscampeBoard board, String player) {
        int score = 0;
        String enemy = (player.equals("noir")) ? "blanc" : "noir";

        // 1. Menaces immédiates et critique
        if (canCaptureLicorne(board, player)) {
            return Integer.MAX_VALUE - 1; // Coup gagnant
        }
        if (canCaptureLicorne(board, enemy)) {
            return Integer.MIN_VALUE + 1; // Défaite imminente
        }

        // 2. Position relative des licornes
        Position myLicorne = findLicorne(board, player);
        Position enemyLicorne = findLicorne(board, enemy);

        // 3. Distance des paladins à la licorne ennemie (PRIORITAIRE)
        List<Position> myPaladins = getPaladins(board, player);
        for (Position p : myPaladins) {
            int dist = optimizedDistance(board, p, enemyLicorne);
            score += (50 / (dist + 1)); // Bonus exponentiel pour proximité
        }

        // 4. Sécurité de la licorne alliée
        List<Position> enemyPaladins = getPaladins(board, enemy);
        for (Position p : enemyPaladins) {
            int dist = optimizedDistance(board, p, myLicorne);
            score -= (30 / (dist + 1));
        }

        // 5. Contrôle des liserés stratégiques
        int[][] liseres = board.getLiseres();
        for (Position p : myPaladins) {
            score += liseres[p.row][p.col] * 3;
        }

        // 6. Mobilité future (anticipation des contraintes)
        int nextLisere = board.getLastMoveLisere();
        score += countAccessibleTiles(board, player, nextLisere) * 2;

        return score;
    }

    private boolean canCaptureLicorne(EscampeBoard board, String player) {
        String enemy = player.equals("noir") ? "blanc" : "noir";
        Position enemyLicorne = findLicorne(board, enemy);
        if (enemyLicorne == null) return true; // Licorne déjà capturée

        List<Position> myPaladins = getPaladins(board, player);
        String enemyLicornePos = this.positionToNotation(enemyLicorne.row, enemyLicorne.col);

        for (Position p : myPaladins) {
            String move = this.positionToNotation(p.row, p.col) + "-" + enemyLicornePos;
            if (board.isValidMove(move, player)) {
                return true;
            }
        }
        return false;
    }

    private Position findLicorne(EscampeBoard board, String player) {
        char target = player.equals("noir") ? 'N' : 'B';

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                if (board.getBoardCell(row, col) == target) {
                    return new Position(row, col);
                }
            }
        }
        return null; // Si non trouvée (fin de partie)
    }

    private List<Position> getPaladins(EscampeBoard board, String player) {
        List<Position> paladins = new ArrayList<>();
        char target = player.equals("noir") ? 'n' : 'b';

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                if (board.getBoardCell(row, col) == target) {
                    paladins.add(new Position(row, col));
                }
            }
        }
        return paladins;
    }

    private int findShortestPathLength(EscampeBoard board, Position start, Position end) {
        Set<Position> visited = new HashSet<>();
        Queue<Position> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start);

        int depth = 0;

        while(!queue.isEmpty()) {
            int levelSize = queue.size(); // Number of nodes at current depth

            for (int i = 0; i < levelSize; i++) {
                Position current = queue.poll();

                if (current != null) {
                    for (Direction dir : Direction.values()) {
                        Position next = dir.move(current);

                        if (next.isValid()
                                && !visited.contains(next)
                                && board.isNextPosValid(board.getBoardCell(next.row, next.col))
                                && !visited.contains(next)) {

                            queue.add(next);
                            visited.add(next);
                        }
                    }
                }
            }
        }

        return depth;
    }

    private int optimizedDistance(EscampeBoard board, Position a, Position b) {
        // Combine distance Manhattan et chemin réel
        int manhattan = Math.abs(a.row - b.row) + Math.abs(a.col - b.col);
        //int pathLength = findShortestPathLength(board, a, b); // Via BFS
        //return (manhattan + pathLength) / 2;
        return manhattan;
    }

    public String positionToNotation(int row, int col) {
        // Convertit les coordonnées (row, col) en notation de jeu (ex: 0,0 -> A1)
        if (row < 0 || row >= 6 || col < 0 || col >= 6) {
            throw new IllegalArgumentException("Coordonnées invalides: row=" + row + ", col=" + col);
        }

        char columnChar = (char) ('A' + col);
        int rowNumber = row + 1; // Les lignes sont numérotées de 1 à 6
        return String.valueOf(columnChar) + rowNumber;
    }

    public Position notationToPosition(String notation) {
        if (notation == null || notation.length() < 2) {
            throw new IllegalArgumentException("Format invalide: " + notation);
        }

        char colChar = notation.charAt(0);
        int col = colChar - 'A';

        int row = Integer.parseInt(notation.substring(1)) - 1;

        if (col < 0 || col >= 6 || row < 0 || row >= 6) {
            throw new IllegalArgumentException("Position hors plateau: " + notation);
        }

        return new Position(row, col);
    }

    private int countAccessibleTiles(EscampeBoard board, String player, int nextLisere) {
        int score = 0;
        List<String> possibleMoves = board.possiblesMoves(player);
        int[][] liseres = board.getLiseres();

        for (String move : possibleMoves) {
            String[] moveSplit = move.split("-");
            String endPos = moveSplit[1];

            Position p = notationToPosition(endPos);
            if (nextLisere == liseres[p.row][p.col]) score += nextLisere;
        }

        return score;
    }

    @Override
    public void declareLeVainqueur(int colour) {
        System.out.println("[SuperFort] Vainqueur : " + (colour == BLANC ? "Blanc" : "Noir"));
    }

    @Override
    public void mouvementEnnemi(String coup) {
        this.printLogsBeforeMove(this.getCouleurEnnemiString());
        System.out.println("[Ennemi] Coup ennemi possibles : " + this.escampeBoard.possiblesMoves(this.getCouleurEnnemiString()));
        this.escampeBoard.play(coup, this.getCouleurEnnemiString());
        this.printLogsAfterMove(this.logNames[1], coup);
    }

    public String initialPhaseMovement() {
        phaseInitiale = false;

        if (this.couleur == 1) { // If I'm black I always start
            this.escampeBoard.play(initPosBottom, this.getCouleurString());
            this.printLogsAfterMove(this.logNames[0], initPosBottom);

            return initPosBottom; // We arbitrary choose to start at the bottom as it doesn't matter
        } else { // If I'm white I need to check the side black player chose, then place my pawn
            if (this.escampeBoard.checkInitSide().equals("Top")) {
                System.out.println("initialPhaseMovement TOP chosen");
                this.escampeBoard.play(initPosBottom, this.getCouleurString());
                this.printLogsAfterMove(this.logNames[0], initPosBottom);
                return initPosBottom;
            } else {
                this.escampeBoard.play(initPosTop, this.getCouleurString());
                this.printLogsAfterMove(this.logNames[0], initPosTop);
                return initPosTop;
            }
        }
    }

    public void printLogsBeforeMove(String joueur) {
        System.out.println(joueur + " Plateau avant le coup :");
        this.escampeBoard.printBoard();
    }

    public void printLogsAfterMove(String joueur, String coup) {
        System.out.println(joueur + " Coup choisi : " + coup);
        System.out.println(joueur + " Plateau après le coup :");
        this.escampeBoard.printBoard();
        System.out.println();
    }

    public String getCouleurString() {
        return this.couleur == -1 ? "blanc" : "noir";
    }

    public String getCouleurEnnemiString() {
        return this.couleur == -1 ? "noir" : "blanc";
    }

    @Override
    public String binoName() {
        return "SuperFortDuTurfu";
    }
}