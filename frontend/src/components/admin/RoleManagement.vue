<template>
  <div class="role-management">
    <AdminTabs :tabs="tabs" v-model="activeTab" />

    <!-- 编辑模式 -->
    <div v-if="showAddModal || showEditModal" class="edit-mode">
      <div class="edit-mode__header">
        <h3>{{ showAddModal ? '添加新角色' : '编辑角色' }}</h3>
        <div class="edit-mode__actions">
          <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
          <button type="submit" form="role-form" class="btn btn-primary">{{ showAddModal ? '添加' : '保存' }}</button>
        </div>
      </div>
      <form id="role-form" @submit.prevent="handleSubmit" class="role-form">
        <div class="form-group">
          <label>角色名称</label>
          <input
            v-model="formData.roleName"
            type="text"
            placeholder="如：MODERATOR"
            :disabled="showEditModal"
            class="form-input"
            required
          />
        </div>
        <div class="form-group">
          <label>角色描述</label>
          <input
            v-model="formData.description"
            type="text"
            placeholder="角色描述"
            class="form-input"
            required
          />
        </div>
        <div class="form-group">
          <label>权限列表</label>
          <div class="permission-checkboxes">
            <div class="permission-check" v-for="perm in permissions" :key="perm.id">
              <div class="perm-checkbox">
                <input
                  type="checkbox"
                  :id="'perm-' + perm.id"
                  :value="perm.name"
                  v-model="selectedPermissions"
                />
              </div>
              <div class="perm-name">
                <label :for="'perm-' + perm.id">{{ perm.name }}</label>
              </div>
              <div class="perm-desc">
                {{ perm.description }}
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>

    <!-- 查看模式 -->
    <template v-else>
      <!-- 角色权限标签页 -->
      <div v-if="activeTab === 'roles'" class="tab-content">
        <div class="role-list">
          <div class="role-card" v-for="role in roles" :key="role.id">
            <div class="role-col role-name">
              {{ role.roleName }}
            </div>
            <div class="role-col role-desc-col">
              {{ role.description }}
            </div>
            <div class="role-col role-action">
              <button class="btn btn-secondary btn-sm" @click="editRole(role)">编辑</button>
            </div>
          </div>
        </div>

        <div class="action-bar">
          <button class="btn btn-primary" @click="showAddModal = true">
            <span>+ 添加新角色</span>
          </button>
        </div>
      </div>

      <!-- 权限查询标签页 -->
      <div v-if="activeTab === 'permissions'" class="tab-content">
        <div class="search-filter">
          <input 
            type="text" 
            v-model="permissionSearchQuery" 
            placeholder="搜索权限名称或描述" 
            class="search-input form-input"
          />
          <span class="item-count">共 {{ filteredPermissions.length }} 个</span>
        </div>

        <div class="permission-list">
          <div class="permission-cards">
            <div class="permission-card" v-for="perm in filteredPermissions" :key="perm.id">
              <div class="perm-key">{{ perm.name }}</div>
              <div class="perm-desc">{{ perm.description }}</div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import AdminTabs from './AdminTabs.vue';
import { get, post, put } from '../../utils/api';
import { showModal } from '../../stores/modal';

interface Tab {
  id: string;
  label: string;
  icon: string;
}

