<template>
  <div class="rich-text-editor">
    <div v-if="editor" class="editor-toolbar">
      <button
        type="button"
        @click="editor.chain().focus().toggleBold().run()"
        :class="['toolbar-btn', { active: editor.isActive('bold') }]"
        title="加粗"
      >
        <strong>B</strong>
      </button>
      <button
        type="button"
        @click="editor.chain().focus().toggleItalic().run()"
        :class="['toolbar-btn', { active: editor.isActive('italic') }]"
        title="斜体"
      >
        <em>I</em>
      </button>
      <button
        type="button"
        @click="editor.chain().focus().toggleUnderline().run()"
        :class="['toolbar-btn', { active: editor.isActive('underline') }]"
        title="下划线"
      >
        <u>U</u>
      </button>
      <button
        type="button"
        @click="editor.chain().focus().toggleStrike().run()"
        :class="['toolbar-btn', { active: editor.isActive('strike') }]"
        title="删除线"
      >
        <s>S</s>
      </button>
      <span class="toolbar-divider">|</span>
      <button
        type="button"
        @click="editor.chain().focus().toggleBulletList().run()"
        :class="['toolbar-btn', { active: editor.isActive('bulletList') }]"
        title="无序列表"
      >
        • 列表
      </button>
      <button
        type="button"
        @click="editor.chain().focus().toggleOrderedList().run()"
        :class="['toolbar-btn', { active: editor.isActive('orderedList') }]"
        title="有序列表"
      >
        1. 列表
      </button>
      <button
        type="button"
        @click="editor.chain().focus().toggleBlockquote().run()"
        :class="['toolbar-btn', { active: editor.isActive('blockquote') }]"
        title="引用"
      >
        ❝ 引用
      </button>
      <button
        type="button"
        @click="editor.chain().focus().toggleCodeBlock().run()"
        :class="['toolbar-btn', { active: editor.isActive('codeBlock') }]"
        title="代码块"
      >
        &lt;/&gt; 代码
      </button>
      <span class="toolbar-divider">|</span>
      <button type="button" @click="editor.chain().focus().setTextAlign('left').run()" :class="['toolbar-btn', { active: editor.isActive({ textAlign: 'left' }) }]" title="左对齐">⬅</button>
      <button type="button" @click="editor.chain().focus().setTextAlign('center').run()" :class="['toolbar-btn', { active: editor.isActive({ textAlign: 'center' }) }]" title="居中">⬌</button>
      <button type="button" @click="editor.chain().focus().setTextAlign('right').run()" :class="['toolbar-btn', { active: editor.isActive({ textAlign: 'right' }) }]" title="右对齐">➡</button>
      <span class="toolbar-divider">|</span>
      <button type="button" @click="editor.chain().focus().toggleHeading({ level: 1 }).run()" :class="['toolbar-btn', { active: editor.isActive('heading', { level: 1 }) }]" title="H1">H1</button>
      <button type="button" @click="editor.chain().focus().toggleHeading({ level: 2 }).run()" :class="['toolbar-btn', { active: editor.isActive('heading', { level: 2 }) }]" title="H2">H2</button>
      <button type="button" @click="editor.chain().focus().toggleHeading({ level: 3 }).run()" :class="['toolbar-btn', { active: editor.isActive('heading', { level: 3 }) }]" title="H3">H3</button>
      <button type="button" @click="editor.chain().focus().toggleHeading({ level: 4 }).run()" :class="['toolbar-btn', { active: editor.isActive('heading', { level: 4 }) }]" title="H4">H4</button>
      <button type="button" @click="editor.chain().focus().toggleHeading({ level: 5 }).run()" :class="['toolbar-btn', { active: editor.isActive('heading', { level: 5 }) }]" title="H5">H5</button>
      <button
        type="button"
        @click="editor.chain().focus().setParagraph().run()"
        :class="['toolbar-btn', { active: editor.isActive('paragraph') }]"
        title="正文"
      >
        正文
      </button>
    </div>
    <div class="editor-area">
      <EditorContent :editor="editor" class="content-editable rich-content-wrapper" />
      <div v-if="showSlashMenu" class="slash-menu" :style="slashMenuStyle">
        <div class="slash-menu-header">快速格式</div>
        <div
          v-for="(cmd, index) in filteredCommands"
          :key="index"
          class="slash-menu-item"
          :class="{ active: index === activeSlashIndex }"
          @mousedown.prevent="selectSlashCommand(index)"
        >
          <span class="menu-icon">{{ cmd.icon }}</span>
          <span class="menu-text">{{ cmd.name }}</span>
        </div>
        <div v-if="filteredCommands.length === 0" class="slash-menu-empty">无匹配格式</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Underline from '@tiptap/extension-underline'
import TextAlign from '@tiptap/extension-text-align'
import Placeholder from '@tiptap/extension-placeholder'

