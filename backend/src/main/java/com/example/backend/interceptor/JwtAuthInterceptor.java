package com.example.backend.interceptor;

import com.example.backend.service.TokenService;
import com.example.backend.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        boolean isPublicArticleGet = "GET".equalsIgnoreCase(request.getMethod()) && path.matches(".*/api/article/\\d+$");
        boolean isArticleSearch = "GET".equalsIgnoreCase(request.getMethod()) && path.matches(".*/api/article/search(/paginated)?$");
        boolean isArticlePublished = "GET".equalsIgnoreCase(request.getMethod()) && path.matches(".*/api/article/published(/paginated)?$");
        boolean isCommentGet = "GET".equalsIgnoreCase(request.getMethod()) && path.matches(".*/api/comment/article/\\d+(/count)?$");

        String authHeader = request.getHeader("Authorization");

        boolean isPublicEndpoint = isPublicArticleGet || isArticleSearch || isArticlePublished || isCommentGet;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (isPublicEndpoint) return true;
            sendUnauthorizedResponse(response, "缺少token或token格式错误");
            return false;
        }

        String token = authHeader.substring(7);

        if (!jwtUtils.validateToken(token)) {
            if (isPublicEndpoint) return true;
            sendUnauthorizedResponse(response, "token无效或已过期");
            return false;
        }

        String username = jwtUtils.getUsernameFromToken(token);
        Long userId = jwtUtils.getUserIdFromToken(token);
        String role = jwtUtils.getRoleFromToken(token);

        if (username == null || userId == null) {
            if (isPublicEndpoint) return true;
            sendUnauthorizedResponse(response, "token信息解析失败");
            return false;
        }

        // 验证token是否是用户当前活跃的token
        if (!tokenService.isTokenActive(userId, token)) {
            if (isPublicEndpoint) return true;
            sendUnauthorizedResponse(response, "账号已在别处登录，请重新登录");
            return false;
        }

        boolean isAdminRole = "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("isAdmin", isAdminRole);

        return true;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("code", 401);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
