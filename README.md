# git-commit-message-generator

![Build](https://github.com/mirenqinggege/git-commit-message-generator/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## 模板待办清单
- [x] 创建一个新的 [IntelliJ 平台插件模板][template] 项目。
- [x] 熟悉[模板文档][template]。
- [ ] 调整 [group](./gradle.properties)，以及 [id](./src/main/resources/META-INF/plugin.xml)、[name](./src/main/resources/META-INF/plugin.xml) 和[源码包](./src/main/kotlin)。
- [ ] 调整插件[描述](./src/main/resources/META-INF/plugin.xml)（参见[提示][docs:plugin-description]）并修改本 README，以描述你插件的功能。
- [ ] 审阅[法律协议](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate)。
- [ ] 首次[手动发布插件](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate)。
- [ ] 设置上面 README 徽章中的 `MARKETPLACE_ID`。插件发布到 JetBrains Marketplace 后即可获取。
- [ ] 设置[插件签名](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate)相关的[环境变量](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables)。
- [ ] 设置[部署令牌](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate)。
- [ ] 点击 [IntelliJ 平台插件模板][template] 顶部的 <kbd>Watch</kbd> 按钮，以便在发布包含新功能与修复的新版本时收到通知。

这个漂亮的 IntelliJ 平台插件将承载你那些绝妙的创意。

## 安装

- 使用 IDE 内置插件系统：

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > 搜索 “git-commit-message-generator” >
  <kbd>Install</kbd>

- 使用 JetBrains Marketplace：

  前往 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)，如果你的 IDE 正在运行，可通过点击 <kbd>Install to ...</kbd> 按钮进行安装。

  你也可以从 JetBrains Marketplace 下载[最新版本](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions)，并使用
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd> 手动安装。

- 手动安装：

  下载[最新版本](https://github.com/mirenqinggege/git-commit-message-generator/releases/latest)，并使用
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd> 手动安装。

---
插件基于 [IntelliJ 平台插件模板][template] 构建。

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
