# 校园二手拍卖系统

一锤定音组课程项目。蒋宏涛负责组长及后端，严维彦负责前端，李杰负责独立测试。本次更新为第二周后端成果。

## 第二周更新

新增商品图片上传和读取。每场拍卖最多一张JPG或PNG图片，原文件与转换后的图片不超过5MiB；校验实际格式、尺寸及发布者身份。只有无人出价且未截止的拍卖可上传或替换图片，出价后或到期后禁止修改。

图片重新编码为PNG后存入H2，列表与详情新增imageUrl字段。旧无图商品继续有效，旧数据库升级与重启后图片保留已通过真实HTTP验证。付款沿用模拟付款，不发生实际扣款。图片前端页面接入及测试成员的独立验收仍待完成。

| 老师与组员检查内容 | 入口 |
| --- | --- |
| 后端源码及自动测试 | [backend](backend/) |
| 图片规则、接口与前端接入任务 | [第二周图片接口说明](docs/第2周图片上传接口与联调说明.md) |
| 第二周实际成果和验证结论 | [后端测试与成果记录](docs/第2周后端测试与成果记录.md) |
| 本次测试的机器可读摘要 | [验证结果](docs/第2周发布验证摘要.json) |
| 原交易接口 | [接口说明](docs/接口说明.md) |
| 初版业务规则 | [业务规则初稿](docs/业务规则初稿.md)，图片规则以第二周说明为准 |
| 第一周历史验证 | [测试记录](docs/测试记录.md) |
| GitHub协作与成员提交方式 | [协作说明](docs/GitHub协作说明.md) |
| 代码提交历史 | [Commits](https://github.com/Talkmytalk/campus-secondhand-auction/commits/main/) |

## 运行后端

技术栈：Java17、Spring Boot3.5.16、Maven、Spring Data JPA、H2文件数据库，不需要另装MySQL。

免费版IDEA可打开backend文件夹，配置JDK17，等待Maven依赖加载后运行src/main/java/edu/campus/auction/AuctionApplication.java的main方法。工作目录使用backend。

也可在backend目录执行：

```powershell
mvn spring-boot:run
```

浏览器访问 http://127.0.0.1:8080/api/health ，返回status为UP说明后端已启动；这是健康检查接口，不是拍卖页面。

运行测试并构建：

```powershell
mvn clean verify
java '-Djdk.net.unixdomain.tmpdir=.' -jar target/campus-auction-0.1.0-SNAPSHOT.jar
```

首次需要联网下载依赖。项目提供backend/.mvn/settings.xml；若IDEA的Maven导入未采用它，可在Maven设置中指定该文件后重新加载。Windows遇到本地连接初始化错误时，IDEA运行配置的VM options可增加-Djdk.net.unixdomain.tmpdir=.。

数据库保存于启动工作目录下的data文件夹。升级现有演示数据前，先停止程序并备份data，再从同一工作目录启动新版；不要同时让两个后端占用8080端口或同一数据库。

## 图片与原交易接口

- POST /api/auctions/{id}/image：发布者登录后，以multipart/form-data提交file字段。
- GET /api/auctions/{id}/image：公开读取PNG图片；无图片返回404。
- 发布、出价、到期结算、订单及模拟付款接口保持原约定；无图拍卖正常使用。

前端请求仍使用http://127.0.0.1:8080，默认允许localhost或127.0.0.1的5173和3000端口。上传用FormData，Content-Type由浏览器自动填写；不要用现有强制JSON的请求方法直接上传。

## 验证与分工

本轮重新执行35项自动测试：15项出价规则、19项完整流程及图片测试、1项真实HTTP健康检查。结果及真实multipart、旧库升级和重启验证见上表文档。自动测试不代替测试成员的人工验收，也不代表大规模压力测试。

本次提交包括后端及相关说明；组员页面源码和人工测试报告由各成员按实际工作提交。源码目录的frontend、testing可查看各自提交说明，不能据目录存在推断页面或独立测试已经完成。Word周报和可运行交付包单独分发，不纳入源码仓库。

当前只用于本机课程演示。没有校园身份认证、真实支付、收货、管理员管理、多图或图片删除入口。第一版预计2026年11月9日验收，成员新进度需依据实际提交更新。
