<template>
  <div class="rich-content-view">
    <div class="content-view rich-content-wrapper" v-html="processedContent"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DOMPurify from 'dompurify'

interface Props {
  content: string
}

const props = withDefaults(defineProps<Props>(), {
  content: ''
})

// 解析内容，给标题添加 id 并进行 XSS 防护
const processedContent = computed(() => {
  if (!props.content) return ''
  // 先进行 XSS 消毒
  let html = DOMPurify.sanitize(props.content, {
    ALLOWED_TAGS: [
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'br', 'hr',
      'ul', 'ol', 'li', 'blockquote', 'pre', 'code',
      'strong', 'b', 'em', 'i', 'u', 's', 'a',
      'div', 'span'
    ],
    ALLOWED_ATTR: ['id', 'class', 'href', 'title', 'target', 'rel']
  })
  let idCounter = 0
  // 给所有标题添加 id
  html = html.replace(/<(h[1-6])\s*([^>]*)>/gi, (_match, tag, attrs) => {
    idCounter++
    const id = `heading-${idCounter}`
    return `<${tag} id="${id}" ${attrs}>`
  })
  return html
})

// 提取目录
const headings = computed(() => {
  if (!props.content) return []
  const result: Array<{ id: string; level: number; text: string }> = []
  let idCounter = 0
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = props.content
  const headingTags = tempDiv.querySelectorAll('h1, h2, h3, h4, h5, h6')
  headingTags.forEach((h) => {
    idCounter++
    result.push({
      id: `heading-${idCounter}`,
      level: parseInt(h.tagName.charAt(1)),
      text: h.textContent || ''
    })
  })
  return result
})

// 暴露给父组件
defineExpose({ headings })

</script>

<style scoped>
@import '../../styles/rich-content.css';

.rich-content-view {
  width: 100%;
}

.content-view {
  font-size: 18px;
  line-height: 1.8;
  color: #374151;
  word-break: break-word;
}
</style>
