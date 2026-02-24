
# TripSphere 项目结构与作用说明

> 说明基于仓库中的 README、Taskfile、Docker Compose、依赖清单与协议定义整理；功能职责以目录命名与依赖/编排信息为依据，具体细节以代码实现为准。

## 1. 项目总体定位
- TripSphere 是 AI-native 的微服务分布式系统，采用多语言栈（Java/Go/Python/TS/Next.js）。
- 仓库为 monorepo：通过 Taskfile 统一执行 proto 生成、构建与编排启动。
- Docker Compose 作为本地开发环境的一键编排入口。

### 1.1 项目作用与目标
- 面向旅行场景提供一体化的 AI 能力：对话咨询、行程规划、内容生成（如笔记/摘要）以及服务数据管理（景点/酒店/用户/评论）。
- 通过微服务拆分业务域，便于独立扩展与部署，同时使用 gRPC/HTTP 协议对齐接口规范。
- 借助服务发现、异步消息、向量/图数据库等组件，为智能推荐与摘要提供基础能力。

## 2. 顶层目录概览
- contracts/：跨服务协议定义。
  - protobuf/：gRPC/Protobuf 定义，使用 Buf 管理。
  - openapi/：HTTP API 文档，目前包含 chat 服务 OpenAPI。
- data-pipeline/：数据采集与处理脚本（当前为 AMap POI 抓取/清洗）。
- otel-collector/：OpenTelemetry Collector 配置，用于本地观测数据。
- mysql/：本地 MySQL 初始化脚本（user_db、review_db）。
- trip-*-service / trip-*-assistant / trip-*-planner：各业务服务与 AI agent。
- trip-next-frontend/：Next.js 前端应用。
- docker-compose.yaml：本地开发环境的服务、中间件、依赖编排。
- Taskfile.yaml：顶层任务入口，聚合各子服务 Taskfile。

## 3. 协议与共享代码
### 3.1 Protobuf / gRPC
- contracts/protobuf 通过 Buf 维护 proto 模块，`task gen-proto` 统一生成多语言 gRPC 代码。
- Python 服务的 `libs/tripsphere` 为生成的 SDK（由 proto 编译产生）。

### 3.2 OpenAPI
- chat 服务有完整 OpenAPI 文档，定义如 `/api/v1/conversations` 等对话相关接口。

## 4. 业务服务（按技术栈/职责）
### 4.1 Java (Spring Boot + Maven)
- trip-attraction-service：景点/POI 相关服务。
- trip-hotel-service：酒店相关服务。
- trip-note-service：旅行笔记相关服务。
- trip-user-service：用户体系（认证/用户数据），包含 Spring Security、JWT、JPA、MySQL、gRPC 等依赖。

### 4.2 Python (uv + FastAPI/ASGI)
- trip-chat-service：对话/聊天服务，FastAPI + MongoDB + Nacos；依赖 LLM 接入（OpenAI）。
- trip-itinerary-service：行程服务，gRPC Server；包含 itinerary/metadata/planning 三类 gRPC 服务；使用 MongoDB 并通过 Nacos 注册服务实例。
- trip-itinerary-planner：行程规划器，包含 LLM 规划能力（LangChain/LangGraph），提供 HTTP 服务。
- trip-journey-assistant：旅程助手类 agent（Google ADK / LLM / MCP Weather）。
- trip-review-summary：评论摘要服务，FastAPI + Celery + 向量数据库（Qdrant）+ 图数据库（Neo4j）+ RocketMQ。

### 4.3 Go
- trip-file-service：文件/对象存储服务，gRPC + MinIO + Nacos。
- trip-review-service：评论服务，MySQL + RocketMQ，提供 gRPC 接口。

### 4.4 前端
- trip-next-frontend：Next.js 前端 UI，运行时通过环境变量接入 gRPC/HTTP 后端服务。

### 4.5 轻量/实验模块
- trip-hotel-advisor：酒店推荐/咨询相关的 Python 包骨架。
- trip-note-creator：旅行笔记生成相关的 Python 包骨架。

## 5. 基础设施与中间件（docker-compose）
- Nacos：服务注册/发现。
- MongoDB：聊天与行程等服务存储。
- MySQL：用户与评论数据。
- MinIO：文件对象存储。
- RocketMQ（namesrv/broker/proxy/dashboard）：评论与摘要相关异步链路。
- Qdrant：向量检索。
- Neo4j：知识图谱/图检索。
- Redis：Celery 等任务队列/缓存依赖。
- OpenTelemetry Collector：采集与导出日志/指标/链路数据。

## 6. 执行流程与交互方式
### 6.1 启动流程（本地）
1. （可选）生成协议代码：使用 Taskfile 调用 Buf 生成 gRPC SDK。
2. 通过 `task start` 触发 Docker Compose，拉起基础设施与业务服务。
3. 中间件启动并进入健康状态（Nacos、MongoDB、MySQL、RocketMQ、Redis、Qdrant、Neo4j、MinIO 等）。
4. 业务服务读取配置并启动 HTTP/gRPC 端口，同时注册到 Nacos 以便服务发现。
5. 前端服务启动并对接后端地址（环境变量注入）。

### 6.2 运行时请求流程（示例）
- 对话/助手：前端发起聊天请求 → chat 服务处理 → 调用 LLM（OpenAI）并读写 MongoDB → 返回对话结果。
- 行程规划：前端发起规划请求 → itinerary-planner 调用 LLM/规划算法 → 可结合 itinerary 服务持久化与其它服务数据 → 返回行程方案。
- 评价与摘要：review 服务写入 MySQL → 通过 RocketMQ 产生日志/事件 → review-summary consumer/worker 消费 → 生成摘要并存入 Qdrant/Neo4j。
- 文件存储：文件相关请求进入 file 服务 → 对象写入 MinIO → 返回可访问的文件引用。

### 6.3 典型交互方式
- 前端通过 `GRPC_ATTRACTION_URL`、`HTTP_CHAT_URL` 等环境变量连接后端服务。
- 多数后端服务通过 Nacos 注册，供其它服务/网关发现。
- gRPC/HTTP 由 contracts 中的 proto 或 OpenAPI 文档约束。

## 7. 运行/开发建议（简要）
- 使用 `task gen-proto` 生成 gRPC 代码，`task build` 构建镜像，`task start` 通过 Docker Compose 启动全栈环境。
- 依赖外部 API 的模块需准备密钥：例如 chat/itinerary-planner/journey-assistant/review-summary 需要 `OPENAI_API_KEY`；data-pipeline/amap 脚本使用 `AMAP_API_KEY`/`AMAP_SECRET`。
