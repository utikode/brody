package com.genapp.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.genapp.core.model.Aplikasi;
import com.genapp.core.service.BuildOrchestrator;
import com.genapp.core.service.NineRouterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST API Controller untuk GenApp
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GenAppController {

    private final NineRouterService nineRouterService;
    private final BuildOrchestrator buildOrchestrator;
    
    // Store build status
    private final Map<String, BuildStatus> buildStatusMap = new ConcurrentHashMap<>();

    public GenAppController(NineRouterService nineRouterService, 
                           BuildOrchestrator buildOrchestrator) {
        this.nineRouterService = nineRouterService;
        this.buildOrchestrator = buildOrchestrator;
    }

    /**
     * POST /api/chat - Terima pesan user, panggil 9router, return JSON struktur + Preview Code
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        try {
            // Call 9router AI
            JsonNode appStructure = nineRouterService.generateAppStructure(request.message());
            
            // Parse to Aplikasi object
            Aplikasi aplikasi = nineRouterService.parseAplikasi(appStructure);
            
            // Generate preview code (Kotlin)
            String kotlinPreview = generateKotlinPreview(aplikasi);
            
            // Generate preview code (Swift)
            String swiftPreview = generateSwiftPreview(aplikasi);
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("appStructure", appStructure);
            response.put("kotlinCode", kotlinPreview);
            response.put("swiftCode", swiftPreview);
            response.put("message", "App structure generated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * POST /api/build - Terima JSON struktur, jalankan BuildOrchestrator, return URL download
     */
    @PostMapping("/build")
    public ResponseEntity<Map<String, Object>> build(@RequestBody BuildRequest request) {
        try {
            String buildId = "build_" + System.currentTimeMillis();
            
            // Set initial status
            BuildStatus status = new BuildStatus();
            status.setId(buildId);
            status.setStatus("queued");
            status.setProgress(0);
            status.setMessage("Build queued");
            buildStatusMap.put(buildId, status);
            
            // Start build in background
            new Thread(() -> {
                try {
                    status.setStatus("building");
                    status.setProgress(10);
                    status.setMessage("Generating code...");
                    
                    // Write files
                    var writtenFiles = buildOrchestrator.writeFiles(request.appDefinition());
                    status.setProgress(50);
                    status.setMessage("Files written: " + writtenFiles.size());
                    
                    // Build Android if requested
                    if (request.platform() == null || request.platform().equals("android")) {
                        status.setMessage("Building Android APK...");
                        String apkPath = buildOrchestrator.buildAndroid(
                            "output/" + request.appDefinition().packageName().replace(".", "_"));
                        status.setApkPath(apkPath);
                        status.setProgress(80);
                    }
                    
                    // Build iOS if requested
                    if (request.platform() != null && request.platform().equals("ios")) {
                        status.setMessage("Building iOS IPA...");
                        String ipaPath = buildOrchestrator.buildiOS(
                            "output/" + request.appDefinition().packageName().replace(".", "_"),
                            request.appDefinition().nama().replaceAll("\\s+", ""));
                        status.setIpaPath(ipaPath);
                        status.setProgress(90);
                    }
                    
                    status.setStatus("completed");
                    status.setProgress(100);
                    status.setMessage("Build completed successfully");
                    
                } catch (Exception e) {
                    status.setStatus("failed");
                    status.setMessage("Build failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
            
            // Return build ID immediately
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("buildId", buildId);
            response.put("message", "Build started");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * GET /api/status/{buildId} - Cek status build real-time
     */
    @GetMapping("/status/{buildId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String buildId) {
        BuildStatus status = buildStatusMap.get(buildId);
        
        if (status == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Build not found");
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("buildId", status.getId());
        response.put("status", status.getStatus());
        response.put("progress", status.getProgress());
        response.put("message", status.getMessage());
        
        if (status.getApkPath() != null) {
            response.put("apkUrl", "/download/" + status.getApkPath());
        }
        if (status.getIpaPath() != null) {
            response.put("ipaUrl", "/download/" + status.getIpaPath());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/status - List all builds
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAllStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("builds", buildStatusMap.values());
        return ResponseEntity.ok(response);
    }

    private String generateKotlinPreview(Aplikasi aplikasi) {
        StringBuilder preview = new StringBuilder();
        preview.append("// MainActivity.kt\n");
        preview.append("package ").append(aplikasi.packageName()).append("\n\n");
        preview.append("import android.os.Bundle\n");
        preview.append("import androidx.activity.ComponentActivity\n");
        preview.append("// ... (").append(aplikasi.screens().size()).append(" screens)\n");
        return preview.toString();
    }

    private String generateSwiftPreview(Aplikasi aplikasi) {
        StringBuilder preview = new StringBuilder();
        preview.append("// ContentView.swift\n");
        preview.append("import SwiftUI\n\n");
        preview.append("@main\n");
        preview.append("struct ").append(aplikasi.nama().replaceAll("\\s+", "")).append("App: App {\n");
        preview.append("    // ... (").append(aplikasi.screens().size()).append(" screens)\n");
        preview.append("}\n");
        return preview.toString();
    }

    // Request/Response Records
    public record ChatRequest(String message) {}
    
    public record BuildRequest(
        Aplikasi appDefinition,
        String platform
    ) {}
    
    public static class BuildStatus {
        private String id;
        private String status; // queued, building, completed, failed
        private int progress;
        private String message;
        private String apkPath;
        private String ipaPath;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getApkPath() { return apkPath; }
        public void setApkPath(String apkPath) { this.apkPath = apkPath; }
        public String getIpaPath() { return ipaPath; }
        public void setIpaPath(String ipaPath) { this.ipaPath = ipaPath; }
    }
}
