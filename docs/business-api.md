# 完整业务接口（不包含文件上传）

正式 API 基础地址：`http://124.222.78.245:8080`。
业务接口包含 `/api` 前缀，例如登录：`POST http://124.222.78.245:8080/api/auth/login`。
前端若已在请求路径中包含 `/api`，baseURL 使用上述基础地址，避免重复拼接 `/api`。
健康检查：`GET http://124.222.78.245:8080/actuator/health`；服务器发布脚本使用 `http://127.0.0.1:8080/actuator/health` 检查本机服务。

所有资源除登录外需要 `Authorization: Bearer <token>`。返回 `{code,message,data}`；新增/修改业务校验失败仍可能是 HTTP 200 + code 400/409，前端同时检查 code。JWT认证失败 HTTP401，权限不足 HTTP403，格式错误 HTTP400。文件、合同附件上传/下载/删除不在本次范围。

## 启动

使用 JDK17：`JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home scripts/run_backend.sh`。启动脚本读取已忽略的 `.env`；JWT_SECRET 至少32字节，不可提交版本控制。应用会从工作目录自动加载 `.env`（Java properties 格式，使用 `JWT_SECRET=值`，不加引号或 export）；系统环境变量优先。IDE 工作目录需设为项目根目录。配置缺失时拒绝启动，禁止默认公用密钥。变更密钥会使全部旧token失效。

一次性基础数据初始化：给启动脚本增加 `-Dspring-boot.run.arguments="--directory.bootstrap=true"`，从现有项目/账期提取物业和公司，建立系统管理员角色并关联现有管理员。不创建默认密码，不拆分组合负责人姓名，不自动绑定项目账期。之后正常启动不带此参数。

## 登录和权限

- POST `/api/auth/login`：`{username,password}`。返回 token/username/roles/permissions，有效期2小时。
- POST `/api/auth/logout`：递增账号token版本，撤销该账号现有token。
- GET `/api/user/info`：当前用户、角色和操作权限。

签名算法固定HS256，验证issuer/audience/subject/exp/iat/jti/version及签名，容许30秒时钟误差。每个请求从数据库检查账号启用状态、token版本和实时角色权限。旧明文密码仅用于首次成功登录后自动升级BCrypt；新建和修改密码均使用BCrypt。账号修改立即使旧token失效。

权限清单 GET `/api/roles/permissions`：ledger.view/edit/receipt/export、projects.view/edit、personnel.view/edit、base.view/edit、access.view/edit。写操作自动补齐对应view权限。无角色映射的历史非管理员默认只允许项目和账期查看。前端应根据permissions展示按钮，最终权限由服务端控制。

## 账号角色

- `/api/accounts`：GET列表（search）、POST新增；`/{id}` GET详情、PUT全量修改。
- 账号体 `{login,name,role,enabled,password}`，role为角色ID，新增密码必填，8字符以上、UTF8不超过72字节。PUT省略password保留密码。停用通过PUT enabled=false，必须同时传其他必填字段。账号名不区分大小写判重。无账号删除接口，使用停用。
- `/api/roles`：GET列表、POST新增；`/{id}` GET/PUT/DELETE。
- 角色体 `{name,note,permissions:["projects.edit"]}`。已有引用不能删除；系统管理员角色保留全部权限，受保护管理员保持启用及管理员权限。

## 基础数据和人员

- `/api/master-data`：GET（type=property/company/region、search）、POST；`/{id}` GET/PUT/DELETE。体 `{type,name}`。
- `/api/personnel`：GET（search姓名或电话、role）、POST；`/{id}` GET/PUT/DELETE。体 `{name,role,phone,joined,education}`，joined为YYYY-MM-DD。
- 姓名/名称去重，人员姓名与角色必填，电话可空。人员 role 固定为“组长”“组员”“师傅”三个中文值，新增和修改均校验；前端使用固定选项，不调用权限角色接口。此字段与账号权限角色无关。
- 改名事务同步项目和账期的对应文本；存在引用禁止删除。历史组合姓名不自动拆分，需先人工规范人员名称。

