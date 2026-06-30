package com.iceblyte.aicodemother.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.iceblyte.aicodemother.constant.AppConstant;
import com.iceblyte.aicodemother.exception.BusinessException;
import com.iceblyte.aicodemother.exception.ErrorCode;
import com.iceblyte.aicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 抽象代码文件保存器 - 模板方法模式
 *
 * @author iceblyte
 */
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存根目录
    protected static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模板方法：保存代码的标准流程（使用 appId）
     *
     * @param result 代码结果对象
     * @param appId  应用 ID
     * @return 保存的目录
     */
    public final File saveCode(T result, Long appId) {
        // 1. 验证输入
        validateInput(result);
        // 2. 构建基于 appId 的目录
        String baseDirPath = buildUniqueDir(appId);
        // 3. 归档旧版本，避免覆盖后无法对比历史代码
        archiveOldVersion(baseDirPath);
        // 4. 清理当前目录，确保删除的文件不会残留
        FileUtil.clean(baseDirPath);
        // 5. 保存文件（具体实现由子类提供）
        saveFiles(result, baseDirPath);
        // 6. 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param result 代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 构建基于 appId 的目录路径
     *
     * @param appId 应用 ID
     * @return 目录路径
     */
    protected final String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        }
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 归档当前代码目录，后续可用于版本对比
     *
     * @param baseDirPath 当前代码目录
     */
    private void archiveOldVersion(String baseDirPath) {
        File baseDir = new File(baseDirPath);
        File[] files = baseDir.listFiles();
        if (!baseDir.exists() || files == null || files.length == 0) {
            return;
        }
        String appDirName = baseDir.getName();
        String versionKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String versionDirPath = AppConstant.CODE_VERSION_ROOT_DIR + File.separator + appDirName + File.separator + versionKey;
        FileUtil.copyContent(baseDir, new File(versionDirPath), true);
    }

    /**
     * 写入单个文件的工具方法
     *
     * @param dirPath  目录路径
     * @param filename 文件名
     * @param content  文件内容
     */
    protected final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取代码类型（由子类实现）
     *
     * @return 代码生成类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 保存文件的具体实现（由子类实现）
     *
     * @param result      代码结果对象
     * @param baseDirPath 基础目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
