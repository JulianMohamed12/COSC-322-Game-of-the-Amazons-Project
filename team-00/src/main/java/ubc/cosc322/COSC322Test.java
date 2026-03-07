
package ubc.cosc322;

import java.util.Map;

import java.util.ArrayList;
import java.util.HashMap;

import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;

/**
 * An example illustrating how to implement a GamePlayer
 * @author Yong Gao (yong.gao@ubc.ca)
 * Jan 5, 2021
 *
 */
public class COSC322Test extends GamePlayer{

    private GameClient gameClient = null; 
    private BaseGameGUI gamegui = null;
	
    private String userName = null;
    private String passwd = null;

	private int playerType = 0;
	private int opponentType = 0;
	private BoardState currentBoard = null;
	private AmazonsAI thisAI = null;
 
	
    /**
     * The main method
     * @param args for name and passwd (current, any string would work)
     */
    public static void main(String[] args) {				 
    	COSC322Test player = new COSC322Test("args[0]", "args[1]");
    	
    	if(player.getGameGUI() == null) {
    		player.Go();
    	}
    	else {
    		BaseGameGUI.sys_setup();
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                	player.Go();
                }
            });
    	}
    }
	
    /**
     * Any name and passwd 
     * @param userName
      * @param passwd
     */
    public COSC322Test(String userName, String passwd) {
    	this.userName = userName;
    	this.passwd = passwd;
    	
    	//To make a GUI-based player, create an instance of BaseGameGUI
    	//and implement the method getGameGUI() accordingly
    	//this.gamegui = new BaseGameGUI(this);
		this.gamegui = new BaseGameGUI(this);
		this.currentBoard = new BoardState();
    }
 


    @Override
    public void onLogin() {
		userName = gameClient.getUserName();
		if(gamegui != null) {
			gamegui.setRoomInformation(gameClient.getRoomList());
		}
    }

    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
    	//This method will be called by the GameClient when it receives a game-related message
    	//from the server.
	
    	//For a detailed description of the message types and format, 
    	//see the method GamePlayer.handleGameMessage() in the game-client-api document. 
		System.out.println("Recieved game message - Type: " + messageType + ", Details: " + msgDetails);
		if (messageType.equals(GameMessage.GAME_ACTION_START)) {
        
			// Extract the usernames assigned to each color
			String whitePlayerName = (String) msgDetails.get(AmazonsGameMessage.PLAYER_WHITE);
			String blackPlayerName = (String) msgDetails.get(AmazonsGameMessage.PLAYER_BLACK);

			if (whitePlayerName.equals(this.userName)) {
				this.playerType = 1;
				this.opponentType = 2;
			} 
			else if (blackPlayerName.equals(this.userName)) {
				this.playerType = 2;
				this.opponentType = 1;
			}
			thisAI = new AmazonsAI(playerType);
			if (playerType == 1) {
                makeAndSendMove();
            }
		}
		if(messageType.equals(GameMessage.GAME_STATE_BOARD)){
			ArrayList<Integer> gameState = (ArrayList<Integer>)msgDetails.get(AmazonsGameMessage.GAME_STATE);
			gamegui.setGameState(gameState);

			currentBoard.updateFromServer(gameState);
			System.out.println(currentBoard.board);

		}
		if(messageType.equals(GameMessage.GAME_ACTION_MOVE)){
			ArrayList<Integer> queenCurrent = (ArrayList<Integer>)msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR);
			ArrayList<Integer> queenNew = (ArrayList<Integer>)msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT);
			ArrayList<Integer> arrow = (ArrayList<Integer>)msgDetails.get(AmazonsGameMessage.ARROW_POS);
			gamegui.updateGameState(queenCurrent, queenNew, arrow);

			// Convert server 1-based arrays to your 0-based Move object
            Move opponentMove = new Move(
                new int[]{queenCurrent.get(0) - 1, queenCurrent.get(1) - 1},
                new int[]{queenNew.get(0) - 1, queenNew.get(1) - 1},
                new int[]{arrow.get(0) - 1, arrow.get(1) - 1}
            );
            
            // Update internal board with opponent's move
			currentBoard.applyMove(opponentMove, opponentType);
			makeAndSendMove();
		}
    	return true;   	
    }
    
    
    @Override
    public String userName() {
    	return userName;
    }

	@Override
	public GameClient getGameClient() {
		// TODO Auto-generated method stub
		return this.gameClient;
	}

	@Override
	public BaseGameGUI getGameGUI() {
		// TODO Auto-generated method stub
		return this.gamegui;
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub
    	gameClient = new GameClient(userName, passwd, this);			
	}

    private void makeAndSendMove() {
        // Find best move (Depth 2 is a safe start for timeouts)
        Move bestMove = thisAI.findBestMove(currentBoard, 2); 
        
        // Update internal board with our move
        currentBoard.applyMove(bestMove, playerType);
        
        // Update GUI
        gamegui.updateGameState(
            bestMove.formatForServer(bestMove.queenStart), 
            bestMove.formatForServer(bestMove.queenEnd), 
            bestMove.formatForServer(bestMove.arrowPos)
        );
        
        // Send to Server
        Map<String, Object> moveToSend = new HashMap<>();
        moveToSend.put(AmazonsGameMessage.QUEEN_POS_CURR, bestMove.formatForServer(bestMove.queenStart));
        moveToSend.put(AmazonsGameMessage.QUEEN_POS_NEXT, bestMove.formatForServer(bestMove.queenEnd));
        moveToSend.put(AmazonsGameMessage.ARROW_POS, bestMove.formatForServer(bestMove.arrowPos));
        
        this.getGameClient().sendMoveMessage(moveToSend);
    }

 
}//end of class