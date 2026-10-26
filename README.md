# 校园二手拍卖系统——小组课程项目

项目仓库：[Talkmytalk/campus-secondhand-auction](https://github.com/Talkmytalk/campus-secondhand-auction)  
检查工作记录：[提交历史](https://github.com/Talkmytalk/campus-secondhand-auction/commits/main/) · [Pull Requests](https://github.com/Talkmytalk/campus-secondhand-auction/pulls) · [问题记录](https://github.com/Talkmytalk/campus-secondhand-auction/issues)

三人小组共同开发校园二手拍卖系统，分工为后端、前端和测试。当前已整理入本地项目的主要成果为后端第一版：用户名密码注册登录、发布拍卖、查看列表与详情、出价校验、到期结算、本人订单和获胜者模拟付款。前端页面及测试成员的独立成果等待本人提交与确认。

第一版范围不含图片上传、真实支付、校园账号认证和收货流程。

## 老师与组员查看入口

| 内容 | 入口 |
| --- | --- |
| 后端源码与自动测试 | [backend](backend/) |
| 业务规则与出价规则 | [业务规则初稿](docs/业务规则初稿.md) |
| 前后端接口约定 | [接口说明](docs/接口说明.md) |
| 已有后端验证证据 | [测试记录](docs/测试记录.md) · [验证结果摘要](docs/验证结果摘要.json) |
| 独立测试的用例与反馈要求 | [测试用例清单](docs/测试用例清单.md) · [测试任务与反馈模板](docs/测试任务与反馈模板.md) |
| 第 1 周个人周报（阶段草稿） | [个人周报](docs/第1周个人工作周报_阶段草稿.md) |
| 第 1 周小组周报（阶段草稿） | [小组周报](docs/第1周小组工作周报_阶段草稿.md) |
| 前端任务与提交位置 | [前端任务单](docs/前端开发任务单.md) · [frontend](frontend/) |
| 测试成员成果提交位置 | [testing](testing/) |
| GitHub 协作与记录方式 | [协作说明](docs/GitHub协作说明.md) |

已有测试记录的执行日期为 2026 年 10 月 19 日；交接说明整理于 10 月 23 日。周报仍保留真实统计截止日期和待填写署名，提交老师前应根据成员反馈更新。Git 提交时间表示文件何时纳入版本记录，具体工作的完成和验证日期见对应文档。

技术：Java 17、Spring Boot、Maven、Spring Data JPA、H2 本地文件数据库。Spring Boot 的具体版本见 `backend/pom.xml`。本地开发无需另外安装数据库。

## 先用 IDEA 打开

此前本机交付还提供 `交付材料/campus-auction.jar` 和同目录的 `启动后端.cmd`。交付包独立分发，Git 仓库保留源码；从仓库获取项目后，可以按下列步骤使用 IDEA，或使用下文的 Maven 命令生成可运行程序。如果已拿到交付包，将 JAR 与启动脚本放在同一文件夹，双击脚本即可启动。该方式仍需要 Java 17，数据库保存在脚本所在目录的 `data` 文件夹。

1. 打开 IDEA，选择 **Open（打开）**，选择本目录下的 `backend` 文件夹。如果询问是否作为 Maven 项目打开，选择是。
2. 在 **File → Project Structure → Project** 中将 SDK 设为 Java 17。
3. 等待 IDEA 加载 Maven 依赖。第一次需要网络，可能花几分钟。
4. 打开 `src/main/java/edu/campus/auction/AuctionApplication.java`，点击 `main` 方法旁的绿色三角形运行。
5. 在浏览器访问 `http://127.0.0.1:8080/api/health`，看到 `"status":"UP"` 说明后端已经启动。这个地址用于检查服务，不是拍卖页面。
6. 停止时点击 IDEA 的红色方块。运行配置的 **Working directory（工作目录）** 建议设为 `backend`，数据库会保存到该目录下的 `data` 文件夹。

若 8080 已被占用，可在 `application.properties` 中将 `server.port` 改为 8081；前端连接地址也要一起改。

如果日志出现 `Unable to establish loopback connection`，可在 IDEA 的该应用运行配置中增加 VM options：`-Djdk.net.unixdomain.tmpdir=.`，将本地通信临时文件放到当前工作目录。本次自动验证遇到了这一 Windows 环境问题，加入该参数后继续验证；它不改变业务规则。说明依据见 [Oracle Java 网络属性文档](https://docs.oracle.com/en/java/javase/18/docs/api/java.base/java/net/doc-files/net-properties.html)。

## 也可以在终端运行

在 `backend` 文件夹打开 PowerShell：

```powershell
mvn spring-boot:run
```

运行自动测试：

```powershell
mvn test
```

生成可运行文件：

```powershell
mvn package
java -jar target/campus-auction-0.1.0-SNAPSHOT.jar
```

在存在上述本地连接问题的环境中，最后一行改为：

```powershell
java '-Djdk.net.unixdomain.tmpdir=.' -jar target/campus-auction-0.1.0-SNAPSHOT.jar
```

本次自动构建使用项目根目录的 `.maven-cache` 缓存依赖。要复用已下载的依赖，可在 `backend` 内使用：

```powershell
mvn '-Dmaven.repo.local=../.maven-cache' test
mvn '-Dmaven.repo.local=../.maven-cache' spring-boot:run
```

普通命令或 IDEA 会使用自己的 Maven 缓存，首次可能仍需下载。依赖缓存不属于代码成果，无需打包提交。

项目在 `backend/.mvn/maven.config` 中指定 Maven 官方仓库的配置文件，避免受本机已有下载镜像影响。如果 IDEA 的 Maven 导入未采用该配置，可在 IDEA 的 Maven 设置中将 User settings file 指向 `backend/.mvn/settings.xml`，然后重新加载项目。

## 先理解这几个文件

| 位置 | 作用 |
| --- | --- |
| docs/业务规则初稿.md | 说明允许什么、不允许什么，标注已确认与默认规则 |
| docs/接口说明.md | 给前端和测试同学看的接口约定与示例 |
| backend/src/main/java/edu/campus/auction/web | 接收请求、校验格式、返回结果 |
| backend/src/main/java/edu/campus/auction/service | 登录、拍卖、结算与付款的业务处理 |
| backend/src/main/java/edu/campus/auction/domain/BidRules.java | 判断一笔出价是否符合规则 |
| backend/src/main/java/edu/campus/auction/data | 数据表对应对象，以及查询、加锁和保存操作 |
| backend/src/test | 自动测试，独立测试数据库，不污染演示数据 |

例如点击“出价”，会经过：页面提交金额 → 接口取得登录身份 → 业务层锁定拍卖记录 → 校验最新价格和截止时间 → 保存出价并更新最高价 → 返回结果。

## 演示顺序

1. 注册两个账号，例如 `student_a`、`student_b`，密码使用 `Password123`；分别登录并保存各自的令牌。
2. 用 A 的令牌发布一件物品：起拍价 20 元，最低加价 2 元，截止时间设为几分钟后。
3. 用 B 的令牌出价 20 元，应成功；再出 21 元应失败，出 22 元应成功。
4. 用 A 的令牌出价，应提示不能给自己的物品出价。
5. 等到截止时间，查询拍卖详情，应显示 `SOLD`；尝试继续出价，应失败。
6. 用 B 的令牌查看 `/api/orders/mine`，取得订单编号，再请求模拟付款接口，应显示 `PAID` 且 `simulated=true`。
7. 重复付款，应返回同一笔订单和相同付款时间。

具体请求示例见 `docs/接口说明.md`。只运行后端不会出现页面，需要前端同学完成对应页面并连接接口。

## 当前边界

- 只用于本地课程演示，未进行公网部署和与组员前端的实际联调。
- 不真实验证校园身份，不处理实际转账、违约、退款或交付纠纷。
- 没有修改和撤销拍卖、找回密码、管理员管理、图片、搜索筛选或即时消息。
- 拍卖倒计时以服务端为准。应用停止时后台任务不运行，重启后会补结算已到期的拍卖。
- 运行时数据库数据与源码分开保存；移动或更换工作目录可能使用另一个数据库文件。
- 阶段周报需要由组长核对，其他成员的未确认进度不能写成已完成。
