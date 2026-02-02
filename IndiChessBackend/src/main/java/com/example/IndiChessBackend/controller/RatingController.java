package com.example.IndiChessBackend.controller;

import com.example.IndiChessBackend.model.User;
import com.example.IndiChessBackend.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {

    private final UserRepo userRepo;

    /**
     * Get current user's rating information
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyRating(Principal principal) {
        String username = principal.getName();
        
        User user = userRepo.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Integer rating = user.getRating() != null ? user.getRating() : 1200;
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("rating", rating);
        response.put("country", user.getCountry());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get rating for a specific user
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<Map<String, Object>> getUserRating(@PathVariable String username) {
        User user = userRepo.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        Integer rating = user.getRating() != null ? user.getRating() : 1200;
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("rating", rating);
        response.put("country", user.getCountry());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get leaderboard - top rated players
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        List<User> allUsers = userRepo.findAll();
        
        List<Map<String, Object>> leaderboard = allUsers.stream()
            .sorted((u1, u2) -> {
                int rating1 = u1.getRating() != null ? u1.getRating() : 1200;
                int rating2 = u2.getRating() != null ? u2.getRating() : 1200;
                return Integer.compare(rating2, rating1); // Descending order
            })
            .skip((long) page * size)
            .limit(size)
            .map(user -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("username", user.getUsername());
                entry.put("rating", user.getRating() != null ? user.getRating() : 1200);
                entry.put("country", user.getCountry());
                return entry;
            })
            .collect(Collectors.toList());
        
        // Add ranks
        int rank = page * size + 1;
        for (Map<String, Object> entry : leaderboard) {
            entry.put("rank", rank++);
        }
        
        return ResponseEntity.ok(leaderboard);
    }
}
