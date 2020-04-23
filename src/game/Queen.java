package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Queen extends Piece {

	public Queen(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}

	@Override
	public List<Pair> getMoves() {
		List<Pair> moves = new ArrayList<Pair>();
		
		// Arrays used to automate checking the queen's file and rank 
		int[] row_increments = 	  {0, 1,  0, -1};
		int[] column_increments = {1, 0, -1,  0};
		
		// Iterate from the queen outward in both directions along its row
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
				
				// If we've hit an enemy piece, stop because the queen can't move any farther
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
		
		// Iterate along all 4 diagonals until the edge of the
		// board or another piece is reached
		int[] row_diagonal_increments =    {1,  1, -1, -1};
		int[] column_diagonal_increments = {1, -1, -1,  1};
		
		check_row = row;
		check_column = column;
		for(int i = 0; i < row_diagonal_increments.length; i++) {
			row_increment = row_diagonal_increments[i];
			column_increment = column_diagonal_increments[i];
			
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
	 * Create a String representation of this bishop
	 * 
	 * @return A String representation of this bishop
	 */
	public String toString () {
		return (colour == Colour.WHITE) ? "Q" : "q";
	}
}
