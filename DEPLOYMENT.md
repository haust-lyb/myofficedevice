# Docker 镜像发布与部署

## GitHub 自动发布

推送到 `main` 分支后，`.github/workflows/docker-publish.yml` 会自动构建并发布以下平台的镜像：

- `linux/amd64`（Intel / AMD 64 位）
- `linux/arm64`（ARM 64 位）

镜像发布到 GitHub Container Registry：

```text
ghcr.io/<GitHub 用户名>/<仓库名>:latest
```

每次构建还会发布不可变的提交标签 `sha-<短提交号>`。推送 `v*` 格式的 Git 标签（例如 `v1.0.0`）时，也会发布对应版本标签。

## 首次使用 GitHub

1. 在 GitHub 创建仓库，将代码推送到 `main` 分支。
2. 打开仓库的 **Actions** 页面，等待 `Build and publish Docker image` 完成。
3. 在仓库或个人主页的 **Packages** 中找到镜像。
4. 如果需要让所有人免登录拉取，在 Package settings 中将镜像可见性改为 **Public**。

工作流使用 GitHub 自动提供的 `GITHUB_TOKEN`，发布到 GHCR 不需要手动添加密码或 Token。

## 使用 Docker Compose 部署

复制环境变量模板：

```bash
cp .env.example .env
```

编辑 `.env`：

- 将 `NETDESK_IMAGE` 中的用户名和仓库名改成实际地址。
- 为 `NETDESK_DATA_SECRET` 设置一个长期不变的随机密钥。修改该密钥后，已经加密的数据将无法读取。
- 为 `NETDESK_ADMIN_PASSWORD` 设置强密码。

生成随机密钥可以使用：

```bash
openssl rand -hex 32
```

启动服务：

```bash
docker compose pull
docker compose up -d
```

浏览器访问：

```text
http://服务器地址:8765/mod/
```

更新到最新镜像：

```bash
docker compose pull
docker compose up -d
```

查看日志：

```bash
docker compose logs -f myofficedevice
```

## 数据持久化与备份

SQLite 数据库保存在 Compose 命名卷 `myofficedevice-data` 中。重新创建或更新容器不会删除该卷。

不要执行 `docker compose down -v`，除非确定要同时删除数据库。备份时应同时保存数据库卷和当前使用的 `NETDESK_DATA_SECRET`。
