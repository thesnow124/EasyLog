# easy-log 项目规划（基于 Spring Boot 3 / Java 17）

本文档基于“如何优雅地记录操作日志”的思路整理，作为 easy-log SDK 的目标、设计与路线图说明，便于快速对齐项目方向与落地细节。

## 1. 愿景
- 提供轻量、低侵入、可扩展的“操作日志”SDK，解耦业务与日志记录，让日志文本可读、可配、可回溯。
- 通过注解+AOP+模板解析（SpEL 与自定义函数）自动生成操作日志，覆盖常见新增/修改/批量变更场景。
- 最小依赖作为“业务 SDK”分发，业务仅按需引入 Web/Servlet 等运行时能力。

## 2. 背景与问题
- 系统日志偏向问题排查，面向开发；操作日志面向用户/运营，需要“人能看懂”。
- 传统写法（直接 `log.info` 或手工拼接模板）易与业务耦合、分散重复、难统一。
- Canal 等基于 DB Binlog 的方式与业务逻辑解耦，但只能覆盖本库表改动，跨服务/外部系统变更无法感知。

easy-log 选择 AOP 注解方式，在方法调用维度收集上下文并拼装“人读得懂”的操作日志。

## 3. 目标与非目标
- 目标
  - 注解式埋点，模板化日志（支持成功/失败版本）。
  - 动态模板：SpEL 取参、返回值、异常信息，自定义函数增强文案。
  - 变更详情：对 JSON old/new 自动生成字段差异列表。
  - 可扩展：操作人/平台、日志落地、自定义函数均可替换。
  - 最小依赖：SDK 不强加 web 容器或 starter 聚合依赖。
- 非目标
  - 不负责内置持久化存储（默认打印日志）；业务可自行实现存储（DB/ES/MQ）。
  - 不提供跨进程全链路变更归并（聚合可在业务层实现）。

## 4. 典型使用场景
- 人员操作记录：如修改用户资料、禁用账号、调整权限。
- 业务对象生命周期：订单、工单、发票的创建/修改/审核/提交/取消。
- 表单差异展示：保存前后字段变化（包含新增/修改/删除字段）。

## 5. 总体设计
### 5.1 核心概念
- 操作日志 vs 系统日志：面向阅读者不同，easy-log 聚焦“用户可读”的操作内容。
- AOP 注解：用切面统一采集方法上下文，拼装日志实体并输出/持久化。
- 模板语法：
  - SpEL：`{{ <SpEL> }}`，示例 `{{#userDto.name}}`、`{{#_result}}`、`{{#_errMsg}}`、`{{@bean.method(#arg)}}`。
  - 文本占位符：`success`/`fail` 模板中 `${}` 顺序占位，对应 `successParamList`/`failParamList` 的解析结果（按顺序替换）。
- 上下文：`EasyLogContext`（ThreadLocal 变量上下文，模板里 `{{#key}}` 可直接取值）。

### 5.2 模块划分（现有实现）
- 注解：`@EasyLog`、`@EasyLogs`
- 切面：`EasyLogAspect`（切点、执行/异常捕获、模板渲染与落地）
- 表达式解析：`EasyLogCachedExpressionEvaluator`、`EasyLogEvaluationContext`、`EasyLogParser`
- 自动装配：`EasyLogAutoConfiguration`、`EasyLogProperties`
- 输出与扩展：`ILogRecordService`（默认打印）与 `IOperatorService`（默认空）
- 工具与模型：`PlaceholderResolver`、`EasyLogInfo`、`FieldInfo` 等

### 5.3 关键时序
1) 解析注解参数 → 收集模板键集合 → 预解析不依赖 `#_result/#_errMsg` 的 SpEL（前置快照）
2) 执行业务方法（记录成功/异常/耗时）
3) 渲染模板（SpEL；依赖 `#_result/#_errMsg` 的在后置求值，前置值复用）→ 生成 `EasyLogInfo` 列表 → 调用 `ILogRecordService.record`
4) 嵌套与变量上下文：通过 `EasyLogContext` push/pop 管理，完成后清理

## 6. 公共 API 与注解
### 6.1 注解参数（`@EasyLog`）
- `platform`: 平台标识，默认从 `IOperatorService.getPlatform()` 取
- `operator`: 操作者，默认从 `IOperatorService.getOperator()` 取
- `module`: 模块名
- `type`: 操作类型（如 CREATE/UPDATE/DELETE/READ）
- `bizNo`: 业务对象标识（如订单号、用户ID）
- `success` / `fail`: 成功/失败模板（支持 `{{}}`、`${}`）
- `successParamList` / `failParamList`: 文本 `${}` 的顺序参数键（模板解析后按顺序替换）
- `detail`: 详情字符串；若为 JSON 数组字符串 `[oldJson, newJson]` 会生成字段差异
- `condition`: 记录条件（SpEL），为空视为记录

### 6.2 模板与表达式
- SpEL：`{{#param.path}}`、`{{#_result}}`、`{{#_errMsg}}`、`{{@bean.method(#args)}}`
- 文本占位：`用户：${} 已被禁用，原因：${}` → `successParamList = {"{{#user.name}}","{{#reason}}"}`

