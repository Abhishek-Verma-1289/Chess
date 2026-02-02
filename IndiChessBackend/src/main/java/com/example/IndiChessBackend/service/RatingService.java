package com.example.IndiChessBackend.service;

import com.example.IndiChessBackend.model.*;
import com.example.IndiChessBackend.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for calculating and updating player ratings using ELO system
 */
@Service
@RequiredArgsConstructor
public class RatingService {

    private static final Logger logger = LoggerFactory.getLogger(RatingService.class);
    
    private final UserRepo userRepo;
    
    // ELO rating constants
    private static final int DEFAULT_RATING = 1200;
    private static final int K_FACTOR = 32; // Standard K-factor for ratings under 2400
    
    /**
     * Update ratings for both players after a game
     * @param match The completed match
     */
    @Transactional
    public void updateRatingsAfterGame(Match match) {
        try {
            User player1 = match.getPlayer1();
            User player2 = match.getPlayer2();
            MatchStatus status = match.getStatus();
            
            // Get current ratings (default if not set)
            int rating1 = player1.getRating() != null ? player1.getRating() : DEFAULT_RATING;
            int rating2 = player2.getRating() != null ? player2.getRating() : DEFAULT_RATING;
            
            logger.info("📊 Calculating rating changes for match {} - {} ({}) vs {} ({})", 
                       match.getId(), player1.getUsername(), rating1, player2.getUsername(), rating2);
            
            // Calculate expected scores
            double expectedScore1 = calculateExpectedScore(rating1, rating2);
            double expectedScore2 = calculateExpectedScore(rating2, rating1);
            
            // Determine actual scores based on match result
            double actualScore1;
            double actualScore2;
            
            if (status == MatchStatus.PLAYER1_WON) {
                actualScore1 = 1.0; // Win
                actualScore2 = 0.0; // Loss
            } else if (status == MatchStatus.PLAYER2_WON) {
                actualScore1 = 0.0; // Loss
                actualScore2 = 1.0; // Win
            } else if (status == MatchStatus.DRAW) {
                actualScore1 = 0.5; // Draw
                actualScore2 = 0.5; // Draw
            } else {
                logger.warn("⚠️ Match {} has no final result, skipping rating update", match.getId());
                return;
            }
            
            // Calculate rating changes
            int ratingChange1 = calculateRatingChange(expectedScore1, actualScore1);
            int ratingChange2 = calculateRatingChange(expectedScore2, actualScore2);
            
            // Apply new ratings
            int newRating1 = rating1 + ratingChange1;
            int newRating2 = rating2 + ratingChange2;
            
            // Ensure ratings don't go below minimum
            newRating1 = Math.max(100, newRating1);
            newRating2 = Math.max(100, newRating2);
            
            // Update user ratings
            player1.setRating(newRating1);
            player2.setRating(newRating2);
            
            userRepo.save(player1);
            userRepo.save(player2);
            
            logger.info("✅ Ratings updated:");
            logger.info("   {}: {} → {} ({}{}) ", 
                       player1.getUsername(), rating1, newRating1, 
                       ratingChange1 >= 0 ? "+" : "", ratingChange1);
            logger.info("   {} : {} → {} ({}{}) ", 
                       player2.getUsername(), rating2, newRating2, 
                       ratingChange2 >= 0 ? "+" : "", ratingChange2);
            
        } catch (Exception e) {
            logger.error("❌ Failed to update ratings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update player ratings", e);
        }
    }
    
    /**
     * Calculate expected score using ELO formula
     * Expected score = 1 / (1 + 10^((opponent_rating - player_rating) / 400))
     */
    private double calculateExpectedScore(int playerRating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentRating - playerRating) / 400.0));
    }
    
    /**
     * Calculate rating change
     * Change = K * (actual_score - expected_score)
     */
    private int calculateRatingChange(double expectedScore, double actualScore) {
        return (int) Math.round(K_FACTOR * (actualScore - expectedScore));
    }
    
    /**
     * Get player's current rating
     */
    public int getPlayerRating(User user) {
        Integer rating = user.getRating();
        return rating != null ? rating : DEFAULT_RATING;
    }
    
    /**
     * Get rating statistics for a user
     */
    public RatingStats getRatingStats(User user) {
        int currentRating = getPlayerRating(user);
        
        // TODO: Calculate from actual game history
        // For now, return basic stats
        RatingStats stats = new RatingStats();
        stats.setCurrentRating(currentRating);
        stats.setHighestRating(currentRating); // Will be calculated from history
        stats.setLowestRating(currentRating);  // Will be calculated from history
        stats.setGamesPlayed(0);               // Will be calculated from matches
        
        return stats;
    }
    
    /**
     * Inner class for rating statistics
     */
    public static class RatingStats {
        private int currentRating;
        private int highestRating;
        private int lowestRating;
        private int gamesPlayed;
        
        public int getCurrentRating() { return currentRating; }
        public void setCurrentRating(int currentRating) { this.currentRating = currentRating; }
        
        public int getHighestRating() { return highestRating; }
        public void setHighestRating(int highestRating) { this.highestRating = highestRating; }
        
        public int getLowestRating() { return lowestRating; }
        public void setLowestRating(int lowestRating) { this.lowestRating = lowestRating; }
        
        public int getGamesPlayed() { return gamesPlayed; }
        public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }
    }
}
