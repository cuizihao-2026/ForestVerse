import { ref } from 'vue'
import router from '../router'
import { showModal } from './modal'
import { loadNotifications } from './notification'
import { get } from '../utils/api'
import {
  currentUser,
  isLoggedIn,
  getToken,
  setToken,
  removeToken,
  updateCurrentUser,
  logout
} from './auth'

interface WebsiteSettings {
  heartbeatEnabled: boolean
  heartbeatRate: number
  heartbeatTimeout: number
}

export const onlineUserIds = ref<Set<number>>(new Set())

let webSocketInitialized = false
let settings: WebsiteSettings = {
  heartbeatEnabled: true,
  heartbeatRate: 30,
  heartbeatTimeout: 60
}
let ws: WebSocket | null = null
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 5
const RECONNECT_DELAY = 3000
let heartbeatTimer: number | null = null
let pongWatcherTimer: number | null = null
let lastHeartbeatTime = ''
let lastPongTime = 0
let isUserDeleted = false
let isAccountKicked = false
let isLoggingOut = false

export const getIsAccountKicked = () => isAccountKicked
export const getIsLoggingOut = () => isLoggingOut
export const setLoggingOut = (v: boolean) => { isLoggingOut = v }

const emitWsStatus = () => {
  let status = 'disconnected'
  if (ws) {
    switch (ws.readyState) {
      case WebSocket.CONNECTING:
        status = 'connecting'
        break
      case WebSocket.OPEN:
        status = 'connected'
        break
      case WebSocket.CLOSING:
        status = 'disconnecting'
        break
      case WebSocket.CLOSED:
        status = 'disconnected'
        break
    }
  }
  const event = new CustomEvent('ws-status-changed', {
    detail: { status, lastHeartbeat: lastHeartbeatTime }
  })
  window.dispatchEvent(event)
}

const startHeartbeat = () => {
  stopHeartbeat()
  if (!settings.heartbeatEnabled) return

  heartbeatTimer = window.setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN && currentUser.value) {
      const pingMessage = {
        type: 'ping',
        userId: currentUser.value.id,
        timestamp: Date.now()
      }
      ws.send(JSON.stringify(pingMessage))
    }
  }, settings.heartbeatRate * 1000)
}

const stopHeartbeat = () => {
  if (heartbeatTimer !== null) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
  stopPongWatcher()
}

const startPongWatcher = () => {
  stopPongWatcher()
  const timeout = Math.max(settings.heartbeatTimeout * 1000, 15000)
  pongWatcherTimer = window.setInterval(() => {
    if (!lastPongTime) return
    const elapsed = Date.now() - lastPongTime
    if (elapsed > timeout) {
      console.log('Pong 响应超时，主动断开 WebSocket 以触发重连')
      if (ws) {
        ws.close()
        ws = null
      }
      webSocketInitialized = false
    }
  }, 5000)
}

const stopPongWatcher = () => {
  if (pongWatcherTimer !== null) {
    clearInterval(pongWatcherTimer)
    pongWatcherTimer = null
  }
  lastPongTime = 0
}

const loadSettings = async () => {
  try {
    const data = await get('/api/settings/public/heartbeat-config')
    if (data) {
      settings = { ...settings, ...data }
    }
  } catch (error) {
    console.error('加载心跳设置失败:', error)
  }
}

export const restartHeartbeat = async () => {
  await loadSettings()
  stopHeartbeat()
  startHeartbeat()
  startPongWatcher()
}

