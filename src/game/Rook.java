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
		Piece pinner = board.isPinned(this);
		if(pinner == null) {
			return getNormalMoves();
		}
		else if(pinner instanceof Bishop) {
			return new ArrayList<Pair>();
		}
		else {
			// For this to be able to move at all, the pinning piece
			// has to be in the same row or column
			if(pinner.getRow() == this.getRow()
			|| pinner.getColumn() == this.getColumn()) {
				// Figure out which direction will take this Rook toward its pinner
				int rowIncrement = sign(pinner.getRow() - this.getRow());
				int columnIncrement = sign(pinner.getColumn() - this.getColumn());
				
				List<Pair> moves = new ArrayList<Pair>();
				
				// Move forward until we run into the pinner
				int checkRow = this.getRow()+rowIncrement;
				int checkColumn = this.getColumn()+columnIncrement;
				while(board.isMovable(checkRow, checkColumn, colour)) {
					moves.add(new Pair(checkRow, checkColumn));
					
					// If we've reached the pinner
					if(!board.isEmpty(checkRow, checkColumn)) {
						break;
					}
				}
				
				rowIncrement *= -1;
				columnIncrement *= -1;
				
				// Move backward until we hit the ally King
				checkRow = this.getRow()+rowIncrement;
				checkColumn = this.getColumn()+columnIncrement;
				while(board.isMovable(checkRow, checkColumn, colour)) {
					moves.add(new Pair(checkRow, checkColumn));
				}
				
				return moves;
			}
			else {
				return new ArrayList<Pair>();
			}
		}
	}	
	
	/**
	 * Return the sign of the given integer, or 0 if it is 0
	 * @param num - The integer whose sign should be computed
	 * @return -1 if num < 0, 1 if num > 0 and 0 if num == 0
	 */
	private int sign(int num) {
		if(num > 0) {
			return 1;
		}
		else if (num == 0) {
			return 0;
		}
		else {
			return -1;
		}
	}
	
	/**
	 * Compute where this piece can move to by only considering the rules governing
	 * this piece's movement, without consideration of, for example, whether or not
	 * moving this piece places the King in check
	 * 
	 * @return A List of Pairs representing all the squares that this piece can move to,
	 * according to the rules of Chess governing the movement of a Rook
	 */
	private List<Pair> getNormalMoves() {
		List<Pair> moves = new ArrayList<Pair>();
		// Arrays used to automate checking the rook's file and rank 
		int[] row_increments = 	  {0, 1,  0, -1};
		int[] column_increments = {1, 0, -1,  0};
		
		// Iterate from the rook outward in both directions along its row
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
	 * Compute all squares that this piece is PROTECTING. A protected
	 * square is a square that this piece is preventing the enemy king
	 * from moving to. In other words, it's a square that the enemy
	 * king can't move to, lest he put himself in check.
	 * 
	 * For example, this might be a square the piece can move to,
	 * or a square occupied by an allied piece who this piece is protecting,
	 * or it might be a square diagonal to a pawn (the pawn can't move there,
	 * but it's neither can the enemy king, thanks to the pawn).
	 * 
	 * @return A list of Pairs, where each pair represents a square protected by
	 * this piece
	 */
	@Override
	public List<Pair> getProtectedSquares() {
		List<Pair> protected_squares = new ArrayList<Pair>();
		
		// Arrays used to automate checking the rook's file and rank 
		int[] row_increments = 	  {0, 1,  0, -1};
		int[] column_increments = {1, 0, -1,  0};
		
		// Iterate from the rook outward in both directions along its row
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
			// Iterate until we hit the edge of the board or a piece
			while(board.isMovable(check_row, check_column, colour)) {		
				protected_squares.add(new Pair(check_row, check_column));
				
				// If we've hit an enemy piece, stop because the rook can't move any farther
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
	 * Create a String representation of this rook
	 * 
	 * @return A String representation of this rook
	 */
	public String toString() {
		return (colour == Colour.WHITE) ? "R" : "r";
	}
}
