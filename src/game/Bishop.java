package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Bishop extends Piece {
	public Bishop(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}
	
	/**
	 * Compute where this piece can move to on the board
	 * 
	 * @return A List of pairs (row,column), where each pair represents
	 * a square that this piece can move to, according to its rules of movement
	 */
	@Override
	public List<Pair> getMoves() {
		List<Pair> moves = new ArrayList<Pair>();
		
		// Iterate along all 4 diagonals until the edge of the
		// board or another piece is reached
		int[] row_increments =    {1,  1, -1, -1};
		int[] column_increments = {1, -1, -1,  1};
		
		// Iterate from the bishop outward along all diagonals until we hit the edge
		// of the board or a piece, adding it to moves if it's an enemy
		int check_row = row;
		int check_column = column;
		int row_increment, column_increment;
		for(int i = 0; i < row_increments.length; i++) {
			row_increment = row_increments[i];
			column_increment = column_increments[i];
			
			check_row += row_increment;
			check_column += column_increment;
			while(board.isMovable(check_row, check_column, colour)) { 
				moves.add(new Pair(check_row, check_column));
				
				// If we've hit an empty piece, we can't go any farther
				if(!board.isEmpty(check_row, check_column) &&
					board.getPiece(check_row, check_column).colour != colour) {
					break;
				}
				
				check_row += row_increment;
				check_column += column_increment;
			}
			
			check_row = row;
			check_column = column;
		}
		
		return moves;
	}
	
	/**
	 * Compute all squares that this piece is PROTECTING. A protected
	 * square is a square that is currently occupied by an allied piece,
	 * but which the enemy king can't move to without placing himself
	 * in check.
	 * 
	 * This might sound like a strange definition, but see the Javadoc
	 * for the King class's implementation of this method to understand
	 * why we phrase it this way.
	 * 
	 * This is useful for computing where a King cannot legally move
	 * without placing himself in check.
	 * 
	 * @return A list of Pairs, where each pair represents a square protected by
	 * this piece
	 */
	@Override
	public List<Pair> getProtectedSquares() {
		List<Pair> protected_squares = new ArrayList<Pair>();
		
		// Iterate along all 4 diagonals until the edge of the
		// board or another piece is reached
		int[] row_increments =    {1,  1, -1, -1};
		int[] column_increments = {1, -1, -1,  1};
		
		
		// Iterate from the bishop outward along all diagonals until we hit
		// a piece or the edge of the board, and add any allied pieces
		// encountered this way
		int check_row = row;
		int check_column = column;
		int row_increment, column_increment;
		for(int i = 0; i < row_increments.length; i++) {
			row_increment = row_increments[i];
			column_increment = column_increments[i];
			
			check_row += row_increment;
			check_column += column_increment;
			while(board.isMovable(check_row, check_column, colour)) { 
				// If we've hit an enemy piece, we can't go any farther
				if(!board.isEmpty(check_row, check_column) &&
					board.getPiece(check_row, check_column).colour != colour) {
					break;
				}
				
				check_row += row_increment;
				check_column += column_increment;
			}
			
			if(board.isPiece(check_row, check_column) &&
			   board.getPiece(check_row, check_column).getColour() == colour) {
				protected_squares.add(new Pair(check_row, check_column));
			}
			
			check_row = row;
			check_column = column;
		}
		
		return protected_squares;
	}
	
	/**
	 * Create a String representation of this bishop
	 * 
	 * @return A String representation of this bishop
	 */
	public String toString () {
		return (colour == Colour.WHITE) ? "B" : "b";
	}
}
