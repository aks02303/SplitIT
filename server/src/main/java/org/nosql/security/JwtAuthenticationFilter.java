package org.nosql.security;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

// @Component lets Spring manage this filter and inject it into SecurityConfig
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        // 1. Look for the "jwttoken" cookie in the incoming request
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwttoken".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        // 2. If a token was found, validate it
        if (token != null) {
            try {
                // Parse the JWT to extract the User ID
                String userId = Jwts.parserBuilder()
                        .setSigningKey(secretKey.getBytes())
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject();

                // Create an authentication object (Replaces req.rootUser / req.userID)
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());

                // Save it in the Security Context so the rest of the app knows who is logged in
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // If the token is expired or invalid, clear the context
                SecurityContextHolder.clearContext();
            }
        }

        // 3. Continue the request to the next filter or controller
        filterChain.doFilter(request, response);
    }
}