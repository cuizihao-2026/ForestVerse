import { getToken, logout } from '../stores/auth'
import { getIsAccountKicked, getIsLoggingOut, setLoggingOut } from '../stores/websocket'
import { showModal } from '../stores/modal'

// 后端基础地址配置
// 开发环境用 Vite 代理，生产环境用相对路径（Nginx代理）
const API_BASE_URL = import.meta.env.DEV ? '' : ''

interface RequestOptions extends RequestInit {
  skipAuth?: boolean
}

// 构建完整的请求 URL
const buildUrl = (path: string): string => {
  // 如果已经是完整 URL，直接返回
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }
  // 否则拼接基础 URL
  return `${API_BASE_URL}${path}`
}

export const apiRequest = async (url: string, options: RequestOptions = {}) => {
  const { skipAuth = false, ...fetchOptions } = options;

  const headers: Record<string, string> = {};
  
  // 复制现有的 headers
  if (fetchOptions.headers) {
    if (fetchOptions.headers instanceof Headers) {
      fetchOptions.headers.forEach((value, key) => {
        headers[key] = value;
      });
    } else if (Array.isArray(fetchOptions.headers)) {
      fetchOptions.headers.forEach(([key, value]) => {
        headers[key] = value;
      });
    } else {
      Object.assign(headers, fetchOptions.headers);
    }
  }

  // 只有当 body 不是 FormData 时才设置 Content-Type
  if (!(fetchOptions.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  if (!skipAuth) {
    const token = getToken();
    if (token) {
      (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
    }
  }

  const response = await fetch(buildUrl(url), {
    ...fetchOptions,
    headers
  });

  const text = await response.text();

  if (!response.ok) {
    let errorData: any = {};
    try {
      errorData = JSON.parse(text);
    } catch {}
    
    // 如果是401且消息是"账号已在别处登录"，且 WS 还没有通知过，才弹窗
    if (response.status === 401 && errorData.message?.includes('账号已在别处登录') && !getIsAccountKicked() && !getIsLoggingOut()) {
      setLoggingOut(true)
      showModal({
        title: '提示',
        message: errorData.message,
        type: 'warning',
        closable: false,
        onConfirm: logout
      });
      return Promise.reject(new Error(errorData.message));
    }
    
    // 提取合适的错误消息
    let errorMessage;
    if (typeof errorData === 'string') {
      errorMessage = errorData;
    } else if (errorData.message) {
      errorMessage = errorData.message;
    } else if (text && text.trim()) {
      errorMessage = text;
    } else {
      errorMessage = `请求失败: ${response.status}`;
    }
    
    throw new Error(errorMessage);
  }

  // 尝试解析为 JSON，如果失败则返回文本
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
};

export const get = (url: string, params?: any, options?: RequestOptions) => {
  let fullUrl = url;
  if (params) {
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        searchParams.append(key, String(value));
      }
    });
    const queryString = searchParams.toString();
    if (queryString) {
      fullUrl += (url.includes('?') ? '&' : '?') + queryString;
    }
  }
  return apiRequest(fullUrl, { ...options, method: 'GET' });
};

export const post = (url: string, data?: any, options?: RequestOptions) => {
  let body;
  if (data instanceof FormData) {
    body = data;
  } else if (data !== undefined) {
    body = JSON.stringify(data);
  }
  return apiRequest(url, {
    ...options,
    method: 'POST',
    body
  });
};

export const put = (url: string, data?: any, options?: RequestOptions) => {
  let body;
  if (data instanceof FormData) {
    body = data;
  } else if (data !== undefined) {
    body = JSON.stringify(data);
  }
  return apiRequest(url, {
    ...options,
    method: 'PUT',
    body
  });
};

export const del = (url: string, options?: RequestOptions) => {
  return apiRequest(url, { ...options, method: 'DELETE' });
};

// 文件上传工具函数
export const uploadFile = async (file: File, directory: string = '') => {
  const formData = new FormData();
  formData.append('file', file);
  if (directory) {
    formData.append('directory', directory);
  }

  const token = getToken();
  const response = await fetch(buildUrl('/api/file/upload'), {
    method: 'POST',
    headers: token ? { 'Authorization': `Bearer ${token}` } : {},
    body: formData
  });

  if (!response.ok) {
    throw new Error('文件上传失败');
  }

  return await response.text();
};

// 导出 API 基础 URL 供其他地方使用
export { API_BASE_URL }