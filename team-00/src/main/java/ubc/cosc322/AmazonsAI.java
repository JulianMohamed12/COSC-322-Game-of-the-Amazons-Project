package ubc.cosc322;

import java.util.List;

public class AmazonsAI {
    
    private int myPlayerType; // 1 for White, 2 for Black
    private int opponentType;

    public AmazonsAI(int myPlayerType) {
        this.myPlayerType = myPlayerType;
        this.opponentType = (myPlayerType == 1) ? 2 : 1;
    }

    public Move findBestMove(BoardState currentBoard, int depth) {
        int bestScore = Integer.MIN_VALUE;
        Move bestMove = null;
        
        List<Move> legalMoves = currentBoard.generateLegalMoves(myPlayerType);
        
        for (Move move : legalMoves) {
            // 1. Create a hypothetical board for this move
            BoardState simulatedBoard = new BoardState(currentBoard);
            simulatedBoard.applyMove(move, myPlayerType);
            
            // 2. Evaluate using Minimax
            int score = minimax(simulatedBoard, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int minimax(BoardState board, int depth, int alpha, int beta, boolean isMaximizing) {
        if (depth == 0) {
            return evaluateBoard(board); // Call your BFS Territory Evaluation here
        }
        int currentPlayer = isMaximizing ? this.myPlayerType : this.opponentType;
        List<Move> legalMoves = board.generateLegalMoves(currentPlayer);
        if (legalMoves.size() > 50) {
            java.util.Collections.shuffle(legalMoves);
            legalMoves = legalMoves.subList(0, 50);
        }

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
        int[][] myDistances = calculateDistances(board, this.myPlayerType);
        int[][] oppDistances = calculateDistances(board, this.opponentType);
        
        int myTerritory = 0;
        int oppTerritory = 0;

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                if (board.board[r][c] == 0) {
                    if (myDistances[r][c] < oppDistances[r][c]) {
                        myTerritory++;
                    } else if (oppDistances[r][c] < myDistances[r][c]) {
                        oppTerritory++;
                    }
                }
            }
        }
        int myMobility = calculateMobility(board, this.myPlayerType);
        int oppMobility = calculateMobility(board, this.opponentType);
        int territoryScore = myTerritory - oppTerritory;
        int mobilityScore = (myMobility - oppMobility) * 2;
        
        return territoryScore + mobilityScore;
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

    private int calculateMobility(BoardState boardState, int playerType) {
        int mobility = 0;
        List<int[]> amazons = boardState.findAmazons(boardState.board, playerType);
        int[][] directions = { {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1} };
        
        for (int[] pos : amazons) {
            for (int[] dir : directions) {
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
}