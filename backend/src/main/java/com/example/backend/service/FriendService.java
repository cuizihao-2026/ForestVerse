package com.example.backend.service;

import com.example.backend.dto.FriendWithChatInfo;
import com.example.backend.entity.FriendRequest;
import com.example.backend.entity.User;

import java.util.List;

public interface FriendService {
    FriendRequest sendFriendRequest(Long senderId, Long receiverId, String message);
    void acceptFriendRequest(Long requestId, Long userId);
    void rejectFriendRequest(Long requestId, Long userId);
    List<FriendRequest> getReceivedRequests(Long userId);
    List<FriendRequest> getSentRequests(Long userId);
    List<User> getFriends(Long userId);
    List<FriendWithChatInfo> getFriendsWithChatInfo(Long userId);
    List<User> searchUsers(String keyword, Long excludeUserId);
    void deleteFriend(Long userId, Long friendId);
    boolean isFriend(Long userId1, Long userId2);
    void updateFriendGroup(Long userId, Long friendId, String groupName);
    void updateFriendRemark(Long userId, Long friendId, String remark);
}
