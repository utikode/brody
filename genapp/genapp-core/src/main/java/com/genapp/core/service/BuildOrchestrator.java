package com.genapp.core.service;

import com.genapp.core.generator.CodeGenerator;
import com.genapp.core.generator.ComposeCodeGenerator;
import com.genapp.core.generator.SwiftUICodeGenerator;
import com.genapp.core.model.Aplikasi;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrator untuk build process Android dan iOS
 */
@Service
public class BuildOrchestrator {

    private static final String ANDROID_TEMPLATE_DIR = "android-template";
    private static final String IOS_TEMPLATE_DIR = "ios-template";
    private static final String OUTPUT_DIR = "output";

    private final ComposeCodeGenerator composeCodeGenerator;
    private final SwiftUICodeGenerator swiftUICodeGenerator;

    public BuildOrchestrator(ComposeCodeGenerator composeCodeGenerator, 
                            SwiftUICodeGenerator swiftUICodeGenerator) {
        this.composeCodeGenerator = composeCodeGenerator;
        this.swiftUICodeGenerator = swiftUICodeGenerator;
    }

    /**
     * Write generated code to template files
     */
    public Map<String, String> writeFiles(Aplikasi appDefinition) throws IOException {
        Map<String, String> writtenFiles = new HashMap<>();
        
        // Create output directory
        Path outputPath = Paths.get(OUTPUT_DIR, appDefinition.packageName().replace(".", "_"));
        Files.createDirectories(outputPath);

        // Generate and write Android code if platform includes Android
        if (appDefinition.platform() == Aplikasi.Platform.ANDROID || 
            appDefinition.platform() == Aplikasi.Platform.BOTH) {
            
            Path androidOutputPath = outputPath.resolve("android");
            Files.createDirectories(androidOutputPath);
            
            // Copy template
            copyTemplate(ANDROID_TEMPLATE_DIR, androidOutputPath.toString());
            
            // Generate Kotlin code
            String kotlinCode = composeCodeGenerator.generateAppCode(appDefinition);
            
            // Write MainActivity
            String mainActivityPath = androidOutputPath + "/app/src/main/java/" + 
                appDefinition.packageName().replace(".", "/") + "/MainActivity.kt";
            writeFile(mainActivityPath, extractMainActivity(kotlinCode));
            writtenFiles.put("android_main", mainActivityPath);
            
            // Write screens
            for (int i = 0; i < appDefinition.screens().size(); i++) {
                String screenCode = composeCodeGenerator.generateScreenCode(appDefinition, i);
                String screenName = appDefinition.screens().get(i).nama();
                String screenPath = androidOutputPath + "/app/src/main/java/" + 
                    appDefinition.packageName().replace(".", "/") + "/ui/" + screenName + "Screen.kt";
                writeFile(screenPath, screenCode);
                writtenFiles.put("android_screen_" + i, screenPath);
            }
            
            // Write ViewModels
            for (int i = 0; i < appDefinition.screens().size(); i++) {
                String viewModelCode = composeCodeGenerator.generateViewModel(
                    appDefinition.screens().get(i).nama(), appDefinition);
                String screenName = appDefinition.screens().get(i).nama();
                String viewModelPath = androidOutputPath + "/app/src/main/java/" + 
                    appDefinition.packageName().replace(".", "/") + "/viewModel/" + 
                    screenName + "ViewModel.kt";
                writeFile(viewModelPath, viewModelCode);
                writtenFiles.put("android_viewmodel_" + i, viewModelPath);
            }
        }

        // Generate and write iOS code if platform includes iOS
        if (appDefinition.platform() == Aplikasi.Platform.IOS || 
            appDefinition.platform() == Aplikasi.Platform.BOTH) {
            
            Path iosOutputPath = outputPath.resolve("ios");
            Files.createDirectories(iosOutputPath);
            
            // Copy template
            copyTemplate(IOS_TEMPLATE_DIR, iosOutputPath.toString());
            
            // Generate Swift code
            String swiftCode = swiftUICodeGenerator.generateAppCode(appDefinition);
            
            // Write ContentView
            String contentViewPath = iosOutputPath + "/" + appDefinition.nama().replaceAll("\\s+", "") + "App/" + 
                swiftUICodeGenerator.getMainFileName() + ".swift";
            writeFile(contentViewPath, extractContentView(swiftCode));
            writtenFiles.put("ios_content", contentViewPath);
            
            // Write screens
            for (int i = 0; i < appDefinition.screens().size(); i++) {
                String screenCode = swiftUICodeGenerator.generateScreenCode(appDefinition, i);
                String screenName = appDefinition.screens().get(i).nama();
                String screenPath = iosOutputPath + "/" + appDefinition.nama().replaceAll("\\s+", "") + "App/" + 
                    screenName + "View.swift";
                writeFile(screenPath, screenCode);
                writtenFiles.put("ios_screen_" + i, screenPath);
            }
            
            // Write ViewModels
            for (int i = 0; i < appDefinition.screens().size(); i++) {
                String viewModelCode = swiftUICodeGenerator.generateViewModel(
                    appDefinition.screens().get(i).nama(), appDefinition);
                String screenName = appDefinition.screens().get(i).nama();
                String viewModelPath = iosOutputPath + "/" + appDefinition.nama().replaceAll("\\s+", "") + "App/" + 
                    screenName + "ViewModel.swift";
                writeFile(viewModelPath, viewModelCode);
                writtenFiles.put("ios_viewmodel_" + i, viewModelPath);
            }
        }

        return writtenFiles;
    }

