package com.example.backend.config;

public class WebsiteSettings {
  private boolean heartbeatEnabled;
  private int heartbeatRate;
  private int heartbeatTimeout;
  private boolean emailEnabled;
  private String emailFrom;
  private int verificationCodeLength;
  private int verificationCodeExpireMinutes;
  private String smtpHost;
  private int smtpPort;
  private String smtpUsername;
  private String smtpPassword;
  private boolean smtpSsl;
  private boolean captchaRegisterEnabled;
  private boolean captchaForgotPasswordEnabled;
  private boolean captchaLoginEnabled;
  private int captchaNoiseCount;
  private int captchaLineCount;
  private int captchaCodeLength;
  private boolean captchaCaseSensitive;
  private boolean captchaRotateEnabled;
  private boolean articleAuditEnabled;
  private boolean commentAuditEnabled;
  private String siteTitle;
  private String siteIcon;
  private String siteFavicon;
  private String aiProvider;
  private String aiApiKey;
  private String aiBaseUrl;
  private String aiModel;
  private String aiSystemPrompt;
  private String aiArticleAuditPrompt;
  private String aiCommentAuditPrompt;
  private boolean aiReadingEnabled;
  private boolean aiArticleAuditEnabled;
  private boolean aiCommentAuditEnabled;
  private boolean aiArticleAutoAuditEnabled;
    private boolean aiCommentAutoAuditEnabled;
    private boolean aiWritingEnabled;
    private boolean backupEnabled;
    private String backupTime;
    private boolean backupRetentionEnabled;
    private int backupRetentionCount;

    public boolean isBackupEnabled() {
        return backupEnabled;
    }

    public void setBackupEnabled(boolean backupEnabled) {
        this.backupEnabled = backupEnabled;
    }

    public String getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(String backupTime) {
        this.backupTime = backupTime;
    }

    public boolean isBackupRetentionEnabled() {
        return backupRetentionEnabled;
    }

    public void setBackupRetentionEnabled(boolean backupRetentionEnabled) {
        this.backupRetentionEnabled = backupRetentionEnabled;
    }

    public int getBackupRetentionCount() {
        return backupRetentionCount;
    }

    public void setBackupRetentionCount(int backupRetentionCount) {
        this.backupRetentionCount = backupRetentionCount;
    }

    public boolean isAiReadingEnabled() {
        return aiReadingEnabled;
    }

    public void setAiReadingEnabled(boolean aiReadingEnabled) {
        this.aiReadingEnabled = aiReadingEnabled;
    }

    public boolean isAiArticleAuditEnabled() {
        return aiArticleAuditEnabled;
    }

    public void setAiArticleAuditEnabled(boolean aiArticleAuditEnabled) {
        this.aiArticleAuditEnabled = aiArticleAuditEnabled;
    }

    public boolean isAiCommentAuditEnabled() {
        return aiCommentAuditEnabled;
    }

    public void setAiCommentAuditEnabled(boolean aiCommentAuditEnabled) {
        this.aiCommentAuditEnabled = aiCommentAuditEnabled;
    }

    public boolean isAiArticleAutoAuditEnabled() {
        return aiArticleAutoAuditEnabled;
    }

    public void setAiArticleAutoAuditEnabled(boolean aiArticleAutoAuditEnabled) {
        this.aiArticleAutoAuditEnabled = aiArticleAutoAuditEnabled;
    }

    public boolean isAiCommentAutoAuditEnabled() {
        return aiCommentAutoAuditEnabled;
    }

