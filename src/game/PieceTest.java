package game;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class PieceTest {
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		String input = in.nextLine();
		Board board = null;
		
		boolean promotionRequired = false;
		System.out.println();
		
		/*
		 * Operations:
		 * load a board - load filename
		 * print piece moves - moves row,col
		 * print piece protected squares - protsquares row,col
		 * print is pinned - pinned row,col
		 * print check - check colour
		 * print checkmate - checkmate colour
		 * move piece - move row,col->row,col
		 * promote pawn - promote charRep
		 */
		while(!input.equalsIgnoreCase("q")) {
			String[] splitted = input.split(" ");
			String command = splitted[0];
			String rest = "";
			if(splitted.length > 1) {
				rest = splitted[1];
			}
			
			
			if(command.equals("load")) {
				board = new Board();
				try {
					int result = board.initialize(new Scanner(new File(rest)));
					if(result == 1) {
						System.out.println("Invalid board file");
					}
					else {
						System.out.println(board);
						System.out.println("Loaded board");
					}
				}
				catch (IOException e) {
					System.out.println("File could not be opened. See below:");
					e.printStackTrace();
				}
			}
			else if(command.equals("moves")) {
				try {
					int row = Integer.parseInt(rest.split(",")[0]);
					int column = Integer.parseInt(rest.split(",")[1]);
					Piece piece = board.getPiece(row, column);	
					if(piece == null) {
						System.out.println("There is no piece at that location");
					}
					else {
						printMoves(board, piece.getMoves());
					}
				}
				catch(NumberFormatException e) {
					System.out.println("Invalid format. Usage: moves row,col");
				}
			}
			else if(command.equals("protsquares")) {
				try {
					int row = Integer.parseInt(rest.split(",")[0]);
					int column = Integer.parseInt(rest.split(",")[1]);
					Piece piece = board.getPiece(row, column);	
					if(piece == null) {
						System.out.println("There is no piece at that location");
					}
					else {
						printMoves(board, piece.getProtectedSquares());
					}
				}
				catch(NumberFormatException e) {
					System.out.println("Invalid format. Usage: protsquares row,col");
				}
			}
			else if(command.equals("pinned")) {
				System.out.println(board);
				try {
					int row = Integer.parseInt(rest.split(",")[0]);
					int column = Integer.parseInt(rest.split(",")[1]);
					Piece piece = board.getPiece(row, column);	
					if(piece == null) {
						System.out.println("There is no piece at that location");
					}
					else {
						System.out.println("Pinned: " + board.isPinned(piece));
					}
				}
				catch(NumberFormatException | IndexOutOfBoundsException e) {
					System.out.println("Invalid format. Usage: pinned row,col");
				}
			}
			else if(command.equals("check")) {
				System.out.println(board);
				if(rest.charAt(0) == 'w' || rest.charAt(0) == 'W') {
					Colour colour = Colour.WHITE;
					System.out.println(colour + " Check: " + board.isCheck(colour));
				}
				else if (rest.charAt(0) == 'b' || rest.charAt(0) == 'B') {
					Colour colour = Colour.BLACK;
					System.out.println(colour + " Check: " + board.isCheck(colour));
				}
				else {
					System.out.println("Invalid input. Usage: check w or check b");
				}
			}
			else if(command.equals("checkmate")) {
				System.out.println(board);
				if(rest.charAt(0) == 'w' || rest.charAt(0) == 'W') {
					Colour colour = Colour.WHITE;
					System.out.println(colour + " Checkmate: " + board.isCheckmate(colour));
				}
				else if (rest.charAt(0) == 'b' || rest.charAt(0) == 'B') {
					Colour colour = Colour.BLACK;
					System.out.println(colour + " Checkmate: " + board.isCheckmate(colour));
				}
				else {
					System.out.println("Invalid input. Usage: checkmate w or checkmate b");
				}
			}
			else if(command.equals("move")) {
				try {
					Pair src = new Pair(Integer.parseInt(rest.split("->")[0].split(",")[0]), Integer.parseInt(rest.split("->")[0].split(",")[1]));
					Pair dest = new Pair(Integer.parseInt(rest.split("->")[1].split(",")[0]), Integer.parseInt(rest.split("->")[1].split(",")[1]));
					
					int result = board.move(src, dest);
					switch(result) {
						case -1:
							System.out.println(board);
							System.out.println("A promotion is now required. Please use the \"promote\" command to promote your pawn");
							promotionRequired = true;
							break;
						case 0:
							System.out.println(board);
							System.out.println("Move executed");
							break;
						case 1:
							System.out.println(board);
							System.out.println("That is an invalid move");
							break;
						case 2:
							System.out.println(board);
							System.out.println("It is the other colour's turn");
							break;
						case 3:
							System.out.println(board);
							System.out.println("A promotion must be made before any moves can be processed.");
							promotionRequired = true;
							break;
					}
				}
				catch(NumberFormatException | IndexOutOfBoundsException d) {
					System.out.println("Invalid format. Usage: move src_row,src_col->dest_row,dest_col");
				}
			}
			else if (command.equals("promote")) {
				if(promotionRequired) {
					board.promote(rest.charAt(0));
					System.out.println(board);
				}
				else {
					System.out.println("No promotion is required");
				}
			}
			else if(command.equals("save")) {
				try {
					board.saveGame(new FileOutputStream(new File("src/game/saved.txt")));
					System.out.println("Game saved successfully");
				}
				catch(IOException e) {
					System.out.println("Game could not be saved");
				}
			}
			else {
				System.out.println("Invalid command");
			}
			
			input = in.nextLine();
		}
		in.close();
	}
	
	public static void printMoves(Board board, List<Pair> moves) {
		for(int row = 7; row >= 0; row--) {
			for(int column = 0; column < 8; column++) {
				if(moves.contains(new Pair(row,column))) {
					System.out.print("O");
				}
				else {
					if(board.getPiece(row, column) != null) {
						System.out.print(board.getPiece(row, column));
					}
					else {
						System.out.print("X");
					}
				}
			}
			System.out.println();
		}
	}
}
