package com.iceblyte.aicodemother.core.builder;

/**
 * 构建进度监听器
 * <p>
 * 由 {@link VueProjectBuilder} 在构建过程中回调，
 * {@link BuildTask} 提供实现将进度转发给 SSE 订阅者。
 */
public interface BuildProgressListener {

    /**
     * 空实现：不关心进度时使用（保持原有同步构建行为）
     */
    BuildProgressListener NO_OP = new BuildProgressListener() {
    };

    /**
     * 阶段进度变更
     *
     * @param phase   构建阶段
     * @param status  阶段状态
     * @param message 进度描述
     */
    default void onPhase(BuildPhaseEnum phase, BuildStatusEnum status, String message) {
    }

    /**
     * 构建日志行
     *
     * @param line 日志原文
     */
    default void onLog(String line) {
    }

    /**
     * 子进程已启动（用于注册 Process，以便取消构建时强制终止）
     *
     * @param process 构建子进程
     */
    default void onProcessStarted(Process process) {
    }

    /**
     * 构建任务是否已被取消（构建器在启动每个命令前应主动检查，及时停止）
     */
    default boolean isCancelled() {
        return false;
    }
}
