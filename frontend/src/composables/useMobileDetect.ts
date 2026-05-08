import { ref } from 'vue'

const MOBILE_PROMPT_KEY = 'mobile_prompt_dismissed'

export function useMobileDetect() {
  const isMobile = ref(false)

  const checkMobile = () => {
    const ua = navigator.userAgent || ''

    const mobileRegex = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile|CriOS/i
    if (mobileRegex.test(ua)) {
      isMobile.value = true
      return
    }

    if (/Tablet|iPad/i.test(ua) || (navigator.maxTouchPoints > 1 && window.innerWidth < 1024)) {
      isMobile.value = true
      return
    }

    isMobile.value = false
  }

  const isPromptDismissed = () => {
    return sessionStorage.getItem(MOBILE_PROMPT_KEY) === '1'
  }

  const dismissPrompt = () => {
    sessionStorage.setItem(MOBILE_PROMPT_KEY, '1')
  }

  checkMobile()

  return {
    isMobile,
    isPromptDismissed,
    dismissPrompt
  }
}
