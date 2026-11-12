# OfferFlow

OfferFlow 是面向高校求职者的求职进度管理系统，支持账号注册与登录、求职投递管理、面试复盘和数据统计。

## 技术栈

- 前端：Vue 3、TypeScript、Vite
- 后端：Java 17、Spring Boot 3.3.4、Spring Security、Spring Data JPA
- 数据库：MySQL 8、Flyway；自动化测试使用 H2

## 环境要求

- Java 17
- Maven 3.9+
- Node.js 24+ 和 npm
- MySQL 8

## 首次配置

1. 按 [backend/database/01-create-local-database.sql](backend/database/01-create-local-database.sql) 的模板准备本机 MySQL 数据库和专用账号。将示例密码替换为本机强密码；不要把真实密码保存到项目文件。
2. 后端读取 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。未设置时，`DB_URL` 默认指向本机 `offerflow`；Hibernate 使用 `validate`，Flyway 会在启动时写入迁移历史并执行未应用的迁移。
3. 启动前必须核对 JDBC URL 和账号。不要对含有需保留数据的库运行迁移；隔离演示/验证应使用单独数据库和仅授权该库的账号。

## 本地启动

在项目根目录打开 PowerShell 窗口一启动后端。密码以不回显的提示读取，Maven 使用项目现有本地仓库：

```powershell
Set-Location backend
try {
  $env:DB_URL = Read-Host 'Target MySQL JDBC URL'
  $env:DB_USERNAME = Read-Host 'Target MySQL username'
  $targetPath = ($env:DB_URL -split '\?', 2)[0]
  $targetSchema = [regex]::Match($targetPath, '/([^/]+)$').Groups[1].Value
  Write-Host "Database target: $targetPath (user: $env:DB_USERNAME)"
  if (-not $targetSchema -or (Read-Host "Type schema name '$targetSchema' to confirm") -cne $targetSchema) { throw 'Database target not confirmed; backend was not started.' }
  $securePassword = Read-Host 'OfferFlow database password' -AsSecureString
  $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
  mvn `
    "-Dmaven.repo.local=..\work\maven-repository" `
    spring-boot:run
} finally {
  Remove-Item Env:DB_PASSWORD,Env:DB_URL,Env:DB_USERNAME -ErrorAction SilentlyContinue
  Remove-Variable securePassword -ErrorAction SilentlyContinue
  Remove-Variable targetPath -ErrorAction SilentlyContinue
  Remove-Variable targetSchema -ErrorAction SilentlyContinue
}
```

看到 `Tomcat started on port 8080` 后，健康检查地址为 <http://127.0.0.1:8080/api/health>，Swagger 地址为 <http://127.0.0.1:8080/swagger-ui/index.html>。

在第二个 PowerShell 窗口启动前端：

```powershell
Set-Location frontend
npm run dev -- --host 127.0.0.1 --port 5173
```

浏览器打开 <http://127.0.0.1:5173/>。默认 API 地址为 `http://localhost:8080/api`，如需调整可在启动前设置 `VITE_API_BASE`。

## 演示流程

1. 确认前后端均已启动，并访问后端健康检查。
2. 在页面选择“登录 / 注册”，注册一个演示用户，或使用已有账号登录。
3. 确认页面显示“已连接”和“已连接真实数据”。未登录时的示例记录只用于预览，不会保存到数据库。
4. 创建一条测试投递，编辑其阶段，再添加面试复盘；检查看板、投递列表和统计数据。
5. 刷新页面，确认账号登录状态和数据仍可恢复；演示结束后退出登录。

## 验证与数据安全

- 后端测试配置使用 H2，不需要连接本机 MySQL。执行完整后端测试：

  ```powershell
  cd backend
  mvn `
    "-Dmaven.repo.local=..\work\maven-repository" test
  ```

- 测试或演示前确认数据源 URL。不要对 `offerflow` 执行清库、重建或迁移重放操作。
- 开发服务分别在其 PowerShell 窗口按 `Ctrl+C` 停止；除非另有需要，不必停止 MySQL 服务。
- 前端构建命令为 `npm run build`，在 `frontend/` 执行。

## 目录

- `frontend/`：Vue 前端
- `backend/`：Spring Boot API、Flyway migration 与后端测试
- `outputs/`：项目策划和使用说明
- `work/`：本地 Maven/npm 缓存，不提交到 Git
