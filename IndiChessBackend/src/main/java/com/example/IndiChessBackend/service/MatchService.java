package com.example.IndiChessBackend.service;

import com.example.IndiChessBackend.model.Match;
import com.example.IndiChessBackend.model.User;
import com.example.IndiChessBackend.repo.MatchRepo;
import com.example.IndiChessBackend.repo.UserRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.IndiChessBackend.model.MatchStatus.IN_PROGRESS;

@Service
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    // Store waiting players and the timestamp when they started waiting
    // username -> timestampMillis
    private static final Map<String, Long> waitingPlayers = new ConcurrentHashMap<>();
    private static final Map<Long, String[]> matchPlayers = new ConcurrentHashMap<>();

    // Dedicated lock for matching operations
    private static final Object MATCH_LOCK = new Object();

    // Time-to-live for waiting entries (5 minutes)
    private static final long WAITING_TTL_MS = 5 * 60 * 1000L;

    private final JwtService jwtService;
    private final UserRepo userRepo;
    private final MatchRepo matchRepo;
    private final GameService gameService;

    @Autowired
    MatchService(JwtService jwtService, UserRepo userRepo, MatchRepo matchRepo, GameService gameService) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.matchRepo = matchRepo;
        this.gameService = gameService;

        // Clean up old entries periodically
        new Timer().schedule(new TimerTask() {
            public void run() {
                cleanupOldEntries();
            }
        }, 0, 60000); // Clean up every minute
    }

    public String getJwtFromCookie(HttpServletRequest request) {
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

    private void cleanupOldEntries() {
        long cutoff = System.currentTimeMillis() - WAITING_TTL_MS;
        List<String> removed = new ArrayList<>();
        for (Map.Entry<String, Long> e : waitingPlayers.entrySet()) {
            Long ts = e.getValue();
            if (ts == null || ts < cutoff) {
                removed.add(e.getKey());
            }
        }
        for (String u : removed) {
            waitingPlayers.remove(u);
            logger.debug("Removed stale waiting user: {}", u);
        }
    }

    // Create match or add to waiting queue
    public Optional<Long> createMatch(HttpServletRequest request) {
        String tk = getJwtFromCookie(request);
        String userName = jwtService.extractUsername(tk);

        if (userName == null) {
            return Optional.empty();
        }

        logger.debug("User {} requesting match", userName);

        long now = System.currentTimeMillis();
        long cutoff = now - WAITING_TTL_MS;

        synchronized (MATCH_LOCK) {
            // Remove stale waiting entries first
            Iterator<Map.Entry<String, Long>> it = waitingPlayers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                Long ts = entry.getValue();
                if (ts == null || ts < cutoff) {
                    it.remove();
                    logger.debug("Removed stale waiting user during match creation: {}", entry.getKey());
                }
            }

            // Check if there's already a waiting player
            for (String waitingPlayer : waitingPlayers.keySet()) {
                if (!waitingPlayer.equals(userName)) {
                    // Found opponent
                    User player1 = userRepo.getUserByUsername(waitingPlayer);
                    User player2 = userRepo.getUserByUsername(userName);

                    if (player1 != null && player2 != null) {
                        // Create the match
                        Match newMatch = matchRepo.save(new Match(player1, player2, IN_PROGRESS, 1));
                        Long matchId = newMatch.getId();

                        // Store match info
                        matchPlayers.put(matchId, new String[]{waitingPlayer, userName});

                        // Remove waiting player
                        waitingPlayers.remove(waitingPlayer);

                        logger.debug("Match created: {}", matchId);

                        // Initialize game state
                        gameService.getGameDetails(matchId, request);

                        return Optional.of(matchId);
                    }
                }
            }

            // No opponent found, add/update waiting queue with timestamp
            waitingPlayers.put(userName, now);
            logger.debug("User {} added to waiting queue at {}", userName, new Date(now));

            return Optional.of(-1L);
        }
    }

    // Method for Player1 to check if match was created
    public Optional<Long> checkMatch(HttpServletRequest request) {
        String tk = getJwtFromCookie(request);
        String userName = jwtService.extractUsername(tk);

        if (userName == null) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        long cutoff = now - WAITING_TTL_MS;

        synchronized (MATCH_LOCK) {
            // Remove stale waiting entries first
            Iterator<Map.Entry<String, Long>> it = waitingPlayers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                Long ts = entry.getValue();
                if (ts == null || ts < cutoff) {
                    it.remove();
                    logger.debug("Removed stale waiting user during match check: {}", entry.getKey());
                }
            }

            // Check if user is still waiting
            if (waitingPlayers.containsKey(userName)) {
                // User is still waiting
                return Optional.of(-1L);
            }

            // Check if user has a match in matchPlayers
            for (Map.Entry<Long, String[]> entry : matchPlayers.entrySet()) {
                String[] players = entry.getValue();
                if (players[0].equals(userName) || players[1].equals(userName)) {
                    Long matchId = entry.getKey();
                    // Clean up after retrieving
                    matchPlayers.remove(matchId);
                    waitingPlayers.remove(players[0]);
                    waitingPlayers.remove(players[1]);
                    logger.debug("Returning match {} to {}", matchId, userName);
                    return Optional.of(matchId);
                }
            }
        }

        return Optional.empty();
    }

    // Method to cancel waiting
    public boolean cancelWaiting(HttpServletRequest request) {
        String tk = getJwtFromCookie(request);
        String userName = jwtService.extractUsername(tk);

        if (userName == null) {
            return false;
        }

        synchronized (MATCH_LOCK) {
            boolean removed = waitingPlayers.remove(userName) != null;
            if (removed) {
                logger.debug("User {} cancelled waiting", userName);
            }
            return removed;
        }
    }

    private Map<String, Object> createPlayerInfo(User user) {
        Map<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("id", user.getUserId());
        playerInfo.put("username", user.getUsername());
        // Add other user info if needed
        return playerInfo;
    }

    private boolean determineIfMyTurn(Match match, boolean isPlayer1) {
        // Simple logic: if currentPly is even, it's white's turn
        // You may need more sophisticated logic based on your game state
        Integer currentPly = match.getCurrentPly();
        if (currentPly == null) {
            currentPly = 0;
        }

        // Even ply = white's turn, odd ply = black's turn
        boolean isWhiteTurn = currentPly % 2 == 0;

        // Player1 is white, Player2 is black
        return (isPlayer1 && isWhiteTurn) || (!isPlayer1 && !isWhiteTurn);
    }

    public Map<String, Object> getGameDetailsForFrontend(Long matchId, HttpServletRequest request) {
        String token = getJwtFromCookie(request);
        if (token == null) {
            throw new RuntimeException("Not authenticated");
        }

        String username = jwtService.extractUsername(token);
        if (username == null) {
            throw new RuntimeException("Invalid token");
        }

        // Find the match
        Optional<Match> matchOpt = matchRepo.findById(matchId);
        if (matchOpt.isEmpty()) {
            throw new RuntimeException("Game not found");
        }

        Match match = matchOpt.get();

        // Check if user is part of this match
        User player1 = match.getPlayer1();
        User player2 = match.getPlayer2();

        boolean isPlayer1 = player1.getUsername().equals(username);
        boolean isPlayer2 = player2 != null && player2.getUsername().equals(username);

        if (!isPlayer1 && !isPlayer2) {
            throw new RuntimeException("Not authorized to view this game");
        }

        // Determine player color
        String playerColor = isPlayer1 ? "white" : "black";

        // Determine if it's this player's turn
        // For initial implementation: white starts, then alternate based on currentPly
        boolean isMyTurn = determineIfMyTurn(match, isPlayer1);

        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("matchId", match.getId());
        response.put("player1", createPlayerInfo(player1));

        if (player2 != null) {
            response.put("player2", createPlayerInfo(player2));
        }

        response.put("status", match.getStatus() != null ? match.getStatus().toString() : "IN_PROGRESS");
        response.put("playerColor", playerColor);
        response.put("isMyTurn", isMyTurn);
        response.put("createdAt", match.getCreatedAt());
        response.put("startedAt", match.getStartedAt());
        response.put("currentPly", match.getCurrentPly());
        response.put("fenCurrent", match.getFenCurrent());

        return response;
    }
}