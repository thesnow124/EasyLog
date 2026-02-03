# EasyLog SDK (Spring Boot 2.5 / Java 8)

面向业务项目的轻量级操作日志 SDK，提供注解式埋点、模板渲染、字段差异对比等能力，当前基于 Java 8 与 Spring Boot 2.5（javax 命名空间）。

**核心能力**
- 操作日志：成功/失败模板、参数占位符、条件记录
- 详情变更：支持 JSON 字段差异比对（新增/修改/删除）
- 表达式解析：SpEL（支持调用 Spring Bean 方法）、方法参数、方法返回值/异常信息
- 最小依赖：作为 SDK 使用，尽量减少对业务的传递依赖

参考文章：https://tech.meituan.com/2021/09/16/operational-logbook.html

---

项目规划文档：`docs/PROJECT_PLAN.md`

## 要求
- Java: 8+
- Spring Boot: 2.5.x+（javax 命名空间）
- 不兼容 Spring Boot 3.x（Jakarta 命名空间）。如需 3.x，请使用对应版本。

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
- 必需：`spring-boot-autoconfigure`、`spring-aop`、`spring-tx`、`aspectjrt`（编译）、`aspectjweaver`（运行时）、`commons-lang3`、`fastjson2`、`javers-core`
- 可选/不传递：`spring-web`（用于 `RequestContextHolder`）、`javax.servlet-api`（scope=provided）、`javax.annotation-api`、`slf4j-api`
- 仅编译期：`lombok`（scope=provided）

注意：
- Web 项目通常已有 `spring-boot-starter-web`，无需显式添加 `javax.servlet-api`；非 Web 项目也可正常使用（无请求上下文时 IP/URL/HTTP 方法为空）。
- 如需进一步精简，可由业务侧自行提供 `aspectjweaver`，此时可将其从 SDK 移除或改为 optional。

## 快速开始
1) 可选配置（默认已开启）：

```properties
easylog.enable=true       # 是否启用（默认 true）
easylog.after-commit=false # 是否在事务提交后再落地（默认 false）
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
  before = "{{ @easyLogFunctions.loadOldJson(#userDto.id) }}",
  after  = "{{#_result}}",
  extra  = "备注：{{#userDto.remark}}"
)
public User update(UserDto userDto) { ... }
```

多日志示例：

```java
@EasyLogs({
  @EasyLog(module = "用户管理", type = "UPDATE",
           success = "测试多个日志-1： ${} 这是后缀${}",
           successParamList = {"{{ @easyLogFunctions.getBeforeName(#p0) }}"}),
  @EasyLog(module = "用户管理", type = "READ",
           success = "测试多个日志-2： {{ @easyLogFunctions.getBeforeName(#p0) }}")
})
public void manyLog(String name) { ... }
```

## 模板与表达式说明
- 直接嵌入（SpEL）：`{{...}}`
  - 方法参数：`{{#userDto.name}}`，或 `{{#p0}}`/`{{#a0}}`
  - 返回结果：`{{#_result}}`
  - 异常信息：`{{#_errMsg}}`
  - 调用 Bean 方法：`{{ @beanName.method(...) }}`（以 Bean 函数替代自定义 DSL）
- 自定义函数 DSL：`{funcName{SpEL}}`
  - 实现 `ParseFunction` 并注册为 Spring Bean
  - 可通过 `executeBefore()` 在方法执行前获取旧值
- 变更快照：
  - `before`/`after` 用于字段差异对比
  - `extra` 用于扩展信息（不参与 diff）
- 内容占位符：`${}`
  - 用于 `success`/`fail` 文本中顺序占位；对应参数从 `successParamList`/`failParamList` 解析后按顺序替换
  - 示例：`success = "用户：${} 已被禁用，原因：${}"`

## 用法清单与示例
- SpEL 基础：
  - `{{ #p0 }}` / `{{ #userDto.name }}` / `{{ #_result }}` / `{{ #_errMsg }}`
- 调用 Bean 方法：
  - `{{ @easyLogFunctions.userLabel(#p0) }}`
  - 旧值（前置执行）：`{{ @easyLogFunctions.loadOldJson(#id) }}`
  - 新值（后置执行，依赖结果）：`{{ @easyLogFunctions.buildNew(#_result) }}`
- 自定义函数 DSL：
  - `success = "修改配送员：从 {userLabel{#oldId}} 改为 {userLabel{#newId}}"`（`executeBefore=true` 时可取旧值）
- 变量上下文（在方法内设置临时变量供模板使用）：
  - 代码：`EasyLogContext.put("oldAddress", oldAddress);`
  - 模板：`"从 {{#oldAddress}} 改为 {{#_result.address}}"`
- 失败日志：
  - `fail = "操作失败：{{#_errMsg}}"`
- 变更差异：
  - `before = "{{ @easyLogFunctions.loadOldJson(#id) }}"`，`after = "{{ #_result }}"`
- 条件记录：
  - `condition = "{{ #p0 != null }}"`
- 文本占位与顺序参数：
  - `success = "用户：${} 被禁用，原因：${}"`
  - `successParamList = {"{{ #user.name }}", "{{ #reason }}"}`

## 自动装配与覆盖
本 SDK 提供自动装配：
- `EasyLogAutoConfiguration` 受 `easylog.enable` 控制（默认开启）
- 若业务侧提供同名 Bean，则自动替换默认实现：
  - `IOperatorService`（操作者/平台）
  - `ILogRecordService`（日志落地），业务侧实现即可覆盖默认日志输出

## 迁移说明
- 运行环境：JDK 8+ 与 Spring Boot 2.5.x（javax 命名空间）
- 若从 Boot 3/Jakarta 回退：包名 `jakarta.*` → `javax.*`
  - 例如：`jakarta.annotation.PostConstruct` → `javax.annotation.PostConstruct`
           `jakarta.servlet.http.HttpServletRequest` → `javax.servlet.http.HttpServletRequest`
- 自定义函数 DSL 仍支持：`{funcName{SpEL}}`；也可使用 `{{ @beanName.method(SpEL) }}` 调用 Spring Bean
- `detail` 已移除：请使用 `before`/`after` 生成字段差异，扩展信息放入 `extra`
- Spring Boot 3.x 需要 Jakarta 版本，不与当前版本兼容

## 构建与测试
使用 Maven Wrapper：

```bash
chmod +x mvnw
./mvnw clean package
./mvnw clean test
```

- 若需自定义存储（DB/ES/MQ），实现并注入 `ILogRecordService` 即可覆盖默认行为。

---

如需更进一步的最小依赖或与现有监控/日志系统集成（如 DB/ES/MQ），可实现并注入上述扩展接口，我可以协助补充示例与脚手架。
