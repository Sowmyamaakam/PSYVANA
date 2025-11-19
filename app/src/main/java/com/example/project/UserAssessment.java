package com.example.project;


import java.util.List;

public class UserAssessment {
    private int moodScore;
    private int anxietyScore;
    private int stressScore;
    private int sleepScore;
    private int availableTime;
    private List<String> preferences;
    private String currentSymptoms;
    private long timestamp;
    private int depressionScore;

    public UserAssessment() {
        this.timestamp = System.currentTimeMillis();
    }

    public UserAssessment(int depressionScore, int anxietyScore, int stressScore,
                          int sleepScore, int availableTime, List<String> preferences) {
        this.depressionScore = depressionScore;
        this.anxietyScore = anxietyScore;
        this.stressScore = stressScore;
        this.sleepScore = sleepScore;
        this.availableTime = availableTime;
        this.preferences = preferences;
        this.timestamp = System.currentTimeMillis();
    }
    // Getter and setter
    public int getDepressionScore() { return depressionScore; }
    public void setDepressionScore(int depressionScore) { this.depressionScore = depressionScore; }

    // Getters and setters
    public int getMoodScore() { return moodScore; }
    public void setMoodScore(int moodScore) { this.moodScore = moodScore; }

    public int getAnxietyScore() { return anxietyScore; }
    public void setAnxietyScore(int anxietyScore) { this.anxietyScore = anxietyScore; }

    public int getStressScore() { return stressScore; }
    public void setStressScore(int stressScore) { this.stressScore = stressScore; }

    public int getSleepScore() { return sleepScore; }
    public void setSleepScore(int sleepScore) { this.sleepScore = sleepScore; }

    public int getAvailableTime() { return availableTime; }
    public void setAvailableTime(int availableTime) { this.availableTime = availableTime; }

    public List<String> getPreferences() { return preferences; }
    public void setPreferences(List<String> preferences) { this.preferences = preferences; }

    public String getCurrentSymptoms() { return currentSymptoms; }
    public void setCurrentSymptoms(String currentSymptoms) { this.currentSymptoms = currentSymptoms; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Calculate overall severity level
    public String getOverallSeverity() {
        double average = (depressionScore + anxietyScore + stressScore ) / 4.0;

        if (average <= 3) return "crisis";
        else if (average <= 5) return "moderate";
        else if (average <= 7) return "mild";
        else return "maintenance";
    }
}

