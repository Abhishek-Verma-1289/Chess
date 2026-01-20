package com.example.IndiChessBackend.config;

import com.example.IndiChessBackend.service.JwtService;
import com.example.IndiChessBackend.service.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null) {
                String username = jwtService.extractUsername(token);
                if (username != null) {
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        if (jwtService.isTokenValid(token, userDetails)) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            accessor.setUser(authentication);
                            logger.debug("WebSocket CONNECT authenticated user={}", username);
                        }
                    } catch (Exception ex) {
                        logger.debug("WebSocket auth failed for username {}: {}", username, ex.getMessage());
                    }
                }
            } else {
                logger.debug("No JWT token found in WebSocket CONNECT headers or cookies");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1) Check Authorization header
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearerToken = authHeaders.get(0);
            if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }

        // 2) Check query parameter (for SockJS or direct websocket URL like /ws?token=...)
        List<String> queryHeaders = accessor.getNativeHeader("query");
        if (queryHeaders != null && !queryHeaders.isEmpty()) {
            String query = queryHeaders.get(0);
            if (query != null && query.contains("token=")) {
                String after = query.substring(query.indexOf("token=") + 6);
                int amp = after.indexOf('&');
                return amp > 0 ? after.substring(0, amp) : after;
            }
        }

        // 3) Check Cookie header (some SockJS implementations forward cookies as a native header)
        List<String> cookieHeaders = accessor.getNativeHeader("cookie");
        if (cookieHeaders == null) {
            cookieHeaders = accessor.getNativeHeader("Cookie");
        }
        if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
            for (String cookieHeader : cookieHeaders) {
                if (cookieHeader == null) continue;
                // Look for JWT=... (cookie format: "JWT=tokenValue; Path=/; ...")
                int idx = cookieHeader.indexOf("JWT=");
                if (idx >= 0) {
                    String after = cookieHeader.substring(idx + 4);
                    int semi = after.indexOf(';');
                    String token = semi > 0 ? after.substring(0, semi) : after;
                    if (StringUtils.hasText(token)) {
                        return token;
                    }
                }
            }
        }

        return null;
    }
}