## 项目和账期

项目继续使用 `/api/projects`、`/api/projects/page` 和 `/{id}`；增加company/note，新增/修改时校验同公司名称唯一。已有关联账期的项目不能删除；修改项目资料同步已关联账期。公司未知时不覆盖账期中已有公司，待补齐项目公司后同步。

账期 `/api/bills` 与 `/api/receivables` 同义：GET分页汇总、POST新增、`/{id}` GET/PUT/DELETE。新增必须传projectId，项目资料从项目表读取。历史未关联账期可继续编辑；PUT全量更新时需要保留projectId，显式null可解除错误关联。

回款 `/api/receipts`：GET（bill可选）、POST；`/{id}` GET/PUT/DELETE。旧 `/api/receivables/receipts` 保留。体 `{bill,amount,date,method,reference,note}`。

GET `/api/bills/by-project/{id}` 返回 `{bills,receipts}`。
GET `/api/bills/export?name=&property=&owner=&tab=all` 导出整个筛选集CSV，按ledger.export授权，处理公式前缀及CSV转义。

写入统一使用PUT全量更新，不支持PATCH，未传普通字段会清空。详情返回的source字段不可伪造覆盖。名称唯一规则适用于新建和编辑，历史重复合同记录不自动合并。

## 历史账期处理

历史导入账期允许 projectId 为空，作为独立台账继续维护，不要求补关联。已移除历史项目匹配、批量确认和别名管理接口。新增账期仍必须选择项目并传入 projectId；项目下查询和资料同步仅覆盖已关联账期。


## 2026-11-20 数据库升级验证

已在授权数据库完成新增表和字段初始化，回填物业366条、我司企业7条，系统管理员角色含12项权限。独立连接核对：项目304条、合同金额11199680.59元；账期2574条、应收39197460.82元；已关联账期0条。人员为空，未擅自拆分历史人员文本或绑定项目。

当前账号表为空，因此没有现有管理员可迁移。执行 `scripts/initialize_admin.sh`，交互输入并确认首个admin密码（至少12字符、最多72字节），启动后创建账号；密码只通过环境传入并以BCrypt存储，不写入脚本或命令行参数。账号表非空时不会创建或重置。使用JDK17运行；正常启动仍使用 `scripts/run_backend.sh`。

## 地区基础数据

地区使用 type=region，通过 GET /api/master-data?type=region 获取，通过现有基础数据 POST/PUT/DELETE 接口维护。名称原样使用：泉州永春、泉州丰泽、泉州洛江区、泉州市泉港区。执行一次性 directory.bootstrap=true 初始化时补入这四项，其他地区由前端维护，不从历史项目自动提取。项目字段为 region；地区改名同步项目，已被项目引用的地区禁止删除。

## 负责人下拉与筛选

项目管理、回款管理的负责人下拉统一调用 GET /api/personnel，读取 data 数组，以人员 name 作为 label 和 value；查询时将选中姓名传入 owner。该接口需要 personnel.view 权限。

GET /api/projects、/api/projects/page、/api/bills（兼容 /api/receivables）及账期 CSV 导出均按负责人文本包含选中姓名筛选。例如选择“庄耀铭”，可匹配“庄耀铭”“庄耀铭（主）、王俊垚”“庄耀铭（主）王志强”。owner 为空时不筛选。历史多人负责人文本保持原样；此规则不自动拆分或新增人员。

回款台账全量查询：GET /api/bills?tab=all&size=0（/api/receivables 同义）。不传 size 默认全量返回，data.items 包含全部符合筛选条件的账期，data.total 为总条数，size=0 表示不分页。查询不要求 projectId，包含历史未关联记录；如需全部数据，请同时清空 name/property/owner 筛选。显式传 size=1–100 时仍按 page 分页。

基础数据列表、详情及保存响应返回 id/type/name/projectCount。projectCount 按全部项目的对应文本字段精确匹配统计（property 对应 propertyUnit，company 对应 company，region 对应 region），不受项目分页或分类筛选影响，也不依赖账期 projectId。前端“关联项目”列读取 projectCount。
