import request from './request'

export const createSession = (data) => {
  return request({
    url: '/chat/session/create',
    method: 'post',
    data
  })
}

export const getSessionList = () => {
  return request({
    url: '/chat/session/list',
    method: 'get'
  })
}

export const deleteSession = (id) => {
  return request({
    url: `/chat/session/${id}`,
    method: 'delete'
  })
}

export const getMessageList = (sessionId) => {
  return request({
    url: `/chat/session/${sessionId}/messages`,
    method: 'get'
  })
}

export const streamChat = async (sessionId, content, requestId, signal) => {
  const token = localStorage.getItem('token')
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ sessionId, content, requestId }),
    signal
  })

  if (!response.ok) {
    const text = await response.text()
    throw new Error(text || `请求失败 (${response.status})`)
  }

  if (!response.body) {
    throw new Error('浏览器不支持流式读取')
  }

  return response.body.getReader()
}
