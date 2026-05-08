<template>
  <div class="comment-section">
    <div class="comment-header">
      <h4>评论 ({{ totalCount }})</h4>
    </div>

    <div v-if="hasCommentPermission" class="comment-input-wrapper">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="写下你的评论..."
        class="comment-input"
        maxlength="300"
        show-word-limit
      />
      <div class="comment-actions">
        <el-button type="primary" size="small" @click="submitComment" :loading="submitting">
          发送评论
        </el-button>
      </div>
    </div>
    
    <div v-else class="comment-input-wrapper no-permission">
      <div class="no-permission-text">您的账号暂无评论权限</div>
    </div>

    <div class="comment-list">
      <div v-if="mainComments.length === 0" class="empty-comments">
        暂无评论，快来抢沙发吧！
      </div>
      <div v-else>
        <div v-for="comment in mainComments" :key="comment.id" class="comment-item">
          <img 
            v-if="comment.userAvatar" 
            :src="comment.userAvatar" 
            :alt="comment.userName || '用户头像'" 
            class="comment-avatar avatar"
            style="width: 40px; height: 40px;"
          />
          <div 
            v-else 
            class="comment-avatar avatar fallback"
            style="width: 40px; height: 40px; font-size: 16px;"
          >
            {{ (comment.userName || '用户')[0] }}
          </div>
          <div class="comment-content-wrapper">
            <div class="comment-meta">
              <span class="comment-author">{{ comment.userName || '匿名用户' }}</span>
              <span v-if="comment.status === 1" class="pending-badge">审核中</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <div class="comment-text">
              <span v-if="comment.replyToUserName" class="reply-to">
                回复 {{ comment.replyToUserName }}:
              </span>
              {{ comment.content }}
            </div>
            <div v-if="hasCommentPermission" class="comment-footer">
              <span class="reply-btn" @click="startReply(comment)">回复</span>
            </div>
            
            <div v-if="getAllRepliesForRoot(comment.id).length > 0" class="replies-wrapper">
              <div v-if="!expandedReplies.has(comment.id)" class="expand-btn" @click="expandedReplies.add(comment.id)">
                展开 {{ getAllRepliesForRoot(comment.id).length }} 条回复
              </div>
              <div v-else>
                <div class="replies-list">
                  <div v-for="reply in getAllRepliesForRoot(comment.id)" :key="reply.id" class="comment-item reply-item">
                    <img 
                      v-if="reply.userAvatar" 
                      :src="reply.userAvatar" 
                      :alt="reply.userName || '用户头像'" 
                      class="comment-avatar avatar"
                      style="width: 32px; height: 32px;"
                    />
                    <div 
                      v-else 
                      class="comment-avatar avatar fallback"
                      style="width: 32px; height: 32px; font-size: 14px;"
                    >
                      {{ (reply.userName || '用户')[0] }}
                    </div>
                    <div class="comment-content-wrapper">
                      <div class="comment-meta">
                        <span class="comment-author">{{ reply.userName || '匿名用户' }}</span>
                        <span v-if="reply.status === 1" class="pending-badge">审核中</span>
                        <span class="comment-time">{{ formatTime(reply.createdAt) }}</span>
                      </div>
                      <div class="comment-text">
                        <span v-if="reply.replyToUserName" class="reply-to">
                          回复 {{ reply.replyToUserName }}:
                        </span>
                        {{ reply.content }}
                      </div>
                      <div v-if="hasCommentPermission" class="comment-footer">
                        <span class="reply-btn" @click="startReply(reply)">回复</span>
                      </div>
                      
                      <div v-if="replyingTo === reply.id" class="reply-input-wrapper">
                        <el-input
                          v-model="replyText"
                          type="textarea"
                          :rows="2"
                          resize="none"
                          placeholder="回复评论..."
                          size="small"
                          maxlength="300"
                          show-word-limit
                        />
                        <div class="reply-actions">
                          <el-button size="small" @click="cancelReply">取消</el-button>
                          <el-button type="primary" size="small" @click="submitReply" :loading="submitting">
                            发送
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <button class="collapse-btn" @click="expandedReplies.delete(comment.id)" type="button">
                  收起回复
                </button>
              </div>
            </div>

            <div v-if="replyingTo === comment.id" class="reply-input-wrapper">
              <el-input
                v-model="replyText"
                type="textarea"
                :rows="2"
                resize="none"
                placeholder="回复评论..."
                size="small"
                maxlength="300"
                show-word-limit
              />
              <div class="reply-actions">
                <el-button size="small" @click="cancelReply">取消</el-button>
                <el-button type="primary" size="small" @click="submitReply" :loading="submitting">
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { get, post } from '../../utils/api';
import { ElMessageBox, ElMessage } from 'element-plus';
import { isLoggedIn } from '../../stores/auth';
import { hasPermission } from '../../stores/permission';

const props = defineProps<{
  articleId: number;
}>();

const router = useRouter();

const emit = defineEmits<{
  countChange: [count: number];
}>();

const comments = ref<any[]>([]);
const newComment = ref('');
const replyText = ref('');
const replyingTo = ref<number | null>(null);
const submitting = ref(false);
const expandedReplies = ref(new Set<number>());

const hasCommentPermission = computed(() => hasPermission('comment:use'));

const mainComments = computed(() => {
  return comments.value.filter(c => !c.parentId);
});

// 找到评论的根评论ID（主评论ID）
const getRootCommentId = (comment: any): number => {
  let current = comment;
  let depth = 0;
  while (current.parentId && depth < 100) { // 防止无限循环
    const parent = comments.value.find(c => c.id === current.parentId);
    if (!parent) break;
    current = parent;
    depth++;
  }
  return current.id;
};

