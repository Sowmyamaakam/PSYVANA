package com.example.project;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.*;

public class RecommendationService {

    private static final String OPENROUTER_API_KEY = "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"; // Replace with your Hugging Face API token
    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "openai/gpt-oss-20b:free";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private OkHttpClient client;

    public RecommendationService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        client = new OkHttpClient();
    }

    // Callback interface for async responses
    public interface GPTRecommendationCallback {
        void onSuccess(RecommendationResponse response);
        void onError(String error);
    }

    public void getPersonalizedRecommendations(UserAssessment assessment, GPTRecommendationCallback callback) {
        try {
            String severity = assessment.getOverallSeverity();
            String prompt = buildPrompt(assessment, severity);
            makeOpenRouterRequest(prompt, assessment, callback);
        } catch (Exception e) {
            callback.onError("Failed to create recommendation request");
        }
    }

    private String buildPrompt(UserAssessment assessment, String severity) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a compassionate and licensed mental health professional.\n\n");

        // Include only the scores we have
        prompt.append("USER ASSESSMENT RESULTS:\n");
        prompt.append("- Depression Score: ").append(assessment.getDepressionScore()).append("/27\n");
        prompt.append("- Anxiety Score: ").append(assessment.getAnxietyScore()).append("/21\n");
        prompt.append("- Stress Score: ").append(assessment.getStressScore()).append("/40\n");
        prompt.append("- Overall Severity: ").append(severity.toUpperCase()).append("\n\n");

        // Instructions for structured output
        prompt.append("Based on these scores, provide exactly 4 personalized recommendations in JSON format with the following fields:\n");
        prompt.append("- title (short descriptive name of the activity)\n");
        prompt.append("- type (e.g., exercise, breathing, meditation, lifestyle, therapy)\n");
        prompt.append("- duration (approximate time in minutes)\n");
        prompt.append("- difficulty (beginner, intermediate, advanced)\n");
        prompt.append("- description (clear instructions for the user)\n");
        prompt.append("- why_helpful (explain why this will help)\n");
        prompt.append("- priority (high, medium, low)\n\n");

        // Optional fields
        prompt.append("Optionally, include:\n");
        prompt.append("- general_advice (daily mental health advice)\n");
        prompt.append("- emergency_message (if the scores indicate severe or crisis level)\n");
        prompt.append("- professional_help (if referral to a licensed professional is recommended)\n\n");

        prompt.append("Your response should be safe, practical, actionable, and compassionate, like advice from a real mental health professional.");

        return prompt.toString();
    }


    // ✅ This function now sends requests in OpenRouter (OpenAI) format
    private void makeOpenRouterRequest(String prompt, UserAssessment assessment, GPTRecommendationCallback callback) {
        try {
            // Build OpenAI-style request
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            requestBody.put("messages", messages);

            RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(OPENROUTER_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + OPENROUTER_API_KEY)
                    .addHeader("HTTP-Referer", "https://yourappname.com")  // optional but recommended
                    .addHeader("X-Title", "Mental Health Assistant")
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject json = new JSONObject(responseBody);

                            String content = json
                                    .getJSONArray("choices")
                                    .getJSONObject(0)
                                    .getJSONObject("message")
                                    .getString("content");

                            RecommendationResponse recResponse = parseGPTResponse(content, assessment);
                            saveToFirebase(recResponse);
                            callback.onSuccess(recResponse);

                        } catch (Exception e) {
                            callback.onError("Failed to parse AI response");
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No response body";
                        callback.onError("AI service error: " + response.code() + " | " + errorBody);
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Request creation error: " + e.getMessage());
        }
    }

    private RecommendationResponse parseGPTResponse(String text, UserAssessment assessment) {
        try {
            int start = text.indexOf("{");
            int end = text.lastIndexOf("}");
            String jsonStr = (start != -1 && end != -1 && end > start) ? text.substring(start, end + 1) : "{}";

            JSONObject json = new JSONObject(jsonStr);

            RecommendationResponse response = new RecommendationResponse();
            String severity = assessment.getOverallSeverity();
            response.setSeverityLevel(severity);
            response.setGeneralAdvice(json.optString("general_advice", "Follow these daily activities for better mental health."));
            response.setEmergencyMessage(json.optString("emergency_message", severity.equals("crisis") ? "Contact a professional immediately." : ""));
            response.setProfessionalHelp(json.optString("professional_help", severity.equals("crisis") ? "Connect with a licensed mental health professional." : ""));

            JSONArray recsArray = json.optJSONArray("recommendations");
            List<Recommendation> recommendations = new ArrayList<>();

            if (recsArray != null) {
                for (int i = 0; i < recsArray.length(); i++) {
                    JSONObject recObj = recsArray.getJSONObject(i);
                    Recommendation rec = new Recommendation(
                            recObj.optString("title", "Activity " + (i + 1)),
                            recObj.optString("type", "general"),
                            recObj.optString("duration", "10 min"),
                            recObj.optString("difficulty", "beginner"),
                            recObj.optString("description", ""),
                            recObj.optString("why_helpful", ""),
                            recObj.optString("priority", "medium")
                    );
                    recommendations.add(rec);
                }
            }

            response.setRecommendations(recommendations);

            if (severity.equals("severe") || severity.equals("crisis")) {
                response.setEmergencyMessage("Your assessment indicates a severe condition. Please contact a mental health professional immediately.");
                response.setProfessionalHelp("Click here to connect with a licensed professional.");
            }

            return response;

        } catch (Exception e) {
            return fallbackResponse(assessment);
        }
    }

    private RecommendationResponse fallbackResponse(UserAssessment assessment) {
        RecommendationResponse response = new RecommendationResponse();
        String severity = assessment.getOverallSeverity();
        response.setSeverityLevel(severity);
        response.setGeneralAdvice("Breathe deeply and take things one step at a time.");

        List<Recommendation> recs = new ArrayList<>();
        recs.add(new Recommendation("Deep Breathing", "breathing", "5 min", "beginner",
                "Inhale 4 counts, hold 4, exhale 6, repeat 5 times", "Calms the nervous system", "medium"));
        response.setRecommendations(recs);

        if (severity.equals("severe") || severity.equals("crisis")) {
            response.setEmergencyMessage("Your assessment indicates a severe condition. Please contact a mental health professional immediately.");
            response.setProfessionalHelp("Connect with a licensed professional.");
        }

        return response;
    }

    private void saveToFirebase(RecommendationResponse response) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("gpt_recommendations")
                .document(String.valueOf(System.currentTimeMillis()))
                .set(response);
    }
}
