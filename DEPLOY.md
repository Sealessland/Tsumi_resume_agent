# Docker + Caddy 部署说明

本项目是标准的 Vite 静态站点，推荐使用下面这套结构：

- `resume-app` 容器：负责构建并提供静态页面
- `caddy` 容器：负责 HTTPS 证书申请、80/443 监听、反向代理

## 1. 服务器前置条件

部署前请先确认：

- 你的域名 `resume.hatsumi.fun` 已经添加 `A` 记录，指向云服务器公网 IP
- 云服务器安全组 / 防火墙已放行 `80` 和 `443`
- 服务器已安装 Docker 与 Docker Compose Plugin

Ubuntu/Debian 常见安装方式：

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
```

## 2. 上传项目到服务器

例如放到：

```bash
cd /opt
sudo git clone <你的仓库地址> tsumi_resume
cd tsumi_resume
```

如果你是直接上传本地项目文件，也可以，只要最终目录里包含：

- `Dockerfile`
- `docker-compose.yml`
- `Caddyfile`
- `deploy/nginx.conf`

## 3. 修改域名

默认 `docker-compose.yml` 里已经写了：

```yaml
environment:
  DOMAIN: resume.hatsumi.fun
```

如果你后续更换域名，改这里即可。

## 4. 启动服务

在项目根目录执行：

```bash
docker compose up -d --build
```

首次启动时会发生两件事：

- `resume-app` 镜像会先执行 `npm ci` 和 `npm run build`
- `caddy` 会在首次收到公网访问时自动申请 TLS 证书

## 5. 查看运行状态

查看容器：

```bash
docker compose ps
```

查看 Caddy 日志：

```bash
docker compose logs -f caddy
```

查看前端容器日志：

```bash
docker compose logs -f resume-app
```

如果 DNS、80/443 端口都正确，浏览器访问：

```text
https://resume.hatsumi.fun
```

就可以直接打开页面。

## 6. 后续更新

以后项目有改动，服务器上更新步骤就是：

```bash
cd /opt/tsumi_resume
git pull
docker compose up -d --build
```

## 7. 常见排查

### 证书没有签发

通常检查下面几项：

- 域名是否已经真正解析到当前服务器
- `80` / `443` 是否被云厂商安全组拦截
- 服务器上是否已有别的程序占用了 `80` / `443`

可以执行：

```bash
sudo ss -lntp | grep -E ':80|:443'
```

### 页面打不开，但容器正常

执行：

```bash
docker compose ps
docker compose logs --tail=100 caddy
docker compose logs --tail=100 resume-app
```

重点看：

- `caddy` 是否成功监听 `:80` / `:443`
- `resume-app` 是否正常启动
- `resume-app` 是否能被 `caddy` 通过容器名 `resume-app:80` 访问

## 8. 如果你已经在宿主机安装了 Caddy

如果你的服务器已经有一个“宿主机上的 Caddy”统一管理多个站点，那么可以不启动 `docker-compose.yml` 里的 `caddy` 服务，只启动前端容器，并把它映射到本机回环地址。

你可以把 `resume-app` 改成：

```yaml
resume-app:
  build:
    context: .
    dockerfile: Dockerfile
  container_name: tsumi-resume-app
  restart: unless-stopped
  ports:
    - "127.0.0.1:8080:80"
```

然后宿主机上的 Caddy 配置写成：

```caddyfile
resume.hatsumi.fun {
  encode zstd gzip
  reverse_proxy 127.0.0.1:8080
}
```

这样也完全可用，而且适合一台服务器托管多个站点。
