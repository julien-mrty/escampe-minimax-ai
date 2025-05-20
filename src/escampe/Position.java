package escampe;

public class Position {
    int row, col;

    Position(int r, int c) { row = r; col = c; }

    public boolean isValid() {
        return row >= 0 && row < 6 && col >= 0 && col < 6;
    }

    public boolean equals(Position p) {
        return this.row == p.row && this.col == p.col;
    }
}
