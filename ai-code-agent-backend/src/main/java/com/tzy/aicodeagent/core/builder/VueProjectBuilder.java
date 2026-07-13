package com.tzy.aicodeagent.core.builder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 回传给 AI 修复的构建日志最大长度（报错通常集中在日志尾部，超长时只保留末尾）
     */
    private static final int MAX_OUTPUT_LENGTH = 5000;

    /**
     * 执行命令并捕获输出（stdout + stderr 合并）
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 构建结果，失败时携带命令输出
     */
    private BuildResult executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = new ProcessBuilder(command.split("\\s+"))
                    .directory(workingDir)
                    .redirectErrorStream(true) // 合并 stderr 到 stdout，避免双流读取死锁
                    .start();
            // 单独线程持续读取输出，防止缓冲区写满导致进程阻塞
            StringBuilder outputBuilder = new StringBuilder();
            Thread reader = Thread.ofVirtual().start(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        synchronized (outputBuilder) {
                            outputBuilder.append(line).append('\n');
                        }
                    }
                } catch (Exception ignored) {
                }
            });
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return BuildResult.fail(command, "命令执行超时（" + timeoutSeconds + "秒）:\n" + tail(outputBuilder));
            }
            reader.join(5000);
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return BuildResult.success();
            }
            String output = tail(outputBuilder);
            log.error("命令执行失败，退出码: {}，输出:\n{}", exitCode, output);
            return BuildResult.fail(command, "退出码 " + exitCode + "，输出:\n" + output);
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return BuildResult.fail(command, "执行命令异常: " + e.getMessage());
        }
    }

    /**
     * 截取输出尾部（报错信息一般在末尾）
     */
    private String tail(StringBuilder outputBuilder) {
        synchronized (outputBuilder) {
            String output = outputBuilder.toString();
            if (output.length() <= MAX_OUTPUT_LENGTH) {
                return output;
            }
            return "...(前面日志已截断)...\n" + output.substring(output.length() - MAX_OUTPUT_LENGTH);
        }
    }

    /**
     * 执行 npm install 命令
     */
    private BuildResult executeNpmInstall(File projectDir) {
        log.info("执行 npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private BuildResult executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }


    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        return buildProjectWithResult(projectPath).isSuccess();
    }

    /**
     * 构建 Vue 项目并返回详细结果（失败时携带编译报错，供自修复闭环使用）
     *
     * @param projectPath 项目根目录路径
     * @return 构建结果
     */
    public BuildResult buildProjectWithResult(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return BuildResult.fail("前置检查", "项目目录不存在: " + projectPath);
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return BuildResult.fail("前置检查", "package.json 文件不存在，项目结构不完整");
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 执行 npm install
        BuildResult installResult = executeNpmInstall(projectDir);
        if (!installResult.isSuccess()) {
            log.error("npm install 执行失败");
            return BuildResult.fail("npm install", installResult.getOutput());
        }
        // 执行 npm run build
        BuildResult buildResult = executeNpmBuild(projectDir);
        if (!buildResult.isSuccess()) {
            log.error("npm run build 执行失败");
            return BuildResult.fail("npm run build", buildResult.getOutput());
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return BuildResult.fail("校验 dist", "构建命令执行成功但 dist 目录未生成，请检查构建输出配置");
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return BuildResult.success();
    }

    /**
     * 异步构建项目（不阻塞主流程）
     *
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        // 在单独的线程中执行构建，避免阻塞主流程
        Thread.ofVirtual().name("vue-builder-" + System.currentTimeMillis()).start(() -> {
            try {
                buildProject(projectPath);
            } catch (Exception e) {
                log.error("异步构建 Vue 项目时发生异常: {}", e.getMessage(), e);
            }
        });
    }

}
