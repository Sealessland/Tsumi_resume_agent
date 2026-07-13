# 接入现有 Caddy 的部署方式

从你给出的 Caddyfile 看，当前更适合采用“只部署前端容器，继续复用现有 Caddy 反向代理”的方案。

## 1. 先确认 Caddy 所在的 Docker 网络

执行：

```bash
docker network ls
```

再找到你现在运行 Caddy 容器所连接的网络名称，例如可能是：

- `caddy`
- `caddy_net`
- `proxy`

如果网络名不是 `caddy`，启动时通过环境变量传入即可。

## 2. 启动简历站点容器

项目里已经新增了：

- `docker-compose.existing-caddy.yml`

默认它会把 `resume-app` 接入名为 `caddy` 的外部网络。

如果你的 Caddy 网络就是 `caddy`，直接执行：

```bash
docker compose -f docker-compose.existing-caddy.yml up -d --build
```

如果你的 Caddy 网络不是 `caddy`，例如叫 `proxy`，执行：

```bash
CADDY_DOCKER_NETWORK=proxy docker compose -f docker-compose.existing-caddy.yml up -d --build
```

## 3. 在你现有的 Caddyfile 里追加下面这段

```caddyfile
resume.hatsumi.fun {
    reverse_proxy tsumi-resume-app:80
}
```

这段配置的含义是：

- 域名 `resume.hatsumi.fun`
- 交给现有 Caddy 处理 HTTPS
- 反代到同一 Docker 网络中的 `tsumi-resume-app:80`

## 4. 重载 Caddy

如果你的 Caddy 是 Docker 容器运行的，常见方式是：

```bash
docker exec <你的-caddy-容器名> caddy reload --config /etc/caddy/Caddyfile
```

或者直接重启 Caddy 容器：

```bash
docker restart <你的-caddy-容器名>
```

## 5. 如果你的 Caddy 不在同一个 Docker 网络

那就不要用容器名反代，改成把应用映射到宿主机端口，再让 Caddy 反代宿主机地址。

这种情况下你可以把 `resume-app` 改成：

```yaml
services:
  resume-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: tsumi-resume-app
    restart: unless-stopped
    ports:
      - "127.0.0.1:8080:80"
```

然后 Caddyfile 写成：

```caddyfile
resume.hatsumi.fun {
    reverse_proxy 172.17.0.1:8080
}
```

如果你的 Caddy 是宿主机进程而不是 Docker 容器，也可以写成：

```caddyfile
resume.hatsumi.fun {
    reverse_proxy 127.0.0.1:8080
}
```
