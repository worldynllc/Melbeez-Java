package com.mlbeez.feeder.config.jwtconfig;

import com.mlbeez.feeder.service.exception.InvalidJwtTokenException;
import com.mlbeez.feeder.service.exception.JwtExpiryException;
import io.jsonwebtoken.*;
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
            Date expiry = claims.getExpiration();
            if (username == null || userDetails == null) {
                throw new InvalidJwtTokenException("Required claims missing in token");
            }
            if (expiry != null && expiry.before(new Date())) {
                throw new JwtExpiryException("JWT token expired");
            }
            return username.equals(userDetails.getUsername());
        } catch (ExpiredJwtException ex) {
            logger.warn("JWT expired: {}", ex.getMessage());
            throw new JwtExpiryException("JWT token expired");
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException ex) {
            logger.warn("Invalid JWT: {}", ex.getMessage());
            throw new InvalidJwtTokenException("Invalid JWT token");
        } catch (JwtException ex) {
            logger.error("JWT processing error: {}", ex.getMessage());
            throw new InvalidJwtTokenException("JWT processing failed");
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
            String username = claims.get("unique_name", String.class);
            if (username == null) {
                throw new InvalidJwtTokenException("Username claim missing");
            }
            return username;
        }catch (ExpiredJwtException ex) {
            logger.warn("JWT expired while extracting username: {}", ex.getMessage());
            throw new JwtExpiryException("JWT token expired");
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException ex) {
            logger.warn("Invalid JWT while extracting username: {}", ex.getMessage());
            throw new InvalidJwtTokenException("Invalid JWT token");
        } catch (JwtException ex) {
            logger.error("JWT processing error while extracting username: {}", ex.getMessage());
            throw new InvalidJwtTokenException("JWT processing failed");
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String role = claims.get("role", String.class);
            logger.info("Extracted role from token: {}", role);
            if (role == null) {
                throw new InvalidJwtTokenException("Role claim missing");
            }
            return "ROLE_" + role.toUpperCase();
        }catch (ExpiredJwtException ex) {
            logger.warn("JWT expired while extracting role: {}", ex.getMessage());
            throw new JwtExpiryException("JWT token expired");
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException ex) {
            logger.warn("Invalid JWT while extracting role: {}", ex.getMessage());
            throw new InvalidJwtTokenException("Invalid JWT token");
        } catch (JwtException ex) {
            logger.error("JWT processing error while extracting role: {}", ex.getMessage());
            throw new InvalidJwtTokenException("JWT processing failed");
        }
    }

    public Key getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}