### 6.3 示例
```
@EasyLog(
  module = "用户管理",
  type   = "UPDATE",
  bizNo  = "{{#userDto.id}}",
  success= "更新了用户信息：{{#userDto.name}}",
  detail = "{{#_result}}"
)
public User update(UserDto userDto) { ... }

@EasyLogs({
  @EasyLog(module="用户管理", type="UPDATE",
           success="测试多个日志-1： ${} 这是后缀${}",
           successParamList={"{{ @easyLogFunctions.getBeforeName(#p0) }}"}),
  @EasyLog(module="用户管理", type="READ",
           success="测试多个日志-2： {{ @easyLogFunctions.getBeforeName(#p0) }}")
})
public void manyLog(String name) { ... }
```

## 7. 扩展点
- `IOperatorService`：提供 `getOperator()` 与 `getPlatform()`，用于默认取值
- `ILogRecordService`：输出/持久化日志（文件、DB、ES、MQ 均可）

## 8. 运行时行为与边界
- 嵌套方法：使用 `ThreadLocal<Stack<...>>` 隔离日志，避免覆盖/污染
- 异常策略：业务异常被捕获用于生成失败日志后，再抛出回业务调用方
- 性能：
  - SpEL 表达式缓存（`CachedExpressionEvaluator`）
  - 模板仅在必要时解析；函数前置值可复用
  - 避免在模板中进行重逻辑（如远程调用），建议放入自定义函数
- 线程/异步：若业务切换线程，`OpLogContext` 不会自动传递；如需跨线程上下文，可结合 TTL（TransmittableThreadLocal）进行扩展

## 9. 依赖与兼容性
- 运行环境：Java 17+，Spring Boot 3.x（Jakarta 命名空间）
  - 最小依赖策略：
  - 必需：`spring-boot-autoconfigure`、`spring-aop`、`aspectjrt`、`aspectjweaver`(runtime)、`commons-lang3`、`guava`、`fastjson`、`javers-core`
  - 可选/不传递：`spring-web`（请求上下文）、`jakarta.servlet-api`（provided）、`slf4j-api`
  - 编译期：`lombok`（provided）
- 不兼容 Boot 2.x（`javax.*` → `jakarta.*` 已整体迁移）

## 10. 持久化与查询建议
- 默认实现：`DefaultLogRecordServiceImpl` 使用 `log.info` 输出 JSON
- 业务自定义：实现 `ILogRecordService`，建议模型对齐 `EasyLogInfo` 字段：
  - 基本信息：`operator`、`platform`、`operateTime`、`module`、`type`、`bizNo`
  - 请求上下文：`ip`、`url`、`httpMethod`、`classMethod`、`param`
  - 结果与异常：`success`、`result`、`errorMsg`、`stackTrace`、`executeTime`
  - 文案与详情：`content`、`contentParam`、`detail`、`fieldInfoList`
- 检索字段建议：`platform,module,type,bizNo,operator,operateTime`

## 11. 安全与合规
- 敏感信息：建议在 `ILogRecordService` 或上游环节做脱敏/过滤（如手机号、证件号、授权 Token）
- 过滤策略：支持在 `condition`、模板函数中自行判断是否记录或剪裁字段

## 12. 配置项（`EasyLogProperties`）
- `easylog.enable`：是否开启（默认 true）
- `easylog.store`：日志落地方式：`log`（默认）。如需其它方式请自定义 `ILogRecordService`
- `easylog.after-commit`：是否在事务提交后再落地（默认 false；存在事务时有效）
- `platform`：默认从 `spring.application.name` 注入；可由 `IOperatorService` 覆盖

## 13. 测试与质量
- 单元测试：表达式解析、函数执行、占位符替换、差异计算
- 集成测试：AOP 切面执行路径（成功/异常）、嵌套上下文、`ILogRecordService` 自定义实现
- 性能基准：模板解析/函数执行的热点路径，在典型负载下的耗时与 GC 评估

## 14. 版本路线图
- v1.0（已完成）
  - Boot 3 / Java 17 支持；`javax`→`jakarta`
  - 注解、AOP、SpEL、函数、差异对比、默认输出
  - 最小依赖打包策略
- v1.1
  - `spring-boot-configuration-processor`（可选，IDE 配置提示）
  - 字段脱敏 SPI（可插拔）
  - 条件表达式增强与更友好的错误提示
- v1.2
  - 常用函数包（如用户/部门/字典翻译、ID→名称映射）
  - 批量操作合并展示策略（可选）
- v2.0（规划）
  - 上下文跨线程传递的可选支持（TTL 集成）
  - 更丰富的埋点注解与链路聚合能力（保持轻量）

## 15. 迁移指南
- 自定义函数 DSL 删除：`{funcName{SpEL}}` → `{{ @beanName.method(SpEL) }}`
- 导入包迁移：`javax.*` → `jakarta.*`（如 `PostConstruct`、`HttpServletRequest`）
- 运行环境：JDK 17+

## 16. FAQ（节选）
- Q：非 Web 项目能用吗？
  - A：可以。没有请求上下文时 `ip/url/httpMethod` 为空，不影响日志记录。
- Q：如何在日志中输出“修改前/修改后”？
  - A：`detail` 传入 `[oldJson,newJson]` 字符串，自动生成 `fieldInfoList`；或使用自定义函数查询旧值。
- Q：如何避免方法签名为记录日志而“加参”？
  - A：通过“前置函数”在切入点执行前计算所需值，模板中 `{func{#arg}}` 引用即可。

---

附：灵感来源文章《如何优雅地记录操作日志》强调“与业务解耦、模板化、可读可配”，easy-log 的设计与实现沿着该思路，结合 Spring AOP 与 SpEL、自定义函数、上下文栈与最小依赖策略落地，适配 Boot 3 / Java 17 生态。
