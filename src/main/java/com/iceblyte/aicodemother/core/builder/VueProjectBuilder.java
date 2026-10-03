package com.iceblyte.aicodemother.core.builder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 构建 Vue 项目
 * <p>
 * 支持两种用法：
 * <ul>
 *     <li>{@link #buildProject(String)} 同步构建，不关心进度（部署、工作流节点等场景沿用）</li>
 *     <li>{@link #buildProject(String, BuildProgressListener)} 同步构建并通过监听器上报
 *     阶段进度与实时日志（配合 {@link BuildProgressManager} 异步执行 + SSE 推送）</li>
 * </ul>
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * 单条命令最多向前端转发的日志行数（防止异常庞大的输出拖垮 SSE）
     */
    private static final int MAX_LOG_LINES = 500;

    /**
     * npm install 超时时间（秒）
     */
    private static final int INSTALL_TIMEOUT_SECONDS = 300;

    /**
     * npm run build 超时时间（秒）
     */
    private static final int BUILD_TIMEOUT_SECONDS = 180;

    /**
     * 构建 Vue 项目（不关心进度）
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        return buildProject(projectPath, BuildProgressListener.NO_OP);
    }

    /**
     * 构建 Vue 项目（通过监听器上报进度与日志）
     *
     * @param projectPath 项目根目录路径
     * @param listener    构建进度监听器
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath, BuildProgressListener listener) {
        File projectDir = new File(projectPath);
        // 1. 预检查
        listener.onPhase(BuildPhaseEnum.PREPARE, BuildStatusEnum.RUNNING, "正在检查项目目录...");
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            listener.onPhase(BuildPhaseEnum.PREPARE, BuildStatusEnum.FAILED, "项目目录不存在: " + projectPath);
            return false;
        }
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            listener.onPhase(BuildPhaseEnum.PREPARE, BuildStatusEnum.FAILED, "package.json 文件不存在: " + packageJson.getAbsolutePath());
            return false;
        }
        listener.onPhase(BuildPhaseEnum.PREPARE, BuildStatusEnum.SUCCESS, "项目文件检查通过");
        // 2. 执行 npm install
        if (listener.isCancelled()) {
            return false;
        }
        listener.onPhase(BuildPhaseEnum.INSTALL, BuildStatusEnum.RUNNING, "正在安装依赖（npm install）...");
        if (!executeNpmCommand(projectDir, "install", BuildPhaseEnum.INSTALL, listener, INSTALL_TIMEOUT_SECONDS)) {
            log.error("npm install 执行失败");
            return false;
        }
        listener.onPhase(BuildPhaseEnum.INSTALL, BuildStatusEnum.SUCCESS, "依赖安装完成");
        // 3. 执行 npm run build
        if (listener.isCancelled()) {
            return false;
        }
        listener.onPhase(BuildPhaseEnum.BUILD, BuildStatusEnum.RUNNING, "正在构建项目（npm run build）...");
        if (!executeNpmCommand(projectDir, "run build", BuildPhaseEnum.BUILD, listener, BUILD_TIMEOUT_SECONDS)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 4. 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            listener.onPhase(BuildPhaseEnum.BUILD, BuildStatusEnum.FAILED, "构建完成但 dist 目录未生成: " + distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        listener.onPhase(BuildPhaseEnum.BUILD, BuildStatusEnum.SUCCESS, "项目构建成功");
        return true;
    }

    /**
     * 执行一条 npm 命令：实时转发输出日志，超时由看门狗线程强制终止进程
     *
     * @param projectDir    工作目录
     * @param commandArgs   npm 子命令（如 "install"、"run build"）
     * @param phase         所属构建阶段（用于失败时上报阶段）
     * @param listener      进度监听器
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeNpmCommand(File projectDir, String commandArgs, BuildPhaseEnum phase,
                                       BuildProgressListener listener, int timeoutSeconds) {
        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add(isWindows() ? "npm.cmd" : "npm");
            for (String arg : commandArgs.split("\\s+")) {
                command.add(arg);
            }
            log.info("在目录 {} 中执行命令: npm {}", projectDir.getAbsolutePath(), commandArgs);
            Process processToWatch = new ProcessBuilder(command)
                    .directory(projectDir)
                    // 合并 stderr 到 stdout，一并转发给前端
                    .redirectErrorStream(true)
                    .start();
            process = processToWatch;
            listener.onProcessStarted(process);

            // 看门狗：到达超时时间仍存活则强制终止，保证读取线程不会永久阻塞
            AtomicBoolean timedOut = new AtomicBoolean(false);
            Thread watchdog = Thread.ofVirtual().name("npm-watchdog").start(() -> {
                try {
                    Thread.sleep(timeoutSeconds * 1000L);
                } catch (InterruptedException e) {
                    return;
                }
                if (processToWatch.isAlive()) {
                    log.error("命令执行超时（{}秒），强制终止进程: npm {}", timeoutSeconds, commandArgs);
                    timedOut.set(true);
                    processToWatch.destroyForcibly();
                }
            });

            // 逐行读取并转发构建日志
            int logLineCount = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(processToWatch.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (logLineCount++ < MAX_LOG_LINES) {
                        listener.onLog(line);
                    }
                }
            }
            // 输出流关闭后进程即将结束（或已被看门狗终止），等待退出码
            processToWatch.waitFor();
            watchdog.interrupt();

            if (timedOut.get()) {
                listener.onPhase(phase, BuildStatusEnum.FAILED,
                        String.format("命令执行超时（%d 秒）: npm %s", timeoutSeconds, commandArgs));
                return false;
            }
            int exitCode = processToWatch.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: npm {}", commandArgs);
                return true;
            }
            log.error("命令执行失败，退出码: {}（npm {}）", exitCode, commandArgs);
            listener.onPhase(phase, BuildStatusEnum.FAILED,
                    String.format("命令执行失败（退出码 %d）: npm %s", exitCode, commandArgs));
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("命令执行被中断: npm {}", commandArgs);
            listener.onPhase(phase, BuildStatusEnum.FAILED, "命令执行被中断: npm " + commandArgs);
            return false;
        } catch (Exception e) {
            log.error("执行命令失败: npm {}, 错误信息: {}", commandArgs, e.getMessage());
            listener.onPhase(phase, BuildStatusEnum.FAILED, "执行命令异常: " + e.getMessage());
            return false;
        } finally {
            // 兜底：异常路径下确保进程不残留
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 操作系统检测
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
