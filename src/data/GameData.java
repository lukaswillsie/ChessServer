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
	GAMEID("", 0, 's'),				// ID of the game
	WHITE("", 1, 's'),				// Name of the user playing white
	BLACK("", 2, 's'),				// Name of the user playing black
	STATE("0", 3, 'i'),				// State of the game; 0 if white's turn, 1 if black's turn
	TURN("1", 4, 'i'),				// Current turn number
	WHITE_CHK("0", 5, 'i'),			// 1 if white is in check, 0 if not
	WHITE_CHKMT("0", 6, 'i'),		// 1 if white has been checkmated, 0 if not
	BLACK_CHK("0", 7, 'i'),			// 1 if black is in check, 0 if not
	BLACK_CHKMT("0", 8, 'i'),		// 1 if black has been checkmated, 0 if not
	PROMOTION_NEEDED("0", 9, 'i');	// 1 if the player whose turn it is needs to promote a piece before their turn can end
	
	// This array contains every GameData value, in the order in which they should be sent
	// to clients when sending GameData. We choose to standardize this order here, rather
	// than hard-coding it elsewhere in the program or just using GameData.values() so
	// that it's unequivocally defined somewhere.
	public static final GameData[] order = {GAMEID, WHITE, BLACK, STATE, TURN, WHITE_CHK, WHITE_CHKMT, BLACK_CHK, BLACK_CHKMT, PROMOTION_NEEDED};
	
	// The initial value that this piece of GameData should take upon creation of a new game
	private String initial;
	
	// The column that this piece of data occupies in the .csv file, starting from 0
	// This number is assigned in order of appearance in the list of enum values
	private int column;
	
	// One of 'i' or 's'. Represents whether the type of this data should be processed as a 
	// String or integer
	public final char type;
	
	GameData(String initial, int column, char type) {
		this.initial = initial;
		this.column = column;
		this.type = type;
	}
	
	String getInitial() {
		return this.initial;
	}
	
	int getColumn() {
		return this.column;
	}
}
