# OfferFlow 使用说明

## 1. 项目简介

OfferFlow 是一个面向高校求职者的求职进度管理平台，用于记录岗位投递、跟踪招聘阶段、整理面试复盘，并查看求职数据统计。

项目由两部分组成：

- `frontend`：Vue 3 + TypeScript 前端页面
- `backend`：Spring Boot + MySQL REST API

## 2. 环境要求

- Windows 10/11
- Node.js 24 或更高版本
- Java 17
- Maven 3.9 或更高版本
- MySQL 8
- 推荐浏览器：Chrome 或 Edge

## 3. 第一次使用

### 3.1 初始化数据库

确保 MySQL 客户端可从 PowerShell 的 `PATH` 调用，然后打开 PowerShell 登录 MySQL：

```powershell
mysql -u root -p
```

输入 root 密码后，执行下面的 SQL。请把密码替换成你自己设置的专用密码，不要把真实密码写入项目文件：

```sql
CREATE DATABASE IF NOT EXISTS offerflow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'offerflow_app'@'localhost'
  IDENTIFIED BY '你的专用数据库密码';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP
  ON offerflow.* TO 'offerflow_app'@'localhost';

FLUSH PRIVILEGES;
EXIT;
```

### 3.2 启动后端

在项目根目录打开第一个 PowerShell 窗口：

```powershell
Set-Location backend

try {
  $env:DB_URL = Read-Host "Target MySQL JDBC URL"
  $env:DB_USERNAME = Read-Host "Target MySQL username"
  $targetPath = ($env:DB_URL -split '\?', 2)[0]
  $targetSchema = [regex]::Match($targetPath, '/([^/]+)$').Groups[1].Value
  Write-Host "Database target: $targetPath (user: $env:DB_USERNAME)"
  if (-not $targetSchema -or (Read-Host "Type schema name '$targetSchema' to confirm") -cne $targetSchema) { throw "Database target not confirmed; backend was not started." }
  $securePassword = Read-Host "OfferFlow database password" -AsSecureString
  $env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password

  mvn `
    "-Dmaven.repo.local=..\work\maven-repository" `
    spring-boot:run
} finally {
  Remove-Item Env:DB_PASSWORD,Env:DB_URL,Env:DB_USERNAME -ErrorAction SilentlyContinue
  Remove-Variable securePassword,targetSchema,targetPath -ErrorAction SilentlyContinue
}
```

命令前台运行时请保留终端窗口；按 `Ctrl+C` 停止后，`finally` 块会清理当前 PowerShell 会话中的连接环境变量。密码只保存在当前进程环境中，不要写入文件或命令参数。若数据库 URL、账号或 schema 无法确认，不要启动后端。默认配置指向 `offerflow`，仅限本机明确准备好的目标库使用。

看到以下信息，说明后端启动成功：

```text
Tomcat started on port 8080
```

后端地址：

- 健康检查：http://127.0.0.1:8080/api/health
- Swagger：http://127.0.0.1:8080/swagger-ui/index.html

后端运行期间不要关闭这个 PowerShell 窗口。

### 3.3 启动前端

在项目根目录打开第二个 PowerShell 窗口：

```powershell
Set-Location frontend
npm run dev -- --host 127.0.0.1 --port 5173
```

打开：

http://127.0.0.1:5173/

## 4. 登录与注册

1. 点击右上角“登录 / 注册”。
2. 第一次使用选择“创建新账号”。
3. 用户名至少 3 位，密码至少 8 位，并填写有效邮箱。
4. 注册成功后会自动登录。
5. 登录且数据同步成功后，页面顶部状态会显示“已连接”；后端不可用或凭据失效时会显示连接错误提示。
6. 点击用户名按钮可以退出登录。

登录凭据只保存在当前浏览器会话中，不会写入项目文件。关闭浏览器或退出登录后，需要重新登录。

## 5. 求职看板

看板包含六个招聘阶段：

- 待投递
- 已投递
- 笔试
- 面试中
- Offer
- 已拒绝

### 新增投递

1. 点击“新增投递”。
2. 填写公司名称和岗位名称。
3. 选择城市、当前阶段和投递来源。
4. 可在备注中记录下一步行动。
5. 点击“保存记录”。

登录并连接后端时，数据会保存到当前显式配置的数据源；未登录时，页面使用演示数据模式，演示记录不会写入数据库。

### 编辑、推进和删除

每张投递卡片右上角提供以下操作：

- 编辑：修改公司、岗位、阶段和备注
- 箭头：推进到下一个招聘阶段
- `×`：标记为已拒绝
- 删除：删除投递及其关联的面试复盘

删除操作不可恢复，系统会在删除前弹出确认提示。

### 搜索和筛选

- 搜索框支持公司、岗位和城市关键词。
- 阶段按钮可以快速查看某一招聘阶段的记录。
- “今日行动”区域可以快速筛选待投递、面试待复盘和 Offer 待跟进记录。

## 6. 面试复盘

在投递卡片底部点击“复盘”：

1. 点击“新增复盘”。
2. 填写面试轮次、面试时间和面试结果。
3. 记录面试问题。
4. 记录本轮表现、问题和下次改进方法。
5. 点击“保存复盘”。

已有复盘可以编辑或删除。面试复盘与对应投递绑定，删除投递时会一并删除关联复盘。

## 7. 数据统计

顶部统计区域展示：

- 累计投递：不包括“待投递”记录
- 面试进行中：当前处于面试阶段的记录数量
- 已获 Offer：当前处于 Offer 阶段的记录数量
- Offer 转化率：Offer 数量除以已投递记录数量

登录后统计来自后端数据库；未登录时显示示例数据，仅用于预览页面效果。

## 8. 演示模式

如果后端没有启动，前端仍然可以访问：

http://127.0.0.1:5173/

页面会显示“演示模式”和示例记录。演示模式适合查看界面，但新增、编辑和面试复盘不会保存到数据库。要保存真实数据，必须启动 MySQL、后端并完成登录。

## 9. 常见问题

### 页面提示无法访问网站

确认对应服务已经启动：

```powershell
Invoke-WebRequest http://127.0.0.1:5173/
Invoke-WebRequest http://127.0.0.1:8080/api/health
```

前端正常时第一条应返回 `200`；后端正常时第二条应返回 `200` 和 JSON。

### 后端提示 `using password: NO`

说明当前 PowerShell 没有设置 `DB_PASSWORD`。重新执行后端启动命令，并在提示时输入 `offerflow_app` 的数据库密码。

### 后端提示 `Access denied for user 'offerflow_app'`

可能原因：

- 密码输入错误
- 用户没有创建成功
- MySQL 服务没有运行

可以先确认服务：

```powershell
Get-Service MySQL80
```

### 页面显示“演示模式”

这表示前端没有成功访问后端。请检查后端是否监听 `8080`，并确认浏览器访问的是 `http://127.0.0.1:5173/`。

### 端口被占用

查看端口：

```powershell
Get-NetTCPConnection -State Listen | Where-Object LocalPort -in 5173,8080
```

前端可以换端口启动，但需要同步修改前端 API 地址；后端默认使用 `8080`，也可以通过 `SERVER_PORT` 环境变量修改。

## 10. 项目目录

```text
co-d/
├─ frontend/       Vue 前端
├─ backend/        Spring Boot 后端
├─ outputs/        项目策划书和使用说明
├─ README.md       开发者说明
└─ work/           本地缓存和运行日志，不提交到 Git
```

## 11. 停止服务

在运行前端或后端的 PowerShell 窗口按：

```text
Ctrl + C
```

即可停止对应服务。MySQL 是 Windows 服务，不需要为了停止前后端而关闭。
