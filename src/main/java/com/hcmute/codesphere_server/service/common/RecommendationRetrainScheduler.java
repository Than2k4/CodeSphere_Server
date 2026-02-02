package com.hcmute.codesphere_server.service.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class RecommendationRetrainScheduler {

    /**
     * Bật/tắt tự động retrain từ cấu hình.
     */
    @Value("${ml.retrain.enabled:true}")
    private boolean retrainEnabled;

    /**
     * Đường dẫn script Python auto_retrain (tương đối hoặc tuyệt đối).
     * Ví dụ (trong application.properties):
     * ml.retrain.script=src/training/auto_retrain.py
     */
    @Value("${ml.retrain.script:src/training/auto_retrain.py}")
    private String retrainScript;

    /**
     * Thư mục làm việc của dự án ML.
     * Ví dụ: ../CodeSphere_ML (nếu Server và ML cùng cấp).
     */
    @Value("${ml.retrain.workdir:../CodeSphere_ML}")
    private String workDir;

    /**
     * Lịch chạy: mỗi ngày lúc 2h sáng.
     * Cron format: second minute hour day month day-of-week
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleRetrain() {
        if (!retrainEnabled) {
            log.info("ML retrain is disabled by configuration (ml.retrain.enabled=false)");
            return;
        }

        log.info("=== BẮT ĐẦU TỰ ĐỘNG RETRAIN RECOMMENDATION MODEL ===");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    retrainScript
            );

            pb.directory(new File(workDir));
            pb.inheritIO(); // log ra console của backend

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Retrain recommendation model SUCCESS (exitCode=0)");
            } else {
                log.error("Retrain recommendation model FAILED (exitCode={})", exitCode);
            }
        } catch (Exception ex) {
            log.error("Error when running ML retrain script", ex);
        }
        log.info("=== KẾT THÚC TỰ ĐỘNG RETRAIN RECOMMENDATION MODEL ===");
    }
}


