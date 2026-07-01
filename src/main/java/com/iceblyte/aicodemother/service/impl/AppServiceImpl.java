package com.iceblyte.aicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.iceblyte.aicodemother.constant.AppConstant;
import com.iceblyte.aicodemother.constant.UserConstant;
import com.iceblyte.aicodemother.core.AiCodeGeneratorFacade;
import com.iceblyte.aicodemother.exception.BusinessException;
import com.iceblyte.aicodemother.exception.ErrorCode;
import com.iceblyte.aicodemother.exception.ThrowUtils;
import com.iceblyte.aicodemother.model.dto.app.AppQueryRequest;
import com.iceblyte.aicodemother.model.dto.app.AppVersionCompareRequest;
import com.iceblyte.aicodemother.model.entity.User;
import com.iceblyte.aicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.iceblyte.aicodemother.model.enums.CodeGenTypeEnum;
import com.iceblyte.aicodemother.model.vo.AppVersionCompareVO;
import com.iceblyte.aicodemother.model.vo.AppVersionVO;
import com.iceblyte.aicodemother.model.vo.AppVO;
import com.iceblyte.aicodemother.model.vo.UserVO;
import com.iceblyte.aicodemother.service.ChatHistoryService;
import com.iceblyte.aicodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.iceblyte.aicodemother.model.entity.App;
import com.iceblyte.aicodemother.mapper.AppMapper;
import com.iceblyte.aicodemother.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/iceblyte">程序员iceblyte</a>
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    private static final String CURRENT_VERSION_KEY = "current";

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 5. 通过校验后，添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // 6. 调用 AI 生成代码（流式）
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 7. 收集AI响应内容并在完成后记录到对话历史
        StringBuilder aiResponseBuilder = new StringBuilder();
        return contentFlux
                .map(chunk -> {
                    // 收集AI响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    }
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9. 返回可访问的 URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

    @Override
    public List<AppVersionVO> listAppVersions(Long appId, User loginUser) {
        App app = getAndCheckVersionPermission(appId, loginUser);
        List<AppVersionVO> versionList = new ArrayList<>();
        File currentDir = getCurrentCodeDir(app);
        if (currentDir.exists() && currentDir.isDirectory()) {
            AppVersionVO currentVersion = new AppVersionVO();
            currentVersion.setVersionKey(CURRENT_VERSION_KEY);
            currentVersion.setVersionName("当前版本");
            currentVersion.setCreateTime(toLocalDateTime(currentDir.lastModified()));
            currentVersion.setCurrent(true);
            versionList.add(currentVersion);
        }
        File historyRootDir = getHistoryRootDir(app);
        File[] historyDirs = historyRootDir.listFiles(File::isDirectory);
        if (historyDirs != null) {
            for (File historyDir : historyDirs) {
                AppVersionVO version = new AppVersionVO();
                version.setVersionKey(historyDir.getName());
                version.setVersionName("历史版本 " + historyDir.getName());
                version.setCreateTime(parseVersionTime(historyDir.getName(), historyDir.lastModified()));
                version.setCurrent(false);
                versionList.add(version);
            }
        }
        versionList.sort(Comparator.comparing(AppVersionVO::getCreateTime).reversed());
        return versionList;
    }

    @Override
    public AppVersionCompareVO compareAppVersion(AppVersionCompareRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long appId = request.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        App app = getAndCheckVersionPermission(appId, loginUser);
        File oldVersionDir = resolveVersionDir(app, request.getOldVersionKey());
        File newVersionDir = resolveVersionDir(app, request.getNewVersionKey());
        ThrowUtils.throwIf(!oldVersionDir.exists() || !oldVersionDir.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "旧版本不存在");
        ThrowUtils.throwIf(!newVersionDir.exists() || !newVersionDir.isDirectory(), ErrorCode.NOT_FOUND_ERROR, "新版本不存在");

        List<String> fileList = collectComparableFiles(oldVersionDir, newVersionDir);
        ThrowUtils.throwIf(fileList.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "暂无可对比的代码文件");
        String filePath = StrUtil.blankToDefault(request.getFilePath(), fileList.get(0));
        ThrowUtils.throwIf(!fileList.contains(filePath), ErrorCode.PARAMS_ERROR, "文件路径无效");

        String oldContent = readVersionFile(oldVersionDir, filePath);
        String newContent = readVersionFile(newVersionDir, filePath);
        int[] diffCounts = countLineDiff(oldContent, newContent);

        AppVersionCompareVO compareVO = new AppVersionCompareVO();
        compareVO.setFileList(fileList);
        compareVO.setFilePath(filePath);
        compareVO.setOldContent(oldContent);
        compareVO.setNewContent(newContent);
        compareVO.setRemovals(diffCounts[0]);
        compareVO.setAdditions(diffCounts[1]);
        return compareVO;
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    private App getAndCheckVersionPermission(Long appId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isOwner = app.getUserId().equals(loginUser.getId());
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看版本对比");
        }
        return app;
    }

    private File getCurrentCodeDir(App app) {
        String dirName = app.getCodeGenType() + "_" + app.getId();
        return new File(AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + dirName);
    }

    private File getHistoryRootDir(App app) {
        String dirName = app.getCodeGenType() + "_" + app.getId();
        return new File(AppConstant.CODE_VERSION_ROOT_DIR + File.separator + dirName);
    }

    private File resolveVersionDir(App app, String versionKey) {
        if (CURRENT_VERSION_KEY.equals(versionKey)) {
            return getCurrentCodeDir(app);
        }
        ThrowUtils.throwIf(StrUtil.isBlank(versionKey), ErrorCode.PARAMS_ERROR, "版本标识不能为空");
        ThrowUtils.throwIf(!versionKey.matches("\\d{17}"), ErrorCode.PARAMS_ERROR, "版本标识无效");
        return new File(getHistoryRootDir(app), versionKey);
    }

    private List<String> collectComparableFiles(File oldVersionDir, File newVersionDir) {
        Set<String> fileSet = new LinkedHashSet<>();
        collectTextFiles(oldVersionDir.toPath(), oldVersionDir.toPath(), fileSet);
        collectTextFiles(newVersionDir.toPath(), newVersionDir.toPath(), fileSet);
        return fileSet.stream().sorted().collect(Collectors.toList());
    }

    private void collectTextFiles(Path rootPath, Path currentPath, Set<String> fileSet) {
        if (!Files.exists(currentPath)) {
            return;
        }
        try (var stream = Files.list(currentPath)) {
            stream.forEach(path -> {
                if (Files.isDirectory(path)) {
                    collectTextFiles(rootPath, path, fileSet);
                    return;
                }
                String filename = path.getFileName().toString().toLowerCase();
                if (filename.endsWith(".html") || filename.endsWith(".css") || filename.endsWith(".js")
                        || filename.endsWith(".ts") || filename.endsWith(".json") || filename.endsWith(".md")
                        || filename.endsWith(".txt")) {
                    fileSet.add(rootPath.relativize(path).toString().replace(File.separatorChar, '/'));
                }
            });
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取版本文件失败");
        }
    }

    private String readVersionFile(File versionDir, String relativeFilePath) {
        try {
            Path rootPath = versionDir.toPath().toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(relativeFilePath).normalize();
            if (!filePath.startsWith(rootPath) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                return "";
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取版本文件内容失败");
        }
    }

    private int[] countLineDiff(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\\R", -1);
        String[] newLines = newContent.split("\\R", -1);
        int oldLength = oldLines.length;
        int newLength = newLines.length;
        int[][] dp = new int[oldLength + 1][newLength + 1];
        for (int i = oldLength - 1; i >= 0; i--) {
            for (int j = newLength - 1; j >= 0; j--) {
                if (oldLines[i].equals(newLines[j])) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        int sameLines = dp[0][0];
        return new int[]{oldLength - sameLines, newLength - sameLines};
    }

    private LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    private LocalDateTime parseVersionTime(String versionKey, long fallbackTimestamp) {
        try {
            return LocalDateTime.parse(versionKey, DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        } catch (Exception e) {
            return toLocalDateTime(fallbackTimestamp);
        }
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }
}
