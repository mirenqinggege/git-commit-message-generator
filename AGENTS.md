# AGENTS.md

## What this is

IntelliJ IDEA plugin (Kotlin + Gradle) that adds a **Generate Commit Message** button to the Git Commit tool window. It reads `git diff HEAD` for the selected changes, sends it to a user-configured OpenAI-compatible API, and streams a Conventional Commits message into the commit editor.

- Plugin ID / root package: `com.mirenqinggege.gitcommit` (version lives in `gradle.properties`)
- Target platform: IntelliJ IDEA 2025.2.x via `org.jetbrains.intellij.platform` Gradle plugin 2.x
- LLM SDK: `com.openai:openai-java` (supports both Chat Completions and Responses APIs, streaming)

## Commands

```bash
./gradlew build          # full build + plugin verification
./gradlew runIde         # launch sandbox IDE to try the plugin
./gradlew test           # JUnit 4 tests (src/test/kotlin)
```

Gradle configuration cache and build cache are enabled; `buildSearchableOptions` is intentionally disabled in `build.gradle.kts`.

## Layout

```
src/main/kotlin/com/mirenqinggege/gitcommit/
├── actions/      GenerateCommitMessageAction (button in ChangesView.CommitToolbar)
├── services/     OpenAiCompatibleClient (streaming API client, ApiType COMPLETIONS/RESPONSES)
├── settings/     GitCommitMessageConfigurable + GitCommitMessageSettings (persistent state)
├── CommitMessageGeneration.kt   CommitPromptBuilder + GitDiffCommandBuilder
└── GitCommitMessageBundle.kt    message-bundle accessor
src/main/resources/
├── META-INF/plugin.xml          extensions & action registration
└── messages/GitCommitMessageBundle*.properties
```

## Rules and gotchas

- **All user-facing strings AND the LLM prompt template live in the message bundle** (`prompt.template` uses `{0}` for the diff). Every key added to `GitCommitMessageBundle.properties` must be mirrored in `GitCommitMessageBundle_zh_CN.properties`, and vice versa.
- The prompt instructs the model to ignore instructions embedded in the diff (`<DIFF_START>`/`<DIFF_END>` markers). Preserve that security section when editing `prompt.template`.
- The action is registered only under `ChangesView.CommitToolbar` in `plugin.xml`; notifications go through the `Git Commit Message Generator` notification group.
- Recent change: OpenAI-compatible requests explicitly disable model "thinking"/reasoning mode — keep this behavior when touching `OpenAiCompatibleClient`.
- This repo's own commit messages follow Conventional Commits (e.g. `feat(client): ...`), often written in Chinese.
