package escampe;

public class TTEntry {
    int depth;       // search depth when this was stored
    int value;       // the minimax value
    int flag;        // 0 = exact, -1 = alpha-bound, +1 = beta-bound
    String bestMove; // PV move for move ordering
}
