import { ref } from 'vue'

interface ModalConfig {
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

const modalRef = ref<any>(null)
const isInitialized = ref(false)

export const initModal = (ref: any) => {
  modalRef.value = ref
  isInitialized.value = true
}

export const showModal = (config: ModalConfig) => {
  if (isInitialized.value && modalRef.value) {
    modalRef.value.showModal(config)
  } else {
    // 降级到原生alert
    alert(config.message)
  }
}

export const showConfirm = (config: Omit<ModalConfig, 'showCancel'>): Promise<boolean> => {
  return new Promise((resolve) => {
    if (isInitialized.value && modalRef.value) {
      modalRef.value.showModal({
        ...config,
        showCancel: true,
        onConfirm: () => resolve(true),
        onCancel: () => resolve(false)
      })
    } else {
      // 降级到原生confirm
      resolve(confirm(config.message))
    }
  })
}
