import { ref } from 'vue'
import { get } from '../utils/api'

export const userPermissions = ref<Set<string>>(new Set())
export const currentRoleDescription = ref<string>('普通用户')

export const loadUserPermissions = async () => {
  try {
    const permissions = await get('/api/user/permissions')
    userPermissions.value = new Set(permissions)
    localStorage.setItem('userPermissions', JSON.stringify(Array.from(permissions)))
    return permissions
  } catch (error) {
    console.error('获取用户权限失败:', error)
  }
}

export const loadUserRoleDescription = async () => {
  try {
    const roleInfo = await get('/api/user/role-info')
    if (roleInfo && roleInfo.description) {
      currentRoleDescription.value = roleInfo.description
      localStorage.setItem('userRoleDescription', roleInfo.description)
    }
  } catch (error) {
    console.error('获取用户角色描述失败:', error)
  }
}

export const hasPermission = (permission: string) => {
  return userPermissions.value.has(permission)
}

export const isAdmin = () => {
  return hasPermission('admin.use')
}
