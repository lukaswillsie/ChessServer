package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Knight extends Piece {
	public Knight(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}
	
	/**
	 * Compute where this piece can move to on the board
	 * 
	 * @return A List of pairs (row,column), where each pair represents
	 * a square that this knight can move to
	 */
	@Override
	public List<Pair> getMoves() {
		List<Pair> moves = new ArrayList<Pair>();
		
		int[] directions = {1,-1};
		int[] offsets = {2,-2};
		
		for(int offset : offsets) {
			for(int direction : directions) {
				if(board.isMovable(row+direction, column+offset, colour)) {
					moves.add(new Pair(row+direction, column+offset));
				}
				if(board.isMovable(row+offset, column+direction, colour)) {
					moves.add(new Pair(row+offset, column+direction));
				}
			}
		}
		
		return moves;
	}
	
	/**
	 * Create a String representation of this knight
	 * 
	 * @return A String representation of this knight
	 */
	public String toString() {
		return (colour == Colour.WHITE) ? "N" : "n";
	}
}
