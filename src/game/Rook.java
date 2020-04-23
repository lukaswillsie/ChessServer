package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Rook extends Piece {
	public Rook(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}

	/**
	 * Compute where this piece can move to on the board
	 * 
	 * @return A List of pairs (row,column), where each pair represents
	 * a square that this rook can move to
	 */
	@Override
	public List<Pair> getMoves() {
		List<Pair> moves = new ArrayList<Pair>();
		
		// Arrays used to automate checking the rook's file and rank 
		int[] row_increments = 	  {0, 1,  0, -1};
		int[] column_increments = {1, 0, -1,  0};
		
		// Iterate from the rook in both directions along its row
		// and column until the edge of the board or a piece is reached
		int check_row = row;
		int check_column = column;
		int row_increment = 0;
		int column_increment = 0;
		for(int i = 0; i < row_increments.length; i++) {
			row_increment = row_increments[i];
			column_increment = column_increments[i];
			
			check_row += row_increment;
			check_column += column_increment;
			while(board.isMovable(check_row, check_column, colour)) {
				moves.add(new Pair(check_row, check_column));
				
				// If we've hit an enemy piece, stop because the rook can't move any farther
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
	 * Create a String representation of this rook
	 * 
	 * @return A String representation of this rook
	 */
	public String toString() {
		return (colour == Colour.WHITE) ? "R" : "r";
	}
}
