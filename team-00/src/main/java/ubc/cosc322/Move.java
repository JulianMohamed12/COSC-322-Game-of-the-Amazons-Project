package ubc.cosc322;

import java.util.ArrayList;

public class Move {
    public int[] queenStart;
    public int[] queenEnd;
    public int[] arrowPos;

    public Move(int[] queenStart, int[] queenEnd, int[] arrowPos) {
        this.queenStart = queenStart;
        this.queenEnd = queenEnd;
        this.arrowPos = arrowPos;
    }

    public ArrayList<Integer> formatForServer(int[] coord) {
        ArrayList<Integer> serverFormat = new ArrayList<>();
        serverFormat.add(coord[0] + 1);
        serverFormat.add(coord[1] + 1);
        return serverFormat;
    }
}