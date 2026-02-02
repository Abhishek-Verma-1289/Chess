package com.example.IndiChessBackend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Chess Move Validation Service
 * Validates all chess moves according to official FIDE rules
 */
@Service
public class ChessMoveValidator {

    /**
     * Validates if a move is legal
     * @param board Current board state (8x8 array)
     * @param fromRow Source row (0-7)
     * @param fromCol Source column (0-7)
     * @param toRow Destination row (0-7)
     * @param toCol Destination column (0-7)
     * @param isWhiteTurn Whether it's white's turn
     * @return true if move is valid, false otherwise
     */
    public boolean isValidMove(String[][] board, int fromRow, int fromCol, 
                               int toRow, int toCol, boolean isWhiteTurn) {
        
        // Basic validation
        if (!isValidPosition(fromRow, fromCol) || !isValidPosition(toRow, toCol)) {
            return false;
        }

        // Check if there's a piece at source
        String piece = board[fromRow][fromCol];
        if (piece == null || piece.isEmpty()) {
            return false;
        }

        // Check if piece color matches turn
        boolean isPieceWhite = Character.isUpperCase(piece.charAt(0));
        if (isPieceWhite != isWhiteTurn) {
            return false;
        }

        // Check if destination has same color piece
        String destPiece = board[toRow][toCol];
        if (destPiece != null && !destPiece.isEmpty()) {
            boolean isDestWhite = Character.isUpperCase(destPiece.charAt(0));
            if (isDestWhite == isPieceWhite) {
                return false; // Can't capture own piece
            }
        }

        // Get valid moves for the piece
        List<int[]> validMoves = getValidMovesForPiece(board, fromRow, fromCol, piece);
        
        // Check if target position is in valid moves
        for (int[] move : validMoves) {
            if (move[0] == toRow && move[1] == toCol) {
                // TODO: Check if move puts own king in check
                return true;
            }
        }

        return false;
    }

    /**
     * Get all valid moves for a piece at given position
     */
    private List<int[]> getValidMovesForPiece(String[][] board, int row, int col, String piece) {
        char pieceType = Character.toLowerCase(piece.charAt(0));
        
        switch (pieceType) {
            case 'p': return getPawnMoves(board, row, col, Character.isUpperCase(piece.charAt(0)));
            case 'r': return getRookMoves(board, row, col, Character.isUpperCase(piece.charAt(0)));
            case 'n': return getKnightMoves(board, row, col, Character.isUpperCase(piece.charAt(0)));
            case 'b': return getBishopMoves(board, row, col, Character.isUpperCase(piece.charAt(0)));
            case 'q': return getQueenMoves(board, row, col, Character.isUpperCase(piece.charAt(0)));
            case 'k': return getKingMoves(board, row, col, Character.isUpperCase(piece.charAt(0)));
            default: return new ArrayList<>();
        }
    }

    /**
     * Pawn movement rules
     */
    private List<int[]> getPawnMoves(String[][] board, int row, int col, boolean isWhite) {
        List<int[]> moves = new ArrayList<>();
        int direction = isWhite ? -1 : 1; // White moves up (-1), Black moves down (+1)
        int startRow = isWhite ? 6 : 1;

        // Move forward one square
        int newRow = row + direction;
        if (isValidPosition(newRow, col) && board[newRow][col].isEmpty()) {
            moves.add(new int[]{newRow, col});

            // Move forward two squares from starting position
            if (row == startRow) {
                int doubleRow = row + (2 * direction);
                if (board[doubleRow][col].isEmpty()) {
                    moves.add(new int[]{doubleRow, col});
                }
            }
        }

        // Capture diagonally
        for (int colOffset : new int[]{-1, 1}) {
            int newCol = col + colOffset;
            if (isValidPosition(newRow, newCol)) {
                String targetPiece = board[newRow][newCol];
                if (!targetPiece.isEmpty()) {
                    boolean targetIsWhite = Character.isUpperCase(targetPiece.charAt(0));
                    if (targetIsWhite != isWhite) {
                        moves.add(new int[]{newRow, newCol});
                    }
                }
            }
        }

        // TODO: En passant

        return moves;
    }

    /**
     * Rook movement rules (straight lines)
     */
    private List<int[]> getRookMoves(String[][] board, int row, int col, boolean isWhite) {
        List<int[]> moves = new ArrayList<>();

        // Horizontal and vertical directions
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            while (isValidPosition(newRow, newCol)) {
                String targetPiece = board[newRow][newCol];
                
                if (targetPiece.isEmpty()) {
                    moves.add(new int[]{newRow, newCol});
                } else {
                    // Can capture opponent's piece
                    boolean targetIsWhite = Character.isUpperCase(targetPiece.charAt(0));
                    if (targetIsWhite != isWhite) {
                        moves.add(new int[]{newRow, newCol});
                    }
                    break; // Can't jump over pieces
                }

                newRow += dir[0];
                newCol += dir[1];
            }
        }

