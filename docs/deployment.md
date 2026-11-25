# CI/CD 部署

正式 API 基础地址：`http://124.222.78.245:8080`。
业务接口包含 `/api` 前缀，例如登录：`POST http://124.222.78.245:8080/api/auth/login`。
前端若已在请求路径中包含 `/api`，baseURL 使用上述基础地址，避免重复拼接 `/api`。
健康检查：`GET http://124.222.78.245:8080/actuator/health`；服务器发布脚本使用 `http://127.0.0.1:8080/actuator/health` 检查本机服务。

PR 运行 Java 17 Maven 测试；main 推送或手动触发在测试成功后发布 production。发布串行执行，SSH 严格校验主机；健康检查失败恢复上一份 jar（不回滚数据库）。前端 /www/wwwroot/web 不在此仓库部署范围。

## 首次配置

服务器安装 Java 17、curl、flock、systemd。创建 ga-backend 服务用户及 /www/wwwroot/java/ga-backend，赋予 SSH 部署用户目录写权限、服务用户读权限。安装 scripts/ga-backend.service 到 /etc/systemd/system/，安装 scripts/deploy-ga-backend 到 /usr/local/sbin/deploy-ga-backend（root 所有、0755、部署用户不可修改）。sudoers 仅允许部署用户免密执行该脚本。执行 systemctl daemon-reload 和 systemctl enable ga-backend。

创建 root 所有、0600 的 /etc/ga-backend.env，设置 DB_URL、DB_USERNAME、DB_PASSWORD、JWT_SECRET（至少32字节）；按需一次性设置 BOOTSTRAP_ADMIN_PASSWORD，初始化后删除。数据库必须先备份；当前 ddl-auto=update 会修改表结构。首次发布前确认 8080 未被其他服务占用。健康检查为 /actuator/health。

GitHub Settings → Environments 创建 production，配置 Secrets：SSH_HOST、SSH_PORT、SSH_USER、SSH_PRIVATE_KEY、SSH_KNOWN_HOSTS。公钥写入服务器部署用户 authorized_keys，KNOWN_HOSTS 必须由可信渠道核对服务器主机指纹后填写。不要提交私钥和数据库密码。

建议保护 main：要求 PR、build 检查通过后合并，禁止强推。环境只允许 main 部署。当前配置未在服务器执行，需完成以上初始化后触发工作流验证。

旧 Git 历史曾包含数据库凭据，发布历史前必须处理，且应轮换该密码。
