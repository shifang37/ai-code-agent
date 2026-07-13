package com.tzy.aicodeagent.langgraph4j.node;

import com.tzy.aicodeagent.constant.AppConstant;
import com.tzy.aicodeagent.core.AiCodeGeneratorFacade;
import com.tzy.aicodeagent.langgraph4j.model.QualityResult;
import com.tzy.aicodeagent.langgraph4j.state.WorkflowContext;
import com.tzy.aicodeagent.model.enums.CodeGenTypeEnum;
import com.tzy.aicodeagent.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class CodeGeneratorNode {

    /**
     * 修复提示词中最多附带的相关源文件数
     */
    private static final int MAX_RELATED_FILES = 3;

    /**
     * 单个相关源文件内容的最大长度
     */
    private static final int MAX_FILE_CONTENT_LENGTH = 4000;

    /**
     * 从构建报错中提取源文件路径（如 src/components/Foo.vue、src/main.ts）
     */
    private static final Pattern SOURCE_FILE_PATTERN =
            Pattern.compile("(?:[\\w.-]+[/\\\\])*[\\w.-]+\\.(?:vue|ts|tsx|js|jsx|json|css|scss|html)");

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 代码生成");

            // 使用增强提示词作为发给 AI 的用户消息
            String userMessage = buildUserMessage(context);
            CodeGenTypeEnum generationType = context.getGenerationType();
            // 获取 AI 代码生成外观服务
            AiCodeGeneratorFacade codeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            log.info("开始生成代码，类型: {} ({})", generationType.getValue(), generationType.getText());
            // 先使用固定的 appId (后续再整合到业务中)
            Long appId = 0L;
            // 调用流式代码生成（内部管道：提示词为系统构造，用户输入已在工作流入口校验）
            Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeStream(userMessage, generationType, appId, true);
            // 同步等待流式输出完成
            codeStream.blockLast(Duration.ofMinutes(10)); // 最多等待 10 分钟
            // 根据类型设置生成目录
            String generatedCodeDir = String.format("%s/%s_%s", AppConstant.CODE_OUTPUT_ROOT_DIR, generationType.getValue(), appId);
            log.info("AI 代码生成完成，生成目录: {}", generatedCodeDir);

            // 更新状态
            context.setCurrentStep("代码生成");
            context.setGeneratedCodeDir(generatedCodeDir);
            // 构建报错已消费，置空避免影响后续轮次
            context.setBuildErrorMessage(null);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 构造用户消息：优先处理构建失败的定点修复，其次处理质检失败的重新生成
     */
    private static String buildUserMessage(WorkflowContext context) {
        // 存在构建报错：说明进入自修复闭环，用真实编译器反馈驱动定点修复
        String buildErrorMessage = context.getBuildErrorMessage();
        if (buildErrorMessage != null && !buildErrorMessage.isBlank()) {
            return buildBuildFixPrompt(context);
        }
        String userMessage = context.getEnhancedPrompt();
        // 检查是否存在质检失败结果
        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            // 直接将错误修复信息作为新的提示词（起到了修改的作用）
            userMessage = buildErrorFixPrompt(qualityResult);
        }
        return userMessage;
    }

    /**
     * 构造构建失败的定点修复提示词：编译报错 + 报错涉及的源文件内容
     */
    private static String buildBuildFixPrompt(WorkflowContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("上一轮生成的项目执行 npm 构建失败，请根据下面的真实编译报错定点修复相关文件，")
                .append("不要重写整个项目，只修改导致报错的文件。\n\n")
                .append("## 构建报错信息（第 ").append(context.getBuildRetryCount()).append(" 次失败）\n")
                .append("```\n").append(context.getBuildErrorMessage()).append("\n```\n");
        // 从报错中提取涉及的源文件，附上当前内容帮助模型精准定位
        String relatedFiles = collectRelatedFiles(context);
        if (!relatedFiles.isEmpty()) {
            prompt.append("\n## 报错涉及的文件当前内容\n").append(relatedFiles);
        }
        prompt.append("\n请修复上述编译错误，确保 npm run build 能够成功执行。");
        return prompt.toString();
    }

    /**
     * 从构建报错中提取源文件路径，读取生成目录下对应文件的内容
     */
    private static String collectRelatedFiles(WorkflowContext context) {
        String generatedCodeDir = context.getGeneratedCodeDir();
        if (generatedCodeDir == null) {
            return "";
        }
        Set<String> filePaths = new LinkedHashSet<>();
        Matcher matcher = SOURCE_FILE_PATTERN.matcher(context.getBuildErrorMessage());
        while (matcher.find() && filePaths.size() < MAX_RELATED_FILES) {
            String path = matcher.group().replace('\\', '/');
            // 过滤依赖目录和构建产物
            if (path.contains("node_modules") || path.contains("dist/") || path.equals("package-lock.json")) {
                continue;
            }
            filePaths.add(path);
        }
        StringBuilder result = new StringBuilder();
        for (String path : filePaths) {
            File file = new File(generatedCodeDir, path);
            if (!file.exists() || !file.isFile()) {
                continue;
            }
            try {
                String content = Files.readString(file.toPath());
                if (content.length() > MAX_FILE_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_FILE_CONTENT_LENGTH) + "\n...(内容过长已截断)";
                }
                result.append("### ").append(path).append("\n")
                        .append("```\n").append(content).append("\n```\n");
            } catch (Exception e) {
                log.warn("读取报错相关文件失败: {}", file.getAbsolutePath());
            }
        }
        return result.toString();
    }

    /**
     * 判断质检是否失败
     */
    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    /**
     * 构造错误修复提示词
     */
    private static String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 上次生成的代码存在以下问题，请修复：\n");
        // 添加错误列表
        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));
        // 添加修复建议（如果有）
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## 修复建议：\n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\n请根据上述问题和建议重新生成代码，确保修复所有提到的问题。");
        return errorInfo.toString();
    }

}
