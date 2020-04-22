package game;

import java.io.FileOutputStream;

/**
 * Represents a chessboard. Can be initialized through use of the
 * initialize(FileOutputStream stream) method, where stream is an open
 * board data file.
 * 
 * Board data files are formatted as follows. They are 8 rows of 8 characters
 * each, not including newlines, with letters used to represent chess pieces as
 * follows:
 * 		P - Pawn
 * 		R - Rook
 * 		N - Knight
 * 		B - Bishop
 * 		Q - Queen
 * 		K - King
 * 		X - empty square
 * UPPERCASE letters represent white pieces
 * LOWERCASE letters represent black pieces
 * 
 * The board should be oriented the same way as the actual board, from
 * white's perspective. That is, the first line of the file should be black's
 * back row and  the last line white's back row.
 * 
 * As an example, this is what the board should look like before any moves have
 * been made:
 * 
 * rnbqkbnr
 * pppppppp
 * XXXXXXXX
 * XXXXXXXX
 * XXXXXXXX
 * XXXXXXXX
 * PPPPPPPP
 * RNBQKBNR
 * 
 * @author Lukas Willsie
 */
public class Board {
	// Represents an actual chessboard as an 8x8 matrix
	// of pieces (null for empty squares). board[0] is the
	// bottom row of the chessboard, from white's perspective,
	// and board[0][0] is bottom row, first column (from the left).
	// So board[0][0] is a1, in traditional chess notation
	private Piece[][] board;
	
	/**
	 * Initialize the board using stream, where stream is assumed to be opened at
	 * the beginning of a correctly-formatted board data file. See class Javadoc for details.
	 * 
	 * @param stream - A FileOutputStream open to the beginning of a valid board data file
	 */
	public void initialize(FileOutputStream stream) {
		
	}
	
	/**
	 * Compute whether or not the given colour is in check
	 * @param colour - The colour
	 * @return A boolean representing whether or not the given colour
	 * is in check
	 */
	boolean isCheck(Colour colour) {
		return false;
	}
	
	/**
	 * Compute whether or not the given colour has been checkmated
	 * @param colour - The colour to check
	 * @return A boolean representing whether or not the given colour
	 * has been checkmated
	 */
	boolean isCheckmate(Colour colour) {
		return false;
	}
}
