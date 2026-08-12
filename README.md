# OfficeMesh

> 面向办公室、小型团队与实验室的自托管网络资产台。

OfficeMesh 用一张可编辑的拓扑图，集中管理网络设备、服务器、虚拟机、终端与 Web 服务入口。设备信息、访问地址和服务凭据保存在自己的服务器上，适合用于记录和交接日常运维环境。

<img width="3024" height="1658" alt="image" src="https://github.com/user-attachments/assets/61d73cef-4f61-44ad-ba58-b282bdf1f2d4" />


## 功能一览

- **可视化拓扑**：拖放公网、路由器、交换机、服务器、虚拟机、台式机和笔记本；支持连线、删除、缩放、小地图与自动整理布局。
- **设备档案**：记录设备名称、状态、DHCP / 固定 IP、操作系统、备注；虚拟机还可记录宿主机、虚拟化平台、CPU、内存和磁盘。
- **服务入口**：为每台设备维护 Web 服务地址、分类、账号和密码；支持打开链接、显示或复制凭据。
- **检索与概览**：按设备类型筛选，搜索设备名称、IP、备注、操作系统、服务名和账号，并显示在线设备统计。
- **编辑保护**：浏览与编辑模式分离；编辑时自动保存。多人同时修改同一拓扑时会检测版本冲突，避免静默覆盖。
- **用户与权限**：超级管理员可管理账号、查看登录记录和导入导出数据；管理员可编辑拓扑；普通用户仅可查看。
- **安全存储**：管理员密码使用 BCrypt 哈希；拓扑、服务地址及凭据使用 AES-GCM 加密后保存至 SQLite。
- **备份恢复**：超级管理员可导出完整 JSON 备份，也可导入 OfficeMesh 备份或包含 `nodes`、`edges` 的拓扑 JSON。
- **登录防护**：登录会记录结果、来源 IP 和 User-Agent；密码连续错误 5 次会锁定账号 10 分钟。

## 快速开始

推荐使用 Docker Compose 部署。需要 Docker Engine 与 Docker Compose 插件。

```bash
git clone <仓库地址>
cd myofficedevice
cp .env.example .env
```

编辑 `.env`，至少替换以下两项为安全且长期保存的值：

```dotenv
NETDESK_DATA_SECRET=请填写一段长随机密钥
NETDESK_ADMIN_PASSWORD=请填写高强度管理员密码
```

若使用本地源码构建镜像：

```bash
docker build -t myofficedevice:local .
```

并在 `.env` 中设置：

```dotenv
NETDESK_IMAGE=myofficedevice:local
```

启动服务：

```bash
docker compose up -d
```

