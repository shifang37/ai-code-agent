package com.tzy.aicodeagent.service;

import com.tzy.aicodeagent.constant.AppConstant;
import com.tzy.aicodeagent.model.entity.App;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 应用封面截图链路手动验证（真实调用 Chrome 截图 + COS 上传 + 数据库更新）。
 * <p>依赖：后端静态资源服务运行在 8123 端口、本机 Chrome、COS 配置有效。</p>
 * <p>验证对象为库中已生成代码的真实应用，成功后该应用将获得封面。</p>
 * <p>默认禁用，需要验证截图链路时移除 @Disabled 手动运行（2026-07-03 已验证通过）。</p>
 */
@Slf4j
@SpringBootTest
@Disabled("手动验证用：真实截图并上传 COS，按需运行")
class AppCoverScreenshotManualTest {

    @Resource
    private AppService appService;

    private static final long EXISTING_APP_ID = 414244598180610048L;

    @Test
    @DisplayName("对已生成代码的应用执行截图，封面应更新为 COS 图片地址")
    void screenshotPipeline() throws Exception {
        App app = appService.getById(EXISTING_APP_ID);
        assertNotNull(app, "验证用应用不存在");
        String previewUrl = String.format("%s/%s_%d/",
                AppConstant.CODE_PREVIEW_HOST, app.getCodeGenType(), EXISTING_APP_ID);
        appService.generateAppScreenshotAsync(EXISTING_APP_ID, previewUrl);
        // 异步虚拟线程执行，轮询等待封面更新（最长 60 秒）
        String cover = null;
        for (int i = 0; i < 30; i++) {
            Thread.sleep(2000);
            cover = appService.getById(EXISTING_APP_ID).getCover();
            if (cover != null && !cover.isBlank()) {
                break;
            }
        }
        log.info("截图完成，封面地址: {}", cover);
        assertNotNull(cover, "封面应已更新（检查 Chrome/COS 配置及 8123 静态服务）");
        assertTrue(cover.startsWith("http"), "封面应为可访问的图片 URL");
    }
}
