function escapeHtml(text = '') {
  return String(text)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

const CODE_PLACEHOLDER = '\u0000'

export function richText(text = '') {
  const source = String(text || '')
  if (!source) return ''

  let escaped = escapeHtml(source)

  // Protect inline code spans first so their content is not processed by
  // other formatting rules (e.g. asterisks inside a code snippet).
  const codeSpans = []
  escaped = escaped.replace(/`([^`]+)`/g, (match, code) => {
    codeSpans.push(`<code>${code}</code>`)
    return `${CODE_PLACEHOLDER}${codeSpans.length - 1}${CODE_PLACEHOLDER}`
  })

  const formatted = escaped
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/~~(.+?)~~/g, '<del>$1</del>')
    .replace(/\*([^*]+?)\*/g, '<em>$1</em>')
    .replace(/_([^_]+?)_/g, '<em>$1</em>')
    // Markdown links: allow optional whitespace around the URL and single-level
    // balanced parentheses inside the URL path.
    .replace(/\[([^\]]+?)\]\s*\(\s*(https?:\/\/(?:[^()\s]|\([^()\s]*\))+)\s*\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>')
    .replace(/\n/g, '<br />')

  return formatted.replace(/\0(\d+)\0/g, (_, index) => codeSpans[Number(index)])
}