    public void setAiCommentAutoAuditEnabled(boolean aiCommentAutoAuditEnabled) {
        this.aiCommentAutoAuditEnabled = aiCommentAutoAuditEnabled;
    }

    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }

    public void setHeartbeatEnabled(boolean heartbeatEnabled) {
        this.heartbeatEnabled = heartbeatEnabled;
    }

    public int getHeartbeatRate() {
        return heartbeatRate;
    }

    public void setHeartbeatRate(int heartbeatRate) {
        this.heartbeatRate = heartbeatRate;
    }

    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public void setHeartbeatTimeout(int heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public int getVerificationCodeLength() {
        return verificationCodeLength;
    }

    public void setVerificationCodeLength(int verificationCodeLength) {
        this.verificationCodeLength = verificationCodeLength;
    }

    public int getVerificationCodeExpireMinutes() {
        return verificationCodeExpireMinutes;
    }

    public void setVerificationCodeExpireMinutes(int verificationCodeExpireMinutes) {
        this.verificationCodeExpireMinutes = verificationCodeExpireMinutes;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public boolean isSmtpSsl() {
        return smtpSsl;
    }

    public void setSmtpSsl(boolean smtpSsl) {
        this.smtpSsl = smtpSsl;
    }

    public boolean isCaptchaRegisterEnabled() {
        return captchaRegisterEnabled;
    }

    public void setCaptchaRegisterEnabled(boolean captchaRegisterEnabled) {
        this.captchaRegisterEnabled = captchaRegisterEnabled;
    }

    public boolean isCaptchaForgotPasswordEnabled() {
        return captchaForgotPasswordEnabled;
    }

    public void setCaptchaForgotPasswordEnabled(boolean captchaForgotPasswordEnabled) {
        this.captchaForgotPasswordEnabled = captchaForgotPasswordEnabled;
    }

    public boolean isCaptchaLoginEnabled() {
        return captchaLoginEnabled;
    }

    public void setCaptchaLoginEnabled(boolean captchaLoginEnabled) {
        this.captchaLoginEnabled = captchaLoginEnabled;
    }

    public int getCaptchaNoiseCount() {
        return captchaNoiseCount;
    }

    public void setCaptchaNoiseCount(int captchaNoiseCount) {
        this.captchaNoiseCount = captchaNoiseCount;
    }

    public int getCaptchaLineCount() {
        return captchaLineCount;
    }

    public void setCaptchaLineCount(int captchaLineCount) {
        this.captchaLineCount = captchaLineCount;
    }

    public int getCaptchaCodeLength() {
        return captchaCodeLength;
    }

    public void setCaptchaCodeLength(int captchaCodeLength) {
        this.captchaCodeLength = captchaCodeLength;
    }

    public boolean isCaptchaCaseSensitive() {
        return captchaCaseSensitive;
    }

    public void setCaptchaCaseSensitive(boolean captchaCaseSensitive) {
        this.captchaCaseSensitive = captchaCaseSensitive;
    }

    public boolean isCaptchaRotateEnabled() {
        return captchaRotateEnabled;
    }

    public void setCaptchaRotateEnabled(boolean captchaRotateEnabled) {
        this.captchaRotateEnabled = captchaRotateEnabled;
    }

    public boolean isArticleAuditEnabled() {
        return articleAuditEnabled;
    }

    public void setArticleAuditEnabled(boolean articleAuditEnabled) {
        this.articleAuditEnabled = articleAuditEnabled;
    }

    public boolean isCommentAuditEnabled() {
        return commentAuditEnabled;
    }

    public void setCommentAuditEnabled(boolean commentAuditEnabled) {
        this.commentAuditEnabled = commentAuditEnabled;
    }

    public String getSiteTitle() {
        return siteTitle;
    }

    public void setSiteTitle(String siteTitle) {
        this.siteTitle = siteTitle;
    }

    public String getSiteIcon() {
        return siteIcon;
    }

    public void setSiteIcon(String siteIcon) {
        this.siteIcon = siteIcon;
    }

    public String getSiteFavicon() {
        return siteFavicon;
    }

    public void setSiteFavicon(String siteFavicon) {
        this.siteFavicon = siteFavicon;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String getAiApiKey() {
        return aiApiKey;
    }

    public void setAiApiKey(String aiApiKey) {
        this.aiApiKey = aiApiKey;
    }

    public String getAiBaseUrl() {
        return aiBaseUrl;
    }

    public void setAiBaseUrl(String aiBaseUrl) {
        this.aiBaseUrl = aiBaseUrl;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getAiSystemPrompt() {
        return aiSystemPrompt;
    }

    public void setAiSystemPrompt(String aiSystemPrompt) {
        this.aiSystemPrompt = aiSystemPrompt;
    }

    public String getAiArticleAuditPrompt() {
        return aiArticleAuditPrompt;
    }

    public void setAiArticleAuditPrompt(String aiArticleAuditPrompt) {
        this.aiArticleAuditPrompt = aiArticleAuditPrompt;
    }

    public String getAiCommentAuditPrompt() {
        return aiCommentAuditPrompt;
    }

    public void setAiCommentAuditPrompt(String aiCommentAuditPrompt) {
        this.aiCommentAuditPrompt = aiCommentAuditPrompt;
    }

    public boolean isAiWritingEnabled() {
        return aiWritingEnabled;
    }

    public void setAiWritingEnabled(boolean aiWritingEnabled) {
        this.aiWritingEnabled = aiWritingEnabled;
    }
}
