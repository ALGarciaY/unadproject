package com.unad.project_video_platform.security;

import com.unad.project_video_platform.dto.ApiResponse;
import com.unad.project_video_platform.entity.User;
import com.unad.project_video_platform.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token: dejar que SecurityConfig maneje el 401
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtService.isTokenValid(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiResponse<?> apiResponse = ApiResponse.unauthorized("Invalid or expired token");
                String jsonResponse = convertToJson(apiResponse);
                response.getWriter().write(jsonResponse);
                response.flushBuffer();
                return;
            }

            Claims claims = jwtService.parseClaims(token);
            String username = claims.getSubject();

            User user = username == null ? null : userRepository.findByEmail(username).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getStatus())) {
                throw new IllegalStateException("Usuario inactivo o inexistente");
            }
            String role = user.getRole() != null ? user.getRole().getRoleName() : "USER";

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<?> apiResponse = ApiResponse.unauthorized("Invalid or expired token");
            String jsonResponse = convertToJson(apiResponse);
            response.getWriter().write(jsonResponse);
            response.flushBuffer();
        }
    }

    private String convertToJson(ApiResponse<?> apiResponse) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"code\":").append(apiResponse.getCode()).append(",");
        json.append("\"status\":\"").append(apiResponse.getStatus()).append("\",");
        json.append("\"message\":\"").append(escapeJson(apiResponse.getMessage())).append("\",");
        json.append("\"error\":").append(apiResponse.getError() != null ? "\"" + escapeJson(apiResponse.getError()) + "\"" : "null").append(",");
        json.append("\"data\":null");
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
