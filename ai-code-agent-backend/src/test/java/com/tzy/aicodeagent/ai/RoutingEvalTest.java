package com.tzy.aicodeagent.ai;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tzy.aicodeagent.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能路由分类准确率评测：批量调用 AiCodeGenTypeRoutingService，
 * 统计三种生成模式（HTML / MULTI_FILE / VUE_PROJECT）的分类准确率与混淆矩阵。
 * <p>
 * 评测集：src/test/resources/routing-eval-dataset.json，60 条口语化需求描述，
 * 三类各 20 条，expected 字段为人工标注的期望生成模式。
 * <p>
 * 运行：mvn test -Dtest=RoutingEvalTest（需 application-local.yml 或环境变量提供 DeepSeek API Key）
 */
public class RoutingEvalTest {

    @Test
    public void evalRoutingAccuracy() {
        // 与生产 routingChatModelPrototype 相同的模型配置
        ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(resolveApiKey())
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-chat")
                .build();
        AiCodeGenTypeRoutingService routingService = AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();

        JSONArray dataset = JSONUtil.parseArray(ResourceUtil.readUtf8Str("routing-eval-dataset.json"));
        int total = 0, correct = 0;
        Map<CodeGenTypeEnum, int[]> perClass = new EnumMap<>(CodeGenTypeEnum.class); // [correct, total]
        Map<CodeGenTypeEnum, Map<CodeGenTypeEnum, Integer>> confusion = new EnumMap<>(CodeGenTypeEnum.class);
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            perClass.put(type, new int[2]);
            confusion.put(type, new EnumMap<>(CodeGenTypeEnum.class));
        }
        List<String> mistakes = new ArrayList<>();

        for (Object entryObj : dataset) {
            JSONObject entry = (JSONObject) entryObj;
            String prompt = entry.getStr("prompt");
            CodeGenTypeEnum expected = CodeGenTypeEnum.valueOf(entry.getStr("expected"));
            CodeGenTypeEnum actual = routingService.routeCodeGenType(prompt);

            total++;
            perClass.get(expected)[1]++;
            confusion.get(expected).merge(actual, 1, Integer::sum);
            boolean ok = expected == actual;
            if (ok) {
                correct++;
                perClass.get(expected)[0]++;
            } else {
                mistakes.add(String.format("期望 %s 实际 %s | %s", expected, actual, prompt));
            }
            System.out.printf("[%02d] %s 期望:%s 实际:%s | %s%n", total, ok ? "✓" : "✗", expected, actual, prompt);
        }

        System.out.println("\n========== 路由评测结果 (" + total + " 条) ==========");
        System.out.printf("总体准确率: %d/%d = %.1f%%%n", correct, total, 100.0 * correct / total);
        for (CodeGenTypeEnum type : CodeGenTypeEnum.values()) {
            int[] stat = perClass.get(type);
            System.out.printf("%-12s 准确率: %d/%d = %.1f%%%n", type, stat[0], stat[1],
                    stat[1] == 0 ? 0 : 100.0 * stat[0] / stat[1]);
        }
        System.out.println("\n混淆矩阵 (行=期望, 列=实际):");
        System.out.printf("%-14s", "");
        for (CodeGenTypeEnum col : CodeGenTypeEnum.values()) {
            System.out.printf("%-14s", col);
        }
        System.out.println();
        for (CodeGenTypeEnum row : CodeGenTypeEnum.values()) {
            System.out.printf("%-14s", row);
            for (CodeGenTypeEnum col : CodeGenTypeEnum.values()) {
                System.out.printf("%-14d", confusion.get(row).getOrDefault(col, 0));
            }
            System.out.println();
        }
        System.out.println("\n错误明细 (" + mistakes.size() + "):");
        mistakes.forEach(m -> System.out.println("  " + m));
    }

    private String resolveApiKey() {
        String env = System.getenv("DEEPSEEK_API_KEY");
        if (env != null && !env.isBlank()) {
            return env;
        }
        String yml = ResourceUtil.readUtf8Str("application-local.yml");
        Matcher matcher = Pattern.compile("api-key:\\s*\"?(sk-\\w+)\"?").matcher(yml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("未找到 DeepSeek API Key（环境变量 DEEPSEEK_API_KEY 或 application-local.yml）");
    }
}