interface Props {
  modelValue: string
  placeholder?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请输入内容...'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const showSlashMenu = ref(false)
const activeSlashIndex = ref(0)
const slashMenuStyle = ref({ top: '0px', left: '0px' })
const slashQuery = ref('')
const slashPos = ref(-1)

interface SlashCommand {
  name: string
  icon: string
  action: () => void
}

const slashCommands: SlashCommand[] = [
  { name: '一级标题', icon: 'H1', action: () => editor.commands.toggleHeading({ level: 1 }) },
  { name: '二级标题', icon: 'H2', action: () => editor.commands.toggleHeading({ level: 2 }) },
  { name: '三级标题', icon: 'H3', action: () => editor.commands.toggleHeading({ level: 3 }) },
  { name: '四级标题', icon: 'H4', action: () => editor.commands.toggleHeading({ level: 4 }) },
  { name: '五级标题', icon: 'H5', action: () => editor.commands.toggleHeading({ level: 5 }) },
  { name: '正文段落', icon: '📝', action: () => editor.commands.setParagraph() },
  { name: '无序列表', icon: '•', action: () => editor.commands.toggleBulletList() },
  { name: '有序列表', icon: '1.', action: () => editor.commands.toggleOrderedList() },
  { name: '引用块', icon: '❝', action: () => editor.commands.toggleBlockquote() },
  { name: '代码块', icon: '&lt;/&gt;', action: () => editor.commands.toggleCodeBlock() },
  { name: '左对齐', icon: '⬅', action: () => editor.commands.setTextAlign('left') },
  { name: '居中', icon: '⬌', action: () => editor.commands.setTextAlign('center') },
  { name: '右对齐', icon: '➡', action: () => editor.commands.setTextAlign('right') }
]

const filteredCommands = computed(() => {
  if (!slashQuery.value) return slashCommands
  const q = slashQuery.value.toLowerCase()
  return slashCommands.filter(c => c.name.toLowerCase().includes(q))
})

const detectSlash = () => {
  const { state } = editor
  const { from, empty } = state.selection
  if (!empty) {
    showSlashMenu.value = false
    slashQuery.value = ''
    return
  }

  const $from = state.doc.resolve(from)
  const blockStart = $from.start()
  const textBefore = state.doc.textBetween(blockStart, from)

  const slashIdx = textBefore.lastIndexOf('/')
  if (slashIdx === -1 || textBefore.substring(0, slashIdx).trim() !== '') {
    showSlashMenu.value = false
    slashQuery.value = ''
    return
  }

  slashPos.value = blockStart + slashIdx
  slashQuery.value = textBefore.substring(slashIdx + 1)
  activeSlashIndex.value = 0

  try {
    const coords = editor.view.coordsAtPos(from)
    const editorRect = editor.view.dom.getBoundingClientRect()
    const menuHeight = Math.min(filteredCommands.value.length * 48 + 50, 360)

    let top = coords.top - editorRect.top - menuHeight - 8
    if (top < 0) {
      top = coords.bottom - editorRect.top + 8
    }

    slashMenuStyle.value = {
      top: `${top}px`,
      left: `${coords.left - editorRect.left}px`
    }
  } catch {
    slashMenuStyle.value = { top: '50px', left: '20px' }
  }

  showSlashMenu.value = true
}

const selectSlashCommand = (index: number) => {
  const cmd = filteredCommands.value[index]
  if (!cmd) return
  showSlashMenu.value = false
  slashQuery.value = ''

  const { from } = editor.state.selection
  editor
    .chain()
    .focus()
    .deleteRange({ from: slashPos.value, to: from })
    .run()

  cmd.action()
}

const handleKeydown = (event: KeyboardEvent) => {
  if (!showSlashMenu.value) return

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeSlashIndex.value = (activeSlashIndex.value + 1) % filteredCommands.value.length
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeSlashIndex.value = (activeSlashIndex.value - 1 + filteredCommands.value.length) % filteredCommands.value.length
  } else if (event.key === 'Enter') {
    event.preventDefault()
    selectSlashCommand(activeSlashIndex.value)
  } else if (event.key === 'Escape') {
    event.preventDefault()
    showSlashMenu.value = false
    slashQuery.value = ''
  }
}

const editor = new Editor({
  extensions: [
    StarterKit.configure({
      heading: {
        levels: [1, 2, 3, 4, 5]
      }
    }),
    Underline,
    TextAlign.configure({
      types: ['heading', 'paragraph']
    }),
    Placeholder.configure({
      placeholder: props.placeholder
    })
  ],
  content: props.modelValue || '',
  onUpdate: ({ editor: ed }) => {
    emit('update:modelValue', ed.isEmpty ? '' : ed.getHTML())
    detectSlash()
  },
  onSelectionUpdate: () => {
    detectSlash()
  },
  editorProps: {
    handleKeyDown: (_view: any, event: KeyboardEvent) => {
      handleKeydown(event)
      return false
    }
  }
})

