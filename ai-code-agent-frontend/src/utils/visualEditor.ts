import { ref, type Ref } from 'vue'

export interface SelectedElementInfo {
  tagName: string
  id?: string
  className?: string
  textContent?: string
  selector: string
}

export function formatElementForPrompt(info: SelectedElementInfo): string {
  const lines: string[] = ['用户在页面上选中了以下元素，请针对该元素进行修改：']
  lines.push(`- 标签：${info.tagName}`)
  if (info.id) lines.push(`- id：${info.id}`)
  if (info.className) lines.push(`- class：${info.className}`)
  if (info.textContent) lines.push(`- 文本内容：${info.textContent}`)
  lines.push(`- CSS 选择器：${info.selector}`)
  return lines.join('\n')
}

/**
 * 可视化编辑器（postMessage 版）
 *
 * 预览 iframe 启用沙箱隔离（无 allow-same-origin）后，主站无法直接访问
 * iframe 的 contentDocument。元素悬浮/选中逻辑由后端注入预览页的
 * visual-editor-agent.js 在 iframe 内部完成，双方通过 postMessage 通信：
 *   主站 -> iframe: visual-editor:enable / visual-editor:disable / visual-editor:clear
 *   iframe -> 主站: visual-editor:selected（携带选中元素信息）
 */
export function useVisualEditor(iframeRef: Ref<HTMLIFrameElement | undefined>) {
  const editMode = ref(false)
  const selectedElement = ref<SelectedElementInfo | null>(null)

  function postToIframe(type: string) {
    // 沙箱化 iframe 为 opaque origin，targetOrigin 只能用 '*'；消息不含敏感数据
    iframeRef.value?.contentWindow?.postMessage({ type }, '*')
  }

  function enable() {
    editMode.value = true
    postToIframe('visual-editor:enable')
  }

  function disable() {
    editMode.value = false
    postToIframe('visual-editor:disable')
    selectedElement.value = null
  }

  function toggle() {
    if (editMode.value) disable()
    else enable()
  }

  function clearSelection() {
    postToIframe('visual-editor:clear')
    selectedElement.value = null
  }

  function handleIframeMessage(e: MessageEvent) {
    // 只接受当前预览 iframe 发来的选中事件
    if (e.source !== iframeRef.value?.contentWindow) return
    if (e.data?.type === 'visual-editor:selected') {
      selectedElement.value = e.data.payload as SelectedElementInfo
    }
  }
  window.addEventListener('message', handleIframeMessage)

  function dispose() {
    window.removeEventListener('message', handleIframeMessage)
  }

  return {
    editMode,
    selectedElement,
    enable,
    disable,
    toggle,
    clearSelection,
    dispose,
  }
}
