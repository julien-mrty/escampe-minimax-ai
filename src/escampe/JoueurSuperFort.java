package escampe;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class JoueurSuperFort implements IJoueur {
    private int color;
    private EscampeBoard escampeBoard;
    private boolean isInitialPhase = true;
    private final String initPosBottom = "F6/E6/F5/C5/D5/B5";
    private final String initPosTop = "F1/A2/C2/E2/F2/D2";
    private final String[] logNames = {"[SuperFort]", "[Ennemi]"};
    private final int minMaxDepthInGame = 10;
    private final int minMaxDepthInitPos = 4;
    private final int nInitPos = 500;

    @Override
    public void initJoueur(int myColor) {
        this.color = myColor;
        this.escampeBoard = new EscampeBoard();
    }

    @Override
    public String choixMouvement() {
        this.printLogsBeforeMove(this.logNames[0]);

        if (isInitialPhase) return initialPhaseMovement();
        else return inGameMovement();
    }

    public String initialPhaseMovement() {
        isInitialPhase = false;

        if (this.color == 1) { // If I'm black, I always start
            this.escampeBoard.play(initPosBottom, this.getCouleurString());
            this.printLogsAfterMove(this.logNames[0], initPosBottom);

            return initPosBottom; // We arbitrarily choose to start at the bottom as it doesn't matter
        }
        else { // If I'm white, I need to check the side black player chose, then place my pawn
            String blackSide = this.escampeBoard.checkInitSide();
            List<String> possiblePositions = new ArrayList<>();

            // Génère toutes les positions initiales possibles selon le côté choisi par le noir
            if (blackSide.equals("Top")) {
                possiblePositions.add(initPosBottom);
                possiblePositions.add("F6/E6/F5/C5/D5/B5"); // Position basique 1
                possiblePositions.add("F6/D6/E5/C5/B5/F5"); // Variante stratégique 1
                possiblePositions.add("E6/F6/D5/C5/E5/B5"); // Variante stratégique 2

                possiblePositions.addAll(generateRandomInitialPositions(blackSide, nInitPos));
            }
            else {
                possiblePositions.add(initPosTop);
                possiblePositions.add("F1/A2/C2/E2/F2/D2"); // Position basique 2
                possiblePositions.add("A1/B1/C2/D2/E2/F2"); // Variante stratégique 3
                possiblePositions.add("B1/A2/C2/D2/F2/E2"); // Variante stratégique 4

                possiblePositions.addAll(generateRandomInitialPositions(blackSide, nInitPos));
            }

            String bestPosition = possiblePositions.get(0);
            int bestScore = Integer.MIN_VALUE;

            // Évaluation MinMax pour chaque configuration possible
            for (String position : possiblePositions) {
                EscampeBoard simulated = this.escampeBoard.clone();
                simulated.play(position, this.getCouleurString());

                int score = minMax(simulated, minMaxDepthInitPos, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

                if (score > bestScore) {
                    bestScore = score;
                    bestPosition = position;
                }
            }

            this.escampeBoard.play(bestPosition, this.getCouleurString());
            this.printLogsAfterMove(this.logNames[0], bestPosition);

            return bestPosition;
        }
    }

    private List<String> generateRandomInitialPositions(String opponentSide, int count) {
        List<String> positions = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            if (opponentSide.equals("Top")) {
                positions.add(generateInitPosition(rand, 5));
            } else {
                positions.add(generateInitPosition(rand, 1));
            }
        }
        return positions;
    }

    private String generateInitPosition(Random rand, int side) {
        // Logique de génération aléatoire pour le bas
        return String.format("%s%d/%s%d/%s%d/%s%d/%s%d/%s%d",
                (char)('A' + rand.nextInt(6)), rand.nextInt(2) + side,
                (char)('A' + rand.nextInt(6)), rand.nextInt(2) + side,
                (char)('A' + rand.nextInt(6)), rand.nextInt(2) + side,
                (char)('A' + rand.nextInt(6)), rand.nextInt(2) + side,
                (char)('A' + rand.nextInt(6)), rand.nextInt(2) + side,
                (char)('A' + rand.nextInt(6)), rand.nextInt(2) + side);
    }

    public String inGameMovement() {
        List<String> possibleMoves = this.escampeBoard.possiblesMoves(this.getCouleurString());
        System.out.println("[SuperFort] Movements possibles : " + possibleMoves);

        String chosenMove = "E";
        if (!possibleMoves.isEmpty()) {
            chosenMove = this.findBestMove(minMaxDepthInGame);
        }
        System.out.println("[SuperFort] log mouvement : " + chosenMove);

        this.escampeBoard.play(chosenMove, this.getCouleurString());
        this.printLogsAfterMove(this.logNames[0], chosenMove);

        return chosenMove;
    }

    private String findBestMove(int depth) {
        List<String> moves = this.escampeBoard.possiblesMoves(getCouleurString());
        int bestValue = Integer.MIN_VALUE;
        boolean isRandomMove = true;
        List<String> topMoves = new ArrayList<>();

        for (String move : moves) {
            EscampeBoard simulated = this.escampeBoard.clone();
            simulated.play(move, getCouleurString());

            int value = minMax(simulated, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            if (value > bestValue) {
                bestValue = value;
                topMoves.clear();
                topMoves.add(move);
            } else if (value == bestValue) {
                topMoves.add(move);
            }
        }

        System.out.println("========== Top moves length : " + topMoves.size());
        // Now pick randomly among the best
        return topMoves.get(ThreadLocalRandom.current().nextInt(topMoves.size()));
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
        } else {
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

        // 1. Immediate and critical threats
        if (canCaptureUnicorn(board, player)) {
            return Integer.MAX_VALUE - 1; // Winning move
        }
        if (canCaptureUnicorn(board, enemy)) {
            return Integer.MIN_VALUE + 1; // Imminent loss
        }

        // 2. Relative position of unicorns
        Position myUnicorn = findUnicorn(board, player);
        Position enemyUnicorn = findUnicorn(board, enemy);

        // 3. Distance of paladins to enemy unicorn (PRIORITY)
        List<Position> myPaladins = getPaladins(board, player);
        for (Position p : myPaladins) {
            int dist = optimizedDistance(board, p, enemyUnicorn);
            score += (50 / (dist + 1)); // Exponential bonus for proximity
        }

        // 4. Safety of allied unicorn
        List<Position> enemyPaladins = getPaladins(board, enemy);
        for (Position p : enemyPaladins) {
            int dist = optimizedDistance(board, p, myUnicorn);
            score -= (30 / (dist + 1));
        }

        // 5. Control of strategic borders
        int[][] borders = board.getLiseres();
        for (Position p : myPaladins) {
            score += borders[p.row][p.col] * 3;
        }

        // 6. Future mobility (anticipation of constraints)
        int nextBorder = board.getLastMoveLisere();
        score += countAccessibleTiles(board, player, nextBorder) * 2;

        return score;
    }

    private boolean canCaptureUnicorn(EscampeBoard board, String player) {
        String enemy = player.equals("noir") ? "blanc" : "noir";
        Position enemyUnicorn = findUnicorn(board, enemy);
        if (enemyUnicorn == null) return true; // Unicorn already captured

        List<Position> myPaladins = getPaladins(board, player);
        String enemyUnicornPos = this.positionToNotation(enemyUnicorn.row, enemyUnicorn.col);

        for (Position p : myPaladins) {
            String move = this.positionToNotation(p.row, p.col) + "-" + enemyUnicornPos;
            if (board.isValidMove(move, player)) {
                return true;
            }
        }
        return false;
    }

    private Position findUnicorn(EscampeBoard board, String player) {
        char target = player.equals("noir") ? 'N' : 'B';

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                if (board.getBoardCell(row, col) == target) {
                    return new Position(row, col);
                }
            }
        }
        return null; // Not found (end of game)
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
        // Early-out if start == end
        if (start.equals(end)) return 0;

        boolean[][] visited = new boolean[6][6];
        Queue<Position> queue = new LinkedList<>();

        queue.add(start);
        visited[start.row][start.col] = true;

        int depth = 0;

        while (!queue.isEmpty()) {
            depth++;
            int levelSize = queue.size();

            for (int i = 0; i < levelSize; i++) {
                Position current = queue.poll();

                for (Direction dir : Direction.values()) {
                    Position next = dir.move(current);

                    if (!next.isValid()
                            || visited[next.row][next.col]
                            || !board.isNextPosValid(board.getBoardCell(next.row, next.col))) {
                        continue;
                    }

                    // Found the target!
                    if (next.equals(end)) {
                        return depth;
                    }

                    visited[next.row][next.col] = true;
                    queue.add(next);
                }
            }
        }

        // If we exhaust the queue without finding end, no path exists
        return Integer.MAX_VALUE;
    }


    private int optimizedDistance(EscampeBoard board, Position a, Position b) {
        // Combine Manhattan distance and actual path
        //int manhattan = Math.abs(a.row - b.row) + Math.abs(a.col - b.col);
        int pathLength = findShortestPathLength(board, a, b); // Via BFS
        //return (manhattan + pathLength) / 2;
        return pathLength;
    }

    public String positionToNotation(int row, int col) {
        // Converts (row, col) coordinates to game notation (e.g., 0,0 -> A1)
        if (row < 0 || row >= 6 || col < 0 || col >= 6) {
            throw new IllegalArgumentException("Invalid coordinates: row=" + row + ", col=" + col);
        }

        char columnChar = (char) ('A' + col);
        int rowNumber = row + 1;
        return String.valueOf(columnChar) + rowNumber;
    }

    public Position notationToPosition(String notation) {
        if (notation == null || notation.length() < 2) {
            throw new IllegalArgumentException("Invalid format: " + notation);
        }

        char colChar = notation.charAt(0);
        int col = colChar - 'A';

        int row = Integer.parseInt(notation.substring(1)) - 1;

        if (col < 0 || col >= 6 || row < 0 || row >= 6) {
            throw new IllegalArgumentException("Position out of bounds: " + notation);
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

    @Override
    public int getNumJoueur() {
        return color;
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
        return this.color == -1 ? "blanc" : "noir";
    }

    public String getCouleurEnnemiString() {
        return this.color == -1 ? "noir" : "blanc";
    }

    @Override
    public String binoName() {
        return "SuperFortDuTurfu";
    }
}