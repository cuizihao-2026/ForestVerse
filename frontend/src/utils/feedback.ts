import { get, post, put } from './api'

export interface Feedback {
  id?: number
  userId?: number
  type: string
  title: string
  content: string
  contact?: string
  priority: string
  status?: string
  createdAt?: string
  updatedAt?: string
  userNickname?: string
  userAvatar?: string
}

export interface FeedbackMessage {
  id?: number
  feedbackId?: number
  senderId?: number
  isAdmin?: boolean
  content: string
  isRead?: boolean
  createdAt?: string
  senderNickname?: string
  senderAvatar?: string
}

export const feedbackApi = {
  getMyFeedbacks: () => get<Feedback[]>('/api/feedback/my'),
  getAllFeedbacks: () => get<Feedback[]>('/api/feedback/all'),
  getFeedbackById: (id: number) => get<Feedback>(`/api/feedback/${id}`),
  createFeedback: (data: Feedback) => post('/api/feedback/create', data),
  updateStatus: (id: number, status: string) => put(`/api/feedback/${id}/status`, { status }),
  
  getMessages: (id: number) => get<FeedbackMessage[]>(`/api/feedback/${id}/messages`),
  sendMessage: (id: number, content: string) => post(`/api/feedback/${id}/messages`, { content }),
  getUnreadCount: (id: number) => get<{ count: number }>(`/api/feedback/${id}/unread-count`),
}
