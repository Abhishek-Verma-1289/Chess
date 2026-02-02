package com.example.IndiChessBackend.service;

import com.example.IndiChessBackend.model.Match;
import com.example.IndiChessBackend.model.Move;
import com.example.IndiChessBackend.model.PieceColor;
import com.example.IndiChessBackend.repo.MatchRepo;
import com.example.IndiChessBackend.repo.MoveRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for persisting chess moves to database
 */
@Service
@RequiredArgsConstructor
public class MoveService {

    private static final Logger logger = LoggerFactory.getLogger(MoveService.class);
    
    private final MoveRepo moveRepo;
    private final MatchRepo matchRepo;

    /**
     * Save a move to the database
     * @param match The match entity
     * @param fromRow Source row
     * @param fromCol Source column
     * @param toRow Destination row
     * @param toCol Destination column
     * @param piece Piece that moved
     * @param isWhite Whether it's white's move
     * @param fenBefore Board state before move
     * @param fenAfter Board state after move
     * @param capturedPiece Piece captured (if any)
     * @return The saved Move entity
     */
    @Transactional
    public Move saveMove(Match match, int fromRow, int fromCol, int toRow, int toCol,
                        String piece, boolean isWhite, String fenBefore, String fenAfter,
                        String capturedPiece, Boolean isCastling, Boolean isEnPassant, 
                        Boolean isPromotion, String promotedTo) {
        
        try {
            // Calculate ply (half-move number)
            int currentPly = match.getMoves().size() + 1;
            int moveNumber = (currentPly + 1) / 2; // Full move number
            
            // Create UCI notation (e.g., "e2e4")
            String uci = positionToUci(fromRow, fromCol) + positionToUci(toRow, toCol);
            if (isPromotion != null && isPromotion && promotedTo != null) {
                uci += promotedTo.toLowerCase(); // Add promotion piece (e.g., "e7e8q")
            }
            
            // Create SAN notation (simplified)
            String san = createSimpleSan(piece, toRow, toCol, capturedPiece != null);
            
            // Create move entity
            Move move = new Move();
            move.setMatch(match);
            move.setPly(currentPly);
            move.setMoveNumber(moveNumber);
            move.setColor(isWhite ? PieceColor.WHITE : PieceColor.BLACK);
            move.setUci(uci);
            move.setSan(san);
            move.setFenBefore(fenBefore);
            move.setFenAfter(fenAfter);
            move.setCreatedAt(LocalDateTime.now());
            
            // Save to database
            Move savedMove = moveRepo.save(move);
            
            // Update match with latest state
            match.setCurrentPly(currentPly);
            match.setFenCurrent(fenAfter);
            match.setLastMoveUci(uci);
            match.setUpdatedAt(LocalDateTime.now());
            matchRepo.save(match);
            
            logger.info("✅ Move saved: {} - {} (ply {})", uci, san, currentPly);
            return savedMove;
            
        } catch (Exception e) {
            logger.error("❌ Failed to save move: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save move to database", e);
        }
    }

    /**
     * Convert board position to UCI notation
     * @param row Row (0-7)
     * @param col Column (0-7)
     * @return UCI position (e.g., "e4")
     */
    private String positionToUci(int row, int col) {
        char file = (char) ('a' + col); // a-h
        int rank = 8 - row; // 1-8 (inverted because array is top-down)
        return "" + file + rank;
    }

    /**
     * Create simplified SAN notation
     * @param piece Piece character
     * @param toRow Destination row
     * @param toCol Destination column
     * @param isCapture Whether it's a capture
     * @return SAN notation
     */
    private String createSimpleSan(String piece, int toRow, int toCol, boolean isCapture) {
        String san = "";
        
        // Piece prefix (uppercase for white, lowercase for black)
        char pieceChar = piece.charAt(0);
        if (Character.toLowerCase(pieceChar) != 'p') { // Not a pawn
            san += Character.toUpperCase(pieceChar);
        }
        
        // Capture notation
        if (isCapture) {
            if (Character.toLowerCase(pieceChar) == 'p') {
                // Pawn captures include file
                san += (char) ('a' + toCol);
            }
            san += "x";
        }
        
        // Destination square
        san += positionToUci(toRow, toCol);
        
        return san;
    }

    /**
     * Get the latest FEN for a match
     */
    public String getLatestFen(Long matchId) {
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return match.getFenCurrent();
    }

    /**
     * Load all moves for a match (for game replay/history)
     */
    public java.util.List<Move> getMovesForMatch(Long matchId) {
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return match.getMoves(); // OrderBy ply is set in Match entity
    }
}
