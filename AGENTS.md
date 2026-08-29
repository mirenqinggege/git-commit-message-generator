# AGENTS.md

## 项目简介

这是一个 IntelliJ IDEA 插件（Kotlin + Gradle），在 Git Commit 工具窗口中添加 **Generate Commit Message** 按钮。插件读取所选变更的 `git diff HEAD`，发送到用户配置的 OpenAI 兼容接口，并将生成的 Conventional Commits 格式提交消息流式填入提交编辑框。

- 插件 ID / 根包名：`com.mirenqinggege.gitcommit`（版本号在 `gradle.properties` 中）
- 目标平台：通过 `org.jetbrains.intellij.platform` Gradle 插件 2.x 面向 IntelliJ IDEA 2025.2.x
- LLM SDK：`com.openai:openai-java`（同时支持 Chat Completions 和 Responses 两种接口，均为流式）

## 常用命令

```bash
./gradlew build          # 完整构建 + 插件校验
./gradlew runIde         # 启动沙箱 IDE 试用插件
./gradlew test           # JUnit 4 测试（src/test/kotlin）
```

Gradle 已启用配置缓存和构建缓存；`buildSearchableOptions` 在 `build.gradle.kts` 中被有意禁用。

## 代码结构

```
src/main/kotlin/com/mirenqinggege/gitcommit/
├── actions/      GenerateCommitMessageAction（ChangesView.CommitToolbar 中的按钮）
├── services/     OpenAiCompatibleClient（流式 API 客户端，ApiType 分 COMPLETIONS/RESPONSES）
├── settings/     GitCommitMessageConfigurable + GitCommitMessageSettings（持久化设置）
├── CommitMessageGeneration.kt   CommitPromptBuilder + GitDiffCommandBuilder
└── GitCommitMessageBundle.kt    消息资源包访问器
src/main/resources/
├── META-INF/plugin.xml          扩展点与 Action 注册
└── messages/GitCommitMessageBundle*.properties
```

## 规则与注意事项

- **所有面向用户的文案以及 LLM 提示词模板都放在消息资源包里**（`prompt.template` 用 `{0}` 占位 diff）。`GitCommitMessageBundle.properties` 中新增的每个 key 都必须同步到 `GitCommitMessageBundle_zh_CN.properties`，反之亦然。
- 提示词中要求模型忽略 diff 内嵌的指令（`<DIFF_START>`/`<DIFF_END>` 标记）。修改 `prompt.template` 时务必保留该安全段落。
- Action 只注册在 `plugin.xml` 的 `ChangesView.CommitToolbar` 下；通知通过 `Git Commit Message Generator` 通知组发送。
- 思考模式由设置页的 `Enable thinking mode` 开关控制（默认关闭，持久化为 `enableThinking`）。关闭时，`OpenAiCompatibleClient.kt` 中的 `applyThinking` 会附加所有已知的"关闭推理"参数写法（`reasoning_effort=none`、`enable_thinking=false`、`thinking={type: disabled}`），因为各服务商只认其中某一种；开启时不附加任何参数。
- 本仓库自身的提交消息遵循 Conventional Commits 规范（如 `feat(client): ...`），通常使用中文书写。
