package com.truthlens.controller;

import com.truthlens.dto.ApiResponse;
import com.truthlens.dto.VerificationRequestDTO;
import com.truthlens.dto.VerificationResultDTO;
import com.truthlens.entity.SubmissionType;
import com.truthlens.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/verify")
public class VerificationController {

    private static final Logger log = LoggerFactory.getLogger(VerificationController.class);

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping(value = "/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<VerificationResultDTO>> verifyText(
            @RequestBody VerificationRequestDTO request,
            HttpServletRequest httpRequest) {

        if (StringUtils.isBlank(request.getContent())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Content cannot be empty"));
        }
        request.setSubmissionType(SubmissionType.TEXT);
        log.info("Text verification request, length={}", request.getContent().length());
        VerificationResultDTO result = verificationService.verify(request, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Verification complete", result));
    }

    @PostMapping(value = "/url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<VerificationResultDTO>> verifyUrl(
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {

        String url     = StringUtils.trimToEmpty(body.get("url"));
        String content = StringUtils.trimToEmpty(body.get("content"));
        if (url.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("URL cannot be empty"));
        }
        if (content.isBlank()) {
            content = extractTextFromUrl(url);
        }
        VerificationRequestDTO request = new VerificationRequestDTO(content, url, SubmissionType.URL);
        log.info("URL verification request: {}", url);
        VerificationResultDTO result = verificationService.verify(request, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("URL verification complete", result));
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VerificationResultDTO>> verifyImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "extractedText", defaultValue = "") String extractedText,
            HttpServletRequest httpRequest) {

        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Image file cannot be empty"));
        }
        String ct = StringUtils.defaultString(imageFile.getContentType());
        if (!ct.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only image files are supported"));
        }
        log.info("Image verification request: filename={}", imageFile.getOriginalFilename());
        String text = extractedText.isBlank()
                ? "Image submitted: " + imageFile.getOriginalFilename()
                : extractedText;
        VerificationRequestDTO request = new VerificationRequestDTO(text, null, SubmissionType.IMAGE);
        VerificationResultDTO result = verificationService.verify(request, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Image verification complete", result));
    }

    @GetMapping("/result/{id}")
    public ResponseEntity<ApiResponse<VerificationResultDTO>> getResult(@PathVariable Long id) {
        return verificationService.getResultById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok("Result retrieved", dto)))
                .orElseGet(() -> ResponseEntity.notFound().<ApiResponse<VerificationResultDTO>>build());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok("Verification statistics", verificationService.getStats()));
    }

    private String extractTextFromUrl(String url) {
        return url.replaceAll("https?://", "").replaceAll("www\\.", "")
                  .replaceAll("[/\\-_?=&#%+]", " ")
                  .replaceAll("\\.(html?|php|aspx?|jsp)", "")
                  .replaceAll("\\s+", " ").trim();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(xff)) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
