package com.example.backend.dto;

public class UserStats {
    private int totalUsers;
    private int onlineUsers;
    private int totalArticles;
    private int publishedArticles;
    private int pendingArticles;
    private int pendingComments;
    private int pendingCount;

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
    public int getOnlineUsers() { return onlineUsers; }
    public void setOnlineUsers(int onlineUsers) { this.onlineUsers = onlineUsers; }
    public int getTotalArticles() { return totalArticles; }
    public void setTotalArticles(int totalArticles) { this.totalArticles = totalArticles; }
    public int getPublishedArticles() { return publishedArticles; }
    public void setPublishedArticles(int publishedArticles) { this.publishedArticles = publishedArticles; }
    public int getPendingArticles() { return pendingArticles; }
    public void setPendingArticles(int pendingArticles) { this.pendingArticles = pendingArticles; }
    public int getPendingComments() { return pendingComments; }
    public void setPendingComments(int pendingComments) { this.pendingComments = pendingComments; }
    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
}