export const initWebSocket = () => {
  if (!currentUser.value) {
    return
  }

  if (webSocketInitialized) {
    console.log('WebSocket正在初始化中，跳过重复连接')
    return
  }

  if (ws && ws.readyState === WebSocket.OPEN) {
    console.log('WebSocket已连接，跳过重复连接')
    return
  }

  if (ws && ws.readyState === WebSocket.CONNECTING) {
    console.log('WebSocket正在连接中，跳过重复连接')
    return
  }

  try {
    webSocketInitialized = true
    emitWsStatus()
    const token = getToken()
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const url = new URL(`${protocol}//${host}/ws/notifications`)
    ws = new WebSocket(url.toString())

    ws.onopen = async () => {
      console.log('WebSocket连接成功，发送认证消息')
      if (ws && token && currentUser.value) {
        ws.send(JSON.stringify({
          type: 'auth',
          userId: currentUser.value.id,
          username: currentUser.value.username,
          token: token
        }))
      }
      reconnectAttempts = 0
      lastHeartbeatTime = new Date().toLocaleTimeString()
      emitWsStatus()
      startHeartbeat()
      startPongWatcher()

      try {
        const token = getToken()
        if (token) {
          const userData = await get('/api/user/profile')

          if (userData.status === 'BANNED') {
            console.log('用户已被禁用，自动登出')
            updateCurrentUser(userData)
            logout()
          } else if (currentUser.value && userData.id === currentUser.value.id) {
            const hasChanged =
              userData.username !== currentUser.value.username ||
              userData.nickname !== currentUser.value.nickname ||
              userData.email !== currentUser.value.email ||
              userData.avatar !== currentUser.value.avatar ||
              userData.bio !== currentUser.value.bio ||
              userData.role !== currentUser.value.role ||
              userData.status !== currentUser.value.status

            if (hasChanged) {
              updateCurrentUser(userData)
            }
          }
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
      }
    }

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)

        if (message.type === 'pong') {
          lastHeartbeatTime = new Date().toLocaleTimeString()
          lastPongTime = Date.now()
          emitWsStatus()
          return
        }

        if (message.type === 'USER_UPDATED' && message.data) {
          updateCurrentUser(message.data)
          console.log('用户信息已更新:', message.data)

          if (message.data.status === 'BANNED') {
            console.log('用户已被禁用，显示通知并自动登出')
            isLoggingOut = true
            showModal({
              title: '账号已被禁用',
              message: '您的账号已被管理员禁用，请联系管理员获取更多信息。',
              type: 'error',
              closable: false,
              onConfirm: logout
            })
          }
        } else if (message.type === 'USER_DELETED') {
          console.log('用户账户已被删除，强制登出')
          isUserDeleted = true
          const isSelfDelete = message.reason === 'SELF_DELETE'
          if (!isSelfDelete) {
            isLoggingOut = true
            showModal({
              title: '账号已被删除',
              message: '您的账号已被管理员删除，请联系管理员获取更多信息。',
              type: 'error',
              closable: false,
              onConfirm: logout
            })
          } else {
            logout()
          }
        } else if (message.type === 'ONLINE_COUNT') {
          console.log('在线人数更新:', message.onlineCount)
          const onlineCountEvent = new CustomEvent('online-count-changed', {
            detail: message.onlineCount
          })
          window.dispatchEvent(onlineCountEvent)
        } else if (message.type === 'ONLINE_USERS') {
          console.log('在线用户列表更新:', message.onlineUsers)
          onlineUserIds.value = new Set(message.onlineUsers || [])
          const onlineUsersEvent = new CustomEvent('online-users-changed', {
            detail: message.onlineUsers || []
          })
          window.dispatchEvent(onlineUsersEvent)
        } else if (message.type === 'TOKEN_REFRESHED' && message.token) {
          console.log('收到token刷新通知，更新token')
          setToken(message.token)
        } else if (message.type === 'NEW_NOTIFICATION') {
          console.log('收到新通知，刷新通知列表')
          loadNotifications()
        } else if (message.type === 'NEW_FRIEND_REQUEST') {
          console.log('收到新好友请求，刷新请求列表')
          const friendRequestEvent = new CustomEvent('new-friend-request')
          window.dispatchEvent(friendRequestEvent)
        } else if (message.type === 'FRIEND_ACCEPTED') {
          console.log('好友请求被接受，刷新好友列表')
          const friendAcceptedEvent = new CustomEvent('friend-accepted')
          window.dispatchEvent(friendAcceptedEvent)
        } else if (message.type === 'NEW_CHAT_MESSAGE') {
          console.log('收到新聊天消息:', message.data)
          const chatMessageEvent = new CustomEvent('new-chat-message', {
            detail: message.data
          })
          window.dispatchEvent(chatMessageEvent)
        } else if (message.type === 'COMMENT_APPROVED') {
          console.log('=== [WebSocket] 收到评论审核通过通知 ===', message)
          const commentApprovedEvent = new CustomEvent('comment-approved', {
            detail: { articleId: message.articleId, commentId: message.commentId }
          })
          window.dispatchEvent(commentApprovedEvent)
          console.log('=== [WebSocket] 已分发 comment-approved 事件 ===')
        } else if (message.type === 'COMMENT_REJECTED') {
          console.log('=== [WebSocket] 收到评论被拒绝通知 ===', message)
          const commentRejectedEvent = new CustomEvent('comment-rejected', {
            detail: { articleId: message.articleId, commentId: message.commentId }
          })
          window.dispatchEvent(commentRejectedEvent)
          console.log('=== [WebSocket] 已分发 comment-rejected 事件 ===')
        } else if (message.type === 'ARTICLE_APPROVED') {
          console.log('=== [WebSocket] 收到文章审核通过通知 ===', message)
          const articleApprovedEvent = new CustomEvent('article-approved', {
            detail: { articleId: message.articleId }
          })
          window.dispatchEvent(articleApprovedEvent)
          console.log('=== [WebSocket] 已分发 article-approved 事件 ===')
        } else if (message.type === 'FORCED_REFRESH') {
          console.log('收到强制刷新通知，刷新当前页面')
          window.location.reload()
        } else if (message.type === 'ARTICLE_UNPUBLISHED') {
          console.log('=== [WebSocket] 收到文章下架通知 ===', message)
          const articleUnpublishedEvent = new CustomEvent('article-unpublished', {
            detail: {
              articleId: message.articleId,
              articleTitle: message.articleTitle,
              newStatus: message.newStatus
            }
          })
          window.dispatchEvent(articleUnpublishedEvent)
          console.log('=== [WebSocket] 已分发 article-unpublished 事件 ===')
        } else if (message.type === 'ACCOUNT_KICKED') {
          console.log('账号在其他地方登录，立即退出')
          isAccountKicked = true

          stopHeartbeat()
          webSocketInitialized = false

          const onLoginPage = window.location.pathname === '/login'

          if (isLoggedIn.value && !onLoginPage && !isLoggingOut) {
            isLoggingOut = true
            showModal({
              title: '提示',
              message: message.message || '您的账号已在其他地方登录',
              type: 'warning',
              closable: false,
              onConfirm: () => {
                isLoggedIn.value = false
                currentUser.value = null
                localStorage.removeItem('currentUser')
                removeToken()
                reconnectAttempts = 0
                isUserDeleted = false
                isAccountKicked = false
                isLoggingOut = false
                if (ws) {
                  ws.close()
                  ws = null
                }
                router.push('/login')
              }
            })
          } else {
            console.log('在登录页或用户已退出，直接清理状态')
            isLoggedIn.value = false
            currentUser.value = null
            localStorage.removeItem('currentUser')
            removeToken()
            reconnectAttempts = 0
            isUserDeleted = false
            isAccountKicked = false
            isLoggingOut = false
            if (ws) {
              ws.close()
              ws = null
            }
          }
        }
      } catch (error) {
        if (event.data === 'pong') {
          lastHeartbeatTime = new Date().toLocaleTimeString()
          lastPongTime = Date.now()
          emitWsStatus()
          return
        }
        console.error('解析WebSocket消息失败:', error)
      }
    }

    ws.onclose = (event) => {
      console.log('WebSocket连接关闭', event)
      webSocketInitialized = false
      stopHeartbeat()
      emitWsStatus()

      if (isUserDeleted || isAccountKicked || isLoggingOut) {
        console.log('用户已被删除、踢下线或正在退出，不进行重连')
        return
      }

      if (!isLoggedIn.value) {
        return
      }

      if (event.code === 1003) {
        console.log('WebSocket连接被拒绝，可能是token失效')
        const onLoginPage = window.location.pathname === '/login'
        if (isLoggingOut || onLoginPage) {
          if (onLoginPage) {
            logout()
          }
        } else {
          isLoggingOut = true
          showModal({
            title: '提示',
            message: '登录状态已失效，请重新登录',
            type: 'warning',
            closable: false,
            onConfirm: logout
          })
        }
        return
      }

      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && isLoggedIn.value) {
        reconnectAttempts++
        console.log(`WebSocket连接关闭，${RECONNECT_DELAY / 1000}秒后尝试重连（${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}）`)
        setTimeout(() => {
          initWebSocket()
        }, RECONNECT_DELAY)
      }
    }

    ws.onerror = (error) => {
      console.error('WebSocket错误:', error)
    }
  } catch (error) {
    console.error('初始化WebSocket失败:', error)
    webSocketInitialized = false
  }
}

export const closeWebSocket = () => {
  stopHeartbeat()
  webSocketInitialized = false
  if (ws) {
    ws.close()
    ws = null
  }
  emitWsStatus()
}

export const checkWsStatus = () => {
  emitWsStatus()
}

window.addEventListener('auth-login', async () => {
  reconnectAttempts = 0
  webSocketInitialized = false
  isAccountKicked = false
  isUserDeleted = false
  isLoggingOut = false
  await loadSettings()
  await new Promise(resolve => setTimeout(resolve, 50))
  initWebSocket()
})

window.addEventListener('auth-logout', () => {
  isLoggingOut = false
  stopHeartbeat()
  closeWebSocket()
  webSocketInitialized = false
  reconnectAttempts = 0
  isUserDeleted = false
  isAccountKicked = false
})
