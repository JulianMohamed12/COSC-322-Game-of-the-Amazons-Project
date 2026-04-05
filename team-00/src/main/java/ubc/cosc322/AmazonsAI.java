package ubc.cosc322;

import java.util.List;

public class AmazonsAI {
    
    private int myPlayerType; // 1 for Black, 2 for White
    private int opponentType;

    private long startTime;
    private long timeLimit;
    private boolean timeIsUp;

    private boolean checkTime() {
        if (System.currentTimeMillis() - startTime > timeLimit) {
            timeIsUp = true;
        }
        return timeIsUp;
    }

    public AmazonsAI(int myPlayerType) {
        this.myPlayerType = myPlayerType;
        this.opponentType = (myPlayerType == 1) ? 2 : 1;
    }

    public Move findBestMove(BoardState currentBoard, long timeLimitSeconds) {
        this.startTime = System.currentTimeMillis();
        this.timeLimit = timeLimitSeconds * 1000 - 500;
        this.timeIsUp = false;
        
        Move bestMoveOverall = null;
        List<Move> legalMoves = currentBoard.generateLegalMoves(this.myPlayerType);
        
        if (legalMoves.isEmpty()) return null;
        bestMoveOverall = legalMoves.get(0); // Fallback move
        
        // Start at Depth 1, keep going deeper until time runs out (up to depth 50)
        for (int depth = 1; depth < 50; depth++) {
            Move bestMoveForThisDepth = null;
            int bestScore = Integer.MIN_VALUE;
            
            for (Move move : legalMoves) {
                if (checkTime()) break; // TIME IS UP! Stop searching this depth.
                
                BoardState simulatedBoard = new BoardState(currentBoard);
                simulatedBoard.applyMove(move, myPlayerType);
                
                int score = minimax(simulatedBoard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                
                // Only accept this score if we didn't get interrupted by the timer
                if (!timeIsUp && score > bestScore) {
                    bestScore = score;
                    bestMoveForThisDepth = move;
                }
            }
            
            if (timeIsUp) {
                System.out.println("Time limit reached! Stopped at depth: " + depth);
                break; // Stop going deeper
            }
            
            if (bestMoveForThisDepth != null) {
                bestMoveOverall = bestMoveForThisDepth;
            }
        }
        
        return bestMoveOverall;
    }

    private int minimax(BoardState board, int depth, int alpha, int beta, boolean isMaximizing) {
        if (depth == 0) {
            return evaluateBoard(board); // Call your BFS Territory Evaluation here
        }
        int currentPlayer = isMaximizing ? this.myPlayerType : this.opponentType;
        List<Move> legalMoves = board.generateLegalMoves(currentPlayer);

        if (legalMoves.isEmpty()) {
            return isMaximizing ? -100000 : 100000;
        }
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : legalMoves) {
                BoardState simulatedBoard = new BoardState(board);
                simulatedBoard.applyMove(move, currentPlayer);
                int eval = minimax(simulatedBoard, depth - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        }else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : legalMoves) {
                BoardState simulatedBoard = new BoardState(board);
                simulatedBoard.applyMove(move, currentPlayer);
                int eval = minimax(simulatedBoard, depth - 1, alpha, beta, true);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }
    
    private int evaluateBoard(BoardState board) {
        // 1. For each point p, compare db= dist(p,Black) and dw=dist(p,White); [cite: 356]
        int[][] myDistances = calculateDistances(board, this.myPlayerType);
        int[][] oppDistances = calculateDistances(board, this.opponentType);
        
        int score = 0;
    
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                // We only care about empty, reachable squares
                if (board.board[r][c] == 0) { 
                    int myD = myDistances[r][c];
                    int oppD = oppDistances[r][c];
    
                    // If at least one player can reach the square
                    if (myD != Integer.MAX_VALUE || oppD != Integer.MAX_VALUE) {
                        
                        // 2. if db < dw Black point (or in our case, 'My' point) [cite: 357]
                        if (myD < oppD) {
                            score++;
                        } 
                        // 3. else if db > dw White point (or in our case, 'Opponent' point) [cite: 358]
                        else if (oppD < myD) {
                            score--;
                        }
                        // 4. else point is neutral (do nothing) [cite: 359]
                    }
                }
            }
        }
        
        return score;
    }

    private int[][] calculateDistances(BoardState boardState, int playerType) {
        int[][] distGrid = new int[10][10];
        
        // 1. Fill the map with "Infinity"
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                distGrid[i][j] = Integer.MAX_VALUE;
            }
        }
        
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        
        // 2. Find the queens and set their starting square to 0 distance
        java.util.List<int[]> queens = boardState.findAmazons(boardState.board, playerType);
        for (int[] q : queens) {
            distGrid[q[0]][q[1]] = 0;
            queue.add(new int[]{q[0], q[1], 0});
        }
        
        int[][] directions = { {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1} };
        
        // 3. The BFS Exploration Loop
        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int r = curr[0];
            int c = curr[1];
            int d = curr[2];
            
            for (int[] dir : directions) {
                int nextR = r + dir[0];
                int nextC = c + dir[1];
                
                // Keep sliding until we hit the edge of the board, a queen, or an arrow
                while (nextR >= 0 && nextR < 10 && nextC >= 0 && nextC < 10 && boardState.board[nextR][nextC] == 0) {
                    
                    // If this is a faster path to this square, record it
                    if (distGrid[nextR][nextC] > d + 1) {
                        distGrid[nextR][nextC] = d + 1;
                        queue.add(new int[]{nextR, nextC, d + 1});
                    }
                    
                    // Move one more square in the same direction
                    nextR += dir[0];
                    nextC += dir[1];
                }
            }
        }
        
        return distGrid;
    }
}
