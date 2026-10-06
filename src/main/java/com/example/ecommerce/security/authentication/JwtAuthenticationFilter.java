package com.example.ecommerce.security.authentication;

import com.example.ecommerce.entity.AccountStatus;
import com.example.ecommerce.exception.InvalidTokenException;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.security.jwt.AccessTokenClaims;
import com.example.ecommerce.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer <jwt>", verifies it and authenticates the request.
 * The user is re-loaded from the database on every request, so suspending an account or changing
 * a role takes effect immediately instead of waiting for the access token to expire.
 * An invalid token does not fail here; the request simply stays anonymous and the security rules
 * answer 401 if the endpoint needs authentication.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AccessTokenClaims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()).trim());
                userRepository.findById(claims.userId())
                        .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                        .ifPresent(user -> {
                            AuthenticatedUser principal = AuthenticatedUser.from(user);
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            principal, null, principal.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
            } catch (InvalidTokenException ex) {
                log.debug("Rejected invalid access token"); // never log the token itself
            }
        }
        chain.doFilter(request, response);
    }
}
