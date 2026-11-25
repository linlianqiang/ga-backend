# 消防系统管理

正式 API 基础地址：`http://124.222.78.245:8080`。
业务接口包含 `/api` 前缀，例如登录：`POST http://124.222.78.245:8080/api/auth/login`。
前端若已在请求路径中包含 `/api`，baseURL 使用上述基础地址，避免重复拼接 `/api`。
健康检查：`GET http://124.222.78.245:8080/actuator/health`；服务器发布脚本使用 `http://127.0.0.1:8080/actuator/health` 检查本机服务。

## 项目管理
1. 项目列表：项目编号、项目名称、物业单位、项目联系人、联系方式、维保负责人、消防主机型号、平台执业人、合同期限、所在区域

## 后端（Java + Spring Boot）
- 代码入口：`src/main/java/com/example/gabackend/GaBackendApplication.java`
- 数据表：`fire_project`、`user`（程序启动后自动创建/更新）

### 运行步骤
1. 安装 JDK 17+ 与 Maven
2. 在项目根目录执行：`mvn spring-boot:run`
3. 浏览器/前端可直接访问 API，默认端口 `8080`

### 认证与授权
系统使用 JWT Token 进行身份验证和权限控制。

#### 统一响应格式
所有接口返回数据均使用统一的 JSON 格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { /* 实际数据 */ }
}
```

**字段说明：**
- `code`：状态码，200表示成功，400表示客户端错误，401表示未授权，404表示资源不存在，502等表示服务器错误
- `message`：提示信息，描述操作结果
- `data`：实际返回的数据，失败时为 null

#### 初始用户设置
系统启动后需要手动在数据库中创建初始用户。以下是测试用户的 SQL 语句：

```sql
INSERT INTO user (username, password, roles) VALUES ('admin', '123456', 'SUPER,ADMIN');
INSERT INTO user (username, password, roles) VALUES ('user', '123456', 'USER');
```

**注意：**
- 当前版本使用明文密码存储，仅用于开发测试。生产环境建议使用 BCrypt 等加密方式。
- `roles` 字段支持多角色，用逗号分隔，如 `'SUPER,ADMIN'` 表示该用户同时拥有 SUPER 和 ADMIN 角色。

#### 用户角色
系统支持多角色分配，一个用户可以同时拥有多个角色：
- `SUPER`：超级管理员
- `ADMIN`：管理员，拥有所有权限
- `USER`：普通用户，可查看和编辑项目
- `VIEWER`：访客，仅可查看项目

#### 认证相关 API
- `POST /api/auth/login`：用户登录，返回 JWT token

**请求体示例：**
```json
{
  "username": "admin",
  "password": "123456"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin",
    "roles": ["SUPER", "ADMIN"]
  }
}
```

**错误响应示例：**
```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null
}
```

#### 用户信息 API
- `GET /api/user/info`：获取当前登录用户信息（需要 Token）

**请求头：**
```
Authorization: Bearer <your-token>
```

**响应示例：**
```json
{
  "code": 200,
  "message": "获取用户信息成功",
  "data": {
    "username": "admin",
    "roles": ["SUPER", "ADMIN"]
  }
}
```

### 项目管理 API
以下接口除 `/api/auth/login` 外，均需要在请求头中携带 Token。

**API 前缀：** `/api/projects`

- `GET /api/projects`：获取全部项目
- `GET /api/projects/{id}`：按 id 获取
- `POST /api/projects`：创建项目（JSON 体见下）
- `PUT /api/projects/{id}`：更新项目
- `DELETE /api/projects/{id}`：删除项目

#### 响应格式说明
所有项目管理接口均返回统一格式：

**成功响应示例（获取项目列表）：**
```json
{
  "code": 200,
  "message": "获取项目列表成功",
  "data": [
    {
      "id": 1,
      "projectNumber": "XF-001",
      "projectName": "城市广场消防",
      "propertyUnit": "XX物业",
      "contactPerson": "张三",
      "contactPhone": "13800000000",
      "maintenanceLeader": "李四",
      "fireHostModel": "海湾GST5000",
      "platformPractitioner": "王五",
      "contractTerm": "2024-2025",
      "region": "上海"
    }
  ]
}
```

**错误响应示例（项目不存在）：**
```json
{
  "code": 404,
  "message": "项目不存在",
  "data": null
}
```

#### 示例 JSON（创建/更新项目）
```json
{
  "projectNumber": "XF-001",
  "projectName": "城市广场消防",
  "propertyUnit": "XX物业",
  "contactPerson": "张三",
  "contactPhone": "13800000000",
  "maintenanceLeader": "李四",
  "fireHostModel": "海湾GST5000",
  "platformPractitioner": "王五",
  "contractTerm": "2024-2025",
  "region": "上海"
}
```


## 完整业务接口（2026-11-19）

新增基础数据、人员、账号角色权限、项目账期关联和匹配建议 API。接口详见 [完整业务接口](docs/business-api.md)。文件及合同附件上传不在本次开发范围。

使用 JDK17。JWT 密钥由环境变量 `JWT_SECRET` 或已忽略的 `.env` 提供，至少32字节，禁止提交密钥。`scripts/run_backend.sh` 会读取 `.env` 后启动；从项目根目录直接 `mvn spring-boot:run` 也会加载 `.env`，IDE 的工作目录需设为项目根目录。旧 token 在升级后失效，需重新登录。

2026-11-20 已完成数据库结构初始化及字典回填：物业366条、我司企业7条、系统管理员角色及12项权限。账号表为空，无现有管理员可迁移；使用 `scripts/initialize_admin.sh` 交互设置首个管理员密码并启动。初始化仅在账号表为空且显式提供密码时创建 admin，不覆盖现有账号。随后正常启动不带初始化开关。历史账期不会自动绑定项目；使用匹配建议和批量确认接口，保留原始数据用于核对。

部署流程见 [CI/CD 部署指南](docs/deployment.md)。
