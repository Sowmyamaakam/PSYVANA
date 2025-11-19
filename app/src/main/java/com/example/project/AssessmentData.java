package com.example.project;



public class AssessmentData {
    private long timestamp;
    private int depressionScore;
    private int anxietyScore;
    private int stressScore;
    private String depressionSeverity;
    private String anxietySeverity;
    private String stressSeverity;

    public AssessmentData() {
        // Default constructor required for Firestore
    }

    public AssessmentData(int depressionScore, int anxietyScore, int stressScore,
                          String depressionSeverity, String anxietySeverity, String stressSeverity) {
        this.timestamp = System.currentTimeMillis();
        this.depressionScore = depressionScore;
        this.anxietyScore = anxietyScore;
        this.stressScore = stressScore;
        this.depressionSeverity = depressionSeverity;
        this.anxietySeverity = anxietySeverity;
        this.stressSeverity = stressSeverity;
    }

    // Getters and Setters
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getDepressionScore() { return depressionScore; }
    public void setDepressionScore(int depressionScore) { this.depressionScore = depressionScore; }

    public int getAnxietyScore() { return anxietyScore; }
    public void setAnxietyScore(int anxietyScore) { this.anxietyScore = anxietyScore; }

    public int getStressScore() { return stressScore; }
    public void setStressScore(int stressScore) { this.stressScore = stressScore; }

    public String getDepressionSeverity() { return depressionSeverity; }
    public void setDepressionSeverity(String depressionSeverity) { this.depressionSeverity = depressionSeverity; }

    public String getAnxietySeverity() { return anxietySeverity; }
    public void setAnxietySeverity(String anxietySeverity) { this.anxietySeverity = anxietySeverity; }

    public String getStressSeverity() { return stressSeverity; }
    public void setStressSeverity(String stressSeverity) { this.stressSeverity = stressSeverity; }

    // Helper method to get overall severity
    public String getOverallSeverity() {
        // Determine overall severity based on highest individual severity
        int maxSeverity = 0;

        if (depressionScore != -1) {
            maxSeverity = Math.max(maxSeverity, getSeverityLevel(depressionSeverity));
        }
        if (anxietyScore != -1) {
            maxSeverity = Math.max(maxSeverity, getSeverityLevel(anxietySeverity));
        }
        if (stressScore != -1) {
            maxSeverity = Math.max(maxSeverity, getSeverityLevel(stressSeverity));
        }

        switch (maxSeverity) {
            case 4: return "severe";
            case 3: return "moderate";
            case 2: return "mild";
            default: return "minimal";
        }
    }

    private int getSeverityLevel(String severity) {
        if (severity == null) return 0;
        switch (severity.toLowerCase()) {
            case "severe":
            case "moderately severe":
            case "high":
                return 4;
            case "moderate":
                return 3;
            case "mild":
            case "low":
                return 2;
            case "minimal":
                return 1;
            default:
                return 0;
        }
    }
}