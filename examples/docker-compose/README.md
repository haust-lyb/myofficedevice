# Docker Compose 示例

本示例使用已经构建并发布的 NetDesk 镜像启动服务，SQLite 数据会保存在当前目录下的 `data/` 中。

## 目录结构

```text
docker-compose/
├── docker-compose.yml
├── example.env
└── data/
    └── .gitkeep
```

## 启动

进入本目录，复制并修改环境变量：

```bash
cp example.env .env
```

至少需要修改 `.env` 中的以下两项：

```dotenv
NETDESK_DATA_SECRET='replace-with-a-long-random-secret'
NETDESK_ADMIN_PASSWORD='replace-with-a-strong-password'
```

启动服务：

```bash
docker compose up -d
```

查看状态和日志：

```bash
docker compose ps
docker compose logs -f myofficedevice
```

浏览器访问 <http://localhost:8765/mod/>。

停止服务：

```bash
docker compose down
```

## 注意事项

- `example.env` 只是配置模板；Docker Compose 默认读取的是 `.env`，因此启动前需要复制或重命名。
- 也可以不复制文件，使用 `docker compose --env-file example.env up -d`，但必须先修改示例密码和密钥。
- `./data` 会绑定到容器的 `/app/data`，删除容器不会删除本地数据库。
- `NETDESK_DATA_SECRET` 用于加密和解密拓扑数据。产生数据后请勿丢失或随意更换。
- 管理员只在数据库中不存在时初始化。已有 `data/myofficedevice.db` 时，修改管理员环境变量不会重置现有账号。
