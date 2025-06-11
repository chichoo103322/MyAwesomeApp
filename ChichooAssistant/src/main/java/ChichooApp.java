// 请确保你的包名(package)与项目结构一致
// package com.example.chichooassistant;


import static spark.Spark.*;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.json.JSONObject;

public class ChichooApp {

    private static final Map<String, String> users = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        port(4567);
        staticFiles.location("/public");

        // --- 最终版 CORS 跨域配置 ---
        before((request, response) -> {
            String origin = request.headers("Origin");
            // 动态地允许请求的来源，这对处理来自 WebView 的各种特殊 Origin 很重要
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Allow-Credentials", "true");
            response.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Content-Length, Accept, Origin");
        });
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            response.status(200);
            return "OK";

        });
        // --- CORS 配置结束 ---


        System.out.println("Chichoo 智能助手后端启动中...");

        // === 用户注册接口 ===
        post("/api/register", (request, response) -> {
            response.type("application/json");
            try {
                JSONObject json = new JSONObject(request.body());
                String username = json.optString("username", "");
                String password = json.optString("password", "");
                if (username.isEmpty() || password.isEmpty()) {
                    response.status(400);
                    return new JSONObject().put("message", "用户名和密码不能为空").toString();
                }
                if (users.containsKey(username)) {
                    response.status(409);
                    return new JSONObject().put("message", "用户名已存在").toString();
                }
                users.put(username, password);
                System.out.println("新用户注册成功: " + username);
                response.status(201);
                return new JSONObject().put("message", "注册成功").toString();
            } catch (Exception e) {
                e.printStackTrace();
                response.status(500);
                return new JSONObject().put("message", "注册失败，发生内部错误").toString();
            }
        });

        // === 用户登录接口 ===
        post("/api/login", (request, response) -> {
            response.type("application/json");
            try {
                JSONObject json = new JSONObject(request.body());
                String username = json.optString("username", "");
                String password = json.optString("password", "");
                if (users.containsKey(username) && users.get(username).equals(password)) {
                    request.session(true);
                    request.session().attribute("username", username);
                    System.out.println("用户登录成功: " + username + ", Session ID: " + request.session().id());
                    response.status(200);
                    return new JSONObject().put("message", "登录成功").toString();
                } else {
                    response.status(401);
                    return new JSONObject().put("message", "用户名或密码错误").toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.status(500);
                return new JSONObject().put("message", "登录失败，发生内部错误").toString();
            }
        });

        // === 新增：用户修改密码接口 ===
        post("/api/change-password", (request, response) -> {
            response.type("application/json");

            // 检查用户是否已登录
            if (request.session(false) == null || request.session().attribute("username") == null) {
                response.status(401);
                return new JSONObject().put("message", "请先登录").toString();
            }

            try {
                String username = request.session().attribute("username"); // 从会话中获取当前用户名

                JSONObject json = new JSONObject(request.body());
                String oldPassword = json.optString("oldPassword");
                String newPassword = json.optString("newPassword");

                // 验证旧密码是否正确
                if (users.containsKey(username) && users.get(username).equals(oldPassword)) {
                    // 更新为新密码
                    users.put(username, newPassword);
                    System.out.println("用户 '" + username + "' 修改密码成功。");
                    response.status(200);
                    return new JSONObject().put("message", "密码修改成功").toString();
                } else {
                    // 旧密码错误
                    response.status(401);
                    return new JSONObject().put("message", "旧密码不正确").toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.status(500);
                return new JSONObject().put("message", "修改密码时发生内部错误").toString();
            }
        });

        // === 用户登出接口 ===
        get("/api/logout", (request, response) -> {
            response.type("application/json");
            if (request.session(false) != null) {
                String username = request.session().attribute("username");
                request.session().invalidate();
                System.out.println("用户登出成功: " + username);
            }
            return new JSONObject().put("message", "已成功登出").toString();
        });

        // === 获取登录状态的接口 (修复用户名显示问题) ===
        get("/api/status", (request, response) -> {
            response.type("application/json");
            JSONObject statusJson = new JSONObject();

            // 添加调试信息
            System.out.println("检查登录状态 - Session: " +
                    (request.session(false) != null ? request.session().id() : "null"));

            if (request.session(false) != null && request.session().attribute("username") != null) {
                String username = request.session().attribute("username");
                System.out.println("用户已登录: " + username);
                statusJson.put("loggedIn", true);
                statusJson.put("username", username); // 直接放入字符串，不要用Optional包装
            } else {
                System.out.println("用户未登录");
                statusJson.put("loggedIn", false);
            }
            return statusJson.toString();
        });

        // === 文件上传接口 ===
        post("/api/upload", (request, response) -> {
            // 检查登录状态
            if (request.session(false) == null || request.session().attribute("username") == null) {
                response.status(401);
                return "请先登录后再进行操作。";
            }

            request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            response.type("text/plain; charset=utf-8");

            String ollamaApiResponse = "";
            String errorMessage = "";

            try {
                Part filePart = request.raw().getPart("file");
                String uploadedFileName = filePart.getSubmittedFileName();

                System.out.println("开始处理上传的文件: " + uploadedFileName);

                // 1. 使用我们的工具类提取文本
                String extractedText;
                try (InputStream inputStream = filePart.getInputStream()) {
                    extractedText = TextExtractor.extractText(inputStream, uploadedFileName);
                }

                if (extractedText.isEmpty()) {
                    return "无法从文档中提取任何文本内容。";
                }

                System.out.println("文本提取成功，准备发送给Ollama进行分析...");

                // 2. 将提取的文本发送给 Ollama
                String promptForOllama = "请对以下内容进行总结和分析：\n\n" + extractedText;

                String ollamaUrl = "http://172.16.24.136:11434/api/chat";
                String modelName = "deepseek-r1:70b";

                List<Map<String, String>> messagesList = new ArrayList<>();
                Map<String, String> userMessageMap = new HashMap<>();
                userMessageMap.put("role", "user");
                userMessageMap.put("content", promptForOllama);
                messagesList.add(userMessageMap);

                JSONObject requestJsonToOllama = new JSONObject();
                requestJsonToOllama.put("model", modelName);
                requestJsonToOllama.put("messages", messagesList);
                requestJsonToOllama.put("stream", false);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(ollamaUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofMinutes(3))
                        .POST(HttpRequest.BodyPublishers.ofString(requestJsonToOllama.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (httpResponse.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(httpResponse.body());
                    ollamaApiResponse = responseJson.getJSONObject("message").getString("content");
                } else {
                    errorMessage = "Ollama API 请求失败，状态码: " + httpResponse.statusCode();
                }

            } catch (IllegalArgumentException e) {
                errorMessage = e.getMessage();
                response.status(400);
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = "处理文件时发生内部错误: " + e.getMessage();
                response.status(500);
            }

            return !errorMessage.isEmpty() ? "Chichoo 错误: " + errorMessage : ollamaApiResponse;
        });

        // === 聊天接口 (添加更详细的调试信息) ===
        post("/chat", (request, response) -> {
            // 添加详细的会话检查和调试信息
            System.out.println("聊天请求 - Session: " +
                    (request.session(false) != null ? request.session().id() : "null"));

            if (request.session(false) != null) {
                String username = request.session().attribute("username");
                System.out.println("Session中的用户名: " + username);
            }

            if (request.session(false) == null || request.session().attribute("username") == null) {
                System.out.println("聊天请求被拒绝：用户未登录");
                response.status(401);
                return "请先登录后再进行操作。";
            }

            String loggedInUsername = request.session().attribute("username");
            String userMessage = request.body();
            System.out.println("收到来自用户 '" + loggedInUsername + "' 的消息: '" + userMessage + "'");

            String ollamaUrl = "http://172.16.24.136:11434/api/chat";
            String modelName = "deepseek-r1:70b";

            String ollamaApiResponse = "";
            String errorMessage = "";
            try {
                List<Map<String, String>> messagesList = new ArrayList<>();
                Map<String, String> systemMessageMap = new HashMap<>();
                systemMessageMap.put("role", "system");
                systemMessageMap.put("content", "You are Chichoo, a helpful AI assistant.");
                messagesList.add(systemMessageMap);
                Map<String, String> userMessageMap = new HashMap<>();
                userMessageMap.put("role", "user");
                userMessageMap.put("content", userMessage);
                messagesList.add(userMessageMap);

                JSONObject requestJsonToOllama = new JSONObject();
                requestJsonToOllama.put("model", modelName);
                requestJsonToOllama.put("messages", messagesList);
                requestJsonToOllama.put("stream", false);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(ollamaUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJsonToOllama.toString(), StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (httpResponse.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(httpResponse.body());
                    ollamaApiResponse = responseJson.getJSONObject("message").getString("content");
                } else {
                    errorMessage = "Ollama API 请求失败，状态码: " + httpResponse.statusCode();
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = "调用Ollama服务时发生内部错误: " + e.getMessage();
            }
            response.type("text/plain; charset=utf-8");
            return !errorMessage.isEmpty() ? "Chichoo 错误: " + errorMessage : ollamaApiResponse;
        });

        System.out.println("Chichoo 后端已启动，所有接口已就绪。");
    }
}