package com.genapp.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

/**
 * Service untuk berkomunikasi dengan 9router AI API
 */
@Service
public class NineRouterService {

    private static final String API_ENDPOINT = "https://9router.decolua.com/v1/chat/completions";
    private static final String SYSTEM_PROMPT = """
        You are a GenApp Architect. Your task is to convert user's app requirements into a structured JSON format.
        
        Output ONLY valid JSON matching this schema:
        {
            "nama": "string",
            "packageName": "string (e.g., com.example.app)",
            "platform": "ANDROID" | "IOS" | "BOTH",
            "version": "string",
            "description": "string",
            "monetization": {
                "enabled": boolean,
                "admobAppId": "string or null",
                "bannerAdUnitId": "string or null",
                "interstitialAdUnitId": "string or null",
                "rewardedAdUnitId": "string or null",
                "facebookPlacementId": "string or null",
                "adFrequency": "LOW" | "MEDIUM" | "HIGH"
            },
            "database": {
                "provider": "SUPABASE" | "FIREBASE" | "NONE",
                "url": "string or null",
                "apiKey": "string or null",
                "anonKey": "string or null",
                "projectId": "string or null",
                "tables": ["string"] or null
            },
            "screens": [
                {
                    "nama": "string",
                    "tipe": "SPLASH" | "LOGIN" | "REGISTER" | "HOME" | "PRODUCT_DETAIL" | "CART" | "CHECKOUT" | "PROFILE" | "SETTINGS" | "CUSTOM",
                    "route": "string",
                    "isDefault": boolean,
                    "komponen": [
                        {
                            "tipe": "TEXT" | "BUTTON" | "TEXTFIELD" | "LAZYCOLUMN" | "LAZYROW" | "COLUMN" | "ROW" | "BOX" | "IMAGE" | "ICON" | "CARD" | "SPACER" | "DIVIDER" | "BOTTOMNAVIGATION" | "TOPAPPBAR" | "FLOATINGACTIONBUTTON" | "DIALOG" | "SNACKBAR" | "CUSTOM",
                            "id": "string",
                            "label": "string",
                            "properties": {},
                            "dataSource": "string or null (e.g., supabase_products)",
                            "actionType": "NONE" | "NAVIGATE" | "FETCH_DATA" | "SUBMIT_FORM" | "ADD_TO_CART" | "REMOVE_FROM_CART" | "SHOW_AD" | "DISMISS_AD" | "LOGOUT" | "REFRESH" | "OPEN_URL" | "CUSTOM",
                            "children": [],
                            "styling": {}
                        }
                    ]
                }
            ]
        }
        
        IMPORTANT RULES:
        1. If user mentions "e-commerce" or "toko online", include screens: SPLASH, LOGIN, HOME, PRODUCT_DETAIL, CART, CHECKOUT, PROFILE
        2. If user mentions "iklan" or "ads", set monetization.enabled = true
        3. If user mentions database operations, set database.provider appropriately
        4. For data-driven components (product lists, etc.), set dataSource and actionType = "FETCH_DATA"
        5. For buttons that navigate, set actionType = "NAVIGATE"
        6. For ad-related buttons/components, set actionType = "SHOW_AD"
        7. NEVER include markdown code blocks (```json) in your response - output RAW JSON only
        
        Generate appropriate UI components based on the screen type.
        """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public NineRouterService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Mengirim prompt ke 9router dan mendapatkan respons JSON
     */
    public JsonNode generateAppStructure(String userPrompt) {
        try {
            // Prepare request body
            Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7,
                "max_tokens", 4000
            );

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make request
            ResponseEntity<String> response = restTemplate.exchange(
                API_ENDPOINT,
                HttpMethod.POST,
                entity,
                String.class
            );

            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String content = responseJson
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();

            // Cleanup markdown if present
            String cleanJson = cleanupMarkdown(content);

            // Parse and return the JSON structure
            return objectMapper.readTree(cleanJson);

        } catch (Exception e) {
            throw new RuntimeException("Failed to communicate with 9router API: " + e.getMessage(), e);
        }
    }

    /**
     * Membersihkan markdown code blocks dari respons AI
     */
    private String cleanupMarkdown(String content) {
        // Remove ```json ... ``` blocks
        if (content.contains("```json")) {
            content = content.replaceAll("```json\\s*", "");
            content = content.replaceAll("```\\s*", "");
        } else if (content.contains("```")) {
            content = content.replaceAll("```\\s*", "");
        }
        return content.trim();
    }

    /**
     * Convert JsonNode to Aplikasi object
     */
    public com.genapp.core.model.Aplikasi parseAplikasi(JsonNode jsonNode) {
        try {
            return objectMapper.treeToValue(jsonNode, com.genapp.core.model.Aplikasi.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse aplikasi JSON: " + e.getMessage(), e);
        }
    }
}