    /**
     * Build Android APK
     */
    public String buildAndroid(String projectPath) throws IOException, InterruptedException {
        Path androidPath = Paths.get(projectPath, "android");
        
        if (!Files.exists(androidPath)) {
            throw new IOException("Android project not found at: " + androidPath);
        }

        // Run Gradle build
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(androidPath.toFile());
        
        // Check OS
        String os = System.getProperty("os.name").toLowerCase();
        String gradleCommand = os.contains("win") ? ".\\gradlew.bat" : "./gradlew";
        
        processBuilder.command(gradleCommand, "assembleDebug");
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("Android build failed with exit code: " + exitCode + 
                "\nOutput: " + output.toString());
        }
        
        // Return APK path
        String apkPath = androidPath + "/app/build/outputs/apk/debug/app-debug.apk";
        if (Files.exists(Paths.get(apkPath))) {
            return apkPath;
        } else {
            throw new IOException("APK file not found after build. Expected at: " + apkPath);
        }
    }

    /**
     * Build iOS IPA
     */
    public String buildiOS(String projectPath, String scheme) throws IOException, InterruptedException {
        Path iosPath = Paths.get(projectPath, "ios");
        
        if (!Files.exists(iosPath)) {
            throw new IOException("iOS project not found at: " + iosPath);
        }

        // Run xcodebuild
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(iosPath.toFile());
        processBuilder.command("xcodebuild", "-scheme", scheme, "-configuration", "Release", "archive");
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        // Read output
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("iOS build failed with exit code: " + exitCode + 
                "\nOutput: " + output.toString());
        }
        
        // Return IPA path (simplified - actual IPA export requires additional steps)
        String archivePath = System.getProperty("user.home") + "/Library/Developer/Xcode/Archives/" + 
            java.time.LocalDate.now() + "/" + scheme + ".xcarchive";
        return archivePath;
    }

    /**
     * Copy template files to output directory
     */
    private void copyTemplate(String sourceDir, String targetDir) throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        Path targetPath = Paths.get(targetDir);
        
        if (!Files.exists(sourcePath)) {
            // Create minimal template structure if it doesn't exist
            createMinimalTemplate(sourceDir);
        }
        
        Files.walk(sourcePath).forEach(source -> {
            try {
                Path target = targetPath.resolve(sourcePath.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy template", e);
            }
        });
    }

    /**
     * Create minimal Android template structure
     */
    private void createMinimalTemplate(String templateDir) throws IOException {
        if (templateDir.equals(ANDROID_TEMPLATE_DIR)) {
            // Create Android template structure
            String[] dirs = {
                ANDROID_TEMPLATE_DIR + "/app/src/main/java/com/example/app",
                ANDROID_TEMPLATE_DIR + "/app/src/main/res/values",
                ANDROID_TEMPLATE_DIR + "/gradle/wrapper"
            };
            
            for (String dir : dirs) {
                Files.createDirectories(Paths.get(dir));
            }
            
            // Create build.gradle
            writeFile(ANDROID_TEMPLATE_DIR + "/build.gradle", """
                // Top-level build file
                plugins {
                    id 'com.android.application' version '8.1.0' apply false
                    id 'org.jetbrains.kotlin.android' version '1.9.0' apply false
                }
                """);
            
            writeFile(ANDROID_TEMPLATE_DIR + "/app/build.gradle", """
                plugins {
                    id 'com.android.application'
                    id 'org.jetbrains.kotlin.android'
                    id 'kotlin-kapt'
                    id 'dagger.hilt.android.plugin'
                }
                
                android {
                    namespace 'com.example.app'
                    compileSdk 34
                    
                    defaultConfig {
                        applicationId "com.example.app"
                        minSdk 24
                        targetSdk 34
                        versionCode 1
                        versionName "1.0"
                    }
                    
                    buildTypes {
                        release {
                            minifyEnabled false
                        }
                    }
                    
                    compileOptions {
                        sourceCompatibility JavaVersion.VERSION_17
                        targetCompatibility JavaVersion.VERSION_17
                    }
                    
                    kotlinOptions {
                        jvmTarget = '17'
                    }
                    
                    buildFeatures {
                        compose true
                    }
                    
                    composeOptions {
                        kotlinCompilerExtensionVersion '1.5.4'
                    }
                }
                
                dependencies {
                    implementation 'androidx.core:core-ktx:1.12.0'
                    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
                    implementation 'androidx.activity:activity-compose:1.8.1'
                    implementation platform('androidx.compose:compose-bom:2023.10.01')
                    implementation 'androidx.compose.ui:ui'
                    implementation 'androidx.compose.ui:ui-graphics'
                    implementation 'androidx.compose.ui:ui-tooling-preview'
                    implementation 'androidx.compose.material3:material3'
                    implementation 'androidx.navigation:navigation-compose:2.7.5'
                    implementation 'com.google.dagger:hilt-android:2.48'
                    kapt 'com.google.dagger:hilt-android-compiler:2.48'
                    implementation 'io.supabase:postgrest-kt:1.10.0'
                    implementation 'com.google.android.gms:play-services-ads:22.5.0'
                }
                """);
            
            // Create settings.gradle
            writeFile(ANDROID_TEMPLATE_DIR + "/settings.gradle", """
                pluginManagement {
                    repositories {
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        google()
                        mavenCentral()
                    }
                }
                rootProject.name = "GenApp"
                include ':app'
                """);
            
            // Create gradle.properties
            writeFile(ANDROID_TEMPLATE_DIR + "/gradle.properties", """
                org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
                android.useAndroidX=true
                kotlin.code.style=official
                android.nonTransitiveRClass=true
                """);
            
            // Create gradlew wrapper script (Unix)
            writeFile(ANDROID_TEMPLATE_DIR + "/gradlew", """
                #!/bin/sh
                # Gradle wrapper script - simplified
                exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
                """);
            
            // Create strings.xml
            writeFile(ANDROID_TEMPLATE_DIR + "/app/src/main/res/values/strings.xml", """
                <resources>
                    <string name="app_name">GenApp</string>
                </resources>
                """);
        } else if (templateDir.equals(IOS_TEMPLATE_DIR)) {
            // Create iOS template structure
            Files.createDirectories(Paths.get(IOS_TEMPLATE_DIR + "/GenApp"));
            
            // Create basic Swift file
            writeFile(IOS_TEMPLATE_DIR + "/GenApp/ContentView.swift", """
                import SwiftUI
                
                struct ContentView: View {
                    var body: some View {
                        Text("Hello, World!")
                    }
                }
                """);
        }
    }

    /**
     * Write content to file
     */
    private void writeFile(String path, String content) throws IOException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }

    /**
     * Extract MainActivity from generated Kotlin code
     */
    private String extractMainActivity(String fullCode) {
        // Extract only the MainActivity class
        int startIndex = fullCode.indexOf("class MainActivity");
        if (startIndex == -1) return fullCode;
        
        int endIndex = fullCode.indexOf("}\n\n", startIndex);
        if (endIndex == -1) endIndex = fullCode.length();
        
        return fullCode.substring(0, endIndex + 1);
    }

    /**
     * Extract ContentView from generated Swift code
     */
    private String extractContentView(String fullCode) {
        // Extract only the ContentView and App struct
        int startIndex = fullCode.indexOf("@main");
        if (startIndex == -1) return fullCode;
        
        int endIndex = fullCode.indexOf("\n\nstruct ", startIndex);
        if (endIndex == -1) endIndex = fullCode.indexOf("\n\n@MainActor", startIndex);
        if (endIndex == -1) endIndex = fullCode.length();
        
        return fullCode.substring(0, endIndex);
    }
}