interface Role {
  id: number;
  roleName: string;
  permissionNames: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

interface Permission {
  id: number;
  name: string;
  resource: string;
  action: string;
  description: string;
  created_at: string;
}

const tabs: Tab[] = [
  { id: 'roles', label: '角色权限', icon: '🔐' },
  { id: 'permissions', label: '权限查询', icon: '📋' }
];

const activeTab = ref<string>('roles');
const roles = ref<Role[]>([]);
const permissions = ref<Permission[]>([]);
const loading = ref(false);
const showAddModal = ref(false);
const showEditModal = ref(false);
const editingRole = ref<Role | null>(null);

const formData = ref({
  roleName: '',
  description: '',
  permissionNames: ''
});

const selectedPermissions = ref<string[]>([]);
const permissionSearchQuery = ref('');

// 过滤后的权限
const filteredPermissions = computed(() => {
  if (!permissionSearchQuery.value.trim()) {
    return permissions.value;
  }
  const query = permissionSearchQuery.value.toLowerCase().trim();
  return permissions.value.filter(perm => 
    perm.name.toLowerCase().includes(query) || 
    perm.description.toLowerCase().includes(query)
  );
});

const loadRoles = async () => {
  loading.value = true;
  try {
    const data = await get('/api/admin/roles');
    roles.value = data;
  } catch (error) {
    console.error('加载角色失败:', error);
  } finally {
    loading.value = false;
  }
};

const loadPermissions = async () => {
  try {
    const data = await get('/api/admin/permissions');
    permissions.value = data;
  } catch (error) {
    console.error('加载权限失败:', error);
  }
};

const editRole = (role: Role) => {
  editingRole.value = role;
  formData.value = {
    roleName: role.roleName,
    description: role.description,
    permissionNames: role.permissionNames
  };
  // 解析已有的权限到复选框
  selectedPermissions.value = role.permissionNames 
    ? role.permissionNames.split(',').map(p => p.trim()).filter(p => p)
    : [];
  showEditModal.value = true;
};

const handleSubmit = async () => {
  try {
    // 把选中的权限转换为逗号分隔的字符串
    const permissionNamesStr = selectedPermissions.value.join(',');
    
    if (showAddModal.value) {
      await post('/api/admin/roles', {
        roleName: formData.value.roleName,
        description: formData.value.description,
        permissionNames: permissionNamesStr
      });
    } else {
      await put(`/api/admin/roles/${editingRole.value?.id}`, {
        roleName: formData.value.roleName,
        description: formData.value.description,
        permissionNames: permissionNamesStr
      });
    }
    await loadRoles();
    closeModal();
    
    // 显示成功弹窗
    showModal({
      type: 'success',
      title: '保存成功',
      message: showAddModal.value ? '角色添加成功！' : '角色保存成功！',
      confirmText: '确定'
    });
  } catch (error) {
    console.error('保存失败:', error);
    showModal({
      type: 'error',
      title: '保存失败',
      message: '保存失败：' + (error as any).message,
      confirmText: '确定'
    });
  }
};

const closeModal = () => {
  showAddModal.value = false;
  showEditModal.value = false;
  editingRole.value = null;
  formData.value = {
    roleName: '',
    description: '',
    permissionNames: ''
  };
  selectedPermissions.value = [];
};

onMounted(() => {
  loadRoles();
  loadPermissions();
});
</script>

<style scoped>
.role-management {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.tab-content {
  min-height: 400px;
}

.role-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.role-card {
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  transition: all 0.2s ease;
  display: grid;
  grid-template-columns: 180px 2fr 100px;
  gap: 16px;
  align-items: center;
}

.role-card:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

.role-col {
  display: flex;
  align-items: center;
}

.role-name {
  font-size: 15px;
  color: #111827;
  font-weight: 600;
  text-align: left;
}

.role-desc-col {
  font-size: 14px;
  color: #6b7280;
  text-align: left;
}

.role-action {
  justify-content: flex-end;
}

.action-bar {
  margin-top: 20px;
}

.search-filter {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  align-items: center;
}

.search-input {
  flex: 1;
  min-width: 0;
}

.item-count {
  font-size: 13px;
  color: #6b7280;
  white-space: nowrap;
}

/* 权限查询标签页样式 */
.permission-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.permission-card {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 16px;
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  transition: all 0.2s ease;
  align-items: center;
}

.permission-card:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

.perm-key {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
}

.perm-desc {
  font-size: 14px;
  color: #6b7280;
}

/* 权限复选框样式 */
.permission-checkboxes {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 320px;
  overflow-y: auto;
  padding: 4px;
}

.permission-check {
  display: grid;
  grid-template-columns: auto 200px 1fr;
  gap: 16px;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.permission-check:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.perm-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #667eea;
}

.perm-name label {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
  cursor: pointer;
}

.perm-desc {
  font-size: 14px;
  color: #6b7280;
}

/* 响应式 */
@media (max-width: 768px) {
  .role-management {
    padding: 16px;
  }

  .permission-cards {
    grid-template-columns: 1fr;
  }
}

/* 编辑模式样式 */
.edit-mode {
  padding-top: 20px;
}

.edit-mode__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.edit-mode__header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}

.edit-mode__actions {
  display: flex;
  gap: 12px;
}

/* 表单样式 */
.role-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 15px;
  font-weight: 500;
  color: #374151;
}
</style>
