package com.example.IndiChessBackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChessMoveValidator
 */
class ChessMoveValidatorTest {

    private ChessMoveValidator validator;
    private String[][] emptyBoard;

    @BeforeEach
    void setUp() {
        validator = new ChessMoveValidator();
        emptyBoard = createEmptyBoard();
    }

    private String[][] createEmptyBoard() {
        String[][] board = new String[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = "";
            }
        }
        return board;
    }

    private String[][] createStandardBoard() {
        return new String[][]{
            {"r", "n", "b", "q", "k", "b", "n", "r"},
            {"p", "p", "p", "p", "p", "p", "p", "p"},
            {"", "", "", "", "", "", "", ""},
            {"", "", "", "", "", "", "", ""},
            {"", "", "", "", "", "", "", ""},
            {"", "", "", "", "", "", "", ""},
            {"P", "P", "P", "P", "P", "P", "P", "P"},
            {"R", "N", "B", "Q", "K", "B", "N", "R"}
        };
    }

    @Test
    void testPawnMoveForwardOneSquare() {
        String[][] board = createStandardBoard();
        assertTrue(validator.isValidMove(board, 6, 4, 5, 4, true), 
                   "White pawn should move forward one square");
        assertTrue(validator.isValidMove(board, 1, 4, 2, 4, false), 
                   "Black pawn should move forward one square");
    }

    @Test
    void testPawnMoveForwardTwoSquaresFromStart() {
        String[][] board = createStandardBoard();
        assertTrue(validator.isValidMove(board, 6, 4, 4, 4, true), 
                   "White pawn should move forward two squares from starting position");
        assertTrue(validator.isValidMove(board, 1, 4, 3, 4, false), 
                   "Black pawn should move forward two squares from starting position");
    }

    @Test
    void testPawnCannotMoveForwardTwoSquaresAfterStart() {
        String[][] board = createEmptyBoard();
        board[5][4] = "P";
        assertFalse(validator.isValidMove(board, 5, 4, 3, 4, true), 
                    "White pawn should not move forward two squares if not at starting position");
    }

    @Test
    void testPawnDiagonalCapture() {
        String[][] board = createEmptyBoard();
        board[5][4] = "P";
        board[4][5] = "p"; // Black pawn to capture
        assertTrue(validator.isValidMove(board, 5, 4, 4, 5, true), 
                   "White pawn should capture diagonally");
    }

    @Test
    void testRookMovesHorizontally() {
        String[][] board = createEmptyBoard();
        board[0][0] = "R";
        assertTrue(validator.isValidMove(board, 0, 0, 0, 7, true), 
                   "Rook should move horizontally");
    }

    @Test
    void testRookMovesVertically() {
        String[][] board = createEmptyBoard();
        board[0][0] = "R";
        assertTrue(validator.isValidMove(board, 0, 0, 7, 0, true), 
                   "Rook should move vertically");
    }

    @Test
    void testRookCannotJumpOverPieces() {
        String[][] board = createEmptyBoard();
        board[0][0] = "R";
        board[0][4] = "P"; // Blocking piece
        assertFalse(validator.isValidMove(board, 0, 0, 0, 7, true), 
                    "Rook should not jump over pieces");
    }

    @Test
    void testKnightMovesInLShape() {
        String[][] board = createStandardBoard();
        assertTrue(validator.isValidMove(board, 7, 1, 5, 2, true), 
                   "Knight should move in L-shape");
        assertTrue(validator.isValidMove(board, 7, 1, 5, 0, true), 
                   "Knight should move in L-shape (other direction)");
    }

    @Test
    void testBishopMovesDiagonally() {
        String[][] board = createEmptyBoard();
        board[4][4] = "B";
        assertTrue(validator.isValidMove(board, 4, 4, 0, 0, true), 
                   "Bishop should move diagonally");
        assertTrue(validator.isValidMove(board, 4, 4, 7, 7, true), 
                   "Bishop should move diagonally");
    }

    @Test
    void testQueenMovesCombined() {
        String[][] board = createEmptyBoard();
        board[4][4] = "Q";
        assertTrue(validator.isValidMove(board, 4, 4, 4, 7, true), 
                   "Queen should move horizontally like rook");
        assertTrue(validator.isValidMove(board, 4, 4, 7, 7, true), 
                   "Queen should move diagonally like bishop");
    }

    @Test
    void testKingMovesOneSquare() {
        String[][] board = createEmptyBoard();
        board[4][4] = "K";
        assertTrue(validator.isValidMove(board, 4, 4, 4, 5, true), 
                   "King should move one square horizontally");
        assertTrue(validator.isValidMove(board, 4, 4, 5, 5, true), 
                   "King should move one square diagonally");
    }

    @Test
    void testKingCannotMoveTwoSquares() {
        String[][] board = createEmptyBoard();
        board[4][4] = "K";
        assertFalse(validator.isValidMove(board, 4, 4, 4, 6, true), 
                    "King should not move two squares");
    }

    @Test
    void testCannotCaptureOwnPiece() {
        String[][] board = createStandardBoard();
        assertFalse(validator.isValidMove(board, 7, 0, 6, 0, true), 
                    "Cannot capture own piece");
    }

    @Test
    void testCanCaptureOpponentPiece() {
        String[][] board = createEmptyBoard();
        board[4][4] = "R";
        board[4][7] = "r"; // Opponent's rook
        assertTrue(validator.isValidMove(board, 4, 4, 4, 7, true), 
                   "Should be able to capture opponent's piece");
    }

    @Test
    void testInvalidMoveWrongTurn() {
        String[][] board = createStandardBoard();
        assertFalse(validator.isValidMove(board, 6, 4, 5, 4, false), 
                    "Should not move white piece on black's turn");
        assertFalse(validator.isValidMove(board, 1, 4, 2, 4, true), 
                    "Should not move black piece on white's turn");
    }

    @Test
    void testIsKingInCheck() {
        String[][] board = createEmptyBoard();
        board[4][4] = "K"; // White king
        board[4][7] = "r"; // Black rook attacking
        assertTrue(validator.isKingInCheck(board, true), 
                   "White king should be in check from black rook");
    }

    @Test
    void testIsKingNotInCheck() {
        String[][] board = createEmptyBoard();
        board[4][4] = "K"; // White king
        board[6][6] = "r"; // Black rook not attacking
        assertFalse(validator.isKingInCheck(board, true), 
                    "White king should not be in check");
    }

    @Test
    void testCannotMoveIntoCheck() {
        String[][] board = createEmptyBoard();
        board[4][4] = "K"; // White king
        board[4][7] = "r"; // Black rook
        assertTrue(validator.wouldKingBeInCheck(board, 4, 4, 4, 5, true), 
                   "Moving king into check should be detected");
    }

    @Test
    void testEmptySquareHasNoPiece() {
        String[][] board = createEmptyBoard();
        assertFalse(validator.isValidMove(board, 4, 4, 5, 5, true), 
                    "Cannot move from empty square");
    }
}
