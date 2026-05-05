import { get, post, del } from './api'
import { uploadFile } from './api'

export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  bio: string
  role: string
  status: string
  remark?: string
  groupName?: string
  signature?: string
}

export interface FriendRequest {
  id: number
  senderId: number
  receiverId: number
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  message: string
  sender?: User
  receiver?: User
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: number
  senderId: number
  receiverId: number
  content: string
  isRead: boolean
  createdAt: string
}

export interface FriendWithChatInfo {
  friend: User
  lastMessage: string
  lastMessageTime: string
  lastTimestamp: number | null
  unreadCount: number
}

export const searchUsers = (keyword: string) => {
  return get(`/api/friends/search?keyword=${encodeURIComponent(keyword)}`)
}

export const sendFriendRequest = (receiverId: number, message?: string) => {
  return post('/api/friends/requests', { receiverId, message })
}

export const acceptFriendRequest = (requestId: number) => {
  return post(`/api/friends/requests/${requestId}/accept`)
}

export const rejectFriendRequest = (requestId: number) => {
  return post(`/api/friends/requests/${requestId}/reject`)
}

export const getReceivedRequests = () => {
  return get('/api/friends/requests/received')
}

export const getSentRequests = () => {
  return get('/api/friends/requests/sent')
}

export const getFriends = () => {
  return get('/api/friends')
}

export const getFriendsWithChatInfo = () => {
  return get('/api/friends/batch')
}

export const deleteFriend = (friendId: number) => {
  return del(`/api/friends/${friendId}`)
}

export const updateFriendGroup = (friendId: number, groupName: string) => {
  return post(`/api/friends/${friendId}/group`, { groupName })
}

export const updateFriendRemark = (friendId: number, remark: string) => {
  return post(`/api/friends/${friendId}/remark`, { remark })
}

// 聊天相关API
export const sendChatMessage = (receiverId: number, content: string) => {
  return post('/api/chat/send', { receiverId, content })
}

export const getConversation = (friendId: number, page: number = 1, size: number = 20) => {
  return get(`/api/chat/conversation/${friendId}?page=${page}&size=${size}`)
}

export const markAsRead = (friendId: number) => {
  return post(`/api/chat/mark-read/${friendId}`)
}

export const getUnreadCount = (friendId: number) => {
  return get(`/api/chat/unread/${friendId}`)
}

// 上传聊天图片
export const uploadChatImage = async (file: File) => {
  return uploadFile(file, 'chat-images')
}
