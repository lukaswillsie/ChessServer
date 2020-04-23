package game;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import utility.Pair;

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
	
	// A list of all Pieces currently on the board
	private List<Piece> pieces;
	
	// A list of all white Pieces currently on the board
	private List<Piece> whitePieces;
	
	// A list of all black Pieces currently on the board
	private List<Piece> blackPieces;
	
	public Board() {
		this.board = new Piece[8][8];
		this.pieces = new ArrayList<Piece>();
		this.whitePieces = new ArrayList<Piece>();
		this.blackPieces = new ArrayList<Piece>();
	}
	
	/**
	 * Replace whatever is on the square with piece.
	 * 
	 * Note that this does not MOVE the given piece. If it is
	 * already on the board somewhere else, it will be in two places
	 * at once, and resulting functionality is undefined
	 * 
	 * 
	 * Precondition: row and column satisfy validSquare(row,column),
	 * and piece is not already on the board
	 * 
	 * @param row - The row to put the piece on
	 * @param column - The column to put the piece on
	 * @param piece - The piece to place on the board
	 */
	public void setPiece(int row, int column, Piece piece) {
		board[row][column] = piece;
		pieces.add(piece);
		
		// If we're placing an empty square on the board
		if(piece == null) {
			// Check if we're replacing an actual piece on the board,
			// in which case we need to remove it from the relevant lists
			if(board[row][column] instanceof Piece) {
				pieces.remove(board[row][column]);
				if(board[row][column].getColour() == Colour.WHITE) {
					whitePieces.remove(board[row][column]);
				}
				else {
					blackPieces.remove(board[row][column]);
				}
			}
		}
		else if(piece.getColour() == Colour.BLACK) {
			blackPieces.add(piece);
		}
		else {
			whitePieces.add(piece);
		}
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
	 * Determine whether or not there is a piece at the square given
	 * by row,column.
	 * 
	 * @param row - The row of the square
	 * @param column - The column of the square
	 * @return true if and only if row,column is a valid square and
	 * the entry at board[row][column] is non-null
	 */
	public boolean isPiece(int row, int column) {
		return validSquare(row, column) && board[row][column] != null;
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
	
	/**
	 * Compute whether or not the given piece is pinned, which means that
	 * moving the piece would put its king in check. 
	 * 
	 * @param piece - The piece of concern
	 * @return true if and only if moving the given piece would place its king in check
	 */
	public boolean isPinned(Piece piece) {
		int row = piece.getRow();
		int column = piece.getColumn();
		Colour colour = piece.getColour();
		
		return false;
	}
	
	/**
	 * Return a list of all squares that pieces with the given colour
	 * can move to, sorted according to the ordering imposed on Pairs.
	 * See compareTo() in the Pair class for details.
	 * 
	 * @param colour - The colour to be searched for
	 * @return A list of all squares that pieces with the given colour
	 * can move to
	 */
	public List<Pair> getMoves(Colour colour) {
		List<Pair> moves = new ArrayList<Pair>();
		List<Pair> pieceMoves;
		List<Piece> pieceList;
		if(colour == Colour.WHITE) {
			pieceList = whitePieces;
		}
		else {
			pieceList = blackPieces;
		}
		
		for(Piece piece : pieceList) {
			pieceMoves = piece.getMoves();
			for(Pair pair : pieceMoves) {
				insert(moves, pair);
			}
		}
		
		return moves;
	}
	
	/**
	 * Return a list of every single square that pieces with the given colour
	 * are protecting, sorted according to the ordering imposed on Pairs.
	 * See compareTo() in the Pair class for details.
	 * 
	 * @param colour - The colour to be searched for
	 * @return A list of all squares that pieces with the given colour
	 * are protecting
	 */
	public List<Pair> getProtectedSquares(Colour colour) {
		List<Pair> protected_squares = new ArrayList<Pair>();
		List<Pair> pieceProtectedSquares;
		List<Piece> pieceList;
		if(colour == Colour.WHITE) {
			pieceList = whitePieces;
		}
		else {
			pieceList = blackPieces;
		}
		
		for(Piece piece : pieceList) {
			pieceProtectedSquares = piece.getProtectedSquares();
			for(Pair pair : pieceProtectedSquares) {
				insert(protected_squares, pair);
			}
		}
		
		return protected_squares;
	}
	
	/**
	 * Inserts the given Pair into list WITHOUT REPITITION so that list
	 * is sorted in increasing order. In this case, that means primarily
	 * in order of the first element, secondarily in order of the second.
	 * See implementation of compareTo() in Pair for details.
	 * 
	 * Precondition: list is sorted in increasing order when this operation begins
	 * @param list
	 * @param pair
	 */
	public void insert(List<Pair> list, Pair pair) {
		int i = 0;
		while(i < list.size() && list.get(i).compareTo(pair) < 0) {
			i++;
		}
		
		if(i == list.size()) {
			list.add(pair);
		}
		// Only insert if pair is not already in list
		else if(!pair.equals(list.get(i))) {
			list.add(i, pair);
		}
	}
	
	/**
	 * Print the board using the same format as described in the class Javadoc
	 * 
	 *@return A string representation of the board
	 */
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
