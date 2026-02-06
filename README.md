# EasyLog

EasyLog 是一个轻量级、基于注解的操作日志 SDK，面向 Spring Boot。它在方法执行时捕获调用信息，使用 SpEL 与自定义函数渲染可读文案，并通过可插拔存储接口落地结构化日志数据。

## 特性
- 注解驱动（`@EasyLog`，可通过 `@EasyLogs` 重复声明）。
- SpEL 模板渲染（方法参数、返回值、异常信息、Spring Bean 调用）。
- 自定义函数，支持“前置执行”（用于查询旧值）。
- 支持 diffKey 结构化差异对比（DiffDTO，字段级别名/忽略）。
- 上下文变量 `EasyLogContext`（用于方法参数之外的数据）。
- 可插拔的操作者/平台服务与日志存储服务。
- 事务场景可选“提交后记录”。

## 安装
Maven：

```xml
<dependency>
  <groupId>com.github</groupId>
  <artifactId>easy-log</artifactId>
  <version>1.0.0-20260204-SNAPSHOT</version>
</dependency>
```

如果项目还未启用 AOP，请添加：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

## 快速开始

```java
import com.github.easylog.annotation.EasyLog;
import com.github.easylog.constants.OperateType;
import com.github.easylog.context.EasyLogContext;

@EasyLog(
    module = "order",
    type = OperateType.UPDATE,
    bizNo = "{{#request.orderNo}}",
    success = "update address from ${} to ${}",
    successParamList = {"{{#oldAddr}}", "{{#request.address}}"},
    diffKey = "{{DIFF(#oldObj,#newObj)}}"
)
public void updateAddress(UpdateRequest request) {
    EasyLogContext.put("oldAddr", queryOldAddress(request.getOrderNo()));
    EasyLogContext.put("oldObj", queryOldAddress(request.getOrderNo()));
    EasyLogContext.put("newObj", request);
    // 业务逻辑...
}
```

### 模板语法
EasyLog 支持两类表达式 + 顺序占位：

1. **SpEL 块**：`{{ ... }}`
   - 示例：`"hello {{#request.operator}}"`
   - 可访问方法参数、上下文变量，以及内置变量：
     - `#_result`：方法返回值
     - `#_errMsg`：异常信息
   - 支持调用 Spring Bean：`{{@labelService.label(#request.labelId)}}`

2. **纯表达式**：`#arg`、`@bean.method(..)`、`T(Class).method(..)`
   - 当模板本身就是表达式时，直接求值。

### 占位符参数
`success` / `fail` 模板支持顺序占位符 `${}`，
通过 `successParamList` / `failParamList` 传值（每个参数本身也是模板，会先渲染）。

```java
@EasyLog(
    success = "create ${} at ${}",
    successParamList = {"{{#request.operator}}", "{{#request.address}}"}
)
```

### 条件记录
使用 `condition` 控制是否记录：

```java
@EasyLog(
    success = "ok",
    condition = "{{#request.enabled}}"
)
```

### diffKey 差异对比
使用 `diffKey` 生成结构化差异（DiffDTO），支持固定 key 或表达式：

```java
@EasyLog(
    diffKey = "{{DIFF(#oldObj,#newObj)}}",
    success = "diff"
)
```

Diff 结果会落在 `EasyLogInfo.diffDTO`，业务系统可自行拼接文案。

字段别名/忽略可通过注解控制：

```java
@EasyLogDiffObject(alias = "user")
class User {
    @EasyLogDiffField(alias = "姓名")
    private String name;

    @EasyLogDiffField(alias = "年龄")
    private Integer age;
}
```

### 一个方法记录多条日志

```java
@EasyLogs({
    @EasyLog(module = "user", type = "CREATE", bizNo = "{{#req.id}}", success = "user-log"),
    @EasyLog(module = "audit", type = "TRACE", bizNo = "{{#req.id}}", success = "audit {{#req.operator}}")
})
public void multi(Request req) {
}
```

## 自定义扩展

### 操作者与平台
提供你自己的 `IOperatorService`：

```java
@Bean
public IOperatorService operatorService() {
    return new IOperatorService() {
        public String getOperator() { return currentUser(); }
        public String getPlatform() { return "order-service"; }
    };
}
```

### 日志落地
提供你自己的 `ILogRecordService`：

```java
@Bean
public ILogRecordService logRecordService() {
    return log -> saveToDatabase(log);
}
```

### 内置 DIFF

Diff 使用内置函数 `DIFF`：

```java
@EasyLog(
    diffKey = "{{DIFF(#oldObj,#newObj)}}"
)
```

如需“旧值”，请在业务内先写入 `EasyLogContext`，再在模板中通过 `{{#oldValue}}` 引用。

## 依赖说明

- 如需统一日志体系，可自行实现 `ILogRecordService`，或在应用侧配置 JUL 桥接。
- `spring-web` 与 `javax.servlet-api` 为可选依赖；未引入时将无法获取请求 IP/URL 等 Web 上下文信息。

## 配置

```properties
# 是否启用 easylog（默认 true）
easylog.enable=true

# 平台名（默认 spring.application.name 或 "unknown"）
easylog.platform=order-service

# 存储类型标签（默认 log，仅作标识；自定义存储请覆盖 ILogRecordService）
easylog.store=log

# 若存在事务，是否在事务提交后再记录（默认 false）
easylog.after-commit=false

# Diff时是否忽略旧对象为null的字段（默认 false）
easylog.diff-ignore-old-object-null-value=false

# Diff时是否忽略新对象为null的字段（默认 false）
easylog.diff-ignore-new-object-null-value=false
```

## 说明
- `EasyLogContext` 为线程本地变量，不会在异步/线程池间自动传递；如需使用请显式传递。
- 默认存储使用 JUL 的 `Logger` 以 INFO 级别输出 JSON；如需写入数据库/ES 等，请实现 `ILogRecordService`。

## License
内部使用，可按需补充 License 信息。
