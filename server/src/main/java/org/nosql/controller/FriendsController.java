package org.nosql.controller;

import org.nosql.model.User;
import org.nosql.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/friends")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FriendsController {

    @Autowired
    private UserRepository userRepository;

    // --- 1. ADD FRIEND ---
    // Replaces: router.post("/:userId/friend/:friendId", ...)
    @PostMapping("/{userId}/friend/{friendId}")
    public ResponseEntity<?> addFriend(@PathVariable String userId, @PathVariable String friendId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<User> friendOpt = userRepository.findById(friendId);

        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("User not found");
        if (friendOpt.isEmpty()) return ResponseEntity.status(404).body("Friend not found");

        User user = userOpt.get();
        User friend = friendOpt.get();

        // Add to each other's lists if they aren't already there
        if (!user.getFriends().contains(friendId)) {
            user.getFriends().add(friendId);
            userRepository.save(user);
        }
        if (!friend.getFriends().contains(userId)) {
            friend.getFriends().add(userId);
            userRepository.save(friend);
        }

        user.setPassword(null); // Never send password back
        return ResponseEntity.ok(user);
    }

    // --- 2. REMOVE FRIEND ---
    // Replaces: router.delete("/:userId/friend/:friendId", ...)
    @DeleteMapping("/{userId}/friend/{friendId}")
    public ResponseEntity<?> removeFriend(@PathVariable String userId, @PathVariable String friendId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<User> friendOpt = userRepository.findById(friendId);

        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("User not found");
        if (friendOpt.isEmpty()) return ResponseEntity.status(404).body("Friend not found");

        User user = userOpt.get();
        User friend = friendOpt.get();

        // Remove from lists (Java's .remove() is much cleaner than JS splice!)
        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userRepository.save(user);
        userRepository.save(friend);

        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // --- 3. GET USER'S FRIENDS ---
    // Replaces: router.get("/:userId", ...)
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserWithFriends(@PathVariable String userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("Friends not found");

        User user = userOpt.get();

        // Manual Populate: Fetch all friend details
        List<Map<String, Object>> populatedFriends = new ArrayList<>();
        for (String friendId : user.getFriends()) {
            userRepository.findById(friendId).ifPresent(f -> {
                Map<String, Object> safeFriend = new HashMap<>();
                safeFriend.put("id", f.getId());
                safeFriend.put("name", f.getName());
                safeFriend.put("email", f.getEmail());
                populatedFriends.add(safeFriend);
            });
        }

        // Build the final response object
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("friends", populatedFriends);
        response.put("totalExpenses", 1); // Kept hardcoded as it was in your Node app

        return ResponseEntity.ok(response);
    }
}