> 后续补充了公司/备注、项目账期关联及删除保护，详见 [完整业务接口](business-api.md)。以下是初版字段与导入记录。

# 项目管理 API

沿用 JWT Bearer Token 和 `{code,message,data}`。业务错误 code=400/404。现有 HTTP 响应习惯不变。

| 方法 | 地址 | 说明 |
|---|---|---|
| GET | /api/projects | 兼容旧接口，data 是完整数组；支持筛选 |
| GET | /api/projects/page | 分页及筛选汇总 |
| GET | /api/projects/{id} | 详情 |
| POST | /api/projects | 新增 |
| PUT | /api/projects/{id} | 全量修改 |
| DELETE | /api/projects/{id} | 删除 |

类型 projectType：QUANZHOU 泉州（含银行）、XIAMEN 厦门、PARTNER 合作伙伴、TERMINATED 合同终止。bankProject 区分银行。category 兼容前端的 总公司/厦门分公司/合作挂靠项目/合同终止，分别映射以上类型。建议写入只传 projectType 或 category 之一。

筛选参数：name（或 projectName）、contractNo、owner、property、region、contact、settlement 为模糊匹配；projectType、category、bankProject、warning 为精确匹配。areaMin/areaMax、contractAmountMin/contractAmountMax 为范围；termStart/termEnd 按合同期限交集查询。日期 YYYY-MM-DD。warning：正常、30天内到期、已到期、未填写、已终止，按上海当前日期计算。

分页 page 从1开始，size 默认10、最大100。data 包含 items/total/page/size/summary；summary 包含 projectCount、contractAmount、expiringThisMonth，汇总整个筛选集而非当前页。

创建/修改示例：
```json
{"name":"示例项目","projectType":"QUANZHOU","bankProject":false,"contractNo":"GAXF-WB-202611001","owner":"负责人","property":"物业公司","region":"泉州","contact":"联系人 13800000000","area":10000.50,"contractAmount":25000,"settlement":"每季度","contractStart":"2026-11-01","contractEnd":"2027-10-31","posted":true}
```
name 必填，projectType 默认 QUANZHOU；金额/面积非负且至多2位小数，合同开始日期不能晚于结束日期。PUT 未传的普通字段清空。name/projectName、owner/maintenanceLeader、property/propertyUnit 兼容。原 projectNumber/contactPerson/contactPhone/fireHostModel/platformPractitioner/contractTerm 保留。contractNo 是合同编号，不等于原表序号；同一合同可能对应多个项目，不对合同号去重。

附加字段：renewalOwner 续约跟进人、followDate 跟进日期、followStatus 跟进情况、contractStatus 原合同状态；posted/contractUploaded 布尔未知为null，postedRaw/contractUploadedRaw 保留原文。上传标记仅代表原表状态，不代表后端已有附件，本次不含合同文件上传接口。

sourceKey/sourceSheet/sourceRow/sourceRaw 是导入溯源信息，API 新建时清除、修改时保留。sharedAmountGroup/sharedAmountRaw 记录跨项目合并金额，金额仅计入首行以防重复汇总，不能理解为该行项目独享金额。尚未按项目名称自动关联回款台账，避免同名项目错配；删除项目不删除回款台账。

## 清洗与导入

```sh
python3 scripts/clean_projects.py '<Excel路径>'
mvn spring-boot:run -Dspring-boot.run.arguments="--projects.import-file=data/projects.json"
```
输出 data/projects.json 与 data/projects-import-report.json。仅处理前四表：合同执行中177、银行25、厦门18、合作伙伴84，共304条。泉州类别合计202条。61项清洗问题记录（含共享金额非首行不重复计入），保留原始数据。混合文字数值、未知日期不推断，结构化值留空；只按明确的合并单元格扩展非金额字段。原表到期提醒是缓存值，不入库，API动态计算。

可明确解析且去除合并重复后的合同金额合计为11199680.59元，不代表所有合同的完整总额。原文件不改动。

导入使用事务和 SHA256+工作表序号+行号唯一键，相同文件重复执行不新增，不覆盖旧项目。修改文件后的新批次需要人工核对，不能作为自动更新使用。正常启动不带 projects.import-file 参数。已完成授权入库，并通过独立数据库连接核对：304条，泉州202（银行25）、厦门18、合作伙伴84；可解析合同金额合计11199680.59元。
