package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.FriendWithChatInfo;
import com.example.backend.entity.FriendRequest;
import com.example.backend.entity.User;
import com.example.backend.service.FriendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private static final Logger logger = LoggerFactory.getLogger(FriendController.class);

    @Autowired
    private FriendService friendService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam String keyword) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            List<User> users = friendService.searchUsers(keyword, currentUserId);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendRequest>> sendFriendRequest(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            Long receiverId = Long.valueOf(requestBody.get("receiverId").toString());
            String message = requestBody.get("message") != null ? requestBody.get("message").toString() : "";
            FriendRequest friendRequest = friendService.sendFriendRequest(currentUserId, receiverId, message);
            return ResponseEntity.ok(ApiResponse.success(friendRequest));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptFriendRequest(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long id) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            friendService.acceptFriendRequest(id, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("好友请求已接受", null));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFriendRequest(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long id) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            friendService.rejectFriendRequest(id, currentUserId);
            return ResponseEntity.ok(ApiResponse.success("好友请求已拒绝", null));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequest>>> getReceivedRequests(
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            List<FriendRequest> requests = friendService.getReceivedRequests(currentUserId);
            return ResponseEntity.ok(ApiResponse.success(requests));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendRequest>>> getSentRequests(
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            List<FriendRequest> requests = friendService.getSentRequests(currentUserId);
            return ResponseEntity.ok(ApiResponse.success(requests));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getFriends(
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            List<User> friends = friendService.getFriends(currentUserId);
            return ResponseEntity.ok(ApiResponse.success(friends));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<FriendWithChatInfo>>> getFriendsWithChatInfo(
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            List<FriendWithChatInfo> friends = friendService.getFriendsWithChatInfo(currentUserId);
            return ResponseEntity.ok(ApiResponse.success(friends));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Void>> deleteFriend(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long friendId) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            friendService.deleteFriend(currentUserId, friendId);
            return ResponseEntity.ok(ApiResponse.success("已删除好友", null));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{friendId}/group")
    public ResponseEntity<ApiResponse<Void>> updateFriendGroup(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long friendId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            String groupName = requestBody.get("groupName").toString();
            friendService.updateFriendGroup(currentUserId, friendId, groupName);
            return ResponseEntity.ok(ApiResponse.success("已更新分组", null));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{friendId}/remark")
    public ResponseEntity<ApiResponse<Void>> updateFriendRemark(
            jakarta.servlet.http.HttpServletRequest request,
            @PathVariable Long friendId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long currentUserId = (Long) request.getAttribute("userId");
            String remark = requestBody.get("remark").toString();
            friendService.updateFriendRemark(currentUserId, friendId, remark);
            return ResponseEntity.ok(ApiResponse.success("已更新备注", null));
        } catch (Exception e) {
            logger.error("操作失败: " + e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
