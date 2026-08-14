/**
 * VRM 模型资源映射配置
 *
 * 维护角色名称到 VRM 模型文件的映射关系
 * 用于在前端根据角色名称回退匹配模型 URL
 *
 * 注意：实际模型文件存放在 frontend/public/models/avatars/ 目录下
 */

// 角色名称 -> 模型文件路径的映射
export const MODEL_MAP = {
  nova: '/models/avatars/nova.vrm',
  sky: '/models/avatars/sky.vrm'
}

// 默认模型（当名称无法匹配时使用）
export const DEFAULT_MODEL = '/models/avatars/nova.vrm'

/**
 * 根据角色名称获取模型 URL
 * 支持英文（不区分大小写）和中文匹配
 * @param {string} name - 角色名称
 * @returns {string} 模型文件 URL
 */
export const getModelUrlByName = (name) => {
  if (!name) return DEFAULT_MODEL

  const lowerName = name.toLowerCase()

  // 1. 精确匹配（不区分大小写）
  if (MODEL_MAP[lowerName]) return MODEL_MAP[lowerName]
  if (MODEL_MAP[name]) return MODEL_MAP[name]

  // 2. 旧数据没有明确 baseModel/modelUrl 时回退到基础模型
  return DEFAULT_MODEL
}
