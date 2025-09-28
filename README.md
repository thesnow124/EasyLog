# EasyLog SDK (Spring Boot 3 / Java 17)

面向业务项目的轻量级操作日志 SDK，提供注解式埋点、模板渲染、字段差异对比等能力，已升级至 Java 17 与 Spring Boot 3（Jakarta 命名空间）。

**核心能力**
- 操作日志：成功/失败模板、参数占位符、条件记录
- 详情变更：支持 JSON 字段差异比对（新增/修改/删除）
- 表达式解析：SpEL、方法参数、方法返回值/异常信息
- 自定义函数：支持前置/环绕函数，拼装更灵活的内容
- 最小依赖：作为 SDK 使用，尽量减少对业务的传递依赖

参考文章：https://tech.meituan.com/2021/09/16/operational-logbook.html

---

项目规划文档：`docs/PROJECT_PLAN.md`

## 要求
- Java: 17+
- Spring Boot: 3.x+（Jakarta 命名空间）
- 不兼容 Spring Boot 2.x（javax 命名空间）。如需 2.x，请使用历史版本。

## 安装
在业务项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
  <groupId>com.github</groupId>
  <artifactId>easy-log</artifactId>
  <version>1.0.0</version>
</dependency>
```

发布仓库视你们公司的制品库而定（例如私有 Nexus）。

## 依赖最小化说明
本 SDK 仅引入必要依赖：
- 必需：`spring-boot-autoconfigure`、`spring-aop`、`aspectjrt`（编译）、`aspectjweaver`（运行时）、`commons-lang3`、`guava`、`fastjson`、`javers-core`
- 可选/不传递：`spring-web`（用于 `RequestContextHolder`）、`jakarta.servlet-api`（scope=provided）、`slf4j-api`
- 仅编译期：`lombok`（scope=provided）

注意：
- Web 项目通常已有 `spring-boot-starter-web`，无需显式添加 `jakarta.servlet-api`；非 Web 项目也可正常使用（无请求上下文时 IP/URL/HTTP 方法为空）。
- 如需进一步精简，可由业务侧自行提供 `aspectjweaver`，此时可将其从 SDK 移除或改为 optional。

## 快速开始
1) 可选配置（默认已开启）：

```properties
easylog.enable=true       # 是否启用（默认 true）
easylog.banner=false      # 是否打印 banner（默认 true）
```

2) 提供操作者/平台信息（推荐）：实现 `IOperatorService` 覆盖默认实现

```java
@Component
public class OperatorServiceImpl implements IOperatorService {
  public String getOperator() { return "userId:123"; }
  public String getPlatform() { return "order-service"; }
}
```

3) 可选：落库/发 MQ（替换默认日志输出），实现 `ILogRecordService`

```java
@Component
public class LogRecordServiceImpl implements ILogRecordService {
  public void record(EasyLogInfo log) {
    // TODO: 自定义落库/ES/MQ
  }
}
```

4) 在业务方法上使用注解

```java
@EasyLog(
  module = "用户管理",
  type   = "UPDATE",
  success = "更新了用户信息：{{#userDto.name}}",
  bizNo   = "{{#userDto.id}}",
  detail  = "{{#_result}}"   // 记录返回值，或使用 JSON 数组记录前后对比
)
public User update(UserDto userDto) { ... }
```

多日志示例：

```java
@EasyLogs({
  @EasyLog(module = "用户管理", type = "UPDATE",
           success = "测试多个日志-1： ${} 这是后缀${}",
           successParamList = {"{getBeforeName{#name}}"}),
  @EasyLog(module = "用户管理", type = "READ",
           success = "测试多个日志-2： {getBeforeName{#name}}")
})
public void manyLog(String name) { ... }
```

## 模板与表达式说明
- 直接嵌入（SpEL）：`{{...}}`
  - 方法参数：`{{#userDto.name}}`
  - 返回结果：`{{#_result}}`
  - 异常信息：`{{#_errMsg}}`
  - 支持 `@beanName.method(...)` 形式（SpEL 调用 Bean 方法）

- 自定义函数：`{functionName{SpEL}}`
  - 例如：`{getBeforeName{#name}}`
  - 扩展点：实现 `ICustomFunction`，可声明 `executeBefore()` 与 `executeAround()`（前置/环绕执行）

- 内容占位符：`${}`
  - 用于 `success`/`fail` 文本中顺序占位；对应参数从 `successParamList`/`failParamList` 解析后按顺序替换
  - 示例：`success = "用户：${} 已被禁用，原因：${}"`

- 详情变更（差异对比）
  - 若 `detail` 为 JSON 数组字符串 `[oldJson, newJson]`，将自动计算差异字段列表
  - 若为普通字符串或 JSON 对象，将按原样记录

## 自动装配与覆盖
本 SDK 提供自动装配：
- `EasyLogAutoConfiguration` 受 `easylog.enable` 控制（默认开启）
- 若业务侧提供同名 Bean，则自动替换默认实现：
  - `IOperatorService`（操作者/平台）
  - `ILogRecordService`（日志落地）
  - `ICustomFunction`（自定义函数，按名称注册）

## 迁移说明（2.x → 3.x）
- Java 17+ 与 Spring Boot 3.x（Jakarta）
- 包名迁移：`javax.*` → `jakarta.*`
  - 例如：`javax.annotation.PostConstruct` → `jakarta.annotation.PostConstruct`
           `javax.servlet.http.HttpServletRequest` → `jakarta.servlet.http.HttpServletRequest`
- 已不再兼容 Spring Boot 2.x 项目

## 构建与测试
使用 Maven Wrapper：

```bash
chmod +x mvnw
./mvnw clean package
./mvnw clean test
```

## 数据库存储（可选）
- 若业务引入了 `DataSource` 且未自定义 `ILogRecordService`，SDK 将自动启用 JDBC 版落库实现。
- 建表脚本见：`docs/sql/easy_log_record.sql`
- 若需自定义存储（DB/ES/MQ），实现并注入 `ILogRecordService` 即可覆盖默认行为。

---

如需更进一步的最小依赖或与现有监控/日志系统集成（如 DB/ES/MQ），可实现并注入上述扩展接口，我可以协助补充示例与脚手架。
