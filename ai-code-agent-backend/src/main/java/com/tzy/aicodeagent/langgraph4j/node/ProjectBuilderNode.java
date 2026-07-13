package com.tzy.aicodeagent.langgraph4j.node;

import com.tzy.aicodeagent.core.builder.BuildResult;
import com.tzy.aicodeagent.core.builder.VueProjectBuilder;
import com.tzy.aicodeagent.langgraph4j.state.WorkflowContext;
import com.tzy.aicodeagent.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ProjectBuilderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 项目构建");

            // 获取必要的参数
            String generatedCodeDir = context.getGeneratedCodeDir();
            // 一定是 Vue 项目类型：使用 VueProjectBuilder 进行构建
            BuildResult buildResult;
            try {
                VueProjectBuilder vueBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                // 执行 Vue 项目构建（npm install + npm run build），捕获构建输出
                buildResult = vueBuilder.buildProjectWithResult(generatedCodeDir);
            } catch (Exception e) {
                log.error("Vue 项目构建异常: {}", e.getMessage(), e);
                buildResult = BuildResult.fail("构建执行", "构建过程发生异常: " + e.getMessage());
            }

            context.setCurrentStep("项目构建");
            if (buildResult.isSuccess()) {
                // 构建成功，返回 dist 目录路径
                String buildResultDir = generatedCodeDir + File.separator + "dist";
                context.setBuildResultDir(buildResultDir);
                context.setBuildErrorMessage(null);
                log.info("Vue 项目构建成功，dist 目录: {}", buildResultDir);
            } else {
                // 构建失败：报错写入上下文并计数，由工作流条件边决定是否回流到代码生成节点自修复
                context.setBuildRetryCount(context.getBuildRetryCount() + 1);
                context.setBuildErrorMessage(String.format("失败阶段: %s\n%s",
                        buildResult.getFailedStage(), buildResult.getOutput()));
                // 失败时先回退为源码目录，若重试耗尽则以此作为最终结果
                context.setBuildResultDir(generatedCodeDir);
                log.error("Vue 项目第 {} 次构建失败，失败阶段: {}",
                        context.getBuildRetryCount(), buildResult.getFailedStage());
            }
            return WorkflowContext.saveContext(context);
        });
    }
}
