package com.example.chatapp.models;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String type;      // "text", "image", "file", "voice"
    private String content;   // Chuỗi văn bản HOẶC Storage Download URL
    private long timestamp;
    private boolean isSeen;
    private long duration;    // Thời lượng voice tính theo giây (chỉ dùng khi type = "voice")

    public Message() {}

    public Message(String messageId, String senderId, String receiverId, String type,
                   String content, long timestamp, boolean isSeen, long duration) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
        this.content = content;
        this.timestamp = timestamp;
        this.isSeen = isSeen;
        this.duration = duration;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isSeen() { return isSeen; }
    public void setSeen(boolean seen) { isSeen = seen; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
}
