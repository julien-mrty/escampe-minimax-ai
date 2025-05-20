package escampe;

public enum Direction {
    UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1);

    final int dr;
    final int dc;

    Direction(int dr, int dc) {
        this.dr = dr;
        this.dc = dc;
    }

    public Position move(Position p) {
        return new Position(p.row + this.dr, p.col + this.dc);
    }
}
