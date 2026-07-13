package com.tzy.aicodeagent.langgraph4j;

import cn.hutool.json.JSONUtil;
import com.tzy.aicodeagent.langgraph4j.WorkflowApp.CodeGenWorkflow;
import com.tzy.aicodeagent.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 构建自修复闭环对比实验：跑 N 条 VUE_PROJECT 需求的完整工作流，
 * 同一次实验同时得出：
 * 1. 一次构建成功率（buildRetryCount==0 且构建成功，等价于"无自修复闭环"的成功率）
 * 2. 自修复后最终成功率（≤3 轮报错回流修复）
 * 3. 失败案例的平均修复轮数
 * 4. 单条需求端到端耗时
 * <p>
 * 结果逐条追加写入 target/build-eval-results.jsonl（中途失败不丢已有数据）。
 * 运行：mvn test -Dtest=BuildSelfHealEvalTest（需本地 MySQL/Redis 与 DeepSeek Key）
 */
@SpringBootTest
public class BuildSelfHealEvalTest {

    private static final long PER_RUN_TIMEOUT_MINUTES = 25;

    private static final List<String> PROMPTS = List.of(
            "用 Vue 做一个待办事项管理应用，支持任务增删改查、按分类筛选、完成状态切换和本地存储",
            "用 Vue 做一个个人记账本应用，可以记录每笔收支、按月份汇总统计并用图表展示",
            "用 Vue 做一个企业官网工程，多页面路由：首页、产品展示页、关于我们和联系表单页",
            "用 Vue 做一个博客前台应用，包含文章列表页、文章详情页和按标签筛选功能",
            "用 Vue 做一个天气查询应用，支持城市搜索、展示未来一周天气趋势和收藏常用城市",
            "用 Vue 做一个电商商品浏览应用，有商品列表、商品详情和购物车结算流程",
            "用 Vue 做一个任务看板应用，多列布局，支持卡片在不同状态列之间拖拽移动",
            "用 Vue 做一个在线简历生成器，左侧表单编辑个人信息和经历，右侧实时预览简历效果",
            "用 Vue 做一个图书管理系统前端，包含图书列表检索、借阅登记和归还管理",
            "用 Vue 做一个日程管理应用，月历视图展示日程，支持添加删除事件和按颜色分类"
    );

    @Test
    public void evalBuildSelfHeal() throws Exception {
        Path resultFile = Path.of("target", "build-eval-results.jsonl");
        Files.createDirectories(resultFile.getParent());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        List<Map<String, Object>> results = new ArrayList<>();
        int idx = 0;
        for (String prompt : PROMPTS) {
            idx++;
            System.out.printf("%n===== [%d/%d] %s =====%n", idx, PROMPTS.size(), prompt);
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("index", idx);
            record.put("prompt", prompt);
            long start = System.currentTimeMillis();

            Future<WorkflowContext> future = executor.submit(
                    () -> new CodeGenWorkflow().executeWorkflow(prompt));
            try {
                WorkflowContext ctx = future.get(PER_RUN_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                boolean buildSuccess = ctx != null && ctx.getBuildErrorMessage() == null
                        && ctx.getBuildResultDir() != null && ctx.getBuildResultDir().endsWith("dist")
                        && new File(ctx.getBuildResultDir()).exists();
                int retryCount = ctx == null ? -1 : ctx.getBuildRetryCount();
                record.put("generationType", ctx == null ? null : String.valueOf(ctx.getGenerationType()));
                record.put("buildSuccess", buildSuccess);
                record.put("firstBuildSuccess", buildSuccess && retryCount == 0);
                record.put("repairRounds", retryCount);
                record.put("buildResultDir", ctx == null ? null : ctx.getBuildResultDir());
            } catch (TimeoutException e) {
                future.cancel(true);
                record.put("buildSuccess", false);
                record.put("firstBuildSuccess", false);
                record.put("repairRounds", -1);
                record.put("error", "timeout after " + PER_RUN_TIMEOUT_MINUTES + " min");
            } catch (Exception e) {
                record.put("buildSuccess", false);
                record.put("firstBuildSuccess", false);
                record.put("repairRounds", -1);
                record.put("error", String.valueOf(e.getMessage()));
            }
            record.put("durationSeconds", (System.currentTimeMillis() - start) / 1000);
            results.add(record);
            Files.writeString(resultFile, JSONUtil.toJsonStr(record) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println(">>> " + JSONUtil.toJsonStr(record));
        }
        executor.shutdownNow();

        long firstOk = results.stream().filter(r -> Boolean.TRUE.equals(r.get("firstBuildSuccess"))).count();
        long finalOk = results.stream().filter(r -> Boolean.TRUE.equals(r.get("buildSuccess"))).count();
        List<Map<String, Object>> healed = results.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("buildSuccess")) && (int) r.get("repairRounds") > 0)
                .toList();
        double avgRepairRounds = healed.stream().mapToInt(r -> (int) r.get("repairRounds")).average().orElse(0);
        double avgDuration = results.stream().mapToLong(r -> (long) r.get("durationSeconds")).average().orElse(0);

        System.out.println("\n========== 构建自修复实验结果 (" + results.size() + " 条) ==========");
        System.out.printf("一次构建成功率(无自修复): %d/%d = %.0f%%%n", firstOk, results.size(), 100.0 * firstOk / results.size());
        System.out.printf("自修复后最终成功率:       %d/%d = %.0f%%%n", finalOk, results.size(), 100.0 * finalOk / results.size());
        System.out.printf("被修复救回的案例: %d 个，平均修复轮数: %.1f%n", healed.size(), avgRepairRounds);
        System.out.printf("单条需求平均端到端耗时: %.0f 秒%n", avgDuration);
    }
}
