package game;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import utility.Pair;

/**
 * Represents a chessboard. Can be initialized through use of the
 * initialize(FileOutputStream stream) method, where stream is an open
 * board data file. <br>
 * <br>
 * Board data files are formatted as follows. They are 8 rows of 8 characters
 * each, not including newlines, with letters used to represent chess pieces as
 * follows: <br>
 * 		P - Pawn <br>
 * 		R - Rook <br>
 * 		N - Knight <br>
 * 		B - Bishop <br>
 * 		Q - Queen <br>
 * 		K - King <br>
 * 		X - empty square <br>
 * UPPERCASE letters represent white pieces <br>
 * LOWERCASE letters represent black pieces <br>
 * <br>
 * The board should be oriented the same way as the actual board, from
 * white's perspective. That is, the first line of the file should be black's
 * back row and  the last line white's back row.
 * 
 * As an example, this is what the board should look like before any moves have
 * been made: <br>
 * <br>
 * rnbqkbnr <br>
 * pppppppp <br>
 * XXXXXXXX <br>
 * XXXXXXXX <br>
 * XXXXXXXX <br>
 * XXXXXXXX <br>
 * PPPPPPPP <br>
 * RNBQKBNR <br>
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
	
	// This method is for testing purposes only
	public void setPiece(Piece piece) {
		board[piece.getRow()][piece.getColumn()] = piece;
	}
	
	/**
	 * Add the given piece to the board, at the square given by
	 * its row and column attributes.
	 * 
	 * Has no effect if this square is already occupied.
	 * @param piece - The piece to add to the board
	 */
	public void addPiece(Piece piece) {
		if(board[piece.getRow()][piece.getColumn()] == null) {
			board[piece.getRow()][piece.getColumn()] = piece;
			if(piece.getColour() == Colour.WHITE) {
				whitePieces.add(piece);
			}
			else {
				blackPieces.add(piece);
			}
		}
	}
	
	/**
	 * "Pick up" the given piece, that is, replace its spot on
	 * the board with an empty square, without removing it
	 * from any of the lists of pieces. This allows the board
	 * to be analyzed as if the piece wasn't there
	 * 
	 * Precondition: the given piece is on the board before
	 * this method is called
	 * 
	 * @param piece - The piece to be picked up
	 */
	public void pickUp(Piece piece) {
		if(board[piece.getRow()][piece.getColumn()] != null) {
			board[piece.getRow()][piece.getColumn()] = null;
		}
	}
	
	/**
	 * Restore the given piece, assumed to have been picked up,
	 * to the board
	 * 
	 * Precondition: pickUp(piece) has previously been called
	 * @param piece
	 */
	public void restore(Piece piece) {
		board[piece.getRow()][piece.getColumn()] = piece;
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
	 * Compute whether or not the given colour is in check. Assumes that
	 * the given colour has one and only one King on the board.
	 * 
	 * @param colour - The colour
	 * @return A boolean representing whether or not the given colour
	 * is in check
	 */
	public boolean isCheck(Colour colour) {
		List<Piece> pieceList;
		if(colour == Colour.WHITE) {
			pieceList = this.whitePieces;
		}
		else {
			pieceList = this.blackPieces;
		}
		
		// Find what square the given colour's king is on
		Pair kingSquare = null;
		for(Piece piece : pieceList) {
			if(piece instanceof King) {
				kingSquare = new Pair(piece.getRow(), piece.getColumn());
				break;
			}
		}
		// If the given colour has NO king, then simply return false, because this is
		// an incorrect game state
		if(kingSquare == null) {
			return false;
		}
				
		Colour enemyColour = (colour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
		List<Pair> enemyMoves = this.getMoves(enemyColour);
		
		// Check if any enemy can capture the king
		return Collections.binarySearch(enemyMoves, kingSquare) >= 0;
	}
	
	/**
	 * Compute whether or not the given colour has been checkmated
	 * @param colour - The colour to check
	 * @return A boolean representing whether or not the given colour
	 * has been checkmated
	 */
	public boolean isCheckmate(Colour colour) {
		// A colour is in checkmate if its King is in check, all the squares
		// around it are protected by enemy pieces (it's getMoves() is emtpy),
		// and no allied pieces can block the check, or take the checking
		// piece (note that if a King is being checked by multiple pieces and
		// all its surrounding squares are protected, it is guaranteed to be
		// in checkmate, since no single move can block more than one check)
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
	 * Compute whether or not the given piece is PINNED, which means that this piece
	 * stands between its King and some attacking enemy piece. For example, if a White
	 * Queen and a Black King are in the same row with a Black Rook between them, the
	 * Rook is pinned. Note that this doesn't necessarily mean that the piece can't move
	 * at all (in our case our Rook can move back and forth in its row, just not up and
	 * down), just that its movement is restricted.
	 * 
	 * If the piece IS pinned, return the enemy piece that is pinning it (there can be only
	 * one). Otherwise return null.
	 * 
	 * @param piece - The piece of concern
	 * @return null if the given piece is NOT pinned, or a reference to the enemy piece that
	 * is pinning this piece if it is
	 */
	public Piece isPinned(Piece piece) {
		List<Piece> pieceList = (piece.getColour() == Colour.WHITE) ? whitePieces : blackPieces;
		King king = null;
		for(Piece allyPiece : pieceList) {
			if(allyPiece instanceof King) {
				king = (King)allyPiece;
				break;
			}
		}
		// If the piece has NO allied King, simply return null
		if(king == null) {
			return null;
		}
		
		// Arrays used to automate checking the rook's file and rank 
		int[] row_increments = 	  {0, 1,  0, -1};
		int[] column_increments = {1, 0, -1,  0};
		
		// Radiate from the king outward in all 4 cardinal directions
		// (not diagonals yet). The given piece is pinned if we encounter
		// 0 or more empty squares followed by the piece followed by
		// 0 or more empty squares followed by an enemy Queen or Rook
		int checkRow = king.getRow();
		int checkColumn = king.getColumn();
		int row_increment = 0;
		int column_increment = 0;
		// Keeps track of whether or not we've run into piece yet
		boolean foundPiece = false;
		for(int i = 0; i < row_increments.length; i++) {
			row_increment = row_increments[i];
			column_increment = column_increments[i];
			
			checkRow += row_increment;
			checkColumn += column_increment;
			while(validSquare(checkRow, checkColumn))
			{
				if(foundPiece) { // If we've found the piece, start looking for an enemy Rook or Queen
					if(enemyRookOrQueen(piece, checkRow, checkColumn)) {
						return board[checkRow][checkColumn];
					}
					// If we've reached a square that is occupied by any piece, ally or enemy, 
					// return null (since we know the piece is not a Rook or Queen thanks
					// to the above if statement)
					else if(!isEmpty(checkRow, checkColumn)) {
						return null;
					}
				}
				else {
					if(board[checkRow][checkColumn] == piece) {
						foundPiece = true;
					}
					// If we've found any piece other than the one we're looking for,
					// we're not going to find the given piece pinned in this row/column,
					// so move on
					else if(!isEmpty(checkRow, checkColumn)) {
						break;
					}
				}
				checkRow += row_increment;
				checkColumn += column_increment;
			}
			checkRow = king.getRow();
			checkColumn = king.getColumn();
		}
		
		
		int[] row_offsets =    {1,  1, -1, -1};
		int[] column_offsets = {1, -1, -1,  1};
		
		// Radiate outward from the king along all 4 diagonals. The given piece is pinned
		// if we encounter 0 or more empty squares followed by the piece followed by
		// 0 or more empty squares followed by a Bishop or Queen
		checkRow = king.getRow();
		checkColumn = king.getColumn();
		foundPiece = false;
		for(int i = 0; i < row_offsets.length; i++) {
			row_increment = row_offsets[i];
			column_increment = column_offsets[i];
			
			checkRow += row_increment;
			checkColumn += column_increment;
			while(validSquare(checkRow, checkColumn)) { 
				if(foundPiece) {
					if(enemyBishopOrQueen(piece, checkRow, checkColumn)) {
						return board[checkRow][checkColumn];
					}
					else if(!isEmpty(checkRow, checkColumn)) {
						return null;
					}
				}
				else {
					if(board[checkRow][checkColumn] == piece) {
						foundPiece = true;
					}
					// If we've found any piece other than the one we're looking for,
					// we're not going to find the given piece pinned in this row/column,
					// so move on
					else if(!isEmpty(checkRow, checkColumn)) {
						break;
					}
				}
				
				checkRow += row_increment;
				checkColumn += column_increment;
			}
			
			checkRow = king.getRow();
			checkColumn = king.getColumn();
		}
		
		return null;
	}
	
	/**
	 * Compute whether or not the given square contains a Rook or Queen
	 * which is an enemy of the given piece
	 * @param piece - Determines what colour of Rooks/Queens to look for
	 * @param row - The row of the square to check
	 * @param column - The column of the square to check
	 * @return true if and only if the given square contains a Rook or Queen with
	 * colour opposite to that of the given piece
	 */
	private boolean enemyRookOrQueen(Piece piece, int row, int column) {
		return  validSquare(row, column) && !isEmpty(row, column)
			&&  board[row][column].getColour() != piece.getColour()
			&& (board[row][column] instanceof Rook || board[row][column] instanceof Queen);
	}
	
	/**
	 * Compute whether or not the given square contains a Bishop or Queen which is an enemy
	 * of the given piece
	 * @param piece - Determines what colour of Rooks/Queens to look for
	 * @param row - The row of the square to check
	 * @param column - The column of the square to check
	 * @return true if and only if the given square contains a Bishop or Queen
	 * with colour opposite to that of the given piece
	 */
	private boolean enemyBishopOrQueen(Piece piece, int row, int column) {
		return  validSquare(row, column) && !isEmpty(row, column)
			&&  board[row][column].getColour() != piece.getColour()
			&& (board[row][column] instanceof Bishop|| board[row][column] instanceof Queen);
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
