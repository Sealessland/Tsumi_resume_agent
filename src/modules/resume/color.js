function normalizeHexColor(hex = '') {
  const value = String(hex).trim()
  if (/^#[\da-fA-F]{6}$/.test(value)) return value
  if (/^#[\da-fA-F]{3}$/.test(value)) {
    const [r, g, b] = value.slice(1)
    return `#${r}${r}${g}${g}${b}${b}`
  }
  return ''
}

function parseRgbColor(color = '') {
  const value = String(color).trim()
  const match = value.match(
    /^rgba?\(\s*([+-]?\d*\.?\d+)\s*[, ]\s*([+-]?\d*\.?\d+)\s*[, ]\s*([+-]?\d*\.?\d+)(?:\s*[,/]\s*([+-]?\d*\.?\d+%?))?\s*\)$/i
  )
  if (!match) return null

  const clamp = (n, min, max) => Math.min(max, Math.max(min, n))
  return {
    r: clamp(Math.round(Number(match[1])), 0, 255),
    g: clamp(Math.round(Number(match[2])), 0, 255),
    b: clamp(Math.round(Number(match[3])), 0, 255),
  }
}

function hexToRgb(hex = '') {
  const normalized = normalizeHexColor(hex)
  if (!normalized) return null
  return {
    r: parseInt(normalized.slice(1, 3), 16),
    g: parseInt(normalized.slice(3, 5), 16),
    b: parseInt(normalized.slice(5, 7), 16),
  }
}

function parseColorToRgb(color = '') {
  return hexToRgb(color) || parseRgbColor(color)
}

function rgbToHsl({ r, g, b }) {
  const nr = r / 255
  const ng = g / 255
  const nb = b / 255
  const max = Math.max(nr, ng, nb)
  const min = Math.min(nr, ng, nb)
  const delta = max - min

  let h = 0
  if (delta) {
    if (max === nr) h = ((ng - nb) / delta) % 6
    else if (max === ng) h = (nb - nr) / delta + 2
    else h = (nr - ng) / delta + 4
  }
  h = Math.round(h * 60)
  if (h < 0) h += 360

  const l = (max + min) / 2
  const s = delta === 0 ? 0 : delta / (1 - Math.abs(2 * l - 1))

  return {
    h,
    s: s * 100,
    l: l * 100,
  }
}

function hslToRgb({ h, s, l }) {
  const ns = s / 100
  const nl = l / 100
  const c = (1 - Math.abs(2 * nl - 1)) * ns
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
  const m = nl - c / 2

  let r = 0
  let g = 0
  let b = 0

  if (h < 60) {
    r = c
    g = x
  } else if (h < 120) {
    r = x
    g = c
  } else if (h < 180) {
    g = c
    b = x
  } else if (h < 240) {
    g = x
    b = c
  } else if (h < 300) {
    r = x
    b = c
  } else {
    r = c
    b = x
  }

  return {
    r: Math.round((r + m) * 255),
    g: Math.round((g + m) * 255),
    b: Math.round((b + m) * 255),
  }
}

function rgbToHex({ r, g, b }) {
  const toHex = (value) => value.toString(16).padStart(2, '0')
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`
}

function brandToneFrom(primaryColor, { saturationBoost = 0, minSaturation = 65, maxSaturation = 90, minLightness = 22, maxLightness = 32, lightnessShift = -35 } = {}) {
  const rgb = parseColorToRgb(primaryColor)
  if (!rgb) return '#1D3A8A'

  const hsl = rgbToHsl(rgb)
  const nextHsl = {
    h: hsl.h,
    s: Math.min(maxSaturation, Math.max(minSaturation, hsl.s + saturationBoost)),
    l: Math.max(minLightness, Math.min(maxLightness, hsl.l + lightnessShift)),
  }
  return rgbToHex(hslToRgb(nextHsl))
}

export function deepBrandFrom(primaryColor) {
  return brandToneFrom(primaryColor, {
    saturationBoost: 15,
    minSaturation: 65,
    maxSaturation: 90,
    minLightness: 22,
    maxLightness: 32,
    lightnessShift: -35,
  })
}

export function nameBrandFrom(primaryColor) {
  return brandToneFrom(primaryColor, {
    saturationBoost: 18,
    minSaturation: 68,
    maxSaturation: 92,
    minLightness: 18,
    maxLightness: 28,
    lightnessShift: -42,
  })
}

export function schoolBrandFrom(primaryColor) {
  return brandToneFrom(primaryColor, {
    saturationBoost: 12,
    minSaturation: 60,
    maxSaturation: 86,
    minLightness: 36,
    maxLightness: 48,
    lightnessShift: -12,
  })
}
