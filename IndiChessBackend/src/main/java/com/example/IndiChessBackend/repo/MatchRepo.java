package com.example.IndiChessBackend.repo;

import com.example.IndiChessBackend.model.Match;
import com.example.IndiChessBackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepo extends JpaRepository<Match, Long> {
    
    // Find all matches for a specific user (as player1 or player2)
    @Query("SELECT m FROM Match m WHERE m.player1 = :user OR m.player2 = :user ORDER BY m.createdAt DESC")
    List<Match> findMatchesByUser(@Param("user") User user);
    
    // Find recent matches for a user (limit to most recent)
    @Query("SELECT m FROM Match m WHERE m.player1 = :user OR m.player2 = :user ORDER BY m.createdAt DESC")
    List<Match> findRecentMatchesByUser(@Param("user") User user, org.springframework.data.domain.Pageable pageable);
}
