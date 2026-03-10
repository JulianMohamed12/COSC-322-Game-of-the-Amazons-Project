package ubc.cosc322;

import java.util.ArrayList;
import java.util.List;

public class BoardState {
    public int[][] board;

    int[][] directions = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}, // N, S, W, E
        {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // NW, NE, SW, SE
    };
    
    public BoardState() {
        board = new int[10][10];
    }
    
    // Copy constructor for Minimax simulations
    public BoardState(BoardState other) {
        this.board = new int[10][10];
        for (int r = 0; r < 10; r++) {
            System.arraycopy(other.board[r], 0, this.board[r], 0, 10);
        }
    }

    // Updates the internal board from the server's 121-element array
    public void updateFromServer(ArrayList<Integer> serverBoard) {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                // Map 0-9 indices to the server's 1-10 padded indices
                int serverIndex = (r + 1) * 11 + (c + 1);
                this.board[r][c] = serverBoard.get(serverIndex);
            }
        }
    }

    // Applies a move to this specific board instance
    public void applyMove(Move m, int playerType) {
        board[m.queenStart[0]][m.queenStart[1]] = 0;          // Remove queen from old spot
        board[m.queenEnd[0]][m.queenEnd[1]] = playerType;     // Place queen in new spot
        board[m.arrowPos[0]][m.arrowPos[1]] = 3;              // Place arrow (3)
    }

    // You will put your Ray-Casting Move Generation logic here
    public List<Move> generateLegalMoves(int playerType) {
        List<Move> legalMoves = new ArrayList<>();
        // 1. Find all Amazons belonging to the current player
        List<int[]> myAmazons = findAmazons(board, playerType);
        
        // 2. For each Amazon, find where it can move
        for (int[] queenPos : myAmazons) {
            List<int[]> possibleQueenDestinations = getValidQueenMoves(board, queenPos[0], queenPos[1]);
            
            // 3. For each valid destination, find where it can shoot an arrow
            for (int[] nextQueenPos : possibleQueenDestinations) {
                
                // Temporarily apply the queen move to the board to accurately calculate arrow paths
                board[queenPos[0]][queenPos[1]] = 0; 
                board[nextQueenPos[0]][nextQueenPos[1]] = playerType;
                
                // Calculate arrow shots from the NEW position
                List<int[]> possibleArrowDestinations = getValidQueenMoves(board, nextQueenPos[0], nextQueenPos[1]);
                
                // Save each complete move
                for (int[] arrowPos : possibleArrowDestinations) {
                    legalMoves.add(new Move(queenPos, nextQueenPos, arrowPos));
                }
                
                // Undo the temporary move
                board[queenPos[0]][queenPos[1]] = playerType; 
                board[nextQueenPos[0]][nextQueenPos[1]] = 0;
            }
        }
        return legalMoves;
    }
    public List<int[]> getValidQueenMoves(int[][] board, int startRow, int startCol) {
        List<int[]> validMoves = new ArrayList<>();
        
        for (int[] dir : directions) {
            int r = startRow + dir[0];
            int c = startCol + dir[1];
            
            // Keep sliding in this direction while the square is on the board and empty (0)
            while (r >= 0 && r < 10 && c >= 0 && c < 10 && board[r][c] == 0) {
                validMoves.add(new int[]{r, c});
                r += dir[0];
                c += dir[1];
            }
        }
        return validMoves;
    }
    public List<int[]> findAmazons(int[][] board, int playerType) {
        List<int[]> amazonPositions = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                if (board[row][col] == playerType) {
                    amazonPositions.add(new int[]{row, col});
                }
            }
        }
        return amazonPositions;
    }
}