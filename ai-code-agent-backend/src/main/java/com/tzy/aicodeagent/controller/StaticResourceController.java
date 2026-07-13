package com.tzy.aicodeagent.controller;

import com.tzy.aicodeagent.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 应用生成根目录（用于浏览）
    private static final String PREVIEW_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 预览页 CSP：sandbox 指令使 AI 生成的页面运行在 opaque origin 中——
     * 即使被恶意提示词诱导生成窃取脚本，也无法读取主站 Cookie、localStorage，
     * 无法以登录态调用主站 API（不含 allow-same-origin，防存储型 XSS）
     */
    private static final String PREVIEW_CSP =
            "sandbox allow-scripts allow-forms allow-popups allow-modals";

    /**
     * 可视化编辑器代理脚本：注入到预览 HTML 中，通过 postMessage 与主站通信，
     * 替代主站直接跨 iframe 访问 contentDocument（安全隔离后不再同源）
     */
    private static final String EDITOR_AGENT_SCRIPT_TAG =
            "<script src=\"/api/visual-editor-agent.js\"></script>";

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }
            // 构建文件路径
            File file = new File(PREVIEW_ROOT_DIR + "/" + deployKey + resourcePath);
            // 路径穿越防护：规范化后必须仍在预览根目录内
            File rootDir = new File(PREVIEW_ROOT_DIR);
            if (!file.getCanonicalPath().startsWith(rootDir.getCanonicalPath() + File.separator)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = getContentTypeWithCharset(file.getPath());
            // HTML 文档：注入可视化编辑器代理脚本，并附加 CSP 沙箱隔离头
            if (contentType.startsWith("text/html")) {
                String html = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                html = injectEditorAgent(html);
                return ResponseEntity.ok()
                        .header("Content-Type", contentType)
                        .header("Content-Security-Policy", PREVIEW_CSP)
                        .header("X-Content-Type-Options", "nosniff")
                        .body(new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8)));
            }
            // 其他静态资源直接返回
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("X-Content-Type-Options", "nosniff")
                    .body(new FileSystemResource(file));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 在 HTML 中注入可视化编辑器代理脚本（尽量放在 body 末尾）
     */
    private String injectEditorAgent(String html) {
        if (html.contains(EDITOR_AGENT_SCRIPT_TAG)) {
            return html;
        }
        int bodyEnd = html.lastIndexOf("</body>");
        if (bodyEnd >= 0) {
            return html.substring(0, bodyEnd) + EDITOR_AGENT_SCRIPT_TAG + html.substring(bodyEnd);
        }
        int htmlEnd = html.lastIndexOf("</html>");
        if (htmlEnd >= 0) {
            return html.substring(0, htmlEnd) + EDITOR_AGENT_SCRIPT_TAG + html.substring(htmlEnd);
        }
        return html + EDITOR_AGENT_SCRIPT_TAG;
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
