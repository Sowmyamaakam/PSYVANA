package com.example.project;

import java.util.List;

public class RecommendationResponse {
    private String severityLevel;
    private String emergencyMessage;
    private String generalAdvice;
    private String professionalHelp;
    private List<Recommendation> recommendations; // ✅ unified model
    private long timestamp;

    public RecommendationResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }

    public String getEmergencyMessage() { return emergencyMessage; }
    public void setEmergencyMessage(String emergencyMessage) { this.emergencyMessage = emergencyMessage; }

    public String getGeneralAdvice() { return generalAdvice; }
    public void setGeneralAdvice(String generalAdvice) { this.generalAdvice = generalAdvice; }

    public String getProfessionalHelp() { return professionalHelp; }
    public void setProfessionalHelp(String professionalHelp) { this.professionalHelp = professionalHelp; }

    public List<Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Utility
    public boolean hasEmergencyMessage() {
        return emergencyMessage != null && !emergencyMessage.trim().isEmpty();
    }

    public boolean isSevereCase() {
        return "severe".equalsIgnoreCase(severityLevel);
    }

    public int getSeverityColor() {
        if (severityLevel == null) return 0xFF6B7280;
        switch (severityLevel.toLowerCase()) {
            case "severe": return 0xFFEF4444;
            case "moderate": return 0xFFF59E0B;
            case "mild": return 0xFF10B981;
            default: return 0xFF6B7280;
        }
    }
}
