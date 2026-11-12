# octopus-demo-service-user

用户服务（Spring Boot 3.4.5 / JDK 21 / Maven）。

## 本地 MySQL 开发

### 起库

```bash
docker compose up -d
```

首次启动（数据卷为空）时，`src/main/resources/db/schema.sql` 会通过 `/docker-entrypoint-initdb.d/01-schema.sql` 自动建库建表。

### 看健康

```bash
docker compose ps
```

`mysql` 状态变为 `healthy`（healthcheck 使用 `mysqladmin ping`）后即可连接。

### 重置

初始化脚本只在空数据卷时执行，修改 `schema.sql` 后需重建：

```bash
docker compose down -v
docker compose up -d
```

### 环境变量覆盖

所有配置均可用同名环境变量覆盖（默认值见括号）：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MYSQL_ROOT_PASSWORD` | root 密码 | `root` |
| `MYSQL_DB` | 数据库名 | `octopus_demo` |
| `MYSQL_USER` | 应用账号 | `octopus` |
| `MYSQL_PASSWORD` | 应用账号密码 | `octopus` |
| `MYSQL_PORT` | 宿主机端口 | `3306` |
| `MYSQL_HOST` | 应用连接的主机名 | `localhost` |

### JDBC 连接串

```
jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DB:octopus_demo}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
```

### 构建与启动

```bash
# 先安装公共库依赖
mvn -B -f ../octopus-demo-java-common/pom.xml install -DskipTests

# 构建并启动服务
mvn -B clean package
java -jar target/*.jar
```

### 运行测试

```bash
# 先安装公共库依赖
mvn -B -f ../octopus-demo-java-common/pom.xml install -DskipTests

# 全量测试
mvn -B test
```

`MybatisUserDaoTest` 与 `AuthControllerTest` 使用 Testcontainers 自动拉起 `mysql:8.4`（随机端口，建表脚本复用 `db/schema.sql`），需 Docker Desktop 运行中；测试不会连接或写入本机 3306 的 `octopus_demo` 库。
P3 cabin dispatch verified
