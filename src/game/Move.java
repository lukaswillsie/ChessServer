package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

/**
 * A dummy class used for testing. Can be placed on the board to mark
 * a particular square, for example as a spot where a piece can move.
 * @author lukas
 *
 */
public class Move extends Piece {

	public Move(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}

	@Override
	public List<Pair> getMoves() {
		return new ArrayList<Pair>();
	}

	@Override
	public List<Pair> getProtectedSquares() {
		return new ArrayList<Pair>();
	}

	public String toString() {
		return "O";
	}
}
