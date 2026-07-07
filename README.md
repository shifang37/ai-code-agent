# AI 零代码应用生成平台(ai-code-agent)

基于 **Spring Boot 3 + LangChain4j + LangGraph4j + Vue 3** 的 AI 零代码应用生成平台。用一句话描述需求,AI 即可自动生成完整的网页应用,支持对话式迭代修改、在线预览、一键部署。

## ✨ 核心功能

- **对话式代码生成**:输入自然语言描述,AI 智能路由自动选择生成模式,SSE 流式返回生成过程
  - **原生 HTML 模式**:生成单文件页面
  - **原生多文件模式**:生成 HTML + CSS + JS 多文件页面
  - **Vue 工程模式**:AI 通过工具调用(读取/写入/修改/删除文件、读取目录)迭代开发完整 Vue 项目,生成完成后自动执行构建
- **AI 工作流(LangGraph4j)**:提示词增强 → 智能路由 → 图片素材收集 → 代码生成 → 代码质量检查 → 项目构建,支持通过 SSE 实时推送工作流执行进度
- **图片素材能力**:Pexels 图片搜索 + 阿里云 DashScope 文生图(wan2.2-t2i-flash),为生成的应用自动配图
- **应用管理**:应用创建 / 修改 / 删除、精选应用(Caffeine + Redis 多级缓存)、一键部署;生成与部署完成后通过 Selenium 自动截取页面作为应用封面并上传腾讯云 COS
- **可视化修改**:生成的应用支持选中页面元素,结合对话进行精准修改
- **用户系统**:注册登录、Spring Session + Redis 分布式会话、注解 + AOP 权限校验、Redisson 接口限流
- **对话历史**:游标分页加载,重新进入应用自动恢复对话上下文与 AI 记忆

## 🏗 技术栈

### 后端(ai-code-agent-backend)

| 技术 | 说明 |
| --- | --- |
| Spring Boot 3.5 / Java 21 | 基础框架,使用虚拟线程执行异步任务 |
| LangChain4j | AI 对话、流式输出、工具调用、结构化输出、护栏 |
| LangGraph4j | AI 工作流编排 |
| DeepSeek API | 对话模型(deepseek-chat)/ 推理模型(deepseek-reasoner) |
| 阿里云 DashScope | AI 文生图 |
| Pexels API | 图片素材搜索 |
| MySQL + MyBatis-Flex + HikariCP | 数据存储 |
| Redis + Spring Session | 分布式会话、AI 对话记忆持久化 |
| Caffeine + Redis | 本地 + 分布式多级缓存 |
| Redisson | 分布式限流 |
| Selenium + WebDriverManager | 网页自动截图 |
| 腾讯云 COS | 对象存储(应用封面图) |
| Knife4j | 接口文档 |

### 前端(ai-code-agent-frontend)

| 技术 | 说明 |
| --- | --- |
| Vue 3 + TypeScript + Vite | 基础框架 |
| Ant Design Vue 4 | UI 组件库 |
| Pinia / Vue Router / Axios | 状态管理 / 路由 / 网络请求 |
| @umijs/openapi | 根据后端 OpenAPI 文档自动生成请求代码 |

## 🚀 快速开始

### 环境要求

- JDK 21+、Maven 3.8+
- MySQL 8.x、Redis 6+
- Node.js 20.19+(或 22.12+)、npm
- Chrome 浏览器(Selenium 截图使用,WebDriver 自动管理)

### 1. 初始化数据库

创建数据库 `ai_code_agent`,按 `model/entity` 下的实体类(`User`、`App`、`ChatHistory`)创建对应的 `user`、`app`、`chat_history` 表。

### 2. 配置后端

仓库中的 `application.yml` 仅为**配置模板**,所有敏感项均为占位符。请在 `ai-code-agent-backend/src/main/resources` 下新建 `application-local.yml` 填入真实配置(该文件已被 `.gitignore` 忽略,**不会提交到仓库**):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_code_agent
    username: <数据库用户名>
    password: <数据库密码>
langchain4j:
  open-ai:
    chat-model:
      api-key: <DeepSeek API Key>
    streaming-chat-model:
      api-key: <DeepSeek API Key>
    reasoning-streaming-chat-model:
      api-key: <DeepSeek API Key>
    routing-chat-model:
      api-key: <DeepSeek API Key>
cos:
  client:
    host: <COS 访问域名>
    secretId: <腾讯云 SecretId>
    secretKey: <腾讯云 SecretKey>
    region: <地域,如 ap-shanghai>
    bucket: <存储桶名称>
pexels:
  api-key: <Pexels API Key>
dashscope:
  api-key: <阿里云 DashScope API Key>
```

### 3. 启动后端

```bash
cd ai-code-agent-backend
mvn spring-boot:run
```

接口文档:<http://localhost:8123/api/doc.html>

### 4. 启动前端

```bash
cd ai-code-agent-frontend
npm install
npm run dev
```

访问:<http://localhost:5173>

## 📁 项目结构

```
ai-code-agent
├── ai-code-agent-backend                # Spring Boot 后端
│   └── src/main
│       ├── java/com/tzy/aicodeagent
│       │   ├── ai                       # AI 服务:代码生成、智能路由、工具调用、护栏
│       │   ├── core                     # 代码生成核心:解析器、保存器、构建器、流处理
│       │   ├── langgraph4j              # AI 工作流:节点、状态、工具
│       │   ├── controller               # 接口:应用、对话历史、用户、工作流 SSE、静态资源
│       │   ├── service                  # 业务逻辑
│       │   ├── ratelimiter              # Redisson 限流(注解 + 切面)
│       │   ├── manager                  # 第三方能力封装(COS 等)
│       │   └── ...
│       └── resources
│           ├── prompt                   # 各环节系统提示词
│           └── application.yml          # 配置模板(仅占位符)
└── ai-code-agent-frontend               # Vue 3 前端
    └── src
        ├── pages                        # 首页、对话生成、应用编辑、用户中心、管理后台
        ├── api                          # OpenAPI 自动生成的请求代码
        └── components / stores / router / utils
```

## 🔒 配置安全说明

- `application.yml` 只包含占位符,可安全公开;真实密钥统一放在 `application-local.yml`,已通过 `.gitignore` 排除,不会进入版本库
- 运行时生成的代码产物目录 `tmp/` 同样已被忽略
- 前端 `.env.development` 仅包含本地后端地址,无敏感信息
