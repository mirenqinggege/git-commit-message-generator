package com.mirenqinggege.gitcommit

import com.google.gson.JsonElement
import com.google.gson.JsonParser

object CommitPromptBuilder {
    fun build(diff: String): String = """
        You are an expert software engineer. Analyze the following git diff and write a concise, standard Conventional Commits message.
        Follow the format `<type>(<scope>): <description>` and add a short body only when it adds useful context.
        Use one of feat, fix, docs, style, refactor, perf, test, build, ci, chore, or revert.
        仅返回提交消息，不要 Markdown 代码围栏、解释或前缀。

        Git diff:
        $diff
    """.trimIndent()
}

object OpenAiResponseParser {
    fun parse(body: String): String {
        val root = JsonParser.parseString(body)
        val text = findText(root) ?: error("The AI response did not contain generated text")
        return text.trim().trim('`').trim()
    }

    private fun findText(element: JsonElement): String? {
        if (element.isJsonObject) {
            val objectValue = element.asJsonObject
            objectValue["text"]?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.let { return it.asString }
            objectValue.entrySet().forEach { (_, value) -> findText(value)?.let { return it } }
        } else if (element.isJsonArray) {
            element.asJsonArray.forEach { findText(it)?.let { value -> return value } }
        }
        return null
    }
}
