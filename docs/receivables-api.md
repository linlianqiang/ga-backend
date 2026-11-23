> 后续新增projectId关联、CSV导出和/api/bills、/api/receipts兼容入口，详见 [完整业务接口](business-api.md)。

# 项目回款 API

前缀 `/api/receivables`，沿用 JWT Bearer Token 和 `{code,message,data}`。业务错误见 code（400/404/409），沿用现有项目 HTTP 响应约定。

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | / | 列表，参数 name/property 模糊匹配、owner 姓名包含匹配（支持多人负责人）、tab=all/unpaid/due、page=1、size=0（默认返回全部；size=1–100 时分页） |
| GET | /{id} | 账期详情 |
| POST | / | 新增账期 |
| PUT | /{id} | 全量更新账期 |
| DELETE | /{id} | 删除账期，有回款时拒绝 |
| GET | /receipts?bill=1 | 回款列表，bill 可选 |
| GET | /receipts/{id} | 回款详情 |
| POST | /receipts | 新增回款 |
| PUT | /receipts/{id} | 全量更新回款 |
| DELETE | /receipts/{id} | 删除回款 |

列表 data 包含 items、total、page、size、summary。summary 为整个筛选集的 unpaid（已开票未回款）、bad（烂账）、due（状态为未开票，且服务结束日期早于上海当天或（付款方式包含“先付”且上海当天在服务开始日至结束日之间，含首尾当天））金额。与预览相同，汇总按应收金额计算，回款流水不自动修改手工状态。

账期字段与 preview 对应：name 项目名称、company 所属公司、property 物业、owner 负责人、amount 应收金额、net 扣费到账金额、quarter 账期、start/end 服务日期、status 原始状态、invoice 开票日期、due 付款到期日、account 账户类型、method 付款方式、follow 跟进/回款日期、bad 烂账标记、note 备注。金额使用 DECIMAL(18,2)，日期 YYYY-MM-DD。新增/更新必填 name、amount、status；status 支持 已回款/已开票/已开票未回款/未开票/已填开收据/未知。

新增账期示例：
```json
{"name":"示例项目","company":"港安","amount":12000,"status":"未开票","quarter":"第一季度","due":"2026-12-01","bad":false}
```
新增回款示例：
```json
{"bill":1,"amount":3000,"date":"2026-11-19","method":"银行转账","reference":"凭证001","note":"部分回款"}
```
回款金额必须大于0，bill 必须存在。历史“已回款”不等于可确认的实际流水，因此导入只创建应收账期。

## 清洗与导入

执行 `python3 scripts/clean_receivables.py <CSV路径>`，输出 data/receivables.json 和 data/import-report.json。剔除标题、空行、汇总、无项目名行，不猜测缺失值；不合规范的日期/金额置空并记录问题，sourceRaw 保留原始15列。未将疑似重复账期自动合并，避免误删真实记录。

执行：
```sh
mvn -Dmaven.compiler.source=17 -Dmaven.compiler.target=17 spring-boot:run -Dspring-boot.run.arguments="--receivables.import-file=data/receivables.json"
```
导入事务整体提交，以源文件 SHA256+行号唯一键实现同一文件重复执行不重复插入。修改源文件后的再次导入视为新批次，须先人工核对。正常启动不带 import-file 参数。数据库表 project_receivable、project_receipt 由 JPA 建立；未与现有 fire_project 强行关联，避免同名项目错配。当前沿用系统 JWT 登录限制，没有额外引入角色权限策略。

回款台账全量查询：GET /api/bills?tab=all&size=0（/api/receivables 同义）。不传 size 默认全量返回，data.items 包含全部符合筛选条件的账期，data.total 为总条数，size=0 表示不分页。查询不要求 projectId，包含历史未关联记录；如需全部数据，请同时清空 name/property/owner 筛选。显式传 size=1–100 时仍按 page 分页。

## 回款 tab 权限
权限目录 GET /api/roles/permissions 新增：
- ledger.tab.all：全部项目
- ledger.tab.unpaid：已开票未回款
- ledger.tab.due：到期未开票

通过现有角色新增/修改接口的 permissions 数组配置；保存 tab 权限自动补齐 ledger.view。
登录和 /api/user/info 的 permissions 返回这些权限，前端据此显示 tab，并选择首个有权限的 tab 请求列表。
列表 /api/bills、/api/receivables 及各自 /export 同时校验对应 tab 权限；不传或传空 tab 按 all 校验。未知/重复 tab 返回 HTTP 400，无权限返回 HTTP 403。导出仍需 ledger.export。
例如仅允许到期未开票并导出：["ledger.tab.due","ledger.export"]。
已有普通角色不会自动获得 tab 权限，需重新配置；管理员自动拥有全部权限。
本次为 tab 入口权限，详情、按项目查看和回款流水仍沿用原有操作权限，不构成账期行级数据隔离。
