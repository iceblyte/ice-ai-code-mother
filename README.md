# ICE AI Code Mother

基于 Spring Boot 3 + Vue 3 的 AI 应用生成平台，支持通过自然语言生成网页应用、实时预览代码结果、管理应用并执行部署。

项目参考自 [liyupi/yu-ai-code-mother](https://github.com/liyupi/yu-ai-code-mother)，并在此基础上进行了本地实现与功能扩展。

## 项目结构

```text
.
├─ src/                           # Spring Boot 后端
├─ sql/                           # 数据库脚本
├─ ice-ai-code-mother-frontend/   # Vue 3 前端
├─ nginx-1.28.3/                  # 本地 Nginx 相关资源
├─ pom.xml                        # 后端 Maven 配置
└─ README.md
```

## 技术栈

- 后端：Java 21、Spring Boot 3、MyBatis Flex、MySQL、LangChain4j
- 前端：Vue 3、TypeScript、Vite、Pinia、Ant Design Vue
- 其他：Knife4j、HikariCP、Hutool

## 核心能力

- 用户通过提示词创建应用
- AI 流式生成页面代码并实时预览
- 管理个人应用、查看应用详情
- 支持管理员管理用户和应用
- 支持应用部署与静态资源访问

## 本地启动

### 1. 启动后端

要求：

- JDK 21
- Maven 3.9+
- MySQL 8+

执行数据库脚本：

```bash
mysql -u root -p < sql/create_table.sql
```

启动后端：

```bash
./mvnw spring-boot:run
```

Windows：

```powershell
.\mvnw.cmd spring-boot:run
```

默认后端地址：

- 服务端口：`8123`
- 接口前缀：`/api`
- 完整基址：`http://localhost:8123/api`

### 2. 启动前端

进入前端目录并安装依赖：

```bash
cd ice-ai-code-mother-frontend
npm install
```

启动开发环境：

```bash
npm run dev
```

构建前端：

```bash
npm run build
```

## 主要目录说明

### 后端

- `src/main/java/com/iceblyte/aicodemother/controller`：接口层
- `src/main/java/com/iceblyte/aicodemother/service`：业务层
- `src/main/java/com/iceblyte/aicodemother/ai`：AI 生成服务
- `src/main/java/com/iceblyte/aicodemother/core`：代码解析与保存核心逻辑
- `src/main/resources/prompt`：提示词模板

### 前端

- `ice-ai-code-mother-frontend/src/pages`：页面模块
- `ice-ai-code-mother-frontend/src/components`：公共组件
- `ice-ai-code-mother-frontend/src/api`：接口定义
- `ice-ai-code-mother-frontend/src/stores`：状态管理

## 接口文档

项目集成了 Knife4j，启动后端后可访问文档页面：

- `http://localhost:8123/api/doc.html`

## 说明

- 当前仓库包含前后端代码
- AI、数据库、部署等能力依赖本地环境与相关配置
- 前端目录内还有一份更细的子项目说明，见 `ice-ai-code-mother-frontend/README.md`

## 参考项目

- https://github.com/liyupi/yu-ai-code-mother
