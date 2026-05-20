import { ref, type Ref } from 'vue'

export interface SelectedElementInfo {
  tagName: string
  id?: string
  className?: string
  textContent?: string
  selector: string
}

const HOVER_STYLE_ID = '__visual_editor_style__'
const HOVER_CLASS = '__ve_hover__'
const SELECTED_CLASS = '__ve_selected__'

const STYLE_CONTENT = `
  .${HOVER_CLASS} {
    outline: 2px dashed #1677ff !important;
    outline-offset: -2px !important;
    cursor: pointer !important;
  }
  .${SELECTED_CLASS} {
    outline: 3px solid #0958d9 !important;
    outline-offset: -3px !important;
    background-color: rgba(22, 119, 255, 0.08) !important;
  }
`

function buildSelector(el: Element): string {
  if (!el || el === el.ownerDocument?.documentElement) return 'html'
  const parts: string[] = []
  let cur: Element | null = el
  let depth = 0
  while (cur && cur.nodeType === 1 && depth < 5) {
    let part = cur.tagName.toLowerCase()
    if (cur.id) {
      part += `#${cur.id}`
      parts.unshift(part)
      break
    }
    const cls = (cur.getAttribute('class') || '').trim().split(/\s+/).filter(Boolean)
      .filter((c) => c !== HOVER_CLASS && c !== SELECTED_CLASS)
    if (cls.length) part += '.' + cls.slice(0, 2).join('.')
    const parent = cur.parentElement
    if (parent) {
      const sameTag = Array.from(parent.children).filter((c) => c.tagName === cur!.tagName)
      if (sameTag.length > 1) {
        const idx = sameTag.indexOf(cur) + 1
        part += `:nth-of-type(${idx})`
      }
    }
    parts.unshift(part)
    cur = parent
    depth++
  }
  return parts.join(' > ')
}

function extractInfo(el: Element): SelectedElementInfo {
  const className = (el.getAttribute('class') || '')
    .split(/\s+/)
    .filter((c) => c && c !== HOVER_CLASS && c !== SELECTED_CLASS)
    .join(' ')
  const text = (el.textContent || '').trim().slice(0, 100)
  return {
    tagName: el.tagName.toLowerCase(),
    id: el.id || undefined,
    className: className || undefined,
    textContent: text || undefined,
    selector: buildSelector(el),
  }
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

export function useVisualEditor(iframeRef: Ref<HTMLIFrameElement | undefined>) {
  const editMode = ref(false)
  const selectedElement = ref<SelectedElementInfo | null>(null)

  let currentHover: Element | null = null
  let currentSelected: Element | null = null
  let attachedDoc: Document | null = null

  function onMouseOver(e: Event) {
    const target = e.target as Element
    if (!target || target === currentSelected) return
    if (currentHover && currentHover !== target) {
      currentHover.classList.remove(HOVER_CLASS)
    }
    currentHover = target
    if (target !== currentSelected) {
      target.classList.add(HOVER_CLASS)
    }
  }

  function onMouseOut(e: Event) {
    const target = e.target as Element
    if (target && target !== currentSelected) {
      target.classList.remove(HOVER_CLASS)
    }
  }

  function onClick(e: Event) {
    e.preventDefault()
    e.stopPropagation()
    const target = e.target as Element
    if (!target) return
    if (currentSelected && currentSelected !== target) {
      currentSelected.classList.remove(SELECTED_CLASS)
    }
    target.classList.remove(HOVER_CLASS)
    target.classList.add(SELECTED_CLASS)
    currentSelected = target

    const info = extractInfo(target)
    selectedElement.value = info
    window.postMessage({ type: 'visual-editor:selected', payload: info }, '*')
  }

  function injectStyle(doc: Document) {
    if (doc.getElementById(HOVER_STYLE_ID)) return
    const style = doc.createElement('style')
    style.id = HOVER_STYLE_ID
    style.textContent = STYLE_CONTENT
    doc.head.appendChild(style)
  }

  function attach() {
    const iframe = iframeRef.value
    if (!iframe) return
    let doc: Document | null = null
    try {
      doc = iframe.contentDocument
    } catch {
      doc = null
    }
    if (!doc || !doc.body) {
      // iframe not ready yet — retry on load
      iframe.addEventListener('load', attach, { once: true })
      return
    }
    detach()
    attachedDoc = doc
    injectStyle(doc)
    doc.addEventListener('mouseover', onMouseOver, true)
    doc.addEventListener('mouseout', onMouseOut, true)
    doc.addEventListener('click', onClick, true)
  }

  function detach() {
    if (!attachedDoc) return
    try {
      attachedDoc.removeEventListener('mouseover', onMouseOver, true)
      attachedDoc.removeEventListener('mouseout', onMouseOut, true)
      attachedDoc.removeEventListener('click', onClick, true)
      if (currentHover) currentHover.classList.remove(HOVER_CLASS)
      if (currentSelected) currentSelected.classList.remove(SELECTED_CLASS)
      const style = attachedDoc.getElementById(HOVER_STYLE_ID)
      if (style) style.remove()
    } catch {
      // ignore cross-origin or unloaded
    }
    currentHover = null
    currentSelected = null
    attachedDoc = null
  }

  function enable() {
    editMode.value = true
    attach()
  }

  function disable() {
    editMode.value = false
    detach()
    selectedElement.value = null
  }

  function toggle() {
    if (editMode.value) disable()
    else enable()
  }

  function clearSelection() {
    if (attachedDoc && currentSelected) {
      try {
        currentSelected.classList.remove(SELECTED_CLASS)
      } catch {
        // ignore
      }
    }
    currentSelected = null
    selectedElement.value = null
  }

  function handleParentMessage(e: MessageEvent) {
    if (e.data?.type === 'visual-editor:selected') {
      selectedElement.value = e.data.payload as SelectedElementInfo
    }
  }
  window.addEventListener('message', handleParentMessage)

  function dispose() {
    window.removeEventListener('message', handleParentMessage)
    detach()
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
