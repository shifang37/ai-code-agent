/**
 * 健康检查。后端启动并暴露 OpenAPI 文档后，可执行 `npm run openapi2ts` 用工具重新生成接口代码。
 */
import request from '@/request'

export function healthCheck() {
  return request.get<string>('/health/')
}
