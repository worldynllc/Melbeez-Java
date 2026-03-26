package com.mlbeez.feeder.filter;


import com.mlbeez.feeder.config.jwtconfig.TokenManager;
import com.mlbeez.feeder.service.JwtUserDetailsService;
import com.mlbeez.feeder.service.exception.BearerTokenNotFoundException;
import com.mlbeez.feeder.service.exception.InvalidJwtTokenException;
import com.mlbeez.feeder.service.exception.JwtExpiryException;
import com.mlbeez.feeder.service.exception.TokenValidationFailedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final TokenManager tokenManager;

    private final JwtUserDetailsService jwtUserDetailsService;

    private final Logger logger= LoggerFactory.getLogger(JwtFilter.class);

    public JwtFilter(TokenManager tokenManager, JwtUserDetailsService jwtUserDetailsService) {
        this.tokenManager = tokenManager;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/api/auth")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/swagger/api-docs")
                || path.equals("/favicon.ico")
                || path.equals("/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        try {
            if (authorizationHeader == null || authorizationHeader.isEmpty()) {
                throw new BearerTokenNotFoundException("Bearer token missing in header");
            }

            if (!authorizationHeader.startsWith("Bearer ")) {
                throw new InvalidJwtTokenException("Authorization header must start with Bearer");
            }

            String jwtToken = authorizationHeader.substring(7);
            String username = tokenManager.getUsernameFromToken(jwtToken);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

                String role = tokenManager.getRoleFromToken(jwtToken);
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

                if (!tokenManager.validateJwtToken(jwtToken, userDetails)) {
                    throw new TokenValidationFailedException("JWT validation failed");
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(request, response);

        } catch (JwtExpiryException | InvalidJwtTokenException | BearerTokenNotFoundException e) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{ \"error\": \"" + message + "\" }");
    }

}
