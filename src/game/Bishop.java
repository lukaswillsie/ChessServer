package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Bishop extends Piece {
	public static final char charRep = 'b';
	
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
			return board.getLegal(getNormalMoves(), colour);
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
			int rowIncrement = sign(pinner.getRow() - this.getRow());
			
			// Determine where the pinner is in relation to this Bishop;
			// left, right, or in the same column
			int columnIncrement = sign(pinner.getColumn() - this.getColumn());
			
			// If we've reached this point, the pinner is a Queen or Bishop
			// in some diagonal with this Bishop. So we simply add every square
			// along this diagonal between the Bishop and the pinner (inclusive
			// on the latter end), and in the other direction between the Bishop
			// and its King
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
				
				checkRow += rowIncrement;
				checkColumn += columnIncrement;
			}
			
			rowIncrement *= -1;
			columnIncrement *= -1;
			checkRow = this.getRow() + rowIncrement;
			checkColumn = this.getColumn() + columnIncrement;
			// Simply move along the diagonal until we hit the ally King,
			// at which point our while loop will break because the square
			// won't be movable
			while(board.isMovable(checkRow, checkColumn, colour)) {
				moves.add(new Pair(checkRow, checkColumn));
				
				checkRow += rowIncrement;
				checkColumn += columnIncrement;
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
		List<Pair> protectedSquares = new ArrayList<Pair>();
		
		// Iterate along all 4 diagonals until the edge of the
		// board or another piece is reached
		int[] row_increments =    {1,  1, -1, -1};
		int[] column_increments = {1, -1, -1,  1};
		
		
		// Iterate from the Bishop outward along all diagonals until we hit
		// a piece or the edge of the board, adding all empty squares
		// and ally pieces encountered
		int checkRow = row;
		int checkColumn = column;
		int row_increment, column_increment;
		for(int i = 0; i < row_increments.length; i++) {
			row_increment = row_increments[i];
			column_increment = column_increments[i];
			
			checkRow += row_increment;
			checkColumn += column_increment;
			// Iterate until we run into the edge of the board or a piece
			while(board.isEmpty(checkRow, checkColumn)) { 				
				protectedSquares.add(new Pair(checkRow, checkColumn));
				
				checkRow += row_increment;
				checkColumn += column_increment;
			}
			
			// Check if we ran into an ally piece that we are protecting
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
	 * Create a String representation of this Bishop.
	 * 
	 * Guaranteed to only be one character long
	 * 
	 * @return A String representation of this Bishop
	 */
	public String toString () {
		return (colour == Colour.WHITE) ? "B" : "b";
	}

	@Override
	public boolean isCheckingKing() {		
		// Iterate along all 4 diagonals until the edge of the
		// board or another piece is reached
		int[] row_increments =    {1,  1, -1, -1};
		int[] column_increments = {1, -1, -1,  1};
		
		// Get a reference to the enemy King
		Piece enemyKing = board.getKing((colour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE);
		if(enemyKing == null) {
			return false;
		}
		
		// Iterate from the bishop outward along all diagonals until we hit the edge
		// of the board or a piece, checking for the enemy King along the way
		int checkRow = row;
		int checkColumn = column;
		int row_increment, column_increment;
		for(int i = 0; i < row_increments.length; i++) {
			row_increment = row_increments[i];
			column_increment = column_increments[i];
			
			checkRow += row_increment;
			checkColumn += column_increment;
			while(board.isEmpty(checkRow, checkColumn)) {
				checkRow += row_increment;
				checkColumn += column_increment;
			}
			
			if(board.isPiece(checkRow, checkColumn)
			&& board.getPiece(checkRow, checkColumn) == enemyKing) {
				return true;
			}
					
			
			checkRow = row;
			checkColumn = column;
		}
		
		return false;
	}
}