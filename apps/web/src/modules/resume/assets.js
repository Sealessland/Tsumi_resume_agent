import { createId } from './factories'
import { createEmptyResume } from './templates'

const ASSETS_KEY = 'resume-builder-assets-v1'
const ACTIVE_ASSET_KEY = 'resume-builder-active-asset-v1'
const VALID_STATUSES = new Set(['draft', 'active', 'archived'])

function trim(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function parseAssets() {
  try {
    const stored = JSON.parse(localStorage.getItem(ASSETS_KEY) || '[]')
    return Array.isArray(stored) ? stored : []
  } catch (error) {
    console.error('Read resume assets failed:', error)
    return []
  }
}

export function normalizeResumeAsset(source = {}) {
  const now = new Date().toISOString()
  const tags = Array.isArray(source.tags)
    ? [...new Set(source.tags.map(trim).filter(Boolean))]
    : []

  return {
    id: trim(source.id) || createId('asset'),
    title: trim(source.title),
    targetRole: trim(source.targetRole),
    targetCompany: trim(source.targetCompany),
    jobDescription: trim(source.jobDescription),
    status: VALID_STATUSES.has(source.status) ? source.status : 'draft',
    tags,
    createdAt: trim(source.createdAt) || now,
    updatedAt: now,
    resume: source.resume || createEmptyResume(),
  }
}

export function createResumeAsset(partial = {}) {
  return normalizeResumeAsset(partial)
}

export async function listResumeAssets() {
  return parseAssets().map(normalizeResumeAsset).sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
}

export async function saveResumeAsset(asset) {
  const normalized = normalizeResumeAsset(asset)
  const assets = parseAssets()
  const index = assets.findIndex((item) => item.id === normalized.id)
  if (index === -1) assets.unshift(normalized)
  else assets.splice(index, 1, normalized)
  localStorage.setItem(ASSETS_KEY, JSON.stringify(assets))
  return normalized
}

export async function deleteResumeAsset(id) {
  localStorage.setItem(ASSETS_KEY, JSON.stringify(parseAssets().filter((asset) => asset.id !== id)))
}

export async function loadActiveResumeAssetId() {
  return localStorage.getItem(ACTIVE_ASSET_KEY) || ''
}

export async function saveActiveResumeAssetId(id) {
  localStorage.setItem(ACTIVE_ASSET_KEY, id)
}

export function duplicateResumeContent(resume) {
  return clone(resume)
}
