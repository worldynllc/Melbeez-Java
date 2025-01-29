package com.mlbeez.feeder.config.jwtconfig;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;



@Component
public class TokenManager implements Serializable {

    @Serial
    private static final long serialVersionUID = 7008375124389347049L;

    @Value("${secret}")
    private String jwtSecret;

    private final Logger logger= LoggerFactory.getLogger(TokenManager.class);

    public Boolean validateJwtToken(String token, UserDetails userDetails) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.get("unique_name", String.class);
            return username.equals(userDetails.getUsername()) && !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {

            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        logger.info("Requested to getUsernameFromToken");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("unique_name", String.class);
        } catch (Exception e) {
            logger.error("Errors parsing JWT token: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String role = claims.get("role", String.class).toUpperCase();
            logger.info("Extracted role from token: {}", role);
            return "ROLE_" + role;
        } catch (Exception e) {
            logger.error("Error parsing JWT token: {}", e.getMessage());
            return null;
        }
    }


    public Key getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

}

