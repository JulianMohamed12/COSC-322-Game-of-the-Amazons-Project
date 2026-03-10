package ubc.cosc322;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AmazonsAI {

    private int myPlayerType;
    private int opponentType;

    // --- Tuning knobs ---
    private static final int    MAX_DEPTH      = 6;      // iterative deepening ceiling
    private static final long   TIME_LIMIT_MS  = 8_500L; // stop searching after ~8.5 s
    private static final int    WIN_SCORE      = 100_000;
    private static final int    LOSE_SCORE     = -100_000;

    // Weight coefficients for evaluation terms
    private static final int W_TERRITORY   = 3;  // squares where we reach first (exclusive)
    private static final int W_CONTESTED   = 1;  // squares where we reach sooner (contested)
    private static final int W_MOBILITY    = 2;  // raw queen slide count advantage
    private static final int W_REACH       = 1;  // total reachable squares advantage

    private static final int[][] DIRECTIONS = {
        {-1,  0}, {1,  0}, {0, -1}, {0, 1},   // N S W E
        {-1, -1}, {-1, 1}, {1, -1}, {1, 1}    // NW NE SW SE
    };

    // Timing
    private long    startTime;
    private boolean timeUp;

    public AmazonsAI(int myPlayerType) {
        this.myPlayerType = myPlayerType;
        this.opponentType = (myPlayerType == 1) ? 2 : 1;
    }

    // =========================================================================
    //  Public entry point - called from COSC322Test with depth as a hint.
    //  We ignore the hint and use iterative deepening with a time budget.
    // =========================================================================
    public Move findBestMove(BoardState currentBoard, int depthHint) {
        startTime = System.currentTimeMillis();
        timeUp    = false;

        List<Move> rootMoves = currentBoard.generateLegalMoves(myPlayerType);
        if (rootMoves.isEmpty()) return null;
        if (rootMoves.size() == 1)  return rootMoves.get(0); // forced move

        // Start with a quick ordering so depth-1 gives us a decent first best
        orderMoves(rootMoves, currentBoard, myPlayerType);

        Move bestMove = rootMoves.get(0); // fallback: first ordered move

        // Iterative deepening
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {

            if (elapsed() > TIME_LIMIT_MS * 0.75) break; // leave time to complete

            Move candidate = searchRoot(currentBoard, rootMoves, depth);

            if (!timeUp && candidate != null) {
                bestMove = candidate;
            }
            if (timeUp) break;
        }

        return bestMove;
    }

    // =========================================================================
    //  Root search at a fixed depth – returns best Move for myPlayerType
    // =========================================================================
    private Move searchRoot(BoardState board, List<Move> moves, int depth) {
        int   bestScore = Integer.MIN_VALUE;
        Move  bestMove  = null;

        for (Move move : moves) {
            if (elapsed() > TIME_LIMIT_MS * 0.90) { timeUp = true; break; }

            BoardState child = new BoardState(board);
            child.applyMove(move, myPlayerType);

            int score = minimax(child, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            if (score > bestScore) {
                bestScore = score;
                bestMove  = move;
            }
        }
        return bestMove;
    }

    // =========================================================================
    //  Alpha-Beta Minimax
    // =========================================================================
    private int minimax(BoardState board, int depth, int alpha, int beta, boolean isMaximizing) {

        // Time guard
        if (elapsed() > TIME_LIMIT_MS * 0.95) {
            timeUp = true;
            return evaluateBoard(board);
        }

        int currentPlayer = isMaximizing ? myPlayerType : opponentType;
        List<Move> moves  = board.generateLegalMoves(currentPlayer);

        // Terminal: current player has no moves → they lose
        if (moves.isEmpty()) {
            return isMaximizing ? LOSE_SCORE - depth : WIN_SCORE + depth;
            // depth bonus: prefer faster wins / slower losses
        }

        if (depth == 0) return evaluateBoard(board);

        // Move ordering improves pruning significantly
        orderMoves(moves, board, currentPlayer);

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                BoardState child = new BoardState(board);
                child.applyMove(move, currentPlayer);
                int eval = minimax(child, depth - 1, alpha, beta, false);
                if (eval > maxEval) maxEval = eval;
                if (eval > alpha)   alpha   = eval;
                if (beta <= alpha)  break;   // β-cutoff
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                BoardState child = new BoardState(board);
                child.applyMove(move, currentPlayer);
                int eval = minimax(child, depth - 1, alpha, beta, true);
                if (eval < minEval) minEval = eval;
                if (eval < beta)    beta    = eval;
                if (beta <= alpha)  break;   // α-cutoff
            }
            return minEval;
        }
    }

    // =========================================================================
    //  Static Board Evaluation
    // =========================================================================
    private int evaluateBoard(BoardState board) {

        // Quick mobility check first (cheap, catches near-terminal states)
        int myMobility  = calculateMobility(board, myPlayerType);
        int oppMobility = calculateMobility(board, opponentType);

        if (myMobility  == 0) return LOSE_SCORE;
        if (oppMobility == 0) return WIN_SCORE;

        // BFS queen-move distances for both players
        int[][] myDist  = calculateQueenDistances(board, myPlayerType);
        int[][] oppDist = calculateQueenDistances(board, opponentType);

        int myExclusive  = 0;  // squares only I can reach
        int oppExclusive = 0;
        int myContested  = 0;  // contested squares where I'm closer
        int oppContested = 0;
        int myReachable  = 0;
        int oppReachable = 0;

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (board.board[r][c] != 0) continue; // occupied

                boolean myR  = myDist[r][c]  < Integer.MAX_VALUE;
                boolean oppR = oppDist[r][c] < Integer.MAX_VALUE;

                if (myR)  myReachable++;
                if (oppR) oppReachable++;

                if (myR && !oppR) {
                    myExclusive++;
                } else if (oppR && !myR) {
                    oppExclusive++;
                } else if (myR && oppR) {
                    if      (myDist[r][c] < oppDist[r][c]) myContested++;
                    else if (oppDist[r][c] < myDist[r][c]) oppContested++;
                }
            }
        }

        int territoryScore  = W_TERRITORY * (myExclusive  - oppExclusive)
                            + W_CONTESTED * (myContested   - oppContested);
        int mobilityScore   = W_MOBILITY  * (myMobility   - oppMobility);
        int reachScore      = W_REACH     * (myReachable  - oppReachable);

        return territoryScore + mobilityScore + reachScore;
    }

    // =========================================================================
    //  BFS – queen-move distance from all of playerType's amazons
    //  (one queen move can slide multiple squares in one direction)
    // =========================================================================
    private int[][] calculateQueenDistances(BoardState boardState, int playerType) {
        int[][] dist = new int[10][10];
        for (int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);

        Queue<int[]> queue = new LinkedList<>();
        for (int[] q : boardState.findAmazons(boardState.board, playerType)) {
            dist[q[0]][q[1]] = 0;
            queue.add(new int[]{q[0], q[1], 0});
        }

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0], c = curr[1], d = curr[2];

            for (int[] dir : DIRECTIONS) {
                int nr = r + dir[0];
                int nc = c + dir[1];
                // Slide along direction until blocked
                while (nr >= 0 && nr < 10 && nc >= 0 && nc < 10
                       && boardState.board[nr][nc] == 0) {
                    if (d + 1 < dist[nr][nc]) {
                        dist[nr][nc] = d + 1;
                        queue.add(new int[]{nr, nc, d + 1});
                    }
                    nr += dir[0];
                    nc += dir[1];
                }
            }
        }
        return dist;
    }

    // =========================================================================
    //  Mobility = total squares reachable by one queen slide from any amazon
    // =========================================================================
    private int calculateMobility(BoardState boardState, int playerType) {
        int mobility = 0;
        for (int[] pos : boardState.findAmazons(boardState.board, playerType)) {
            for (int[] dir : DIRECTIONS) {
                int r = pos[0] + dir[0];
                int c = pos[1] + dir[1];
                while (r >= 0 && r < 10 && c >= 0 && c < 10 && boardState.board[r][c] == 0) {
                    mobility++;
                    r += dir[0];
                    c += dir[1];
                }
            }
        }
        return mobility;
    }

    // =========================================================================
    //  Move Ordering  – sorts moves so the most promising are tried first,
    //  maximising the effectiveness of alpha-beta pruning.
    //
    //  Heuristic: prefer moves whose queen destination is closer to the board
    //  centre AND whose arrow blocks more opponent movement.
    // =========================================================================
    private void orderMoves(List<Move> moves, BoardState board, int playerType) {
        // Pre-compute opponent positions for arrow-scoring
        final int opp = (playerType == myPlayerType) ? opponentType : myPlayerType;

        moves.sort((a, b) -> {
            int sa = quickMoveScore(a, board, opp);
            int sb = quickMoveScore(b, board, opp);
            return sb - sa; // descending
        });
    }

    /**
     * Fast heuristic score for a move (used only for ordering, not evaluation).
     *  +  Queen destination closer to centre → more flexible
     *  +  Arrow closer to an opponent queen  → more restricting
     */
    private int quickMoveScore(Move move, BoardState board, int oppPlayerType) {
        // Centre proximity of queen destination (0–8, higher = closer)
        int dr = Math.abs(move.queenEnd[0] - 4);
        int dc = Math.abs(move.queenEnd[1] - 4);
        int centreScore = 8 - dr - dc;

        // Proximity of arrow to nearest opponent queen
        int arrowScore = 0;
        List<int[]> oppQueens = board.findAmazons(board.board, oppPlayerType);
        for (int[] oq : oppQueens) {
            int dist = Math.abs(move.arrowPos[0] - oq[0]) + Math.abs(move.arrowPos[1] - oq[1]);
            if (dist == 0) arrowScore += 10; // directly adjacent/blocks
            else           arrowScore += Math.max(0, 6 - dist);
        }

        return centreScore + arrowScore;
    }

    // =========================================================================
    //  Utility
    // =========================================================================
    private long elapsed() {
        return System.currentTimeMillis() - startTime;
    }
}