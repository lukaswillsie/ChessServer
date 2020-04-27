package game;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import utility.Pair;

public class PieceTest {
	public static void main(String[] args) {
		Board board = new Board();
		
		try {
			board.initialize(new Scanner(new File("src/game/checkmate.txt")));
			System.out.println(board.getPiece(0,4).getMoves());
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		System.out.println(board);
	}
}
