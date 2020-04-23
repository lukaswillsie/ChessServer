package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import utility.Pair;

public class King extends Piece {

	public King(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}

	/**
	 * TODO: This method isn't finished yet because it doesn't take into account
	 * Protected squares; i.e. enemy pieces that the king can't take because they're
	 * being backed up by another enemy piece
	 */
	@Override
	public List<Pair> getMoves() {
		List<Pair> moves = new ArrayList<Pair>();
		Colour enemy_colour = (colour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
		
		// We remove the King from the board, temporarily, to get a complete
		// picture of where enemy pieces can move. Without this step,
		// the King may misinterpret certain squares as legal.
		// For example, suppose the King has an enemy rook on its left.
		// Then the square to the right of the King is not technically
		// a square that the rook can move to at the moment, but the King still
		// can't move there because if he did he would be in check. We need
		// to remove the King from the board to catch such subtleties. 
		board.setPiece(this.row, this.column, null);
		
		List<Pair> enemyMoves = board.getMoves(enemy_colour);
		
		int[] row_offsets = {-1,1};
		int[] column_offsets= {-1,0,1};
		
		// Check the rows in front of and behind the King
		for(int row_offset : row_offsets) {
			for(int column_offset : column_offsets) {
				// The King can move to a square 
				if(board.validSquare(this.row+row_offset, this.column+column_offset)
				&& Collections.binarySearch(enemyMoves, new Pair(this.row+row_offset, this.column+column_offset)) < 0) {
					moves.add(new Pair(this.row + row_offset, this.column + column_offset));
				}
			}
		}
		
		// Check the square to the right of the King
		if(board.validSquare(this.row, this.column+1) &&
		   Collections.binarySearch(enemyMoves, new Pair(this.row, this.column+1)) < 0) {
			moves.add(new Pair(this.row, this.column + 1));
		}
		
		// Check the square to the left of the King
		if(board.validSquare(this.row, this.column-1) &&
		   Collections.binarySearch(enemyMoves, new Pair(this.row, this.column-1)) < 0) {
			moves.add(new Pair(this.row, this.column - 1));
		}
		
		return moves;
	}
	
	
	public String toString() {
		return (colour == Colour.WHITE) ? "K" : "k";
	}
}
