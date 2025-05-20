package escampe;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class EscampeBoard implements Partie1 {
    private final char[][] board = new char[6][6];
    private int[][] liseres = new int[6][6];
    private int lastMoveLisere = -1;
    private boolean blackChoseHaut = false;

    public EscampeBoard() {
        initLiseres();
        resetBoard();
    }

    private void initLiseres() {
        int[][] preset = {
                {1, 2, 2, 3, 1, 2},
                {3, 1, 3, 1, 3, 2},
                {2, 3, 1, 2, 1, 3},
                {2, 1, 3, 2, 3, 1},
                {1, 3, 1, 3, 1, 2},
                {3, 2, 2, 1, 3, 2}
        };
        liseres = preset;
    }

    public int[][] getLiseres() {
        return liseres;
    }

    public char getBoardCell(int row, int col) {
        return this.board[row][col];
    }

    public int getLastMoveLisere() {
        return lastMoveLisere;
    }

    private void resetBoard() {
        for (char[] row : board) Arrays.fill(row, '-');
    }

    @Override
    public void setFromFile(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int row = 0;
            while ((line = br.readLine()) != null && row < 6) {
                if (line.startsWith("%")) continue;
                String clean = line.replaceAll("[^A-Za-z-]", "");
                if (clean.length() >= 6) {
                    System.arraycopy(clean.toCharArray(), 0, board[row], 0, 6);
                    row++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveToFile(String fileName) {
        try (PrintWriter pw = new PrintWriter(fileName)) {
            for (int i = 0; i < 6; i++) {
                pw.printf("%02d %s %02d%n", i+1, new String(board[i]), i+1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isValidMove(String move, String player) {
        if (move.equals("E") && this.possiblesMoves(player).isEmpty()) {
            this.lastMoveLisere = -1;
            return true;
        }

        if (move.contains("/")) {
            return validateInitialPlacement(move, player);
        } else {
            String[] parts = move.split("-");
            if (parts.length != 2) return false;

            Position start = parsePosition(parts[0]);
            Position end = parsePosition(parts[1]);
            if (start == null || end == null) return false;

            return isValidRegularMove(start, end, player);
        }
    }

    private boolean isValidRegularMove(Position start, Position end, String player) {
        if (!isPlayerPiece(start, player)) return false;
        if (!checkLisereConstraint(start)) return false;

        char piece = board[start.row][start.col];
        char endPiece = board[end.row][end.col];

        int allowedSteps = liseres[start.row][start.col];

        if (!isEndPosValid(piece, endPiece)) return false;

        // Vérification distance de Manhattan
        int manhattan = Math.abs(end.row - start.row) + Math.abs(end.col - start.col);
        if (manhattan > allowedSteps) return false;

        // Vérification du chemin
        return hasValidPath(start, end, allowedSteps);
    }

    public boolean isNextPosValid(char nextPos) {
        return nextPos == '-';
    }

    private boolean isEndPosValid(char piece, char endPiece) {
        boolean isPaladin = (piece == 'n' || piece == 'b');

        if ((piece == 'N' || piece == 'B') && endPiece != '-') return false;
        return !isPaladin || (endPiece == '-' || endPiece == ((piece == 'n') ? 'B' : 'N'));
    }

    // Algorithme DFS pour trouver un chemin valide
    private boolean hasValidPath(Position start, Position end, int steps) {
        Set<Position> visited = new HashSet<>();
        visited.add(start);
        char piece = board[start.row][start.col];
        return dfsPathfinding(start, end, steps, visited, piece);
    }

    private boolean dfsPathfinding(Position current, Position end, int stepsLeft, Set<Position> visited, char piece) {
        //if (current.equals(end) && stepsLeft == 0) return true;
        if (current.equals(end)) return true;
        if (stepsLeft == 0) return current.equals(end);

        for (Direction dir : Direction.values()) {
            Position next = dir.move(current);

            if (next.isValid() && !visited.contains(next)) {
                boolean isValid = (stepsLeft > 1)
                        ? isNextPosValid(board[next.row][next.col])
                        : isEndPosValid(piece, board[next.row][next.col]);

                if (isValid) {
                    Set<Position> newVisited = new HashSet<>(visited);
                    newVisited.add(next);
                    if (dfsPathfinding(next, end, stepsLeft - 1, newVisited, piece)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkLisereConstraint(Position start) {
        return lastMoveLisere == -1 || liseres[start.row][start.col] == lastMoveLisere;
    }

    @Override
    public List<String> possiblesMoves(String player) {
        List<String> moves = new ArrayList<>();
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                if (isPlayerPiece(new Position(r, c), player)) {
                    generateMovesFrom(r, c, moves, player);
                }
            }
        }
        return moves;
    }

    private void generateMovesFrom(int r, int c, List<String> moves, String player) {
        int steps = liseres[r][c];
        Position start = new Position(r, c);

        // Explore toutes les cases à distance de Manhattan = steps
        for (int nr = 0; nr < 6; nr++) {
            for (int nc = 0; nc < 6; nc++) {
                Position end = new Position(nr, nc);
                int distance = Math.abs(nr - r) + Math.abs(nc - c);

                if (distance == steps && isValidRegularMove(start, end, player)) {
                    moves.add(positionToNotation(r, c) + "-" + positionToNotation(nr, nc));
                }
            }
        }
    }

    @Override
    public void play(String move, String player) {
        if (!isValidMove(move, player)) throw new IllegalArgumentException("Coup invalide");

        if (move.contains("/")) {
            handleInitialPlacement(move, player);
        } else if (!move.equals("E")) {
            handleRegularMove(move);
        }
        //currentPlayer = currentPlayer.equals("noir") ? "blanc" : "noir";
    }

    private void handleRegularMove(String move) {
        String[] parts = move.split("-");
        Position start = parsePosition(parts[0]);
        Position end = parsePosition(parts[1]);

        lastMoveLisere = liseres[end.row][end.col];
        board[end.row][end.col] = board[start.row][start.col];
        board[start.row][start.col] = '-';
    }

    // Méthodes utilitaires
    private Position parsePosition(String pos) {
        if (pos.length() < 2) return null;
        int col = pos.charAt(0) - 'A';
        int row = Integer.parseInt(pos.substring(1)) - 1;
        return (row >= 0 && row < 6 && col >= 0 && col < 6) ? new Position(row, col) : null;
    }

    private String positionToNotation(int r, int c) {
        return (char)('A' + c) + "" + (r + 1);
    }

    @Override
    public boolean gameOver() {
        boolean hasBlack = false, hasWhite = false;
        for (char[] row : board) {
            for (char c : row) {
                if (c == 'N') hasBlack = true;
                if (c == 'B') hasWhite = true;
            }
        }
        return !hasBlack || !hasWhite;
    }

    // Vérifie si une pièce appartient au joueur
    private boolean isPlayerPiece(Position pos, String player) {
        if (pos == null) return false;
        char c = board[pos.row][pos.col];
        return (player.equals("noir") && (c == 'N' || c == 'n')) ||
                (player.equals("blanc") && (c == 'B' || c == 'b'));
    }

    // Valide le placement initial des pièces
    private boolean validateInitialPlacement(String move, String player) {
        String[] parts = move.split("/");
        if (parts.length != 6) return false;

        Set<Position> positions = new HashSet<>();
        for (String part : parts) {
            Position pos = parsePosition(part);
            if (pos == null || board[pos.row][pos.col] != '-') return false;
            positions.add(pos);
        }

        if (positions.size() != 6) return false;

        // Détermine les rangées autorisées
        if (player.equals("noir")) {
            boolean validHaut = positions.stream().allMatch(p -> p.row == 4 || p.row == 5);
            boolean validBas = positions.stream().allMatch(p -> p.row == 0 || p.row == 1);

            if (!validHaut && !validBas) return false;
            blackChoseHaut = validHaut;
        } else {
            blackChoseHaut = this.checkInitSide().equals("Top");
            int expectedRow = blackChoseHaut ? 4 : 0;

            return positions.stream().allMatch(p ->
                    p.row == expectedRow || p.row == expectedRow + 1
            );
        }

        return true;
    }

    private void handleInitialPlacement(String move, String player) {
        String[] parts = move.split("/");
        char licorne = player.equals("noir") ? 'N' : 'B';
        char paladin = player.equals("noir") ? 'n' : 'b';

        // Place la licorne
        Position licornePos = parsePosition(parts[0]);
        board[licornePos.row][licornePos.col] = licorne;

        // Place les paladins
        for (int i = 1; i < parts.length; i++) {
            Position p = parsePosition(parts[i]);
            board[p.row][p.col] = paladin;
        }
    }

    public EscampeBoard clone() {
        EscampeBoard copy = new EscampeBoard();
        for (int i = 0; i < 6; i++) {
            System.arraycopy(this.board[i], 0, copy.board[i], 0, 6);
        }
        copy.lastMoveLisere = this.lastMoveLisere;
        return copy;
    }

    public void printBoard() {
        String header = "   A B C D E F ";
        String space = "   ";
        System.out.println(header + space + "  " + header);
        for (int i = 0; i < 6; i++) {
            StringBuilder line = new StringBuilder();
            // Game board
            line.append("0" + (i+1) + " ");
            for (char c : this.board[i]) {
                line.append(c).append(" ");
            }
            line.append("0" + (i+1)).append(space);

            // Liseres
            line.append("0" + (i+1) + " ");
            for (int num : this.liseres[i]) {
                line.append(num).append(" ");
            }
            line.append("0" + (i+1));
            System.out.println(line);
        }
        System.out.println(header + space + "  " + header);
    }

    public String checkInitSide() {
        // Check the two first lines
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 6; j++) {
                if (this.board[i][j] == 'N' || this.board[i][j] == 'n') {
                    return "Top";
                }
            }
        }

        return "Bottom";
    }
}