watch(() => props.modelValue, (newValue) => {
  if (editor && editor.getHTML() !== newValue) {
    editor.commands.setContent(newValue || '')
  }
})

onBeforeUnmount(() => {
  editor.destroy()
})

defineExpose({
  focus: () => editor.commands.focus(),
  setContent: (content: string) => editor.commands.setContent(content || '')
})
</script>

<style scoped>
@import '../../styles/rich-content.css';

.rich-text-editor {
  display: flex;
  flex-direction: column;
  position: relative;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid var(--border);
  border-radius: 8px 8px 0 0;
  border-bottom: none;
}

.toolbar-btn {
  padding: 6px 12px;
  background: white;
  border: 1px solid var(--border);
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  font-family: inherit;
}

.toolbar-btn:hover {
  background: #e2e8f0;
  border-color: #94a3b8;
}

.toolbar-btn.active {
  background: var(--primary-bg, #eff6ff);
  border-color: var(--primary, #3b82f6);
  color: var(--primary, #1e40af);
}

.toolbar-divider {
  width: 1px;
  background: var(--border);
  margin: 0 4px;
}

.editor-area {
  position: relative;
}

.content-editable {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 0 0 8px 8px;
  overflow: hidden;
  min-height: 0;
}

.content-editable:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 4px rgba(30, 64, 175, 0.1);
}

.slash-menu {
  position: absolute;
  background: white;
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  min-width: 200px;
  max-width: 260px;
  max-height: 340px;
  overflow-x: hidden;
  overflow-y: auto;
}

.slash-menu-header {
  padding: 10px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
  border-bottom: 1px solid var(--border);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.slash-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.1s ease;
}

.slash-menu-item:hover,
.slash-menu-item.active {
  background: var(--primary-bg, #eff6ff);
}

.menu-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary, #3b82f6);
  flex-shrink: 0;
}

.slash-menu-item.active .menu-icon {
  background: white;
  border: 1px solid var(--primary, #3b82f6);
}

.menu-text {
  font-size: 14px;
  color: #374151;
}

.slash-menu-item.active .menu-text {
  color: var(--primary, #1e40af);
}

.slash-menu-empty {
  padding: 14px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}
</style>

<style>
.rich-text-editor .ProseMirror {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  padding: 16px;
  font-size: 17px;
  line-height: 1.8;
  outline: none;
  background: white;
  border-radius: 0 0 8px 8px;
}

.rich-text-editor .ProseMirror p.is-editor-empty:first-child::before {
  content: attr(data-placeholder);
  color: #94a3b8;
  pointer-events: none;
  float: left;
  height: 0;
}

.rich-text-editor .ProseMirror h1 { font-size: 36px; font-weight: 700; margin: 2.5em 0 0.5em; color: #111827; line-height: 1.2; letter-spacing: -0.02em; }
.rich-text-editor .ProseMirror h2 { font-size: 28px; font-weight: 600; margin: 2em 0 0.5em; color: #111827; line-height: 1.3; letter-spacing: -0.01em; padding-top: 0.25em; }
.rich-text-editor .ProseMirror h3 { font-size: 24px; font-weight: 600; margin: 1.75em 0 0.5em; color: #111827; line-height: 1.35; }
.rich-text-editor .ProseMirror h4 { font-size: 20px; font-weight: 600; margin: 1.5em 0 0.5em; color: #111827; line-height: 1.4; }
.rich-text-editor .ProseMirror h5 { font-size: 18px; font-weight: 600; margin: 1.25em 0 0.5em; color: #374151; line-height: 1.45; }
.rich-text-editor .ProseMirror p { margin: 1em 0; font-size: 18px; line-height: 1.8; color: #374151; }
.rich-text-editor .ProseMirror ul,
.rich-text-editor .ProseMirror ol { padding-left: 1.5em; margin: 1em 0; }
.rich-text-editor .ProseMirror li { margin: 0.5em 0; font-size: 18px; line-height: 1.8; color: #374151; }
.rich-text-editor .ProseMirror strong,
.rich-text-editor .ProseMirror b { font-weight: 600; color: #111827; }
.rich-text-editor .ProseMirror em,
.rich-text-editor .ProseMirror i { font-style: italic; color: #374151; }
.rich-text-editor .ProseMirror u { text-decoration: underline; text-decoration-color: #2563eb; text-underline-offset: 4px; text-decoration-thickness: 2px; }
.rich-text-editor .ProseMirror blockquote { border-left: 4px solid #3b82f6; padding-left: 16px; margin: 1.5em 0; color: #64748b; font-style: italic; }
.rich-text-editor .ProseMirror pre { background: #1e293b; color: #e2e8f0; padding: 16px 20px; border-radius: 8px; overflow-x: auto; font-size: 15px; line-height: 1.6; margin: 1.5em 0; }
.rich-text-editor .ProseMirror code { background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; color: #e11d48; }
.rich-text-editor .ProseMirror pre code { background: transparent; padding: 0; color: inherit; font-size: inherit; }
</style>
