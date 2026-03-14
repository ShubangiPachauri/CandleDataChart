package com.CandleData.service.HistoricalData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoricalSyncEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public boolean sendHistoricalSyncCompletionMail(
            String toEmail,
            List<String> symbols,
            List<String> intervals,
            Date startTime,
            Date endTime,
            int totalStocksProcessed,
            int totalFailures
    ) {
        try {
            String html = loadTemplate("templates/historical-sync-summary.html");

            html = html.replace("{{DATE}}", new SimpleDateFormat("dd MMM yyyy").format(new Date()))
                       .replace("{{START_TIME}}", startTime.toString())
                       .replace("{{END_TIME}}", endTime.toString())
                       .replace("{{STOCKS}}", String.join(", ", symbols))
                       .replace("{{INTERVALS}}", String.join(", ", intervals))
                       .replace("{{TOTAL_STOCKS}}", String.valueOf(totalStocksProcessed))
                       .replace("{{FAILED_COUNT}}", String.valueOf(totalFailures));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setFrom(fromEmail);
            helper.setSubject("Historical Data Sync Completed Successfully");
            helper.setText(html, true);

            mailSender.send(message);

            log.info("Historical sync completion email sent to {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Failed to send historical sync email", e);
            return false;
        }
    }

    private String loadTemplate(String path) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(path);
        if (is == null) {
            log.error("Email template not found: {}", path);
            return "";
        }
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
}
