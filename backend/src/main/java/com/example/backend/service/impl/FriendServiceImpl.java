package com.example.backend.service.impl;

import com.example.backend.config.WebSocketHandler;
import com.example.backend.dto.FriendWithChatInfo;
import com.example.backend.entity.FriendRequest;
import com.example.backend.entity.Friendship;
import com.example.backend.entity.User;
import com.example.backend.mapper.ChatMessageMapper;
import com.example.backend.mapper.FriendRequestMapper;
import com.example.backend.mapper.FriendshipMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendRequestMapper friendRequestMapper;

    @Autowired
    private FriendshipMapper friendshipMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    @Lazy
    private WebSocketHandler webSocketHandler;

    @Override
    @Transactional
    public FriendRequest sendFriendRequest(Long senderId, Long receiverId, String message) {
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        User receiver = userMapper.findById(receiverId);
        if (receiver == null) {
            throw new RuntimeException("用户不存在");
        }

        if (isFriend(senderId, receiverId)) {
            throw new RuntimeException("对方已经是你的好友了");
        }

        // 检查是否有自己之前发送过的待处理请求
        FriendRequest existingRequest = friendRequestMapper.findBySenderAndReceiver(senderId, receiverId);
        if (existingRequest != null) {
            if ("PENDING".equals(existingRequest.getStatus())) {
                throw new RuntimeException("已发送过好友请求，请等待对方回复");
            }
        }

        // 检查是否有反向的待处理请求
        FriendRequest reverseRequest = friendRequestMapper.findBySenderAndReceiver(receiverId, senderId);
        if (reverseRequest != null && "PENDING".equals(reverseRequest.getStatus())) {
            // 如果有反向的待处理请求，直接接受，双方成为好友
            friendRequestMapper.updateStatus(reverseRequest.getId(), "ACCEPTED");

            Friendship friendship1 = new Friendship();
    friendship1.setUserId(senderId);
    friendship1.setFriendId(receiverId);
    friendship1.setGroupName("我的好友");
    friendshipMapper.insert(friendship1);

    Friendship friendship2 = new Friendship();
    friendship2.setUserId(receiverId);
    friendship2.setFriendId(senderId);
    friendship2.setGroupName("我的好友");
            friendshipMapper.insert(friendship2);

            // 向双方发送WebSocket通知
            webSocketHandler.sendFriendAccepted(senderId);
            webSocketHandler.sendFriendAccepted(receiverId);

            reverseRequest.setStatus("ACCEPTED");
            return reverseRequest;
        }

        // 创建新的好友请求
        FriendRequest request = new FriendRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setStatus("PENDING");
        request.setMessage(message);
        friendRequestMapper.insert(request);

        // 向接收者发送WebSocket通知
        webSocketHandler.sendFriendRequest(receiverId);

        return request;
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("好友请求不存在");
        }
        if (!request.getReceiverId().equals(userId)) {
            throw new RuntimeException("无权处理此请求");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("此请求已被处理");
        }

        friendRequestMapper.updateStatus(requestId, "ACCEPTED");

        // 检查是否有反向的待处理请求，如果有也标记为已接受
        FriendRequest reverseRequest = friendRequestMapper.findBySenderAndReceiver(userId, request.getSenderId());
        if (reverseRequest != null && "PENDING".equals(reverseRequest.getStatus())) {
            friendRequestMapper.updateStatus(reverseRequest.getId(), "ACCEPTED");
        }

        // 创建好友关系
        Friendship friendship1 = new Friendship();
        friendship1.setUserId(userId);
        friendship1.setFriendId(request.getSenderId());
        friendship1.setGroupName("我的好友");
        friendshipMapper.insert(friendship1);

        Friendship friendship2 = new Friendship();
        friendship2.setUserId(request.getSenderId());
        friendship2.setFriendId(userId);
        friendship2.setGroupName("我的好友");
        friendshipMapper.insert(friendship2);

        // 向双方发送WebSocket通知
        webSocketHandler.sendFriendAccepted(request.getSenderId());
        webSocketHandler.sendFriendAccepted(userId);
    }

    @Override
    public void rejectFriendRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestMapper.findById(requestId);
        if (request == null) {
            throw new RuntimeException("好友请求不存在");
        }
        if (!request.getReceiverId().equals(userId)) {
            throw new RuntimeException("无权处理此请求");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("此请求已被处理");
        }

        friendRequestMapper.updateStatus(requestId, "REJECTED");
    }

    @Override
    public List<FriendRequest> getReceivedRequests(Long userId) {
        return friendRequestMapper.findByReceiverId(userId);
    }

    @Override
    public List<FriendRequest> getSentRequests(Long userId) {
        return friendRequestMapper.findBySenderId(userId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        List<Friendship> friendships = friendshipMapper.findByUserId(userId);
        return friendships.stream()
                .map(fs -> {
                    User friend = fs.getFriend();
                    friend.setRemark(fs.getRemark());
                    friend.setGroupName(fs.getGroupName());
                    return friend;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<User> searchUsers(String keyword, Long excludeUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return userMapper.searchUsers(keyword.trim(), excludeUserId);
    }

    @Override
    public void deleteFriend(Long userId, Long friendId) {
        friendshipMapper.deleteByUserAndFriend(userId, friendId);
        friendshipMapper.deleteByUserAndFriend(friendId, userId);
    }

    @Override
    public boolean isFriend(Long userId1, Long userId2) {
        Friendship friendship = friendshipMapper.findByUserAndFriend(userId1, userId2);
        return friendship != null;
    }

    @Override
    public void updateFriendGroup(Long userId, Long friendId, String groupName) {
        friendshipMapper.updateGroup(userId, friendId, groupName);
    }

    @Override
    public void updateFriendRemark(Long userId, Long friendId, String remark) {
        friendshipMapper.updateRemark(userId, friendId, remark);
    }

    @Override
    public List<FriendWithChatInfo> getFriendsWithChatInfo(Long userId) {
        List<Friendship> friendships = friendshipMapper.findByUserId(userId);
        List<User> friends = friendships.stream()
                .map(fs -> {
                    User friend = fs.getFriend();
                    friend.setRemark(fs.getRemark());
                    friend.setGroupName(fs.getGroupName());
                    return friend;
                })
                .collect(Collectors.toList());

        if (friends.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> friendIds = friends.stream().map(User::getId).collect(Collectors.toList());

        List<Map<String, Object>> latestMessages = chatMessageMapper.getLatestMessagesForFriends(userId, friendIds);
        Map<Long, Map<String, Object>> latestMessageMap = new HashMap<>();
        for (Map<String, Object> msg : latestMessages) {
            Long friendId = ((Number) msg.get("friend_id")).longValue();
            latestMessageMap.put(friendId, msg);
        }

        List<Map<String, Object>> unreadCounts = chatMessageMapper.getUnreadCountsForFriends(userId, friendIds);
        Map<Long, Integer> unreadCountMap = new HashMap<>();
        for (Map<String, Object> count : unreadCounts) {
            Long friendId = ((Number) count.get("friend_id")).longValue();
            int unread = ((Number) count.get("unread_count")).intValue();
            unreadCountMap.put(friendId, unread);
        }

        List<FriendWithChatInfo> result = new ArrayList<>();
        for (User friend : friends) {
            String lastMessage = "";
            String lastMessageTime = "";
            Long lastTimestamp = null;
            int unreadCount = unreadCountMap.getOrDefault(friend.getId(), 0);

            if (latestMessageMap.containsKey(friend.getId())) {
                Map<String, Object> msg = latestMessageMap.get(friend.getId());
                lastMessage = (String) msg.get("content");
                if (msg.get("created_at") != null) {
                    LocalDateTime createdAt = (LocalDateTime) msg.get("created_at");
                    lastTimestamp = createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    lastMessageTime = formatTime(createdAt);
                }
            }

            FriendWithChatInfo info = new FriendWithChatInfo(
                    friend,
                    lastMessage,
                    lastMessageTime,
                    lastTimestamp,
                    unreadCount
            );
            result.add(info);
        }

        result.sort((a, b) -> {
            if (a.getLastTimestamp() == null && b.getLastTimestamp() == null) {
                return 0;
            }
            if (a.getLastTimestamp() == null) {
                return 1;
            }
            if (b.getLastTimestamp() == null) {
                return -1;
            }
            return Long.compare(b.getLastTimestamp(), a.getLastTimestamp());
        });

        return result;
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();
        if (time.toLocalDate().equals(now.toLocalDate())) {
            return time.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (time.getYear() == now.getYear()) {
            return time.format(DateTimeFormatter.ofPattern("M月d日"));
        } else {
            return time.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));
        }
    }
}
