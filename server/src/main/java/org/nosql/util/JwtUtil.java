package org.nosql.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

// @Component makes this a Spring-managed utility class we can inject anywhere
@Component
public class JwtUtil {

    // @Value reads the secret from your application.properties!
    // Replaces process.env.JWT_SECRET_KEY
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationTime;

    // Helper method to generate the cryptographic key
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Replaces user.generateAuthToken()
    public String generateToken(String userId) {
        return Jwts.builder()
                .setSubject(userId) // We store the User's ID in the token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}