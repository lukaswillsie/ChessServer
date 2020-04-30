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
		Piece pinner = board.isPinned(this);
		if(pinner == null) {
			return board.getLegal(getNormalMoves(), colour);
		}
		else {
			// The Queen can only move toward and away from the pinning piece
			// without exposing its King. Determine what direction constitutes
			// "toward" the pinning piece
			int rowDirection = sign(pinner.getRow() - pinner.getColumn());
			int columnDirection = sign(pinner.getColumn() - pinner.getColumn());
			
			List<Pair> moves = new ArrayList<Pair>();
			
			// Travel toward pinner, until we run into it, adding every single square
			// encounter in the process, including the square occupied by pinner
			int checkRow = this.row+rowDirection;
			int checkColumn = this.column+columnDirection;
			while(board.isMovable(checkRow, checkColumn, colour)) {
				moves.add(new Pair(checkRow, checkColumn));
				
				// Break once we've hit pinner, because we can't go any farther
				if(board.getPiece(checkRow, checkColumn) == pinner) {
					break;
				}
				
				checkRow += rowDirection;
				checkColumn += columnDirection;
			}
			
			
			// Do the exact same thing, except now we're traveling in the opposite direction,
			// toward the Queen's King.
			rowDirection *= -1;
			columnDirection *= -1;
			
			// Travel toward the Queen's King until we run into it, without adding the
			// square it's occupying
			checkRow = this.row+rowDirection;
			checkColumn = this.column+columnDirection;
			while(board.isMovable(checkRow, checkColumn, colour)) {
				moves.add(new Pair(checkRow, checkColumn));
				
				checkRow += rowDirection;
				checkColumn += columnDirection;
			}
			
			return board.getLegal(moves, colour);
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
	 * moving this piece places the King in check.
	 * 
	 * @return A List of Pairs representing all the squares that this piece can move to,
	 * according to the rules of Chess governing the movement of a Bishop
	 */
	private List<Pair> getNormalMoves() {
		List<Pair> moves = new ArrayList<Pair>();
		
		// Iterate along all 4 diagonals until the edge of the
		// board or another piece is reached
		int[] row_increments =    {1,  1, -1, -1};
		int[] column_increments = {1, -1, -1,  1};
		
		// Iterate from the Queen outward along all diagonals until we hit the edge
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
				
				// If we've hit an enemy piece, we can't go any farther
				if(!board.isEmpty(check_row, check_column)) {
					break;
				}
				
				check_row += row_increment;
				check_column += column_increment;
			}
			
			check_row = row;
			check_column = column;
		}
		
		// Arrays used to automate checking the Queen's row and column
		int[] row_offsets = 	  {0, 1,  0, -1};
		int[] column_offsets = {1, 0, -1,  0};
		
		// Iterate from the Queen outward in both directions along its row
		// and column until the edge of the board or a piece is reached
		check_row = row;
		check_column = column;
		row_increment = 0;
		column_increment = 0;
		for(int i = 0; i < row_offsets.length; i++) {
			row_increment = row_offsets[i];
			column_increment = column_offsets[i];
			
			check_row += row_increment;
			check_column += column_increment;
			while(board.isMovable(check_row, check_column, colour)) {
				moves.add(new Pair(check_row, check_column));
				
				// If we've hit an enemy piece, stop because the Queen can't move any farther
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
		List<Pair> protectedSquares = new ArrayList<Pair>();
		
		// Arrays used to automate checking the Queen's row and column
		int[] rowIncrements = 	  {0, 1,  0, -1};
		int[] columnIncrements = {1, 0, -1,  0};
		
		// Iterate from the Queen outward in both directions along its row
		// and column until the edge of the board or a piece is reached,
		// adding all empty squares and ally pieces encountered
		int checkRow = row;
		int checkColumn = column;
		int rowIncrement = 0;
		int columnIncrement = 0;
		for(int i = 0; i < rowIncrements.length; i++) {
			rowIncrement = rowIncrements[i];
			columnIncrement = columnIncrements[i];
			
			checkRow += rowIncrement;
			checkColumn += columnIncrement;
			// Iterate until we hit the edge of the board or a piece
			while(board.isEmpty(checkRow, checkColumn)) {		
				protectedSquares.add(new Pair(checkRow, checkColumn));
				
				checkRow += rowIncrement;
				checkColumn += columnIncrement;
			}
			// Check if we've run into an ally piece that we are protecting
			if(board.isPiece(checkRow, checkColumn) &&
			   board.getPiece(checkRow, checkColumn).getColour() == colour) {
				protectedSquares.add(new Pair(checkRow, checkColumn));
			}
			
			checkRow = row;
			checkColumn = column;
		}
		
		// Iterate along all 4 diagonals until the edge of the
		// board or another piece is reached
		int[] row_offsets =    {1,  1, -1, -1};
		int[] column_offsets = {1, -1, -1,  1};
		
		
		// Iterate from the Queen outward along all diagonals until we hit
		// a piece or the edge of the board, and add any allied pieces
		// encountered this way
		checkRow = row;
		checkColumn = column;
		for(int i = 0; i < row_offsets.length; i++) {
			rowIncrement = row_offsets[i];
			columnIncrement = column_offsets[i];
			
			checkRow += rowIncrement;
			checkColumn += columnIncrement;
			while(board.isEmpty(checkRow, checkColumn)) { 
				protectedSquares.add(new Pair(checkRow, checkColumn));
				
				checkRow += rowIncrement;
				checkColumn += columnIncrement;
			}
			
			// Check if there's an ally piece that this piece is protecting
			if(board.isPiece(checkRow, checkColumn) &&
			   board.getPiece(checkRow, checkColumn).getColour() == colour) {
				protectedSquares.add(new Pair(checkRow, checkColumn));
			}
			
			checkRow = row;
			checkColumn = column;
		}
		
		return protectedSquares;
	}
	
	/**
	 * Create a String representation of this Queen.
	 * 
	 * Guaranteed to only be one character long
	 * 
	 * @return A String representation of this Queen
	 */
	public String toString () {
		return (colour == Colour.WHITE) ? "Q" : "q";
	}
}
