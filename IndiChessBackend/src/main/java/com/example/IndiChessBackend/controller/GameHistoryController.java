package com.example.IndiChessBackend.controller;

import com.example.IndiChessBackend.model.Match;
import com.example.IndiChessBackend.model.Move;
import com.example.IndiChessBackend.model.User;
import com.example.IndiChessBackend.repo.MatchRepo;
import com.example.IndiChessBackend.repo.UserRepo;
import com.example.IndiChessBackend.service.MoveService;
import com.example.IndiChessBackend.service.MatchService;
import com.example.IndiChessBackend.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for game history and move retrieval
 */
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class GameHistoryController {

    private final MoveService moveService;
    private final MatchService matchService;
    private final MatchRepo matchRepo;
    private final UserRepo userRepo;
    private final JwtService jwtService;

    /**
     * Get all matches for the authenticated user
     */
    @GetMapping("/my-games")
    public ResponseEntity<Map<String, Object>> getMyGames(HttpServletRequest request,
                                                           @RequestParam(defaultValue = "10") int limit) {
        try {
            String token = getJwtFromCookie(request);
            String username = jwtService.extractUsername(token);
            
            if (username == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            
            User user = userRepo.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            // Get recent matches (limited)
            List<Match> matches = matchRepo.findRecentMatchesByUser(user, PageRequest.of(0, limit));
            
            // Convert to simpler response format
            List<Map<String, Object>> gamesList = matches.stream().map(match -> {
                Map<String, Object> gameInfo = new HashMap<>();
                gameInfo.put("id", match.getId());
                gameInfo.put("player1", match.getPlayer1().getUsername());
                gameInfo.put("player2", match.getPlayer2().getUsername());
                gameInfo.put("status", match.getStatus().toString());
                gameInfo.put("createdAt", match.getCreatedAt());
                gameInfo.put("finishedAt", match.getFinishedAt());
                gameInfo.put("currentPly", match.getCurrentPly());
                
                // Determine result for this user
                String result = "In Progress";
                if (match.getStatus() != null) {
                    if (match.getStatus().toString().contains("PLAYER1_WON")) {
                        result = match.getPlayer1().equals(user) ? "Win" : "Loss";
                    } else if (match.getStatus().toString().contains("PLAYER2_WON")) {
                        result = match.getPlayer2().equals(user) ? "Win" : "Loss";
                    } else if (match.getStatus().toString().contains("DRAW")) {
                        result = "Draw";
                    }
                }
                gameInfo.put("result", result);
                
                return gameInfo;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalGames", gamesList.size());
            response.put("games", gamesList);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String getJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Get all moves for a specific match
     */
    @GetMapping("/match/{matchId}/moves")
    public ResponseEntity<Map<String, Object>> getMatchMoves(@PathVariable Long matchId) {
        try {
            List<Move> moves = moveService.getMovesForMatch(matchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("matchId", matchId);
            response.put("totalMoves", moves.size());
            response.put("moves", moves);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get current FEN position for a match
     */
    @GetMapping("/match/{matchId}/fen")
    public ResponseEntity<Map<String, String>> getCurrentFen(@PathVariable Long matchId) {
        try {
            String fen = moveService.getLatestFen(matchId);
            
            Map<String, String> response = new HashMap<>();
            response.put("matchId", String.valueOf(matchId));
            response.put("fen", fen);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