// 获取某个主评论下的所有回复
const getAllRepliesForRoot = (rootId: number) => {
  return comments.value.filter(c => {
    if (!c.parentId) return false;
    try {
      return getRootCommentId(c) === rootId;
    } catch {
      return false;
    }
  });
};

const totalCount = computed(() => comments.value.length);

const loadComments = async () => {
  try {
    const data = await get(`/api/comment/article/${props.articleId}`);
    comments.value = data || [];
    emit('countChange', comments.value.filter(c => c.status === 2).length);
  } catch (error) {
    console.error('加载评论失败', error);
  }
};

const submitComment = async () => {
  if (!newComment.value.trim()) return;
  
  // 检查是否登录
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再发表评论', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login');
    }).catch(() => {});
    return;
  }
  
  // 检查评论权限
  if (!hasCommentPermission.value) {
    ElMessage.error('您的账号暂无评论权限');
    return;
  }
  
  submitting.value = true;
  try {
    await post('/api/comment', {
      articleId: props.articleId,
      content: newComment.value.trim()
    });
    newComment.value = '';
    await loadComments();
  } catch (error) {
    console.error('发送评论失败', error);
  } finally {
    submitting.value = false;
  }
};

const startReply = (comment: any) => {
  // 检查是否登录
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再发表回复', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login');
    }).catch(() => {});
    return;
  }
  
  // 检查评论权限
  if (!hasCommentPermission.value) {
    ElMessage.error('您的账号暂无评论权限');
    return;
  }
  
  replyingTo.value = comment.id;
  replyText.value = '';
};

const cancelReply = () => {
  replyingTo.value = null;
  replyText.value = '';
};

const submitReply = async () => {
  if (!replyText.value.trim()) return;
  
  // 检查是否登录
  if (!isLoggedIn.value) {
    ElMessageBox.confirm('请先登录后再发表回复', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      router.push('/login');
    }).catch(() => {});
    return;
  }
  
  // 检查评论权限
  if (!hasCommentPermission.value) {
    ElMessage.error('您的账号暂无评论权限');
    return;
  }
  
  submitting.value = true;
  try {
    const replyComment = comments.value.find(c => c.id === replyingTo.value);
    await post('/api/comment', {
      articleId: props.articleId,
      content: replyText.value.trim(),
      parentId: replyingTo.value,
      replyToUserId: replyComment?.userId
    });
    replyingTo.value = null;
    replyText.value = '';
    await loadComments();
  } catch (error) {
    console.error('发送回复失败', error);
  } finally {
    submitting.value = false;
  }
};

const formatTime = (dateStr: any) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return '';
  
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;

  return date.toLocaleDateString('zh-CN');
};

const handleCommentApproved = (event: any) => {
  const { articleId } = event.detail;
  console.log('收到评论审核通过事件:', { articleId, currentArticleId: props.articleId });
  if (Number(articleId) === Number(props.articleId)) {
    console.log('评论审核通过，刷新评论列表');
    loadComments();
  }
};

const handleCommentRejected = (event: any) => {
  const { articleId } = event.detail;
  console.log('收到评论被拒绝事件:', { articleId, currentArticleId: props.articleId });
  if (Number(articleId) === Number(props.articleId)) {
    console.log('评论被拒绝，刷新评论列表');
    loadComments();
  }
};

onMounted(() => {
  loadComments();
  window.addEventListener('comment-approved', handleCommentApproved);
  window.addEventListener('comment-rejected', handleCommentRejected);
});

onUnmounted(() => {
  window.removeEventListener('comment-approved', handleCommentApproved);
  window.removeEventListener('comment-rejected', handleCommentRejected);
});

watch(() => props.articleId, () => {
  loadComments();
});
</script>

<style scoped>
.comment-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.comment-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.comment-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.no-permission {
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.no-permission-text {
  color: #9ca3af;
  font-size: 14px;
  text-align: center;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  padding-left: 12px;
  padding-right: 12px;
}

.comment-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  overflow-y: auto;
}

.empty-comments {
  text-align: center;
  color: #9ca3af;
  padding: 20px 0;
  font-size: 14px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 6px 6px 6px 6px;
  border-bottom: 1px solid #f3f4f6;
}

.comment-item:last-child {
  border-bottom: none;
}

.reply-item {
  padding: 8px 0;
  border-bottom: none;
}

.comment-avatar {
  flex-shrink: 0;
  border-radius: 50%;
  object-fit: cover;
}

.comment-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.comment-meta {
  display: flex;
  gap: 10px;
  align-items: center;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
  color: #374151;
}

.pending-badge {
  font-size: 11px;
  color: #f59e0b;
  background: #fff7ed;
  padding: 2px 6px;
  border-radius: 4px;
}

.comment-time {
  font-size: 12px;
  color: #9ca3af;
}

.comment-text {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  word-break: break-word;
  text-align: left;
}

.reply-to {
  color: #3b82f6;
  margin-right: 4px;
}

.comment-footer {
  display: flex;
  gap: 12px;
}

.reply-btn {
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 0;
  transition: color 0.2s;
}

.reply-btn:hover {
  color: #3b82f6;
}

.replies-wrapper {
  margin-top: 12px;
  padding-left: 12px;
  border-left: 2px solid #f3f4f6;
}

.expand-btn,
.collapse-btn {
  font-size: 12px;
  color: #3b82f6;
  cursor: pointer;
  padding: 4px 0;
  transition: color 0.2s;
}

.expand-btn:hover,
.collapse-btn:hover {
  color: #2563eb;
}

.replies-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reply-input-wrapper {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.comment-list::-webkit-scrollbar {
  width: 6px;
}

.comment-list::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.comment-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.comment-list::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
