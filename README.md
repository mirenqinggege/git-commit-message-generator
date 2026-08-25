# Git Commit Message Generator

这是一个 IntelliJ IDEA 插件。它会在 Git Commit 工具窗口的提交消息编辑框上方增加 **Generate Commit Message** 按钮。点击按钮后，插件读取当前项目的 Git diff，并调用用户配置的 OpenAI-compatible API，生成一条符合 Conventional Commits 规范的提交消息并填入编辑框。

## 功能

- 直接集成在 IntelliJ IDEA 的 Git Commit 工具窗口中。
- 使用当前提交范围的 `git diff HEAD` 作为生成上下文。
- 支持 OpenAI-compatible 的 Completions（`/completions`）和 Responses（`/responses`）接口。
- 支持自定义 Base URL、API Key、模型名称和 API 类型。

## 配置

打开 `Settings/Preferences | Tools | Git Commit Message Generator`，填写 Base URL、API key、Model，并选择 API type。Base URL 示例为 `https://api.openai.com/v1`，插件会自动追加相应的接口路径。

保存设置后，在 Git Commit 工具窗口点击提交消息编辑框上方的 **Generate Commit Message** 按钮即可。

## 开发

```bash
./gradlew build
./gradlew runIde
```

插件包名和插件 ID 均为 `com.mirenqinggege.gitcommit`。

## 发布待办

- [ ] 审阅并接受 [JetBrains Marketplace 法律协议](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html)。
- [ ] 首次手动发布插件并获取 Marketplace 插件 ID。
- [ ] 将 README 徽章中的插件 ID 替换为真实值。
- [ ] 配置插件签名环境变量和 Marketplace 部署令牌。
- [ ] 在 [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template) 点击 Watch。