打开 [http://localhost:8765/mod/](http://localhost:8765/mod/)，使用 `.env` 中的管理员账号登录。默认用户名是 `admin`。

常用运维命令：

```bash
# 查看运行状态与日志
docker compose ps
docker compose logs -f myofficedevice

# 更新镜像后重新创建服务
docker compose pull
docker compose up -d

# 停止服务（不会删除数据）
docker compose down
```

## 首次使用

1. 以初始管理员登录，进入拓扑页面。
2. 打开左侧的“启用编辑”，将设备拖入画布或通过“添加新设备”创建资产。
3. 连接设备并按需点击“自动整理”。
4. 选中设备，填写网络与系统信息，并添加服务入口及凭据。
5. 关闭编辑模式前会完成一次保存；状态栏会显示保存结果。
6. 在账户菜单进入系统设置，可创建用户、查看登录记录或导出拓扑备份。

## 角色权限

| 角色 | 浏览拓扑与服务 | 编辑拓扑 | 用户管理、登录记录、备份导入导出 |
| --- | :---: | :---: | :---: |
| `USER` 普通用户 | ✓ | — | — |
| `ADMIN` 管理员 | ✓ | ✓ | — |
| `SUPER_ADMIN` 超级管理员 | ✓ | ✓ | ✓ |

首次启动时会创建由 `NETDESK_ADMIN_*` 配置指定的超级管理员。数据库中存在同名账号后，再修改这些环境变量**不会**修改已创建账号；请在系统设置中重置密码或调整账号。

## 配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `NETDESK_IMAGE` | `ghcr.io/your-github-name/myofficedevice:latest` | Compose 使用的镜像地址。仅 Compose 使用。 |
| `NETDESK_PORT` | `8765` | 对外暴露的端口。 |
| `NETDESK_DATA_DIR` | 当前工作目录 | SQLite 数据库目录；容器内固定为 `/app/data`。 |
| `NETDESK_DATA_SECRET` | 开发默认值 | AES-GCM 加密密钥。生产环境必须设置为随机密钥并保持不变。 |
| `NETDESK_ADMIN_USERNAME` | `admin` | 首次启动时创建的超级管理员用户名。 |
| `NETDESK_ADMIN_PASSWORD` | `admin123` | 首次启动时创建的超级管理员密码。生产环境必须覆盖。 |
| `NETDESK_ADMIN_DISPLAY_NAME` | `管理员` | 首次启动时创建的超级管理员显示名称。 |
| `VITE_API_TARGET` | `http://localhost:8765` | 前端开发服务器的 API 代理地址。仅本地开发使用。 |

可用以下命令生成密钥：

```bash
openssl rand -hex 32
```

> **请妥善保存 `NETDESK_DATA_SECRET`。** 数据库中的拓扑内容需要原密钥解密；丢失或更换密钥后，已有内容将无法读取。导出的 JSON 备份含有服务凭据明文，也应按敏感文件保存。

## 数据与备份

Docker Compose 将 SQLite 数据保存在命名卷 `myofficedevice-data`。`docker compose down` 不会删除数据；请勿执行 `docker compose down -v`，除非确认要删除全部数据库。

建议的备份方式：

1. 以超级管理员身份进入“系统设置 → 数据备份”。
2. 导出当前拓扑 JSON 并保存在受保护的位置。
3. 同时安全保存当前 `NETDESK_DATA_SECRET` 与 `.env` 配置。

恢复时，从同一页面选择 JSON 文件导入。导入会覆盖服务器上的当前拓扑，建议先导出一份现有数据。

更多镜像发布、服务器部署和卷备份说明见 [DEPLOYMENT.md](DEPLOYMENT.md)。

## 本地开发

环境要求：JDK 17、Maven 3.9+、Node.js 22+ 与 npm。

启动后端：

```bash
cd server
NETDESK_DATA_SECRET=development-secret \
NETDESK_ADMIN_PASSWORD=admin123 \
mvn spring-boot:run
```

后端默认地址为 `http://localhost:8765/mod`。

在另一个终端启动前端：

```bash
cd web
npm install --no-package-lock
npm run dev
```

访问 [http://localhost:3000/mod/](http://localhost:3000/mod/)。Vite 会将 `/mod` 请求代理至本地后端。

构建生产产物：

```bash
# 前端
cd web && npm install --no-package-lock && npm run build

# 后端
cd server && mvn package
```

Dockerfile 会先构建前端，再将静态文件打包进 Spring Boot 应用，最终生成单一可运行镜像。

## 技术栈与目录

- 前端：Vue 3、Vite、Vue Router、Vue Flow、Dagre、Axios
- 后端：Java 17、Spring Boot 3、Spring Data JPA、Spring Security Crypto
- 存储：SQLite
- 部署：Docker、Docker Compose

```text
.
├── web/                 # Vue 前端
├── server/              # Spring Boot API 与 SQLite 持久化
├── Dockerfile           # 前后端一体化镜像构建
├── compose.yml          # Docker Compose 配置
├── .env.example         # 部署配置模板
└── DEPLOYMENT.md        # 发布、部署与备份细节
```
