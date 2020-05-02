package game;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import utility.Pair;

/**
 * Represents a chessboard. Can be initialized through use of the
 * initialize(FileOutputStream stream) method, where stream is an open
 * board data file. <br>
 * <br>
 * Board data files are formatted as follows. 
 * First there are 4 lines, each of which contains a single digit; either 0 or 1.
 * They represent boolean values (0 false, 1 true) and should be interpreted as follows:
 * 
 * whiteCanQueensideCastle
 * whiteCanKingsideCastle
 * blackCanQueensideCastle
 * blackCanKingsideCastle
 *
 * Where "colourCanQueensideCastle" means that colour's King hasn't
 * moved this game, and neither has colour's Queenside Rook. Similarly for
 * colourCanKingsideCastle. This information needs to be stored in files,
 * because it can't always be gleaned from just looking at the board whether
 * or not a piece has moved, as it could have done so and returned to its initial position.
 * 
 * Then, there are 8 rows of 8 characters each, not including newlines, with letters used
 * to represent chess pieces as follows: <br>
 * 		P - Pawn <br>
 * 		R - Rook <br>
 * 		N - Knight <br>
 * 		B - Bishop <br>
 * 		Q - Queen <br>
 * 		K - King <br>
 * 		X - empty square <br>
 * 		E - en passant square (see below) <br>
 * UPPERCASE letters represent white pieces <br>
 * LOWERCASE letters represent black pieces <br>
 * 'E' - represents an en passant square. That is, if the LAST MOVE made was by a pawn
 * moving two squares ahead, an E should be placed on the square the pawn hopped over.
 * <br>
 * The board should be oriented the same way as the actual board, from
 * white's perspective. That is, the first line of the file should be black's
 * back row and  the last line white's back row.<br>
 * 
 * Finally, the last line should contain either 0 or 1; 0 if it is currently white's turn,
 * 1 if it is currently black's turn. <br>
 * 
 * As an example, this is what a board file would look like before any moves have
 * been made: <br>
 * <br>
 * 1
 * 1
 * 1
 * 1
 * rnbqkbnr <br>
 * pppppppp <br>
 * XXXXXXXX <br>
 * XXXXXXXX <br>
 * XXXXXXXX <br>
 * XXXXXXXX <br>
 * PPPPPPPP <br>
 * RNBQKBNR <br>
 * 0
 * 
 * @author Lukas Willsie
 */
public class Board {
	// Represents an actual chessboard as an 8x8 matrix
	// of pieces (null for empty squares). board[0] is the
	// bottom row of the chessboard, from white's perspective,
	// and board[0][0] is bottom row, first column (from the left).
	// So board[0][0] is a1, in traditional chess notation
	private Piece[][] board;
	
	// A list of all white Pieces currently on the board
	private List<Piece> whitePieces;
	
	// A list of all black Pieces currently on the board
	private List<Piece> blackPieces;
	
	// Keeps track of what colour's turn it is. Is set when a game is loaded in through
	// the initialize() method. This information is stored in a game's board data file
	// as a 1 or 0; 1 means it's black's turn, 0 means it's white's turn
	private Colour turn;
	
	// Keeps track of whether or not there's a square that can be moved
	// to as part of an "en passant" move. There should only be one
	// of these at a time, as the ability for a player to make such
	// a move is limited to one turn. After a move is made, it should be
	// reset to null. If that move was a pawn's initial double move,
	// this should be updated accordingly
	public Pair enPassant;
	
	// Booleans that partially track whether or not white/black has the ability to castle.
	// Specifically, <colour>CanQueensideCastle means that <colour>'s King hasn't
	// moved this game, and neither has <colour>'s Queenside Rook. Similarly for
	// <colour>CanKingsideCastle.
	// Note that there are other conditions that must be met by the board for
	// a given colour to actually be able to castle; the King can't be in check,
	// all of the squares between the King and the Rook must be empty, etc. These
	// booleans just store the prerequisites regarding whether the relevant pieces
	// have moved.
	// Since the information stored in these booleans cannot always be deduced from
	// simply looking at the board, this information is stored in a game's board data file,
	// (see above) to persist across multiple instances of Board objects, and is loaded
	// into this object through the initialize() method.
	private boolean whiteCanQueensideCastle;
	private boolean whiteCanKingsideCastle;
	private boolean blackCanQueensideCastle;
	private boolean blackCanKingsideCastle;
	
	public Board() {
		this.board = new Piece[8][8];
		this.whitePieces = new ArrayList<Piece>();
		this.blackPieces = new ArrayList<Piece>();
		this.enPassant = null;
	}
	
