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
	public Piece[][] board;
	
	public Board() {
		this.board = new Piece[8][8];
	}
	
	/**
	 * Replace whatever is on the square with piece
	 * 
	 * Precondition: row and column satisfy validSquare(row,column)
	 * 
	 * @param row - The row to put the piece on
	 * @param column - The column to put the piece on
	 * @param piece - The piece to place on the board
	 */
	public void setPiece(int row, int column, Piece piece) {
		board[row][column] = piece;
	}
	
	/**
	 * Access a particular square on the board
	 * 
	 * Precondition: row and column satisfy validSquare(row, column)
	 * 
	 * @param row - The row to access
	 * @param column - The column to access
	 * @return The piece at the specified square, or null if the square is empty 
	 */
	public Piece getPiece(int row, int column) {
		return board[row][column];
	}
	
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
	public boolean isCheck(Colour colour) {
		return false;
	}
	
	/**
	 * Compute whether or not the given colour has been checkmated
	 * @param colour - The colour to check
	 * @return A boolean representing whether or not the given colour
	 * has been checkmated
	 */
	public boolean isCheckmate(Colour colour) {
		return false;
	}
	
	/**
	 * Determine whether a given square is "on the board"
	 * @param row - The row that the square is on
	 * @param column - The column that the square is on
	 * @return true if and only if (row,column) is a valid square
	 */
	public boolean validSquare(int row, int column) {
		return 0 <= row && row <= 7 && 0 <= column && column <= 7;
	}
	
	/**
	 * Compute whether or not the given square is empty.
	 * 
	 * If (row,column) is not a valid square (i.e. out of bounds),
	 * returns false
	 * @param row - The row of the square
	 * @param column - the column of the square
	 * @return true if and only if (row,column) is a valid square
	 * which does not have a piece on it
	 */
	public boolean isEmpty(int row, int column) {
		if(validSquare(row, column)) {
			return  board[row][column] == null;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Compute whether or not a piece with the given colour could move to
	 * the given square, either because it is empty or there is an enemy
	 * piece on it.
	 * 
	 * @param row - The row of the square
	 * @param column - The column of the square
	 * @param colour - The colour of the piece considering moving to the square
	 * @return true if and only if (row,column) is a valid square and is either
	 * empty, or contains a piece with Colour different from colour
	 */
	public boolean isMovable(int row, int column, Colour colour) {
		return validSquare(row,column) && (board[row][column] == null || board[row][column].getColour() != colour);
	}
	
	@Override
	public String toString() {
		StringBuilder rep = new StringBuilder();
		Piece[] row;
		for(int i = 7; i >= 0; i--) {
			row = this.board[i];
			for(Piece piece : row) {
				if(piece == null) {
					rep.append("X");
				}
				else {
					rep.append(piece.toString());
				}
			}
			rep.append("\n");
		}
		
		return rep.toString();
	}
}
