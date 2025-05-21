package escampe;

import java.io.Serial;
import java.io.Serializable;

public class TTEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    int depth;       // search depth when this was stored
    int value;       // the minimax value
    int flag;        // 0 = exact, -1 = alpha-bound, +1 = beta-bound
    String bestMove; // PV move for move ordering
}
