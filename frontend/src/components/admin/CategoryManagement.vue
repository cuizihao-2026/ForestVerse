<template>
  <div v-if="hasArticleManage" class="category-management">
    <div class="page-header">
      <h2>分类管理</h2>
      <button class="btn btn-primary" @click="showAddModal = true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="btn-icon">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        添加分类
      </button>
    </div>

    <div class="category-list">
      <div v-for="(cat, index) in categories" :key="cat.id" class="category-item">
        <div class="category-info">
          <span class="category-order">#{{ index + 1 }}</span>
          <span class="category-name">{{ cat.name }}</span>
        </div>
        <div class="category-actions">
          <button class="btn btn-sm btn-secondary" @click="editCategory(cat)">编辑</button>
          <button class="btn btn-sm btn-danger" @click="deleteCategory(cat)">删除</button>
        </div>
      </div>
      <div v-if="categories.length === 0" class="empty-state">
        <p>暂无分类，请添加</p>
      </div>
    </div>

    <!-- 添加/编辑弹窗 -->
    <div v-if="showAddModal || showEditModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <h3>{{ showEditModal ? '编辑分类' : '添加分类' }}</h3>
        <div class="form-group">
          <label>分类名称</label>
          <input 
            type="text" 
            v-model="formData.name" 
            class="form-input" 
            placeholder="请输入分类名称"
            maxlength="50"
          />
        </div>
        <div class="form-group">
          <label>排序（数字越小越靠前）</label>
          <input 
            type="number" 
            v-model="formData.sortOrder" 
            class="form-input" 
            placeholder="0"
          />
        </div>
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="closeModal">取消</button>
          <button class="btn btn-primary" @click="saveCategory" :disabled="!formData.name.trim()">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="category-management no-permission">
    <h3>您没有权限访问此页面</h3>
    <p>请联系管理员获取相应权限</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { get, post, put, del } from '../../utils/api'
import { showConfirm } from '../../stores/modal'
import { hasPermission } from '../../stores/permission'

const hasArticleManage = computed(() => hasPermission('article.class'))

interface Category {
  id: number
  name: string
  sortOrder: number
}

const categories = ref<Category[]>([])
const showAddModal = ref(false)
const showEditModal = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formData = ref({ name: '', sortOrder: 0 })

const loadCategories = async () => {
  try {
    const data = await get('/api/admin/categories')
    categories.value = data
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const editCategory = (cat: Category) => {
  editingId.value = cat.id
  formData.value = { name: cat.name, sortOrder: cat.sortOrder }
  showEditModal.value = true
}

const closeModal = () => {
  showAddModal.value = false
  showEditModal.value = false
  editingId.value = null
  formData.value = { name: '', sortOrder: 0 }
}

const saveCategory = async () => {
  if (!formData.value.name.trim()) return
  saving.value = true
  try {
    if (showEditModal.value && editingId.value) {
      await put(`/api/admin/categories/${editingId.value}`, formData.value)
    } else {
      await post('/api/admin/categories', formData.value)
    }
    await loadCategories()
    closeModal()
  } catch (error) {
    console.error('保存分类失败:', error)
  } finally {
    saving.value = false
  }
}

const deleteCategory = async (cat: Category) => {
  const confirmed = await showConfirm({
    title: '删除分类',
    message: `确定要删除分类"${cat.name}"吗？已有该分类的文章不会受影响，但将不再显示分类。`,
    type: 'warning',
    confirmText: '确定删除',
    cancelText: '取消'
  })
  if (!confirmed) return
  try {
    await del(`/api/admin/categories/${cat.id}`)
    await loadCategories()
  } catch (error) {
    console.error('删除分类失败:', error)
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.category-management {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #1f2937;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: #f9fafb;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.category-order {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
}

.category-name {
  font-size: 16px;
  color: #374151;
  font-weight: 500;
}

.category-actions {
  display: flex;
  gap: 8px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-sm {
  padding: 6px 14px;
  font-size: 13px;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #e5e7eb;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.btn-danger {
  background: #fef2f2;
  color: #ef4444;
  border: 1px solid #fca5a5;
}

.btn-danger:hover {
  background: #fee2e2;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

.no-permission {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 60px 20px;
}

.no-permission h3 {
  font-size: 24px;
  color: #374151;
  margin-bottom: 10px;
}

.no-permission p {
  font-size: 16px;
  color: #6b7280;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 28px;
  width: 400px;
  max-width: 90vw;
}

.modal-content h3 {
  margin: 0 0 20px;
  font-size: 20px;
  color: #1f2937;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  color: #374151;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #3b82f6;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}
</style>
