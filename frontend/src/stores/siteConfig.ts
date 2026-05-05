import { ref, computed } from 'vue'
import { get, API_BASE_URL } from '../utils/api'

interface SiteConfig {
  siteTitle: string
  siteIcon: string
  siteFavicon: string
}

const siteTitle = ref('')
const siteIcon = ref('')
const siteFavicon = ref('')
const isConfigLoaded = ref(false)

// 计算完整的图片 URL
const fullSiteIconUrl = computed(() => {
  if (!siteIcon.value) return ''
  if (siteIcon.value.startsWith('http://') || siteIcon.value.startsWith('https://')) {
    return siteIcon.value
  }
  return `${API_BASE_URL}${siteIcon.value}`
})

const fullSiteFaviconUrl = computed(() => {
  if (!siteFavicon.value) return ''
  if (siteFavicon.value.startsWith('http://') || siteFavicon.value.startsWith('https://')) {
    return siteFavicon.value
  }
  return `${API_BASE_URL}${siteFavicon.value}`
})

const loadSiteConfig = async () => {
  try {
    const data = await get('/api/settings/public/site-config', undefined, { skipAuth: true })
    siteTitle.value = data.siteTitle || ''
    siteIcon.value = data.siteIcon || ''
    siteFavicon.value = data.siteFavicon || ''
    
    // 设置页面标题
    if (data.siteTitle) {
      document.title = data.siteTitle
    }
    
    // 设置 favicon
    if (data.siteFavicon) {
      setBrowserFavicon(data.siteFavicon)
    }
  } catch (error) {
    console.error('加载网站配置失败:', error)
  } finally {
    isConfigLoaded.value = true
  }
}

const setBrowserFavicon = (url: string) => {
  // 移除所有已存在的 favicon links
  const existingLinks = document.querySelectorAll("link[rel~='icon']")
  existingLinks.forEach(link => link.remove())
  
  if (url) {
    // 创建新的 link 元素
    const link = document.createElement('link')
    link.rel = 'icon'
    // 构建完整的 URL
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      link.href = `${API_BASE_URL}${url}`
    } else {
      link.href = url
    }
    document.getElementsByTagName('head')[0].appendChild(link)
  }
}

const updateSiteConfig = (config: Partial<SiteConfig>) => {
  if (config.siteTitle !== undefined) {
    siteTitle.value = config.siteTitle
    if (config.siteTitle) {
      document.title = config.siteTitle
    }
  }
  if (config.siteIcon !== undefined) {
    siteIcon.value = config.siteIcon
  }
  if (config.siteFavicon !== undefined) {
    siteFavicon.value = config.siteFavicon
    setBrowserFavicon(config.siteFavicon)
  }
}

export {
  siteTitle,
  siteIcon,
  siteFavicon,
  isConfigLoaded,
  fullSiteIconUrl,
  fullSiteFaviconUrl,
  loadSiteConfig,
  updateSiteConfig,
  setBrowserFavicon
}
