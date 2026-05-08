package com.example.backend.controller;

import com.example.backend.config.WebSocketHandler;
import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.UserStats;
import com.example.backend.service.UserService;
import com.example.backend.service.RolePermissionService;
import com.example.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class SystemMonitorController {

    private static final Logger logger = LoggerFactory.getLogger(SystemMonitorController.class);

    private final UserService userService;
    private final WebSocketHandler webSocketHandler;
    private final RolePermissionService rolePermissionService;

    public SystemMonitorController(UserService userService, WebSocketHandler webSocketHandler, RolePermissionService rolePermissionService) {
        this.userService = userService;
        this.webSocketHandler = webSocketHandler;
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            if (!rolePermissionService.hasPermission(user.getRole(), "console.use")) {
                return ResponseEntity.status(403).body(ApiResponse.error("您没有权限访问此接口"));
            }
            
            UserStats stats = userService.getUserStats();
            Map<String, Object> result = new HashMap<>();
            result.put("totalUsers", stats.getTotalUsers());
            result.put("onlineUsers", stats.getOnlineUsers());
            result.put("publishedArticles", stats.getPublishedArticles());
            result.put("pendingCount", stats.getPendingCount());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }



    @GetMapping("/server")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServerStats(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            if (!rolePermissionService.hasPermission(user.getRole(), "console.use")) {
                return ResponseEntity.status(403).body(ApiResponse.error("您没有权限访问此接口"));
            }
            
            Map<String, Object> stats = new HashMap<>();

            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

            long usedHeapMemory = heapMemoryUsage.getUsed();
            long maxHeapMemory = heapMemoryUsage.getMax();
            if (maxHeapMemory == -1) {
                maxHeapMemory = heapMemoryUsage.getCommitted();
            }
            double heapMemoryPercent = (double) usedHeapMemory / maxHeapMemory * 100;

            stats.put("heapMemoryUsed", bytesToMB(usedHeapMemory));
            stats.put("heapMemoryMax", bytesToMB(maxHeapMemory));
            stats.put("heapMemoryPercent", Math.round(heapMemoryPercent * 100.0) / 100.0);

            OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

            double systemLoad = osMXBean.getSystemLoadAverage();
            int availableProcessors = osMXBean.getAvailableProcessors();

            stats.put("systemLoad", systemLoad >= 0 ? Math.round(systemLoad * 100.0) / 100.0 : -1);
            stats.put("availableProcessors", availableProcessors);

            double memoryPercent = 0;
            double usedMemoryMB = 0;
            double totalMemoryMB = 0;
            double cpuPercent = 0;

            try {
                Class<?> sunOsClass = Class.forName("com.sun.management.OperatingSystemMXBean");
                if (sunOsClass.isInstance(osMXBean)) {
                    Object totalMemoryObj = sunOsClass.getMethod("getTotalPhysicalMemorySize").invoke(osMXBean);
                    long totalMemory = ((Number) totalMemoryObj).longValue();

                    Object freeMemoryObj = sunOsClass.getMethod("getFreePhysicalMemorySize").invoke(osMXBean);
                    long freeMemory = ((Number) freeMemoryObj).longValue();

                    long usedMemory = totalMemory - freeMemory;
                    memoryPercent = (double) usedMemory / totalMemory * 100;

                    usedMemoryMB = bytesToMB(usedMemory);
                    totalMemoryMB = bytesToMB(totalMemory);

                    double systemCpuLoad = -1;
                    try {
                        Object systemCpuLoadObj = sunOsClass.getMethod("getSystemCpuLoad").invoke(osMXBean);
                        systemCpuLoad = ((Number) systemCpuLoadObj).doubleValue();
                    } catch (Exception e) {
                    }

                    if (systemCpuLoad >= 0) {
                        cpuPercent = Math.round(systemCpuLoad * 10000.0) / 100.0;
                    } else {
                        try {
                            Object processCpuLoadObj = sunOsClass.getMethod("getProcessCpuLoad").invoke(osMXBean);
                            double processCpuLoad = ((Number) processCpuLoadObj).doubleValue();
                            if (processCpuLoad >= 0) {
                                cpuPercent = Math.round(processCpuLoad * 10000.0) / 100.0;
                            } else {
                                cpuPercent = systemLoad > 0 ? Math.min(systemLoad * 100 / availableProcessors, 100) : 15;
                            }
                        } catch (Exception e) {
                            cpuPercent = systemLoad > 0 ? Math.min(systemLoad * 100 / availableProcessors, 100) : 15;
                        }
                    }
                } else {
                    totalMemoryMB = 8192;
                    usedMemoryMB = totalMemoryMB * 0.5;
                    memoryPercent = 50;
                    cpuPercent = systemLoad > 0 ? Math.min(systemLoad * 100 / availableProcessors, 100) : 15;
                }
            } catch (Exception e) {
                logger.warn("使用默认系统资源值: " + e.getMessage());
                totalMemoryMB = 8192;
                usedMemoryMB = totalMemoryMB * 0.5;
                memoryPercent = 50;
                cpuPercent = systemLoad > 0 ? Math.min(systemLoad * 100 / availableProcessors, 100) : 15;
            }

            cpuPercent = Math.max(0, Math.min(100, cpuPercent));
            memoryPercent = Math.max(0, Math.min(100, memoryPercent));

            stats.put("totalMemory", totalMemoryMB);
            stats.put("usedMemory", usedMemoryMB);
            stats.put("memoryPercent", Math.round(memoryPercent * 100.0) / 100.0);
            stats.put("cpuPercent", Math.round(cpuPercent * 100.0) / 100.0);

            logger.info("Server Stats: CPU=" + stats.get("cpuPercent") +
                               "%, Memory=" + stats.get("memoryPercent") +
                               "%, Heap=" + stats.get("heapMemoryPercent") + "%");

            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private double bytesToMB(long bytes) {
        return Math.round(bytes / (1024.0 * 1024.0) * 100.0) / 100.0;
    }

    @GetMapping("/online")
    public ResponseEntity<ApiResponse<List<User>>> getOnlineUsers(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            if (!rolePermissionService.hasPermission(user.getRole(), "console.use")) {
                return ResponseEntity.status(403).body(ApiResponse.error("您没有权限访问此接口"));
            }
            
            List<User> onlineUsers = webSocketHandler.getOnlineUsers();
            return ResponseEntity.ok(ApiResponse.success(onlineUsers));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/force-refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forceRefresh(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            User user = userService.findById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
            }
            
            if (!rolePermissionService.hasPermission(user.getRole(), "console.use")) {
                return ResponseEntity.status(403).body(ApiResponse.error("您没有权限访问此接口"));
            }
            
            webSocketHandler.broadcastForcedRefresh();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("onlineUsers", webSocketHandler.getOnlineCount());
            logger.info("管理员触发强制刷新");
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
