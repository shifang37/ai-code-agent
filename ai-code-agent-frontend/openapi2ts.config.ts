/**
 * openapi2ts 配置
 *
 * 重要：hook 中把所有 OpenAPI schema 里的 Long（type:integer, format:int64）改写为 string，
 * 避免 JS Number 在 19 位 snowflake ID 上精度丢失。
 * 后端 JsonConfig.java 已经把 Long 序列化为 string，前端也必须用 string 处理。
 */
export default {
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8123/api/v3/api-docs',
  serversPath: './src',
  hook: {
    afterOpenApiDataInited: (openApiData: any) => {
      const convertLongToString = (schema: any) => {
        if (!schema || typeof schema !== 'object') return
        if (schema.type === 'integer' && schema.format === 'int64') {
          schema.type = 'string'
          delete schema.format
          return
        }
        if (schema.properties) {
          for (const key of Object.keys(schema.properties)) {
            convertLongToString(schema.properties[key])
          }
        }
        if (schema.items) {
          convertLongToString(schema.items)
        }
        if (schema.additionalProperties && typeof schema.additionalProperties === 'object') {
          convertLongToString(schema.additionalProperties)
        }
      }
      // 处理 components.schemas
      const schemas = openApiData?.components?.schemas
      if (schemas) {
        for (const key of Object.keys(schemas)) {
          convertLongToString(schemas[key])
        }
      }
      // 处理 paths 中内联的 parameters 和 requestBody
      const paths = openApiData?.paths
      if (paths) {
        for (const pathKey of Object.keys(paths)) {
          const methods = paths[pathKey]
          for (const methodKey of Object.keys(methods)) {
            const op = methods[methodKey]
            if (op?.parameters) {
              for (const param of op.parameters) {
                convertLongToString(param.schema)
              }
            }
            const reqBodySchema = op?.requestBody?.content?.['application/json']?.schema
            if (reqBodySchema) convertLongToString(reqBodySchema)
          }
        }
      }
      return openApiData
    },
  },
}