	/**
	 * Makes the given move; attempts to move the piece at srcSquare
	 * to destSquare. Fails and returns 1 if the given move is invalid;
	 * i.e. if the move is not in the Piece at srcSquares getMoves(), or if
	 * there is no piece at srcSquare
	 * @return 0 if the move is successfully made, 1 otherwise
	 */
	public int move(Pair srcSquare, Pair destSquare) {
		// If either the source or the destination is off the board, or there is
		// no piece at the given source square, invalid move
		if( !this.validSquare(srcSquare.first(), srcSquare.second())
		 ||  this.getPiece(srcSquare.first(), srcSquare.second()) == null
		 || !this.validSquare(destSquare.first(), destSquare.second())) {
			return 1;
		}
		else {
			Piece piece = this.getPiece(srcSquare.first(), srcSquare.second());
			// If the wrong colour is trying to make a move
			if(piece.getColour() != this.turn) {
				return 1;
			}
			
			// If the destination is one of piece's valid moves
			if(piece.getMoves().contains(destSquare)) {
				// Check if the given move is being made by a King
				if(piece instanceof King) {
					// A kingside castle is characterized by a two-square
					// move to the right by a king
					if(destSquare.second() - srcSquare.second() == 2) {
						return this.kingsideCastle(piece.getColour());
					}
					// A queenside castle is characterized by a two-square
					// move to the left by a king
					else if (destSquare.second() - srcSquare.second() == -2) {
						return this.queensideCastle(piece.getColour());
					}
					// Otherwise, white can't castle anymore because the King
					// has moved and we need to record this before proceeding
					else {
						if(piece.getColour() == Colour.WHITE) {
							this.whiteCanKingsideCastle = false;
							this.whiteCanQueensideCastle = false;
						}
						else {
							this.blackCanKingsideCastle = false;
							this.blackCanQueensideCastle = false;
						}
					}
				}
				else if(piece instanceof Rook) { // If a rook is moving, we may have to update our castle booleans
					if(piece.getRow() == 0 && piece.getColumn() == 0) {
						this.whiteCanQueensideCastle = false;
					}
					if(piece.getRow() == 7 && piece.getColumn() == 0) {
						this.blackCanQueensideCastle = false;
					}
					if(piece.getRow() == 0 && piece.getColumn() == 7) {
						this.whiteCanKingsideCastle = false;
					}
					if(piece.getRow() == 7 && piece.getColumn() == 7) {
						this.blackCanKingsideCastle = false;
					}
				}				
				
				// Tell piece that its location on the board has changed
				piece.move(destSquare.first(), destSquare.second());
				
				// Check if the destination is a piece, in which case the move is a capture
				// and we need to remove the captured piece from the proper list
				Piece dest = this.getPiece(destSquare.first(), destSquare.second());
				if(dest != null) {
					List<Piece> pieceList = (dest.getColour() == Colour.WHITE) ? this.whitePieces : this.blackPieces;
					pieceList.remove(dest);
				}
				// If this is an en passant capture
				else if (destSquare.equals(this.enPassant)) {
					Colour enemyColour = (piece.getColour() == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
					int direction = (enemyColour == Colour.WHITE) ? 1 : -1;
					
					// Get the pawn which must have opened up the en passant square and capture it
					Piece capture = this.getPiece(destSquare.first()+direction, destSquare.second());
					List<Piece> pieceList = (capture.getColour() == Colour.WHITE) ? this.whitePieces : this.blackPieces;
					board[capture.getRow()][capture.getColumn()] = null;
					pieceList.remove(capture);
				}
				
				// Actually move the piece on the board
				board[piece.getRow()][piece.getColumn()] = piece;
				board[srcSquare.first()][srcSquare.second()] = null;
				
				// Reset en passant after every move
				this.enPassant = null;
				
				// We need to check if a new en passant square has opened up
				if(piece instanceof Pawn) {
					int direction = (piece.getColour() == Colour.WHITE) ? 1 : -1;
					
					// If the pawn moved 2 squares forward
					if(destSquare.first() - srcSquare.first() == 2 * direction) {
						// The new en passant square is the one between where the piece used
						// to be and where it is now
						this.enPassant = new Pair(srcSquare.first()+direction, srcSquare.second());
					}
				}
				
				// Flip turn
				this.turn = (this.turn == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
				
				return 0;
			}
			else {
				return 1;
			}
		}
	}
	
	/**
	 * "Pick up" the given piece (assumed to be on the board). This means that the board
	 * can be processed as if the piece isn't there; as if it's been literally picked up,
	 * with
	 * 
	 * @param piece
	 */
	public void pickUp(Piece piece) {
		board[piece.getRow()][piece.getColumn()] = null;
	}
	
	/**
	 * Attempt to execute a queenside castle on behalf of the given colour
	 * 
	 * @param colour - The colour that is trying to queenside castle
	 * @return 0 if the castle is successful, 1 otherwise
	 */
	private int queensideCastle(Colour colour) {
		if(!this.canQueensideCastle(colour)) {
			return 1;
		}
		
		Piece rook = this.getPiece((colour == Colour.WHITE) ? 0 : 7, 0);
		if(rook == null) { // Check in case our data was entered incorrectly and the rook actually has moved
			return 1;
		}
		
		Piece king = this.getKing(colour);
		if(king == null) {
			return 1;
		}
		
		// Check to make sure that the King can move over two squares
		if(!this.validSquare(king.getRow(), king.getColumn()-2)) {
			return 1;
		}

		// To queenside castle, move the King two squares to the left
		// and put the queenside rook beside him, on the right
		board[king.getRow()][king.getColumn()] = null;
		board[king.getRow()][king.getColumn()-2] = king;
		king.move(king.getRow(), king.getColumn()-2);
		
		board[rook.getRow()][rook.getColumn()] = null;
		board[king.getRow()][king.getColumn()+1] = rook;
		
		rook.move(king.getRow(), king.getColumn()+1);
		return 0;
	}
	
	/**
	 * Attempt to execute a kingside castle on behalf of the given colour
	 * 
	 * @param colour - The colour that is trying to kingside castle
	 * @return 0 if the castle is successful, 1 otherwise
	 */
	private int kingsideCastle(Colour colour) {
		if(!this.canKingsideCastle(colour)) {
			return 1;
		}
		
		Piece rook = this.getPiece((colour == Colour.WHITE) ? 0 : 7, 7);
		if(rook == null) { // Check in case our data was entered incorrectly and the rook actually has moved
			return 1;
		}
		
		Piece king = this.getKing(colour);
		if(king == null) {
			return 1;
		}
		
		// Check to make sure that the King can move over two squares
		if(!this.validSquare(king.getRow(), king.getColumn()+2)) {
			return 1;
		}
		
		// To kingside castle, move the King two squares to the left
		// and put the kingside rook beside him, on the right
		board[king.getRow()][king.getColumn()] = null;
		board[king.getRow()][king.getColumn()+2] = king;
		king.move(king.getRow(), king.getColumn()+2);
		
		board[rook.getRow()][rook.getColumn()] = null;
		board[king.getRow()][king.getColumn()-1] = rook;
		
		rook.move(king.getRow(), king.getColumn()-1);
		return 0;
	}
	
	// This method is for testing purposes only
	public void setPiece(Piece piece) {
		board[piece.getRow()][piece.getColumn()] = piece;
	}
	
	/**
	 * Add the given piece to the board, at the square given by
	 * its row and column attributes. Assumes the piece is not already
	 * on the board.
	 * 
	 * Has no effect if this square is already occupied.
	 * @param piece - The piece to add to the board
	 */
	public void addPiece(Piece piece) {
		if(board[piece.getRow()][piece.getColumn()] == null) {
			board[piece.getRow()][piece.getColumn()] = piece;
			if(piece.getColour() == Colour.WHITE) {
				whitePieces.add(piece);
			}
			else {
				blackPieces.add(piece);
			}
		}
	}
	
	/**
	 * Fully remove the given piece from the game.
	 * 
	 * @param piece - The piece to be removed
	 */
	public void removePiece(Piece piece) {
		List<Piece> pieceList = (piece.getColour() == Colour.WHITE) ? whitePieces : blackPieces;
		if(pieceList.contains(piece)) {
			board[piece.getRow()][piece.getColumn()] = null;
			pieceList.remove(piece);
		}
	}
	
	/**
	 * Access a particular square on the board
	 * 
	 * Precondition: row and column satisfy validSquare(row, column)
	 * 
	 * @param row - The row to access
	 * @param column - The column to access
	 * @return The piece at the specified square, or null if the square is empty 
	 */
	public Piece getPiece(int row, int column) {
		return board[row][column];
	}
	
	/**
	 * Initialize the board using scanner, where scanner is assumed to be opened at
	 * the beginning of a correctly-formatted board data file. See class Javadoc for details.
	 * 
	 * @param scanner - A Scanner open to the beginning of a valid board data file
	 * @return 0 if the initialization succeeded and no invalid input was encountered, 1 if otherwise
	 */
	public int initialize(Scanner scanner) {
		String line;

		int lineNumber = 0;
		// Read in the castle booleans
		while(lineNumber < 4 && scanner.hasNextLine()) {
			line = scanner.nextLine();
			
			try {
				int bool = Integer.parseInt(line);
				if(bool > 1 || bool < 0) {
					return 1;
				}
				
				
				switch(lineNumber) {
					case 0:
						this.whiteCanQueensideCastle = (bool == 1) ? true : false;
						break;
					case 1:
						this.whiteCanKingsideCastle = (bool == 1) ? true : false;
						break;
					case 2:
						this.blackCanQueensideCastle = (bool == 1) ? true : false;
						break;
					case 3:
						this.blackCanKingsideCastle = (bool == 1) ? true : false;
						break;
				}
			}
			catch(NumberFormatException e) {
				return 1;
			}
			lineNumber++;
		}
		
		int row = 7;
		int column = 0;
		while(scanner.hasNextLine() && row >= 0) {
			line = scanner.nextLine();
			while(column < 8) {
				Colour colour = Character.isLowerCase(line.charAt(column)) ? Colour.BLACK : Colour.WHITE;
				switch((line.toLowerCase()).charAt(column)) {
					case 'p':
						this.addPiece(new Pawn(row, column, colour,this));
						break;
					case 'r':
						this.addPiece(new Rook(row, column, colour,this));
						break;
					case 'n':
						this.addPiece(new Knight(row, column, colour,this));
						break;
					case 'b':
						this.addPiece(new Bishop(row, column, colour,this));
						break;
					case 'q':
						this.addPiece(new Queen(row, column, colour,this));
						break;
					case 'k':
						this.addPiece(new King(row, column, colour,this));
						break;
					case 'x':
						break;
					case 'e':
						this.enPassant = new Pair(row, column);
					default:
						return 1;
						
				}
				column++;
			}
			row--;
			column = 0;
		}
		
		// Initialize the turn variable
		line = scanner.nextLine();
		try {
			int turn = Integer.parseInt(line);
			if(turn > 1 || turn < 0) {
				return 1;
			}
			
			this.turn = (turn == 1) ? Colour.BLACK : Colour.WHITE;
		}
		catch(NumberFormatException e) {
			
		}
		
		return 0;
	}
	
	/**
	 * Preserve the game as it is at the moment this method is called by saving it in a file. 
	 * The given FileOutputStream is assumed to be opened to the exact spot that this program
	 * should start writing to
	 * 
	 * @param stream - The stream that this method should write the game data to
	 * @throws IOException - If something goes wrong with stream, does not try to catch the
	 * exception
	 */
	public void saveGame(FileOutputStream stream) throws IOException {
		String line;
		line = this.whiteCanQueensideCastle ? "1\n" : "0\n";
		stream.write(line.getBytes());
		
		line = this.whiteCanKingsideCastle ? "1\n" : "0\n";
		stream.write(line.getBytes());
		
		line = this.blackCanQueensideCastle ? "1\n" : "0\n";
		stream.write(line.getBytes());
		
		line = this.blackCanKingsideCastle ? "1\n" : "0\n";
		stream.write(line.getBytes());
		
		for(int row = 7; row >= 0; row--) {
			for(int column = 0; column < 8; column++) {
				Piece piece = this.getPiece(row, column);
				if(piece instanceof Pawn) {
					line = (piece.getColour() == Colour.WHITE) ? "P" : "p";
					stream.write(line.getBytes());
				}
				else if(piece instanceof Rook) {
					line = (piece.getColour() == Colour.WHITE) ? "R" : "r";
					stream.write(line.getBytes());				
				}
				else if(piece instanceof Knight) {
					line = (piece.getColour() == Colour.WHITE) ? "N" : "n";
					stream.write(line.getBytes());
				}
				else if(piece instanceof Bishop) {
					line = (piece.getColour() == Colour.WHITE) ? "B" : "b";
					stream.write(line.getBytes());
				}
				else if(piece instanceof Queen) {
					line = (piece.getColour() == Colour.WHITE) ? "Q" : "q";
					stream.write(line.getBytes());
				}
				else if(piece instanceof King) {
					line = (piece.getColour() == Colour.WHITE) ? "K" : "k";
					stream.write(line.getBytes());
				}
				else {
					stream.write("X".getBytes());
				}
			}
			stream.write("\n".getBytes());
		}
		
		line = (this.turn == Colour.WHITE) ? "0\n" : "1\n";
		stream.write(line.getBytes());
	}
	
	/**
	 * Compute whether or not the given colour is in check. Assumes that
	 * the given colour has one and only one King on the board.
	 * 
	 * @param colour - The colour
	 * @return A boolean representing whether or not the given colour
	 * is in check
	 */
	public boolean isCheck(Colour colour) {
		// Get the list of enemy pieces
		List<Piece> pieceList = (colour == Colour.WHITE) ? this.blackPieces : this.whitePieces;
		
		for(Piece piece : pieceList) {
			if(piece.isCheckingKing()) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Compute whether or not the given colour has been checkmated
	 * @param colour - The colour to check
	 * @return A boolean representing whether or not the given colour
	 * has been checkmated
	 */
	public boolean isCheckmate(Colour colour) {
		// A colour is in checkmate if its King is in check, all the squares
		// around it are protected by enemy pieces (it's getMoves() is empty),
		// and no allied pieces can move and block or break the check
		return this.getKing(colour) != null
			&& this.getMoves(colour).size() == 0;
	}
	
	/**
	 * Determine whether or not there is a piece at the square given
	 * by row,column.
	 * 
	 * @param row - The row of the square
	 * @param column - The column of the square
	 * @return true if and only if row,column is a valid square and
	 * the entry at board[row][column] is non-null
	 */
	public boolean isPiece(int row, int column) {
		return validSquare(row, column) && board[row][column] != null;
	}
	
	/**
	 * Determine whether a given square is "on the board"
	 * @param row - The row that the square is on
	 * @param column - The column that the square is on
	 * @return true if and only if (row,column) is a valid square
	 */
	public boolean validSquare(int row, int column) {
		return 0 <= row && row <= 7 && 0 <= column && column <= 7;
	}
	
	/**
	 * Compute whether or not the given square is empty.
	 * 
	 * If (row,column) is not a valid square (i.e. out of bounds),
	 * returns false
	 * @param row - The row of the square
	 * @param column - the column of the square
	 * @return true if and only if (row,column) is a valid square
	 * which does not have a piece on it
	 */
	public boolean isEmpty(int row, int column) {
		if(validSquare(row, column)) {
			return  board[row][column] == null;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Takes a list of squares representing moves that can be made by a non-King piece of the given colour.
	 * Returns the subset of the given list containing all legal moves, taking into account
	 * whether or not the King is in check. This method exists because if a piece's King is in
	 * check, it can only make moves that take the King out of check, and a Piece by itself
	 * does not have the information to take this into account.
	 * 
	 * If the given colour is NOT in check, simply returns the original list.
	 * 
	 * @param moves - The list of moves to pare down according to legality
	 * @param colour - The colour that the moves belong to
	 * @return A subset of the given list containing all the legal moves in the original list
	 */
	public List<Pair> getLegal(List<Pair> moves, Colour colour) {
		if(this.isCheck(colour)) {
			List<Piece> checkingPieces = this.getCheckingPieces(colour);
			if(checkingPieces.size() > 1) {   // If the colour's King is being checked by multiple pieces,
				return new ArrayList<Pair>(); // the only valid move for that whole colour is for the King
			}								  // to move and escape check
			
			Piece checkingPiece = checkingPieces.get(0);
			
			if(this.isBlockable(checkingPiece)) {
				// Obtain a list of all squares that can be moved to that will
				// block the checking piece
				List<Pair> blockingMoves = this.getBlockingSquares(checkingPiece);
				
				List<Pair> legalMoves = new ArrayList<Pair>();
				for(Pair move : moves) {
					// A move is legal if it blocks or captures the checker
					if(blockingMoves.contains(move)
					|| move.equals(new Pair(checkingPiece.getRow(), checkingPiece.getColumn()))) {
						legalMoves.add(move);
					}
				}
				
				return legalMoves;
			}
			else {
				List<Pair> legalMoves = new ArrayList<Pair>();
				for(Pair move : moves) {
					// A move is legal only if it captures the checker, since the checker isn't blockable
					if(move.equals(new Pair(checkingPiece.getRow(), checkingPiece.getColumn()))) {
						legalMoves.add(move);
						break; // There is only a single possible legal move, so break after we find it
					}
				}
				
				return legalMoves;
			}
		}
		else { // If the piece's colour is not in check, all moves it can make are legal
			return moves;
		}
	}
	
	/**
	 * Assuming that piece is a BLOCKABLE piece (see below method) and is currently
	 * checking the King of the opposite colour, return all squares such that, if there
	 * was a piece of the opposite colour there, the check would be broken. For example:
	 * 
	 * r
	 * X
	 * X
	 * X
	 * K
	 * 
	 * Every square between the Black Rook and White King is a blocking square here, for
	 * the rook.
	 * 
	 * Result is undefined if piece is not blockable, or is not checking the enemy King.
	 * 
	 * @param piece - A blockable (Queen, Bishop, or Rook) Piece
	 * @return
	 */
	private List<Pair> getBlockingSquares(Piece piece) {		
		// Obtain a reference to the King this piece is giving check to
		Piece king = getKing((piece.getColour() == Colour.WHITE) ? Colour.BLACK : Colour.WHITE);
		
		// Since piece must either be a Queen, Bishop, or Rook, we assume that we can
		// get from the king to piece by moving either in a diagonal or a straight line.
		// That is, we start at the King and add one of 1, -1, or 0 each to row and column
		// until we hit piece. So we simply determine what direction we're moving in with
		// respect to the row and column, and add each empty square found until we
		// encounter the given piece.
		int rowDirection = sign(piece.getRow() - king.getRow());
		int columnDirection = sign(piece.getColumn() - king.getColumn());
		
		List<Pair> blockingSquares = new ArrayList<Pair>();
		
		int checkRow = king.getRow()+rowDirection;
		int checkColumn = king.getColumn()+columnDirection;
		// Travel toward piece, adding every empty square we find until we hit piece,
		// at which point our while loop will break
		while(this.validSquare(checkRow, checkColumn) && this.isEmpty(checkRow, checkColumn) ) {
			blockingSquares.add(new Pair(checkRow, checkColumn));
			
			checkRow += rowDirection;
			checkColumn += columnDirection;
		}
		
		return blockingSquares;
	}
	
	/**
	 * Return the King with the given colour, or null if no such King exists
	 * 
	 * @param colour - The colour whose King should be searched for.
	 * @return A reference to the King with the given colour
	 */
	public Piece getKing(Colour colour) {
		List<Piece> pieceList = (colour == Colour.WHITE) ? this.whitePieces : this.blackPieces;
		
		for(Piece piece : pieceList) {
			if(piece instanceof King) {
				return piece;
			}
		}
		
		return null;
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
	 * Compute whether or not the given piece is blockable, meaning generally that
	 * if it is giving check to the enemy King, it is theoretically feasible for
	 * an enemy piece to be placed such that the check is broken, without capturing
	 * the piece.
	 * 
	 * The blockable pieces are Queens, Bishops, and Rooks.
	 * 
	 * The unblockable pieces are Pawns, Knights, and Kings
	 * 
	 * @param piece
	 * @return
	 */
	private boolean isBlockable(Piece piece) {
		return piece instanceof Queen
			|| piece instanceof Bishop
			|| piece instanceof Rook;
	}
	
	/**
	 * Compute whether or not the given colour can castle on the queenside
	 * 
	 * @param colour - The colour of concern
	 * @return true if and only if the given colour is able to castle on the
	 * queenside at this moment in the game
	 */
	public boolean canQueensideCastle(Colour colour) {
		// First check that the given colour's King and queenside Rook haven't moved
		boolean piecesHaventMoved = (colour == Colour.WHITE) ? this.whiteCanQueensideCastle : this.blackCanQueensideCastle;
		if(!piecesHaventMoved) {
			System.out.println("Can't Queenside castle because pieces have moved");
			return false;
		}
		
		Piece king = this.getKing(colour);
		if(king == null) {
			System.out.println("Can't castle because no king");
			return false;
		}
		
		// We need to check 3 things:
		// 1) That the King isn't in check
		// 2) That all the squares between the King and Rook are empty
		// 3) That none of the squares between the King and Rook are being
		//	  attacked by the enemy colour
		boolean check = this.isCheck(colour);
		if(check) {
			return false;
		}
		
		Colour enemyColour = (colour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
		List<Pair> enemyProtectedSquares = this.getProtectedSquares(enemyColour);
		
		// Iterate to the left of the King until just short of the Rook (who we know hasn't moved),
		// making sure that every square is empty and un-attacked by the enemy
		int row = king.getRow();
		int column = king.getColumn()-1;
		while(column > 0) {
			if(!isEmpty(row, column)
			||  Collections.binarySearch(enemyProtectedSquares, new Pair(row, column)) >= 0) {
				System.out.println("Can't castle because " + new Pair(row,column) + " is not empty or is attacked");
				return false;
			}
			column -= 1;
		}
		
		return true;
	}
	
	/**
	 * Compute whether or not the given colour can castle on the kingside
	 * 
	 * @param colour - The colour of concern
	 * @return true if and only if the given colour is able to castle on the
	 * kingside at this moment in the game
	 */
	public boolean canKingsideCastle(Colour colour) {
		// First check that the given colour's King and queenside Rook haven't moved
		boolean piecesHaventMoved = (colour == Colour.WHITE) ? this.whiteCanKingsideCastle : this.blackCanKingsideCastle;
		if(!piecesHaventMoved) {
			System.out.println("Can't castle because pieces have moved");
			return false;
		}
		
		Piece king = this.getKing(colour);
		if(king == null) {
			System.out.println("Can't castle because no king");
			return false;
		}
		// We need to check 3 things:
		// 1) That the King isn't in check
		// 2) That all the squares between the King and Rook are empty
		// 3) That none of the squares between the King and Rook are being
		//	  attacked by the enemy colour
		boolean check = this.isCheck(colour);
		if(check) {
			return false;
		}
		
		Colour enemyColour = (colour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
		List<Pair> enemyProtectedSquares = this.getProtectedSquares(enemyColour);
		
		// Iterate to the right of the King until just short of the Rook (who we know hasn't moved),
		// making sure that every square is empty and un-attacked by the enemy
		int row = king.getRow();
		int column = king.getColumn()+1;
		while(column < 7) {
			if(!isEmpty(row, column)
			||  Collections.binarySearch(enemyProtectedSquares, new Pair(row, column)) >= 0) {
				System.out.println("Can't castle because " + new Pair(row, column) + " is not empty or is attacked");
				return false;
			}
			column += 1;
		}
		
		System.out.println("Can kingside castle");
		return true;
	}
	
	/**
	 * Return a list of pieces checking the given colour's King.
	 * Empty if the given colour is not in check.
	 * 
	 * @param colour - The colour whose King's checkers we are returning
	 * @return A list of Pieces checking the given colour's King
	 */
	private List<Piece> getCheckingPieces(Colour colour) {
		List<Piece> enemyPieces = (colour == Colour.WHITE) ? this.blackPieces : this.whitePieces;
		
		Piece king = getKing(colour);
		if(king == null) {
			return new ArrayList<Piece>();
		}
		Pair kingSquare = new Pair(king.getRow(), king.getColumn());
		
		List<Piece> checkers = new ArrayList<Piece>();
		
		for(Piece enemy : enemyPieces) {
			if(enemy.getMoves().contains(kingSquare)) {
				checkers.add(enemy);
			}
		}
		
		return checkers;
	}
	
	/**
	 * Compute whether or not a piece with the given colour could move to
	 * the given square, either because it is empty or there is an enemy
	 * piece on it.
	 * 
	 * @param row - The row of the square
	 * @param column - The column of the square
	 * @param colour - The colour of the piece considering moving to the square
	 * @return true if and only if (row,column) is a valid square and is either
	 * empty, or contains a piece with Colour different from colour
	 */
	public boolean isMovable(int row, int column, Colour colour) {
		return validSquare(row,column) && (board[row][column] == null || board[row][column].getColour() != colour);
	}
	
	/**
	 * Compute whether or not the given piece is PINNED, which means that this piece
	 * stands between its King and some attacking enemy piece. For example, if a White
	 * Queen and a Black King are in the same row with a Black Rook between them, the
	 * Rook is pinned. Note that this doesn't necessarily mean that the piece can't move
	 * at all (in our case our Rook can move back and forth in its row, just not up and
	 * down), just that its movement is restricted.
	 * 
	 * If the piece IS pinned, return the enemy piece that is pinning it (there can be only
	 * one). Otherwise return null.
	 * 
	 * @param piece - The piece of concern
	 * @return null if the given piece is NOT pinned, or a reference to the enemy piece that
	 * is pinning this piece if it is
	 */
	public Piece isPinned(Piece piece) {
		List<Piece> pieceList = (piece.getColour() == Colour.WHITE) ? whitePieces : blackPieces;
		King king = null;
		for(Piece allyPiece : pieceList) {
			if(allyPiece instanceof King) {
				king = (King)allyPiece;
				break;
			}
		}
		// If the piece has NO allied King, simply return null
		if(king == null) {
			return null;
		}
		
		// Arrays used to automate checking the rook's file and rank 
		int[] row_increments = 	  {0, 1,  0, -1};
		int[] column_increments = {1, 0, -1,  0};
		
		// Radiate from the king outward in all 4 cardinal directions
		// (not diagonals yet). The given piece is pinned if we encounter
		// 0 or more empty squares followed by the piece followed by
		// 0 or more empty squares followed by an enemy Queen or Rook
		int checkRow = king.getRow();
		int checkColumn = king.getColumn();
		int row_increment = 0;
		int column_increment = 0;
		// Keeps track of whether or not we've run into piece yet
		boolean foundPiece = false;
		for(int i = 0; i < row_increments.length; i++) {
			row_increment = row_increments[i];
			column_increment = column_increments[i];
			
			checkRow += row_increment;
			checkColumn += column_increment;
			while(validSquare(checkRow, checkColumn))
			{
				if(foundPiece) { // If we've found the piece, start looking for an enemy Rook or Queen
					if(enemyRookOrQueen(piece, checkRow, checkColumn)) {
						return board[checkRow][checkColumn];
					}
					// If we've reached a square that is occupied by any piece, ally or enemy, 
					// return null (since we know the piece is not a Rook or Queen thanks
					// to the above if statement)
					else if(!isEmpty(checkRow, checkColumn)) {
						return null;
					}
				}
				else {
					if(board[checkRow][checkColumn] == piece) {
						foundPiece = true;
					}
					// If we've found any piece other than the one we're looking for,
					// we're not going to find the given piece pinned in this row/column,
					// so move on
					else if(!isEmpty(checkRow, checkColumn)) {
						break;
					}
				}
				checkRow += row_increment;
				checkColumn += column_increment;
			}
			checkRow = king.getRow();
			checkColumn = king.getColumn();
		}
		
		
		int[] row_offsets =    {1,  1, -1, -1};
		int[] column_offsets = {1, -1, -1,  1};
		
		// Radiate outward from the king along all 4 diagonals. The given piece is pinned
		// if we encounter 0 or more empty squares followed by the piece followed by
		// 0 or more empty squares followed by a Bishop or Queen
		checkRow = king.getRow();
		checkColumn = king.getColumn();
		foundPiece = false;
		for(int i = 0; i < row_offsets.length; i++) {
			row_increment = row_offsets[i];
			column_increment = column_offsets[i];
			
			checkRow += row_increment;
			checkColumn += column_increment;
			while(validSquare(checkRow, checkColumn)) { 
				if(foundPiece) {
					if(enemyBishopOrQueen(piece, checkRow, checkColumn)) {
						return board[checkRow][checkColumn];
					}
					else if(!isEmpty(checkRow, checkColumn)) {
						return null;
					}
				}
				else {
					if(board[checkRow][checkColumn] == piece) {
						foundPiece = true;
					}
					// If we've found any piece other than the one we're looking for,
					// we're not going to find the given piece pinned in this row/column,
					// so move on
					else if(!isEmpty(checkRow, checkColumn)) {
						break;
					}
				}
				
				checkRow += row_increment;
				checkColumn += column_increment;
			}
			
			checkRow = king.getRow();
			checkColumn = king.getColumn();
		}
		
		return null;
	}
	
	/**
	 * Compute whether or not the given square contains a Rook or Queen
	 * which is an enemy of the given piece
	 * @param piece - Determines what colour of Rooks/Queens to look for
	 * @param row - The row of the square to check
	 * @param column - The column of the square to check
	 * @return true if and only if the given square contains a Rook or Queen with
	 * colour opposite to that of the given piece
	 */
	private boolean enemyRookOrQueen(Piece piece, int row, int column) {
		return  validSquare(row, column) && !isEmpty(row, column)
			&&  board[row][column].getColour() != piece.getColour()
			&& (board[row][column] instanceof Rook || board[row][column] instanceof Queen);
	}
	
	/**
	 * Compute whether or not the given square contains a Bishop or Queen which is an enemy
	 * of the given piece
	 * @param piece - Determines what colour of Rooks/Queens to look for
	 * @param row - The row of the square to check
	 * @param column - The column of the square to check
	 * @return true if and only if the given square contains a Bishop or Queen
	 * with colour opposite to that of the given piece
	 */
	private boolean enemyBishopOrQueen(Piece piece, int row, int column) {
		return  validSquare(row, column) && !isEmpty(row, column)
			&&  board[row][column].getColour() != piece.getColour()
			&& (board[row][column] instanceof Bishop|| board[row][column] instanceof Queen);
	}
	
	/**
	 * Return a list of all squares that pieces with the given colour
	 * can move to, sorted according to the ordering imposed on Pairs.
	 * See compareTo() in the Pair class for details.
	 * 
	 * @param colour - The colour to be searched for
	 * @return A list of all squares that pieces with the given colour
	 * can move to
	 */
	public List<Pair> getMoves(Colour colour) {
		List<Pair> moves = new ArrayList<Pair>();
		List<Pair> pieceMoves;
		List<Piece> pieceList;
		if(colour == Colour.WHITE) {
			pieceList = whitePieces;
		}
		else {
			pieceList = blackPieces;
		}
		
		for(Piece piece : pieceList) {
			pieceMoves = piece.getMoves();
			for(Pair pair : pieceMoves) {
				insert(moves, pair);
			}
		}
		
		return moves;
	}
	
	/**
	 * Assuming that the given square is diagonally in front of the given pawn,
	 * compute whether or not moving to the given square is a valid EN PASSANT
	 * move for the pawn.
	 * 
	 * @param square - The square under consideration
	 * @param pawn - The pawn under consideration
	 * @return true if and only if 
	 */
	public boolean isEnPassant(Pair square, Pawn pawn) {
		if(pawn.getColour() == Colour.WHITE) {
			// If the Pawn is White, the only en passant moves it can make are into
			// the row directly in front of Black's pawns; row 5. This makes sure that
			// a White pawn can never accidentally capture an ally Pawn in en passant
			return square != null && square.equals(this.enPassant) && square.first() == 5;
		}
		else {
			// If the Pawn is Black, the only en passant moves it can make are into
			// the row directly in front of White's pawns; row 2. This makes sure that
			// a Black pawn can never accidentally capture an ally Pawn in en passant
			return square != null && square.equals(this.enPassant) && square.first() == 2;			
		}
	}
 	
	/**
	 * Return a list of every single square that pieces with the given colour
	 * are protecting, sorted according to the ordering imposed on Pairs.
	 * See compareTo() in the Pair class for details.
	 * 
	 * @param colour - The colour to be searched for
	 * @return A list of all squares that pieces with the given colour
	 * are protecting
	 */
	public List<Pair> getProtectedSquares(Colour colour) {
		List<Pair> protected_squares = new ArrayList<Pair>();
		List<Pair> pieceProtectedSquares;
		List<Piece> pieceList;
		if(colour == Colour.WHITE) {
			pieceList = whitePieces;
		}
		else {
			pieceList = blackPieces;
		}
		
		for(Piece piece : pieceList) {
			pieceProtectedSquares = piece.getProtectedSquares();
			for(Pair pair : pieceProtectedSquares) {
				insert(protected_squares, pair);
			}
		}
		
		return protected_squares;
	}
	
	/**
	 * Inserts the given Pair into list WITHOUT REPITITION so that list
	 * is sorted in increasing order. In this case, that means primarily
	 * in order of the first element, secondarily in order of the second.
	 * See implementation of compareTo() in Pair for details.
	 * 
	 * Precondition: list is sorted in increasing order when this operation begins
	 * @param list
	 * @param pair
	 */
	public void insert(List<Pair> list, Pair pair) {
		int i = 0;
		while(i < list.size() && list.get(i).compareTo(pair) < 0) {
			i++;
		}
		
		if(i == list.size()) {
			list.add(pair);
		}
		// Only insert if pair is not already in list
		else if(!pair.equals(list.get(i))) {
			list.add(i, pair);
		}
	}
	
	/**
	 * Print the board using the same format as described in the class Javadoc
	 * 
	 *@return A string representation of the board
	 */
	@Override
	public String toString() {
		StringBuilder rep = new StringBuilder();
		Piece[] row;
		for(int i = 7; i >= 0; i--) {
			row = this.board[i];
			for(Piece piece : row) {
				if(piece == null) {
					rep.append("X");
				}
				else {
					rep.append(piece.toString());
				}
			}
			rep.append("\n");
		}
		
		return rep.toString();
	}
}
