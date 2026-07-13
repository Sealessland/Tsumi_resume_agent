export const PHOTO_SIZE_MIN = 56
export const PHOTO_SIZE_MAX = 180
export const PHOTO_RATIO_OPTIONS = ['1:1', '3:4', '2:3', '4:3']
export const DEFAULT_PHOTO_RATIO = '1:1'

function isValidRatio(value) {
  return PHOTO_RATIO_OPTIONS.includes(value)
}

function getRatioScale(ratio = DEFAULT_PHOTO_RATIO) {
  if (!isValidRatio(ratio)) {
    return { w: 1, h: 1 }
  }

  const [w, h] = ratio.split(':').map((item) => Number(item))
  if (!Number.isFinite(w) || !Number.isFinite(h) || w <= 0 || h <= 0) {
    return { w: 1, h: 1 }
  }
  return { w, h }
}

export function clampPhotoSize(value, fallback = 96) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return fallback
  return Math.min(PHOTO_SIZE_MAX, Math.max(PHOTO_SIZE_MIN, Math.round(parsed)))
}

export function getPhotoHeightByRatio(width, ratio = DEFAULT_PHOTO_RATIO) {
  const safeWidth = clampPhotoSize(width, 96)
  const { w, h } = getRatioScale(ratio)
  return clampPhotoSize((safeWidth * h) / w, safeWidth)
}

export function normalizePhotoConfig(source = {}) {
  const ratio = isValidRatio(source.ratio) ? source.ratio : DEFAULT_PHOTO_RATIO
  const lockRatio = source.lockRatio !== false
  const width = clampPhotoSize(source.width, 96)
  const height = lockRatio
    ? getPhotoHeightByRatio(width, ratio)
    : clampPhotoSize(source.height, getPhotoHeightByRatio(width, ratio))

  return {
    width,
    height,
    ratio,
    lockRatio,
  }
}
