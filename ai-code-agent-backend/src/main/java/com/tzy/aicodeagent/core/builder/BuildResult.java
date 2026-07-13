package com.tzy.aicodeagent.core.builder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 项目构建结果：除成功标志外，携带构建过程的输出信息，
 * 供构建失败时回流到代码生成节点做定点修复
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildResult implements Serializable {

    /**
     * 是否构建成功
     */
    private boolean success;

    /**
     * 失败阶段（npm install / npm run build / 校验 dist）
     */
    private String failedStage;

    /**
     * 构建过程输出（stdout + stderr 合并，失败时用于错误分析）
     */
    private String output;

    public static BuildResult success() {
        return BuildResult.builder().success(true).build();
    }

    public static BuildResult fail(String failedStage, String output) {
        return BuildResult.builder().success(false).failedStage(failedStage).output(output).build();
    }
}
