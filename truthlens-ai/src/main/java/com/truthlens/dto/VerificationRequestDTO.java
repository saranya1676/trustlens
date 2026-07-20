package com.truthlens.dto;

import com.truthlens.entity.SubmissionType;
import jakarta.validation.constraints.NotBlank;

public class VerificationRequestDTO {

    @NotBlank(message = "Content cannot be empty")
    private String content;

    private String url;

    private SubmissionType submissionType = SubmissionType.TEXT;

    public VerificationRequestDTO() {}

    public VerificationRequestDTO(String content, String url, SubmissionType submissionType) {
        this.content = content;
        this.url = url;
        this.submissionType = submissionType;
    }

    public String getContent()                  { return content; }
    public String getUrl()                      { return url; }
    public SubmissionType getSubmissionType()   { return submissionType; }

    public void setContent(String content)                  { this.content = content; }
    public void setUrl(String url)                          { this.url = url; }
    public void setSubmissionType(SubmissionType type)      { this.submissionType = type; }
}
