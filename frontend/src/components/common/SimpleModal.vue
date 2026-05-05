<script setup lang="ts">
import { ref, computed } from 'vue'

interface ModalConfig {
  show: boolean
  title?: string
  message: string
  type?: 'info' | 'warning' | 'success' | 'error'
  confirmText?: string
  cancelText?: string
  showCancel?: boolean
  closable?: boolean
  onConfirm?: () => void
  onCancel?: () => void
}

const modalConfig = ref<ModalConfig>({
  show: false,
  message: '',
  type: 'info'
})

const showModal = (config: Omit<ModalConfig, 'show'>) => {
  modalConfig.value = {
    closable: true,
    ...config,
    show: true
  }
}

const confirmModal = () => {
  modalConfig.value.show = false
  if (modalConfig.value.onConfirm) {
    modalConfig.value.onConfirm()
  }
}

const cancelModal = () => {
  if (modalConfig.value.closable === false) return
  modalConfig.value.show = false
  if (modalConfig.value.onCancel) {
    modalConfig.value.onCancel()
  }
}

const handleOverlayClick = () => {
  if (modalConfig.value.closable === false) return
  cancelModal()
}

const modalClass = computed(() => {
  const classes = ['simple-modal']
  if (modalConfig.value.type) {
    classes.push(`simple-modal--${modalConfig.value.type}`)
  }
  return classes.join(' ')
})

// 暴露给外部使用
defineExpose({
  showModal
})
</script>

<template>
  <div v-if="modalConfig.show" class="simple-modal-overlay" @click="handleOverlayClick">
    <div :class="modalClass" @click.stop>
      <div v-if="modalConfig.title" class="simple-modal__title">
        {{ modalConfig.title }}
      </div>
      <div class="simple-modal__message">
        {{ modalConfig.message }}
      </div>
      <div class="simple-modal__buttons" :class="{ 'two-buttons': modalConfig.showCancel }">
        <button v-if="modalConfig.showCancel" class="simple-modal__button simple-modal__button--cancel" @click="cancelModal">
          {{ modalConfig.cancelText || '取消' }}
        </button>
        <button class="simple-modal__button" @click="confirmModal">
          {{ modalConfig.confirmText || '确定' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.simple-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.simple-modal {
  background: white;
  border-radius: 12px;
  padding: 32px;
  max-width: 400px;
  width: 90%;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.simple-modal__title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.simple-modal__message {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 28px;
}

.simple-modal__buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.simple-modal__buttons.two-buttons {
  gap: 16px;
}

.simple-modal__button {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
  color: white;
  border: none;
  padding: 14px 40px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  flex: 1;
  box-shadow: 0 4px 12px rgba(100, 116, 139, 0.25);
}

.simple-modal__button:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(100, 116, 139, 0.4);
}

.simple-modal__button:active {
  transform: translateY(-1px);
}

.simple-modal__button--cancel {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(71, 85, 105, 0.25);
}

.simple-modal__button--cancel:hover {
  background: linear-gradient(135deg, #475569 0%, #334155 100%);
  box-shadow: 0 8px 24px rgba(71, 85, 105, 0.4);
  transform: translateY(-3px);
}

/* 不同类型的样式 - 只用于图标/边框，按钮保持统一 */
</style>
