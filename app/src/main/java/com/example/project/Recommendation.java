package com.example.project;

public class Recommendation {
    private String title;
    private String type;
    private String duration;
    private String difficulty;
    private String description;
    private String reason;          // whyHelpful from GPT
    private String severityMatch;   // from GPT severity
    private String priority;        // high/medium/low
    private String videoUrl;
    private String audioUrl;
    private String contentText;
    private boolean isCompleted;
    private int rating;
    private long timestamp;

    public Recommendation() {
        this.timestamp = System.currentTimeMillis();
        this.isCompleted = false;
        this.rating = 0;
        this.priority = "medium";
    }

    public Recommendation(String title, String type, String duration, String difficulty,
                          String description, String reason, String severityMatch) {
        this();
        this.title = title;
        this.type = type;
        this.duration = duration;
        this.difficulty = difficulty;
        this.description = description;
        this.reason = reason;
        this.severityMatch = severityMatch;
    }

    // Getters/Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSeverityMatch() { return severityMatch; }
    public void setSeverityMatch(String severityMatch) { this.severityMatch = severityMatch; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Utility methods
    public int getTypeColor() {
        switch (type.toLowerCase()) {
            case "meditation": return 0xFFA855F7; // Purple
            case "breathing": return 0xFF3B82F6; // Blue
            case "journaling": return 0xFF10B981; // Green
            case "exercise": return 0xFFF59E0B;  // Orange
            case "crisis_support": return 0xFFEF4444; // Red
            default: return 0xFF6B7280; // Gray
        }
    }

    public int getTypeIcon() {
        switch (type.toLowerCase()) {
            case "meditation": return android.R.drawable.ic_menu_compass;
            case "breathing": return android.R.drawable.ic_menu_recent_history;
            case "journaling": return android.R.drawable.ic_menu_edit;
            case "exercise": return android.R.drawable.ic_menu_directions;
            case "crisis_support": return android.R.drawable.ic_dialog_alert;
            default: return android.R.drawable.ic_menu_info_details;
        }
    }

    public String getPriorityDisplay() {
        switch (priority.toLowerCase()) {
            case "high": return "🔴 High Priority";
            case "medium": return "🟡 Medium Priority";
            case "low": return "🟢 Low Priority";
            default: return "Medium Priority";
        }
    }
}
