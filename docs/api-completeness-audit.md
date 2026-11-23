> 本文件是开发前的核验快照。后续开发结果及当前接口以 [完整业务接口](business-api.md) 为准；文件上传已明确取消。

# API 完整性核验

核验日期：2026-11-19。依据 API-HANDOFF.md、preview.html、对应前端源码和当前 Java 源码。
结论：尚未全部开发完成。此前完成的是数据导入和项目/回款基础接口，不是完整 UI 的全部业务闭环。

## 模块结果

| 模块 | 状态 | 已有 | 缺少 |
|---|---|---|---|
| 物业公司、我司企业 | 未实现 | 业务表中的字符串字段 | /api/master-data 及模型、CRUD、名称校验、引用同步、引用删除保护 |
| 人员 | 未实现 | 项目/账期的负责人文本 | /api/personnel，姓名/角色/电话/入职日期/文凭，搜索、角色筛选、唯一姓名、引用同步和删除保护 |
| 账号、角色权限 | 未实现业务管理 | User.roles 字符串、JWT roles 声明 | /api/accounts、/api/roles，启停、角色权限表、账号名不区分大小写判重、角色判重、管理员保护、引用角色删除保护、权限补齐及服务端操作授权 |
| 项目 | 部分完成 | CRUD、/page、筛选、汇总、合同期限校验、上海日期动态预警 | company/note/contractFiles；同公司项目名称唯一；账期关联；修改同步；有关联账期禁止删除；旧数据按公司名推断分类 |
| 回款 | 部分完成 | 账期/流水CRUD、账期筛选分页和汇总、回款金额校验、有流水的账期删除保护 | projectId；按项目查询账期与流水；项目资料统一编辑同步；CSV导出API；接口路径适配 |
| 登录 | 基础实现 | POST /api/auth/login、GET /api/user/info、JWT验证 | 与账号启停/权限模型联动；密码哈希；登录和鉴权集成测试 |
| 合同文件 | 未实现 | 原表“已上传”布尔标记 | /api/files、文件存储上传下载删除、附件与项目关联、文件权限 |

## 关键依据及影响

1. JwtInterceptor.preHandle 只校验 token 签名/有效期，把 roles 写入请求属性后直接放行。所有业务 Controller 没有按 roles 或 permissions 校验。有效 token 可以访问现有写接口，不能支持只读角色约束。
2. FireProject 没有 company、note、contractFiles 字段。同公司同名项目判重无法实现。FireProjectController.delete 只检查项目是否存在，然后删除，不检查关联账期；update 不同步账期。
3. Receivable 没有 projectId，只保存 name/company/property/owner 字符串。UI 基于 projectId 展示项目账期与回款，当前无法按此契约工作。已入库项目和历史账期尚未建立可靠 ID 关系，不能直接按名称自动匹配。
4. User 只有 id/username/password/roles。UserService.validateUser 直接比较密码字符串，没有密码哈希；JwtUtil 签名密钥在源码中固定。账号禁用/角色更改无法动态影响已有 token。
5. 基础数据、人员、账号管理、角色管理、文件管理没有相应 Controller/Repository/Entity；UserService.createUser 是内部方法，不等于账号管理 API。

## 前端契约差异

| UI 交接建议 | 当前实现 | 处理 |
|---|---|---|
| /api/bills | /api/receivables | 可统一路由或前端适配，不单凭路径不同判定功能缺失 |
| /api/receipts | /api/receivables/receipts | 同上 |
| GET /api/projects 分页 | GET /api/projects/page 分页；集合GET返回数组 | 前端明确选择分页接口 |
| PATCH 单条 | PUT 全量更新 | 未实现PATCH，不能把部分更新请求直接改成PUT；PUT未传普通字段会清空 |
| {items,total} | {code,message,data:{items,total,...}} | 前端读取data |

业务错误通常返回 HTTP 200 + body.code，鉴权失败返回 HTTP 401；绑定/类型转换异常没有统一异常处理，错误结构可能不一致。当前列表通过 findAll 后内存筛选/分页，数据量增长后需要数据库层分页及汇总。

## 验证范围

重新运行 mvn test：现有8项单元测试通过（项目5项、回款3项）。覆盖参数、字段映射、部分汇总及删除保护。使用 Mock Repository，不代表真实 HTTP、数据库 CRUD、登录或权限验证通过。本次核验没有修改业务代码、没有写远程数据库、未执行在线端到端测试。

preview 仍是 localStorage 原型，UI 显示出页面和按钮不能证明后端资源已存在。交接文档明确其路由是建议，不能当成实现清单。

## 建议补齐次序

1. 明确项目公司字段、基础数据/人员 ID、项目到账期的关联模型，历史导入账期允许不关联项目，不再规划历史匹配模块。
2. 实现账号/角色/操作权限与登录启停联动、密码哈希。
3. 实现基础数据与人员 CRUD、引用同步及删除保护。
4. 补项目必需字段、公司内判重、项目账期联动与关联删除保护。
5. 补文件接口、导出，统一前端接口契约与错误响应。
6. 增加 HTTP/数据库集成测试、未登录/只读/管理员权限测试，再进行完整 UI 联调。

### 台账导出

仅保留 `GET /api/receivables/export`（兼容 `/api/bills/export`），要求 Bearer Token 和 `ledger.export` 权限。
前端传递当前列表查询条件 `name`、`property`、`owner`、`tab`（all/unpaid/due）。导出匹配条件的全部记录，不受分页限制，按 ID 倒序，与列表一致。无需传递勾选 ID。

可选 `fields` 参数用逗号分隔，按传入顺序输出，例如：

```text
GET /api/receivables/export?tab=unpaid&fields=name,quarter,amount,status
```

不传 `fields` 时保留原默认列：项目、公司、物业、负责人、账期、金额、状态、服务开始、服务结束、开票日期、付款到期、备注。
可选字段：`id,projectId,name,company,property,owner,quarter,status,account,method,amount,net,start,end,invoice,due,follow,bad,note`。空字段、重复字段或非法字段会拒绝。

返回 XLSX 文件（中文表头），前端使用 Blob 下载，文件名为 receivables.xlsx，Content-Type 为 application/vnd.openxmlformats-officedocument.spreadsheetml.sheet。所有单元格使用黑色细边框，行高 24 磅、列宽 60 字符。表格底部新增“合计”行，汇总全部匹配记录的 amount，并对齐所选金额列。不再添加汇总列。未选择金额字段时，合计行首格显示“合计：金额”；仅选择金额字段时使用带“合计：”前缀的数值格式。空结果仅包含表头和合计行，合计为 0。错误按现有 JSON 错误约定处理。已移除按 ID 勾选导出和独立回款明细导出接口。

物业筛选支持重复参数，如 `property=万科&property=上海悦佳`：物业包含任一非空值即命中（OR），与其他查询条件仍为 AND。未传或全为空时不限制物业。列表和导出使用相同规则。

回款管理“已到期未开票”：列表及导出使用 `tab=due`，筛选为 `status=未开票 AND (end < 上海当天 OR (method 包含“先付” AND start <= 上海当天 AND 上海当天 <= end))`。服务结束日期为当天不算已过；“先付”分支要求服务开始、结束日期均非空，且当天处于该区间内（含首尾当天）。付款到期字段 `due` 不参与此筛选，`summary.due` 使用同一规则。
