package data;

/**
 * This enum represents the different data values that are stored for games
 * that are actively being played. The enum values are used as keys in HashMaps
 * that represent existing games. Each game is stored as a row in a .csv file,
 * so each enum represents a column.
 * @author Lukas Willsie
 *
 */
public enum GameData {
	GAMEID("", 0),			// ID of the game
	WHITE("", 1),			// Name of the user playing white
	BLACK("", 2),			// Name of the user playing black
	STATE("0", 3),			// State of the game; 0 if white's turn, 1 if black's turn
	TURN("1", 4),			// Current turn number
	WHITE_CHK("0", 5),		// 1 if white is in check, 0 if not
	WHITE_CHKMT("0", 6),	// 1 if white has been checkmated, 0 if not
	BLACK_CHK("0", 7),		// 1 if black is in check, 0 if not
	BLACK_CHKMT("0", 8);		// 1 if black has been checkmated, 0 if not
	
	// The initial value that this piece of GameData should take upon creation of a new game
	private String initial;
	
	// The column that this piece of data occupies in the .csv file, starting from 0
	// This number is assigned in order of appearance in the list of enum values
	private int column;
	
	GameData(String initial, int column) {
		this.initial = initial;
		this.column = column;
	}
	
	String getInitial() {
		return this.initial;
	}
	
	int getColumn() {
		return this.column;
	}
}
