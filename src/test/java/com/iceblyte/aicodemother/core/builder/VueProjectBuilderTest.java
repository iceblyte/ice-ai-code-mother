package com.iceblyte.aicodemother.core.builder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * VueProjectBuilder 单元测试（不依赖 Spring 上下文 / 数据库 / 真实 npm）
 * 通过注入假的 npm.cmd 脚本模拟各种退出码，验证「退出码异常但产物就绪」的容错判定。
 * 仅 Windows 运行（.cmd 脚本依赖 cmd.exe）。
 */
class VueProjectBuilderTest {

    /**
     * 与用户线上日志一致的崩溃退出码（0xC0000409，Node 进程退出阶段崩溃）
     */
    private static final int CRASH_EXIT_CODE = -1073740791;

    @TempDir
    Path tempDir;

    private File projectDir;

    @BeforeEach
    void setUp() throws IOException {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("windows"),
                "假 npm.cmd 脚本仅 Windows 可执行");
        projectDir = tempDir.resolve("vue_project_1").toFile();
        Assertions.assertTrue(projectDir.mkdirs());
        Files.writeString(projectDir.toPath().resolve("package.json"), "{}", StandardCharsets.UTF_8);
    }

    /**
     * 写一个假的 npm 脚本并返回其绝对路径
     */
    private String createFakeNpm(String scriptBody) throws IOException {
        Path script = tempDir.resolve("fake-npm.cmd");
        Files.writeString(script, scriptBody, StandardCharsets.UTF_8);
        return script.toAbsolutePath().toString();
    }

    /**
     * 核心场景（线上 Bug 复现）：npm install 以 0xC0000409 崩溃退出，但 node_modules 已生成——
     * 修复前误判为构建失败，修复后应视为成功并继续后续构建
     */
    @Test
    void installCrashButArtifactReadyShouldSucceed() throws IOException {
        String fakeNpm = createFakeNpm(
                "@echo off\r\n" +
                "if \"%1\"==\"install\" (mkdir node_modules 2>nul & echo pkg> node_modules\\fake-pkg & echo fake install log & exit /b " + CRASH_EXIT_CODE + ")\r\n" +
                "if \"%1\"==\"run\" (mkdir dist 2>nul & echo x> dist\\index.html & echo fake build log & exit /b 0)\r\n");
        VueProjectBuilder builder = new VueProjectBuilder(fakeNpm);
        Assertions.assertTrue(builder.buildProject(projectDir.getAbsolutePath()),
                "npm install 退出码异常但 node_modules 已就绪，应判定构建成功");
        Assertions.assertTrue(new File(projectDir, "node_modules").isDirectory());
    }

    /**
     * npm run build 崩溃退出但 dist/index.html 已生成——同样应容错视为成功
     */
    @Test
    void buildCrashButDistReadyShouldSucceed() throws IOException {
        String fakeNpm = createFakeNpm(
                "@echo off\r\n" +
                "if \"%1\"==\"install\" (mkdir node_modules 2>nul & exit /b 0)\r\n" +
                "if \"%1\"==\"run\" (mkdir dist 2>nul & echo x> dist\\index.html & exit /b " + CRASH_EXIT_CODE + ")\r\n");
        VueProjectBuilder builder = new VueProjectBuilder(fakeNpm);
        Assertions.assertTrue(builder.buildProject(projectDir.getAbsolutePath()),
                "npm run build 退出码异常但 dist/index.html 已就绪，应判定构建成功");
    }

    /**
     * 真实失败不能误判：退出码非零且产物不存在——必须判定失败
     */
    @Test
    void realInstallFailureShouldFail() throws IOException {
        String fakeNpm = createFakeNpm(
                "@echo off\r\n" +
                "if \"%1\"==\"install\" (echo npm ERR! network timeout & exit /b 1)\r\n");
        VueProjectBuilder builder = new VueProjectBuilder(fakeNpm);
        Assertions.assertFalse(builder.buildProject(projectDir.getAbsolutePath()),
                "npm install 真失败（无 node_modules），不得误判为成功");
    }

    /**
     * build 真失败（无 dist/index.html）不得误判为成功
     */
    @Test
    void realBuildFailureShouldFail() throws IOException {
        String fakeNpm = createFakeNpm(
                "@echo off\r\n" +
                "if \"%1\"==\"install\" (mkdir node_modules 2>nul & exit /b 0)\r\n" +
                "if \"%1\"==\"run\" (echo build error & exit /b 2)\r\n");
        VueProjectBuilder builder = new VueProjectBuilder(fakeNpm);
        Assertions.assertFalse(builder.buildProject(projectDir.getAbsolutePath()),
                "npm run build 真失败（无 dist/index.html），不得误判为成功");
    }

    /**
     * 正常路径零回归：全部退出码 0 且产物齐备 → 成功
     */
    @Test
    void normalSuccessShouldSucceed() throws IOException {
        String fakeNpm = createFakeNpm(
                "@echo off\r\n" +
                "if \"%1\"==\"install\" (mkdir node_modules 2>nul & echo added 100 packages & exit /b 0)\r\n" +
                "if \"%1\"==\"run\" (mkdir dist 2>nul & echo x> dist\\index.html & echo built successfully & exit /b 0)\r\n");
        VueProjectBuilder builder = new VueProjectBuilder(fakeNpm);
        Assertions.assertTrue(builder.buildProject(projectDir.getAbsolutePath()));
    }
}