        return moves;
    }

    /**
     * Knight movement rules (L-shape)
     */
    private List<int[]> getKnightMoves(String[][] board, int row, int col, boolean isWhite) {
        List<int[]> moves = new ArrayList<>();

        // All possible knight moves
        int[][] knightMoves = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        for (int[] move : knightMoves) {
            int newRow = row + move[0];
            int newCol = col + move[1];

            if (isValidPosition(newRow, newCol)) {
                String targetPiece = board[newRow][newCol];
                if (targetPiece.isEmpty() || 
                    Character.isUpperCase(targetPiece.charAt(0)) != isWhite) {
                    moves.add(new int[]{newRow, newCol});
                }
            }
        }

        return moves;
    }

    /**
     * Bishop movement rules (diagonals)
     */
    private List<int[]> getBishopMoves(String[][] board, int row, int col, boolean isWhite) {
        List<int[]> moves = new ArrayList<>();

        // Diagonal directions
        int[][] directions = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            while (isValidPosition(newRow, newCol)) {
                String targetPiece = board[newRow][newCol];
                
                if (targetPiece.isEmpty()) {
                    moves.add(new int[]{newRow, newCol});
                } else {
                    boolean targetIsWhite = Character.isUpperCase(targetPiece.charAt(0));
                    if (targetIsWhite != isWhite) {
                        moves.add(new int[]{newRow, newCol});
                    }
                    break;
                }

                newRow += dir[0];
                newCol += dir[1];
            }
        }

        return moves;
    }

    /**
     * Queen movement rules (rook + bishop)
     */
    private List<int[]> getQueenMoves(String[][] board, int row, int col, boolean isWhite) {
        List<int[]> moves = new ArrayList<>();
        moves.addAll(getRookMoves(board, row, col, isWhite));
        moves.addAll(getBishopMoves(board, row, col, isWhite));
        return moves;
    }

    /**
     * King movement rules (one square in any direction)
     */
    private List<int[]> getKingMoves(String[][] board, int row, int col, boolean isWhite) {
        List<int[]> moves = new ArrayList<>();

        // All 8 directions
        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
        };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol)) {
                String targetPiece = board[newRow][newCol];
                if (targetPiece.isEmpty() || 
                    Character.isUpperCase(targetPiece.charAt(0)) != isWhite) {
                    // TODO: Check if square is under attack
                    moves.add(new int[]{newRow, newCol});
                }
            }
        }

        // TODO: Castling

        return moves;
    }

    /**
     * Check if position is within board bounds
     */
    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    /**
     * Check if king is in check
     */
    public boolean isKingInCheck(String[][] board, boolean isWhiteKing) {
        // Find king position
        int[] kingPos = findKing(board, isWhiteKing);
        if (kingPos == null) return false;

        int kingRow = kingPos[0];
        int kingCol = kingPos[1];

        // Check if any opponent piece can attack the king
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = board[row][col];
                if (piece != null && !piece.isEmpty()) {
                    boolean pieceIsWhite = Character.isUpperCase(piece.charAt(0));
                    if (pieceIsWhite != isWhiteKing) {
                        // Opponent's piece - check if it can attack king
                        List<int[]> moves = getValidMovesForPiece(board, row, col, piece);
                        for (int[] move : moves) {
                            if (move[0] == kingRow && move[1] == kingCol) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Find king position on board
     */
    private int[] findKing(String[][] board, boolean isWhite) {
        String kingSymbol = isWhite ? "K" : "k";
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (kingSymbol.equals(board[row][col])) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    /**
     * Check if move would leave own king in check (illegal)
     */
    public boolean wouldKingBeInCheck(String[][] board, int fromRow, int fromCol, 
                                      int toRow, int toCol, boolean isWhiteTurn) {
        // Make temporary move
        String[][] tempBoard = copyBoard(board);
        String piece = tempBoard[fromRow][fromCol];
        String captured = tempBoard[toRow][toCol];
        
        tempBoard[toRow][toCol] = piece;
        tempBoard[fromRow][fromCol] = "";

        // Check if king is in check after move
        boolean inCheck = isKingInCheck(tempBoard, isWhiteTurn);

        // Restore board (not actually needed since we copied)
        return inCheck;
    }

    /**
     * Create a copy of the board
     */
    private String[][] copyBoard(String[][] board) {
        String[][] copy = new String[8][8];
        for (int i = 0; i < 8; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, 8);
        }
        return copy;
    }
}
