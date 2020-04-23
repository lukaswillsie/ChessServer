package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class PieceTest {
	public static void main(String[] args) {
		Board board = new Board();
		int i = 1;
		int j = 1;
		
		board.setPiece(i, j, new King(i,j,Colour.BLACK,board));
		board.setPiece(5, 0, new Rook(5,0,Colour.WHITE,board));
		board.setPiece(0, 0, new Bishop(0,0,Colour.WHITE,board));
		System.out.println(board);
		
		List<Pair> moves = board.getPiece(i,j).getMoves();
		
		for(Pair pair : moves) {
			board.setPiece(pair.first(), pair.second(), new Rook(0,0,Colour.WHITE,board));
		}
		
		System.out.println(board);
	}
}
