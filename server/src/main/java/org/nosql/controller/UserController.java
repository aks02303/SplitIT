package org.nosql.controller;

import org.nosql.dto.GoogleAuthRequest;
import org.nosql.dto.LoginRequest;
import org.nosql.dto.SignupRequest;
import org.nosql.model.User;
import org.nosql.repository.UserRepository;
import org.nosql.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Allows CORS and Cookies from React!
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // --- SIGNUP ENDPOINT ---
    // Replaces: router.post("/signup", ...)
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        // 1. Check if fields are empty
        if (request.getName() == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.status(422).body(Map.of("error", "please fill the field properly"));
        }

        // 2. Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            return ResponseEntity.status(422).body(Map.of("error", "user already exists"));
        }

        // 3. Check if passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.status(422).body(Map.of("error", "password are not matching"));
        }

        // 4. Create and save user (Hash the password using our PasswordEncoder bean)
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); // Replaces bcryptjs hashing

        userRepository.save(newUser);

        return ResponseEntity.status(201).body(Map.of("message", "User registered Successfully"));
    }

    // --- LOGIN ENDPOINT ---
    // Replaces: router.post("/login", ...)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            if (request.getEmail() == null || request.getPassword() == null) {
                return ResponseEntity.status(400).body(Map.of("error", "incomplete details"));
            }

            // 1. Find user by email
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(422).body(Map.of("error", "invalid Credentials"));
            }

            User user = userOptional.get();

            // 2. Check password match (Replaces bcrypt.compare)
            boolean isMatch = passwordEncoder.matches(request.getPassword(), user.getPassword());
            if (!isMatch) {
                return ResponseEntity.status(422).body(Map.of("error", "invalid Credentials"));
            }

            // 3. Generate JWT Token
            String token = jwtUtil.generateToken(user.getId());

            // Save token to user's tokens array in DB (to match your Node app logic)
            user.getTokens().add(new User.AuthToken(token));
            userRepository.save(user);

            // 4. Set the HTTP-Only Cookie (Replaces res.cookie("jwttoken", token, ...))
            Cookie cookie = new Cookie("jwttoken", token);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(25892000); // Expiration in seconds
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of("message", "user signin sucessfull"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal Server Error"));
        }
    }
    @GetMapping("/details")
    public ResponseEntity<?> getUserDetails() {
        // Grab the ID that our JwtAuthenticationFilter extracted from the cookie
        String userId = (String) org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        // Remove password before sending to frontend
        User safeUser = user.get();
        safeUser.setPassword(null);

        return ResponseEntity.ok(safeUser);
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwttoken", null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Expire instantly
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
    // --- GOOGLE SIGNUP ENDPOINT ---
    // Replaces: router.post("/googlesignup", ...)
    @PostMapping("/googlesignup")
    public ResponseEntity<?> googleSignup(@RequestBody GoogleAuthRequest request, HttpServletResponse response) {
        if (request.getName() == null || request.getEmail() == null) {
            return ResponseEntity.status(422).body(Map.of("error", "please fill the field properly"));
        }

        Optional<User> userExist = userRepository.findByEmail(request.getEmail());
        if (userExist.isPresent()) {
            return ResponseEntity.status(422).body(Map.of("error", "user already exists"));
        }

        // Create user (No password needed for Google Auth)
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        userRepository.save(newUser);

        // Fetch the saved user to get the auto-generated ID
        User savedUser = userRepository.findByEmail(request.getEmail()).get();

        // Generate JWT Token
        String token = jwtUtil.generateToken(savedUser.getId());
        savedUser.getTokens().add(new User.AuthToken(token));
        userRepository.save(savedUser);

        // Set the HTTP-Only Cookie
        Cookie cookie = new Cookie("jwttoken", token);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(25892000);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.status(201).body(Map.of("message", "User registered Successfully"));
    }

    // --- GOOGLE LOGIN ENDPOINT ---
    // Replaces: router.post("/googlelogin", ...)
    @PostMapping("/googlelogin")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleAuthRequest request, HttpServletResponse response) {
        if (request.getEmail() == null) {
            return ResponseEntity.status(400).body(Map.of("error", "incomplete details"));
        }

        Optional<User> userExist = userRepository.findByEmail(request.getEmail());

        if (userExist.isPresent()) {
            User user = userExist.get();

            // Generate JWT Token
            String token = jwtUtil.generateToken(user.getId());
            user.getTokens().add(new User.AuthToken(token));
            userRepository.save(user);

            // Set the HTTP-Only Cookie
            Cookie cookie = new Cookie("jwttoken", token);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(25892000);
            cookie.setPath("/");
            response.addCookie(cookie);

            return ResponseEntity.ok(Map.of("message", "user signin sucessfull"));
        } else {
            return ResponseEntity.status(422).body(Map.of("error", "invalid Credentials"));
        }
    }
    // --- SEARCH USER BY EMAIL ---
    // Replaces: router.post("/search", ...) or similar
    // --- SEARCH USER BY EMAIL ---
    @PostMapping("/search")
    public ResponseEntity<?> searchUser(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("error", "Email is required"));
        }

        // 1. Clean the string! Remove all leading/trailing spaces
        String cleanEmail = email.trim();

        // 2. Use the new Case-Insensitive search
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);

        if (userOpt.isEmpty()) {
            // Print to the backend terminal so you can see exactly what the frontend sent
            System.out.println("Failed search for email: '" + cleanEmail + "'");
            return ResponseEntity.status(404).body(Map.of("error", "no person with the given email id"));
        }

        User user = userOpt.get();
        user.setPassword(null);

        return ResponseEntity.ok(Map.of("user", user));
    }
}