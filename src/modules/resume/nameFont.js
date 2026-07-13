export const DEFAULT_NAME_FONT = 'sans'
export const DEFAULT_SCHOOL_FONT = 'sans'

export const NAME_FONT_OPTIONS = [
  { value: 'serif', label: '衬线正式' },
  { value: 'songti', label: '宋体书卷' },
  { value: 'kaiti', label: '楷体文雅' },
  { value: 'sans', label: '无衬线利落' },
]

const NAME_FONT_FAMILY_MAP = {
  serif: '"Noto Serif SC", "Source Han Serif SC", "STSong", "Songti SC", serif',
  songti: '"Songti SC", "STSong", "SimSun", serif',
  kaiti: '"Kaiti SC", "STKaiti", "KaiTi", serif',
  sans: '"Noto Sans SC", "Source Han Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif',
}

export function normalizeNameFont(value) {
  return NAME_FONT_FAMILY_MAP[value] ? value : DEFAULT_NAME_FONT
}

export function getNameFontFamily(value) {
  return NAME_FONT_FAMILY_MAP[normalizeNameFont(value)]
}

export function normalizeSchoolFont(value) {
  return NAME_FONT_FAMILY_MAP[value] ? value : DEFAULT_SCHOOL_FONT
}

export function getSchoolFontFamily(value) {
  return NAME_FONT_FAMILY_MAP[normalizeSchoolFont(value)]
}
