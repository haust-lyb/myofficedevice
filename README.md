# NetDesk · My Office Device

[中文](#中文说明) | [English](#english)

NetDesk is a self-hosted workspace for documenting office network assets, topology, and service access information.

---

## 中文说明

### 项目简介

NetDesk（My Office Device）是一个轻量级、自托管的办公室网络资产管理工具。它通过可视化拓扑图集中记录网络设备、服务器、虚拟机、终端设备及其 Web 服务入口，方便团队快速了解网络结构并查找运维信息。

### 主要功能

- 可视化编辑网络拓扑，支持拖拽设备、连接节点和调整布局
- 支持公网、路由器、交换机、服务器、虚拟机、台式机和笔记本等设备类型
- 记录设备名称、在线状态、IP、网络配置、操作系统和备注
- 记录虚拟机宿主机、虚拟化平台、CPU、内存和磁盘信息
- 为设备维护 Web 服务地址、分类、账号和密码
- 按设备类型筛选，并搜索设备、IP、服务或账号
- 查看模式与编辑模式分离，编辑内容自动保存
- 管理员登录鉴权，会话默认有效期为 12 小时
- 拓扑及服务凭据使用 AES-GCM 加密后存入 SQLite
- 前后端一体化 Docker 镜像，数据卷持久化

### 技术栈

- 前端：Vue 3、Vite、Vue Router、Vue Flow、Axios
- 后端：Java 17、Spring Boot 3、Spring Data JPA
- 数据库：SQLite
- 部署：Docker、Docker Compose

### 使用 Docker Compose（推荐）

1. 构建本地镜像：

```bash
docker build -t myofficedevice:local .
```

2. 在项目根目录创建 `.env`：

```dotenv
NETDESK_IMAGE=myofficedevice:local
NETDESK_PORT=8765
NETDESK_DATA_SECRET=请替换为一段足够长且随机的密钥
NETDESK_ADMIN_USERNAME=admin
NETDESK_ADMIN_PASSWORD=请替换为高强度密码
NETDESK_ADMIN_DISPLAY_NAME=管理员
```

3. 启动服务：

```bash
docker compose up -d
```

4. 浏览器访问 [http://localhost:8765/mod/](http://localhost:8765/mod/)，使用 `.env` 中配置的管理员账号登录。

停止服务：

```bash
docker compose down
```

SQLite 数据保存在 Compose 管理的 `myofficedevice-data` 数据卷中。普通的 `docker compose down` 不会删除该数据卷。

> 请妥善保存 `NETDESK_DATA_SECRET`。已有数据必须使用原密钥解密；丢失或更换密钥会导致原拓扑数据无法读取。

### 本地开发

环境要求：JDK 17、Maven 3.9+、Node.js 22+、npm。

启动后端：

```bash
cd server
NETDESK_DATA_SECRET=development-secret \
NETDESK_ADMIN_PASSWORD=admin123 \
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8765/mod`。

另开一个终端启动前端：

```bash
cd web
npm ci
npm run dev
```

访问 [http://localhost:3000/mod/](http://localhost:3000/mod/)。Vite 会将 `/mod` 请求代理到本地后端。

用于开发的默认用户名是 `admin`。如果未通过环境变量设置密码，后端默认密码为 `admin123`；该默认值仅适合本地开发，生产环境请务必覆盖。

### 构建

单独构建前端：

```bash
cd web
npm ci
npm run build
```

构建后端 JAR：

```bash
cd server
mvn package
```

Dockerfile 会先构建前端，将产物复制到 Spring Boot 的静态资源目录，再生成可直接运行的一体化镜像。

### 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `NETDESK_PORT` | `8765` | Docker Compose 对外暴露的端口 |
| `NETDESK_DATA_DIR` | 当前工作目录 | SQLite 数据文件目录；容器内固定为 `/app/data` |
| `NETDESK_DATA_SECRET` | 开发用默认值 | 拓扑数据加密密钥，生产环境必须设置并长期保管 |
| `NETDESK_ADMIN_USERNAME` | `admin` | 首次启动时创建的管理员用户名 |
| `NETDESK_ADMIN_PASSWORD` | `admin123` | 首次启动时创建的管理员密码，生产环境必须修改 |
| `NETDESK_ADMIN_DISPLAY_NAME` | `管理员` | 管理员显示名称 |
| `VITE_API_TARGET` | `http://localhost:8765` | 前端开发服务器的 API 代理目标 |

管理员账号只会在数据库中不存在同名用户时创建，因此数据库初始化后修改管理员环境变量不会自动更新已有账号。

### 项目结构

```text
.
├── web/          # Vue 3 前端
├── server/       # Spring Boot 后端
├── Dockerfile    # 多阶段一体化镜像构建
└── compose.yml   # Docker Compose 部署配置
```

---

## English

### Overview

NetDesk (My Office Device) is a lightweight, self-hosted office network asset manager. It provides a visual topology for documenting network hardware, servers, virtual machines, workstations, and their web service entry points in one place.

### Features

- Visual topology editing with draggable devices, links, and layouts
- Internet, router, switch, server, virtual machine, desktop, and laptop nodes
- Device status, IP configuration, operating system, and notes
- VM host, virtualization platform, CPU, memory, and disk details
- Web service URLs, categories, usernames, and passwords attached to devices
- Device filters and search across devices, IPs, services, and accounts
- Separate view/edit modes with automatic persistence
- Administrator authentication with 12-hour sessions by default
- AES-GCM-encrypted topology and credentials stored in SQLite
- Combined frontend/backend Docker image with persistent storage

### Technology

- Frontend: Vue 3, Vite, Vue Router, Vue Flow, Axios
- Backend: Java 17, Spring Boot 3, Spring Data JPA
- Database: SQLite
- Deployment: Docker and Docker Compose

### Run with Docker Compose (recommended)

1. Build the image locally:

```bash
docker build -t myofficedevice:local .
```

2. Create a `.env` file in the project root:

```dotenv
NETDESK_IMAGE=myofficedevice:local
NETDESK_PORT=8765
NETDESK_DATA_SECRET=replace-with-a-long-random-secret
NETDESK_ADMIN_USERNAME=admin
NETDESK_ADMIN_PASSWORD=replace-with-a-strong-password
NETDESK_ADMIN_DISPLAY_NAME=Administrator
```

3. Start the application:

```bash
docker compose up -d
```

4. Open [http://localhost:8765/mod/](http://localhost:8765/mod/) and sign in with the administrator credentials from `.env`.

Stop the application with:

```bash
docker compose down
```

SQLite data is persisted in the Compose-managed `myofficedevice-data` volume. A regular `docker compose down` does not remove this volume.

> Keep `NETDESK_DATA_SECRET` safe and stable. Existing data can only be decrypted with the original secret; losing or changing it makes the stored topology unreadable.

### Local development

Requirements: JDK 17, Maven 3.9+, Node.js 22+, and npm.

Start the backend:

```bash
cd server
NETDESK_DATA_SECRET=development-secret \
NETDESK_ADMIN_PASSWORD=admin123 \
mvn spring-boot:run
```

The backend runs at `http://localhost:8765/mod` by default.

In another terminal, start the frontend:

```bash
cd web
npm ci
npm run dev
```

Open [http://localhost:3000/mod/](http://localhost:3000/mod/). Vite proxies `/mod` requests to the local backend.

The development username defaults to `admin`. If no password environment variable is provided, the backend defaults to `admin123`. Never use this default in production.

### Build

Build the frontend:

```bash
cd web
npm ci
npm run build
```

Build the backend JAR:

```bash
cd server
mvn package
```

The Dockerfile builds the frontend first, copies it into Spring Boot's static resources, and then creates a single runnable image.

### Configuration

| Environment variable | Default | Description |
| --- | --- | --- |
| `NETDESK_PORT` | `8765` | Port exposed by Docker Compose |
| `NETDESK_DATA_DIR` | Current working directory | SQLite data directory; fixed to `/app/data` in the container |
| `NETDESK_DATA_SECRET` | Development fallback | Topology encryption secret; must be set and retained in production |
| `NETDESK_ADMIN_USERNAME` | `admin` | Administrator username created on first startup |
| `NETDESK_ADMIN_PASSWORD` | `admin123` | Administrator password created on first startup; must be changed in production |
| `NETDESK_ADMIN_DISPLAY_NAME` | `管理员` | Administrator display name |
| `VITE_API_TARGET` | `http://localhost:8765` | API proxy target used by the frontend dev server |

The administrator is only created when no user with the configured username exists. Changing these environment variables after database initialization does not update an existing account automatically.

### Project structure

```text
.
├── web/          # Vue 3 frontend
├── server/       # Spring Boot backend
├── Dockerfile    # Multi-stage combined image build
└── compose.yml   # Docker Compose deployment
```
