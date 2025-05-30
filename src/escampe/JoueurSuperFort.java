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
    private final int minMaxDepthInitPos = 5;
    private final int nInitPos = 300;

    // Random 64-bit keys for every piece type × square
    private final long[][] zobristPaladins = new long[2][36];
    private final long[]   zobristUnicorns = new long[2]; // one unicorn per side
    private long           zobristSideToMove;       // random key for side-to-move
    private long[]         zobristNextBorder = new long[4]; // keys for each border constraint (-1, 1, 2, 3)

    // Our cache: zobristHash → TTEntry
    private final Map<Long,TTEntry> transpositionTable = new HashMap<>();

    @Override
    public void initJoueur(int myColor) {
        color = myColor;
        escampeBoard = new EscampeBoard();
        initZobrist();
    }

    @Override
    public void declareLeVainqueur(int colour) {
        System.out.println("[SuperFort] Vainqueur : " + (colour == BLANC ? "Blanc" : "Noir"));
    }

    private void initZobrist() {
        Random rnd = new Random(123456);  // fixed seed for reproducibility
        for(int side=0; side<2; side++){
            for(int sq=0; sq<36; sq++){
                zobristPaladins[side][sq] = rnd.nextLong();
            }
            zobristUnicorns[side] = rnd.nextLong();
        }

        zobristSideToMove = rnd.nextLong();
        for (int i = 0; i < zobristNextBorder.length; i++) {
            zobristNextBorder[i] = rnd.nextLong();
        }
    }

    @Override
    public String choixMouvement() {
        printLogsBeforeMove(logNames[0]);

        if (isInitialPhase) return initialPhaseMovement();
        else return inGameMovement();
    }

    public String initialPhaseMovement() {
        isInitialPhase = false;

        if (color == 1) { // If I'm black, I always start
            escampeBoard.play(initPosBottom, getCouleurString());
            printLogsAfterMove(logNames[0], initPosBottom);

            return initPosBottom; // We arbitrarily choose to start at the bottom as it doesn't matter
        }
        else { // If I'm white, I need to check the side black player chose, then place my pawn
            String blackSide = escampeBoard.checkInitSide();
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
                EscampeBoard simulated = escampeBoard.clone();
                simulated.play(position, getCouleurString());

                int score = minMax(simulated, minMaxDepthInitPos, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

                if (score > bestScore) {
                    bestScore = score;
                    bestPosition = position;
                }
            }

            escampeBoard.play(bestPosition, getCouleurString());
            printLogsAfterMove(logNames[0], bestPosition);

            return bestPosition;
        }
    }

    long computeZobrist(EscampeBoard board, String playerToMove) {
        long h = 0L;
        // piece bitboards
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                int sq = row*6 + col;
                char c = board.getBoardCell(row, col);
                switch(c) {
                    case 'n': h ^= zobristPaladins[0][sq]; break;
                    case 'b': h ^= zobristPaladins[1][sq]; break;
                    case 'N': h ^= zobristUnicorns[0];      break;
                    case 'B': h ^= zobristUnicorns[1];      break;
                }
            }
        }

        // side to move
        if (playerToMove.equals("blanc")) {
            h ^= zobristSideToMove;
        }

        // next-border constraint
        int next = board.getLastMoveLisere();
        if (next == -1) next = 0; // if there is no current border, say its tag is 0
        h ^= zobristNextBorder[next];
        return h;
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
        List<String> possibleMoves = escampeBoard.possiblesMoves(getCouleurString());
        System.out.println("[SuperFort] Movements possibles : " + possibleMoves);

        String chosenMove = "E";
        if (!possibleMoves.isEmpty()) {
            chosenMove = findBestMove(minMaxDepthInGame);
        }

        escampeBoard.play(chosenMove, getCouleurString());
        printLogsAfterMove(logNames[0], chosenMove);

        return chosenMove;
    }

    private String findBestMove(int depth) {
        List<String> moves = escampeBoard.possiblesMoves(getCouleurString());
        int bestValue = Integer.MIN_VALUE;
        List<String> topMoves = new ArrayList<>();

        for (String move : moves) {
            EscampeBoard simulated = escampeBoard.clone();
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
        long zobrist = computeZobrist(board, String.valueOf(maximizingPlayer));
        TTEntry entry = transpositionTable.get(zobrist);

        if (entry != null && entry.depth >= depth) {
            // use cached value
            if (entry.flag == 0) return entry.value; // exact
            if (entry.flag < 0 && entry.value <= alpha) return entry.value; // alpha-bound
            if (entry.flag > 0 && entry.value >= beta)  return entry.value; // beta-bound
        }

        int originalAlpha = alpha;
        int value;
        if (depth == 0 || board.gameOver()) {
            return evaluate(board, getCouleurString());
        }
        else if (maximizingPlayer) {
            value = Integer.MIN_VALUE;

            for (String move : board.possiblesMoves(getCouleurString())) {
                EscampeBoard simulated = board.clone();
                simulated.play(move, getCouleurString());

                value = Math.max(value, minMax(simulated, depth - 1, alpha, beta, false));
                alpha = Math.max(alpha, value);
                if (beta <= alpha) break;
            }
        }
        else {
            value = Integer.MAX_VALUE;

            for (String move : board.possiblesMoves(getCouleurEnnemiString())) {
                EscampeBoard simulated = board.clone();
                simulated.play(move, getCouleurEnnemiString());

                value = Math.min(value, minMax(simulated, depth - 1, alpha, beta, true));
                beta = Math.min(beta, value);
                if (beta <= alpha) break;
            }
        }

        // Store in table
        TTEntry newEntry = new TTEntry();
        newEntry.depth = depth;
        newEntry.value = value;
        newEntry.bestMove = null; // you could track the PV here if desired
        if (value <= originalAlpha) newEntry.flag = -1; // upper bound
        else if (value >= beta)      newEntry.flag = 1; // lower bound
        else                         newEntry.flag = 0; // exact
        transpositionTable.put(zobrist, newEntry);

        return value;
    }

    private int evaluate(EscampeBoard board, String player) {
        int score = 0;
        String enemy = player.equals("noir") ? "blanc" : "noir";

        // Immediate win/loss
        if (canCaptureUnicorn(board, player)) return Integer.MAX_VALUE - 1;
        if (canCaptureUnicorn(board, enemy)) return Integer.MIN_VALUE + 1;

        // Locate pieces
        Position myUnicorn = findUnicorn(board, player);
        Position enemyUnicorn = findUnicorn(board, enemy);
        List<Position> myPaladins = getPaladins(board, player);
        List<Position> enemyPaladins = getPaladins(board, enemy);

        // Mobility differential: compare accessible moves count
        int myMobility = board.possiblesMoves(player).size();
        int enemyMobility = board.possiblesMoves(enemy).size();
        score += (myMobility - enemyMobility) * 5;

        // Threat distance: sum of distances of paladins to focus on unicorn
        int myThreatSum = 0;
        for (Position p : myPaladins) {
            myThreatSum += optimizedDistance(board, p, enemyUnicorn);
        }
        int enemyThreatSum = 0;
        for (Position p : enemyPaladins) {
            enemyThreatSum += optimizedDistance(board, p, myUnicorn);
        }
        // reward smaller myThreatSum, penalize smaller enemyThreatSum
        score += (enemyThreatSum - myThreatSum) * 2;

        // Piece safety zones: discourage unicorn near high-value border if enemy paladin close
        int[][] borders = board.getLiseres();
        int myUnicornValue = borders[myUnicorn.row][myUnicorn.col];
        // find min distance from any enemy paladin
        int minDistEnemyToMyUni = Integer.MAX_VALUE;
        for (Position p : enemyPaladins) {
            int d = optimizedDistance(board, p, myUnicorn);
            minDistEnemyToMyUni = Math.min(minDistEnemyToMyUni, d);
        }
        // if enemy is close (<3), heavy penalty on high-value border
        if (minDistEnemyToMyUni < 3) {
            score -= myUnicornValue * 8;
        }

        // Paladin proximity to enemy unicorn
        for (Position p : myPaladins) {
            int dist = optimizedDistance(board, p, enemyUnicorn);
            score += 50 / (dist + 1);
        }
        // Safety of allied unicorn from enemy paladins
        for (Position p : enemyPaladins) {
            int dist = optimizedDistance(board, p, myUnicorn);
            score -= 30 / (dist + 1);
        }
        // Control of strategic borders
        for (Position p : myPaladins) {
            score += borders[p.row][p.col] * 3;
        }
        // Future mobility
        int nextBorder = board.getLastMoveLisere();
        score += countAccessibleTiles(board, player, nextBorder) * 2;

        return score;
    }

    private boolean canCaptureUnicorn(EscampeBoard board, String player) {
        String enemy = player.equals("noir") ? "blanc" : "noir";
        Position enemyUnicorn = findUnicorn(board, enemy);
        if (enemyUnicorn == null) return true; // Unicorn already captured

        List<Position> myPaladins = getPaladins(board, player);
        String enemyUnicornPos = positionToNotation(enemyUnicorn.row, enemyUnicorn.col);

        for (Position p : myPaladins) {
            String move = positionToNotation(p.row, p.col) + "-" + enemyUnicornPos;
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

    private int findShortestPathLengthBFS(EscampeBoard board, Position start, Position end) {
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
        return findShortestPathLengthBFS(board, a, b);
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
    public void mouvementEnnemi(String coup) {
        printLogsBeforeMove(getCouleurEnnemiString());
        System.out.println("[Ennemi] Coup ennemi possibles : " + escampeBoard.possiblesMoves(getCouleurEnnemiString()));
        escampeBoard.play(coup, getCouleurEnnemiString());
        printLogsAfterMove(logNames[1], coup);
    }

    @Override
    public int getNumJoueur() {
        return color;
    }

    public void printLogsBeforeMove(String joueur) {
        System.out.println(joueur + " Plateau avant le coup :");
        escampeBoard.printBoard();
    }

    public void printLogsAfterMove(String joueur, String coup) {
        System.out.println(joueur + " Coup choisi : " + coup);
        System.out.println(joueur + " Plateau après le coup :");
        escampeBoard.printBoard();
        System.out.println();
    }

    public String getCouleurString() {
        return color == -1 ? "blanc" : "noir";
    }

    public String getCouleurEnnemiString() {
        return color == -1 ? "noir" : "blanc";
    }

    @Override
    public String binoName() {
        return "SuperFortDuTurfu";
    }
}