package com.example.balancedbackend.security;

import com.example.balancedbackend.auth.model.AuthSession;
import com.example.balancedbackend.auth.model.User;
import com.example.balancedbackend.auth.store.InMemorySessionStore;
import com.example.balancedbackend.auth.store.InMemoryUserStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final InMemorySessionStore sessionStore;
    private final InMemoryUserStore userStore;

    public SessionAuthenticationFilter(InMemorySessionStore sessionStore, InMemoryUserStore userStore) {
        this.sessionStore = sessionStore;
        this.userStore = userStore;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/") || path.equals("/auth");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);

        if (authorization != null && authorization.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            Optional<AuthSession> session = sessionStore.findValidSession(token);

            if (session.isPresent()) {
                Optional<User> user = userStore.findById(session.get().userId());
                if (user.isPresent()) {
                    AuthenticatedUser principal = new AuthenticatedUser(
                            user.get().id(),
                            user.get().email(),
                            user.get().name()
                    );
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, List.of());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

