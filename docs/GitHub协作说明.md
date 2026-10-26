# GitHub 协作与成果检查说明

编制日期：2026 年 10 月 26 日。组长确认使用 GitHub 账号 `Talkmytalk`，仓库名为 `campus-secondhand-auction`，公开范围为 Public。目标地址为 `https://github.com/Talkmytalk/campus-secondhand-auction`；是否已创建和推送，以实际 GitHub 连接和上传结果为准。

## 一、仓库用于展示什么

老师可以从仓库首页查看源码、规则、接口、测试记录和周报，再打开提交历史查看每次变更的作者、时间、说明和具体文件差异。

首次导入会记录当前已有成果。此前的工作日期保留在文档内，之后随着成员完成新工作正常提交。提交历史从实际开始记录的时间累积。

## 二、统一目录

| 目录 | 负责人 | 内容 |
| --- | --- | --- |
| backend/ | 组长／后端 | 后端源码、数据处理、出价校验和自动测试 |
| frontend/ | 前端成员 | 页面源码、依赖配置、启动说明 |
| testing/ | 测试成员 | 人工执行记录、问题清单、截图及复测结果 |
| docs/ | 各成员与组长 | 业务规则、接口、工作计划、个人与小组周报 |

运行用的数据库、依赖缓存、生成的程序和交接包保留在本机；源代码与必要文档纳入版本记录。

## 三、首次建库时的设置

仓库名建议 `campus-secondhand-auction`。公开或私有由组长确定：公开仓库方便老师凭链接查看，私有仓库需要邀请老师并确认其已获得访问权限。

如果从 GitHub 网页创建空仓库，已有本地源码时不预先勾选生成 README、.gitignore 或 License，以便直接上传本项目现有文件。是否选择开源许可证由小组另行决定，本次不自动添加许可证。

仓库创建后，组长在仓库的 Settings → Collaborators 中邀请两名队员；私有仓库还需邀请老师。邀请属于账号协作操作，应确认具体 GitHub 用户名后执行。

## 四、每个人保留自己的工作记录

成员在自己电脑获取仓库，使用自己的 GitHub 账号登录，并将 Git 提交邮箱设为该 GitHub 账号已验证的邮箱或 GitHub 提供的私密提交邮箱。

可以在仓库目录配置提交署名（将示例替换为本人信息）：

```text
git config user.name "你的署名"
git config user.email "你的GitHub提交邮箱"
```

GitHub 插件授权用于应用访问 GitHub。文件修改后仍需要执行提交和推送，才能形成远程可查看的记录。

## 五、建议的日常流程

1. 开始新任务前同步仓库。
2. 按实际任务建立分支，例如 `frontend/login-page`、`backend/bid-rules`、`test/auction-flow`。
3. 完成一个可以说明清楚的小任务后提交，写清“改了什么”，例如“完成登录页面并连接登录接口”“补充截止后出价的测试记录”。
4. 推送分支，创建 Pull Request，简述变更和验证结果。组长查看后合并到 main。
5. 工作周报中记录实际成果，可附对应提交或 Pull Request 链接，方便老师核对。

前端和测试成员可以按本人的真实任务持续提交，无需等待整组项目完全完成。队员之间修改接口字段或业务规则时，先沟通并同步文档。

## 六、给老师的查看方式

- 源码和文档：仓库首页的目录与导航表。
- 每次提交：仓库的 Commits 页面，可以展开具体差异。
- 任务讨论与合并：Pull requests 页面。
- 人工问题与复测：testing/ 目录，以及后续实际创建的 Issues。
- 每周成果：docs/ 下对应周报与其中的成果链接。

## 官方参考

- [GitHub：创建仓库](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository)
- [GitHub：配置提交邮箱](https://docs.github.com/en/account-and-profile/how-tos/email-preferences/setting-your-commit-email-address)
- [GitHub：邀请协作者](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/inviting-collaborators-to-a-personal-repository)
