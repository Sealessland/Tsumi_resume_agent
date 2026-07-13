import { PHOTO_RULES } from './constants'

function getPhotoFormat(type = '') {
  if (type.includes('png')) return 'PNG'
  if (type.includes('webp')) return 'WebP'
  return 'JPG'
}

function estimateDataUrlSize(dataUrl) {
  const base64 = dataUrl.split(',')[1] || ''
  const padding = base64.endsWith('==') ? 2 : base64.endsWith('=') ? 1 : 0
  return Math.floor((base64.length * 3) / 4) - padding
}

function loadImageFromFile(file) {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file)
    const image = new Image()

    image.onload = () => {
      URL.revokeObjectURL(objectUrl)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('IMAGE_LOAD_FAILED'))
    }

    image.src = objectUrl
  })
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('FILE_READ_FAILED'))
    reader.readAsDataURL(file)
  })
}

async function convertPhotoToJpg(file, quality, maxEdge) {
  const image = await loadImageFromFile(file)
  const rawWidth = image.naturalWidth || image.width
  const rawHeight = image.naturalHeight || image.height
  const rawMaxEdge = Math.max(rawWidth, rawHeight)
  const ratio = rawMaxEdge > maxEdge ? maxEdge / rawMaxEdge : 1
  const width = Math.max(1, Math.round(rawWidth * ratio))
  const height = Math.max(1, Math.round(rawHeight * ratio))

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) throw new Error('CANVAS_CONTEXT_UNAVAILABLE')

  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, width, height)
  context.drawImage(image, 0, 0, width, height)

  const dataUrl = canvas.toDataURL('image/jpeg', quality)
  const bytes = estimateDataUrlSize(dataUrl)

  return {
    dataUrl,
    bytes,
    width,
    height,
    scaled: ratio < 1,
  }
}

export async function processProfilePhoto(file, rules = PHOTO_RULES) {
  if (!file) {
    return { ok: false, error: '未选择文件。' }
  }

  if (!rules.SUPPORTED_TYPES.has(file.type)) {
    return { ok: false, error: '仅支持 JPG / PNG / WebP 格式。' }
  }

  const originalFormat = getPhotoFormat(file.type)
  const originalSizeKB = Number((file.size / 1024).toFixed(1))

  if (file.size <= rules.MAX_BYTES) {
    const image = await loadImageFromFile(file)
    const dataUrl = await readFileAsDataUrl(file)

    const photoMeta = {
      originalFormat,
      originalSizeKB,
      finalFormat: originalFormat,
      finalSizeKB: originalSizeKB,
      width: image.naturalWidth || image.width,
      height: image.naturalHeight || image.height,
      compressed: false,
    }

    return {
      ok: true,
      photo: dataUrl,
      photoMeta,
      message: `文件≤5MB，已保留原图（${photoMeta.width}×${photoMeta.height}，${originalSizeKB}KB）。`,
    }
  }

  const firstPass = await convertPhotoToJpg(file, rules.PRIMARY_QUALITY, rules.MAX_EDGE)
  let finalPass = firstPass
  let usedSecondaryCompression = false

  if (firstPass.bytes > rules.MAX_BYTES) {
    finalPass = await convertPhotoToJpg(file, rules.SECONDARY_QUALITY, rules.MAX_EDGE)
    usedSecondaryCompression = true
  }

  if (finalPass.bytes > rules.MAX_BYTES) {
    return { ok: false, error: '图片处理后仍超过 5MB，请换一张更小的图片。' }
  }

  const finalSizeKB = Number((finalPass.bytes / 1024).toFixed(1))
  const compressed = usedSecondaryCompression || finalPass.scaled || finalPass.bytes < file.size
  const notes = ['已转换为 JPG']
  if (compressed) notes.push('已自动压缩')

  return {
    ok: true,
    photo: finalPass.dataUrl,
    photoMeta: {
      originalFormat,
      originalSizeKB,
      finalFormat: 'JPG',
      finalSizeKB,
      width: finalPass.width,
      height: finalPass.height,
      compressed,
    },
    message: `${notes.join('，')}（${finalPass.width}×${finalPass.height}，${finalSizeKB}KB）。`,
  }
}
