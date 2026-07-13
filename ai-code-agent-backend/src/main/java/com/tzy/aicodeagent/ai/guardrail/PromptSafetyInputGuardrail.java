package com.tzy.aicodeagent.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PromptSafetyInputGuardrail implements InputGuardrail {

    // 敏感词列表
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "忽略之前的指令", "ignore previous instructions", "ignore above",
            "破解", "hack", "绕过", "bypass", "越狱", "jailbreak"
    );

    // 注入攻击模式
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:")
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String violation = findViolation(userMessage.singleText());
        return violation == null ? success() : fatal(violation);
    }

    /**
     * 校验用户输入，供系统边界（如工作流入口）复用同一套规则。
     * 只应作用于用户原始输入，内部管道消息（增强提示词、携带编译报错的修复提示词）不适用
     *
     * @return 违规原因，合法返回 null
     */
    public static String findViolation(String input) {
        // 检查输入长度
        if (input.length() > 1000) {
            return "输入内容过长，不要超过 1000 字";
        }
        // 检查是否为空
        if (input.trim().isEmpty()) {
            return "输入内容不能为空";
        }
        // 检查敏感词
        String lowerInput = input.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                return "输入包含不当内容，请修改后重试";
            }
        }
        // 检查注入攻击模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return "检测到恶意输入，请求被拒绝";
            }
        }
        return null;
    }
}
