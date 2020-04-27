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
		Piece pinner = board.isPinned(this);
		// If this Bishop isn't pinned, move as normal
		if(pinner == null) {
			return getNormalMoves();
		}
		// If a Bishop is pinned by a Rook, the Bishop can't move at all
		else if (pinner instanceof Rook){
			return new ArrayList<Pair>();
		}
		// Otherwise, it's pinned by a Bishop or Queen, in which case it's possible
		// that it can move, if the pinner is in the same diagonal as this Bishop
		else {
			// Determine where the pinner is in relation to this Bishop;
			// above, below, or on the same row
			int rowIncrement;
			if(pinner.getRow() - this.getRow() > 0) {
				rowIncrement = 1;
			}
			// If on the same row, this piece can't move at all
			else if (pinner.getRow() == this.getRow()) {
				return new ArrayList<Pair>();
			}
			else {
				rowIncrement = -1;
			}
			
			// Determine where the pinner is in relation to this Bishop;
			// left, right, or in the same column
			int columnIncrement;
			if(pinner.getColumn() - this.getColumn() > 0) {
				columnIncrement = 1;
			}
			// If in the same column, this piece can't move at all
			else if(pinner.getColumn() == this.getColumn()) {
				return new ArrayList<Pair>();
			}
			else {
				columnIncrement = -1;
			}
			
			// If we've reached this point, the pinner is a Queen or Bishop
			// in some diagonal with this Bishop. So we simply add every square
			// along this diagonal between the Bishop and the pinner (inclusive
			// on the latter end)
			List<Pair> moves = new ArrayList<Pair>();
			
			int checkRow = this.getRow() + rowIncrement;
			int checkColumn = this.getColumn() + columnIncrement;
			while(board.isMovable(checkRow, checkColumn, colour)) {
				moves.add(new Pair(checkRow, checkColumn));
				
				// If we've run into the pinner, break because that's as far
				// as we can move
				if(!board.isEmpty(checkRow, checkColumn)) {
					break;
				}
			}
			
			return moves;
		}
	}
	
	/**
	 * Compute where this piece can move to by only considering the rules governing
	 * this piece's movement, without consideration of, for example, whether or not
	 * moving this piece places the King in check
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
				protected_squares.add(new Pair(check_row, check_column));
				
				// If we've hit an enemy piece, we can't go any farther
				if(!board.isEmpty(check_row, check_column)) {
					break;
				}
				
				check_row += row_increment;
				check_column += column_increment;
			}
			
			// Check if there's an ally piece that this piece is protecting
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
