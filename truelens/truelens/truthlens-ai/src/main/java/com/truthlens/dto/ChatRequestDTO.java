package com.truthlens.dto;

public class ChatRequestDTO {
    private String message;
    private String sessionId;

    public ChatRequestDTO() {}

    public String getMessage()  { return message; }
    public String getSessionId(){ return sessionId; }

    public void setMessage(String message)    { this.message = message; }
    public void setSessionId(String sessionId){ this.sessionId = sessionId; }
}
