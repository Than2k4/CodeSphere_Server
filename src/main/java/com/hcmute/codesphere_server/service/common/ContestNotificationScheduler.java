package com.hcmute.codesphere_server.service.common;

import com.hcmute.codesphere_server.model.entity.ContestEntity;
import com.hcmute.codesphere_server.model.entity.ContestRegistrationEntity;
import com.hcmute.codesphere_server.model.enums.ContestType;
import com.hcmute.codesphere_server.repository.common.ContestRegistrationRepository;
import com.hcmute.codesphere_server.repository.common.ContestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContestNotificationScheduler {

    private final ContestRepository contestRepository;
    private final ContestRegistrationRepository contestRegistrationRepository;
    private final NotificationService notificationService;

    /**
     * Chạy mỗi phút để kiểm tra và gửi thông báo nhắc nhở 10 phút trước khi contest bắt đầu
     * Cron format: second minute hour day month day-of-week
     * "0 * * * * ?" = mỗi phút
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void sendContestReminderNotifications() {
        try {
            Instant now = Instant.now();
            
            // Lấy tất cả contest OFFICIAL chưa bắt đầu
            List<ContestEntity> upcomingContests = contestRepository.findUpcoming(now);
            
            for (ContestEntity contest : upcomingContests) {
                // Chỉ xử lý OFFICIAL contest
                if (contest.getContestType() != ContestType.OFFICIAL) {
                    continue;
                }
                
                if (contest.getStartTime() == null) {
                    continue;
                }
                
                // Kiểm tra xem contest có bắt đầu trong khoảng 9-11 phút nữa không (để tránh gửi nhiều lần)
                long minutesUntilStart = ChronoUnit.MINUTES.between(now, contest.getStartTime());
                
                // Chỉ gửi notification khi còn đúng 10 phút (khoảng 9-11 phút để tránh gửi nhiều lần)
                if (minutesUntilStart >= 9 && minutesUntilStart <= 11) {
                    // Lấy danh sách user đã đăng ký
                    List<ContestRegistrationEntity> registrations = contestRegistrationRepository.findByContestId(contest.getId());
                    
                    for (ContestRegistrationEntity registration : registrations) {
                        Long userId = registration.getUser().getId();
                        
                        // Kiểm tra xem đã gửi notification cho contest này chưa (tránh gửi trùng)
                        // Kiểm tra tất cả notification đã gửi cho contest này (không giới hạn thời gian)
                        // Để đảm bảo chỉ gửi 1 lần duy nhất cho mỗi user
                        Instant veryLongAgo = now.minus(365, ChronoUnit.DAYS); // Kiểm tra trong 1 năm (thực tế là tất cả)
                        boolean alreadyNotified = notificationService.hasContestNotification(
                                userId, 
                                contest.getId(), 
                                veryLongAgo
                        );
                        
                        if (alreadyNotified) {
                            log.debug("User {} already received notification for contest {} (skipping)", userId, contest.getId());
                            continue;
                        }
                        
                        String title = "Contest Starting Soon";
                        String content = String.format("Contest '%s' will start in 10 minutes. Get ready!", 
                                contest.getTitle());
                        
                        try {
                            notificationService.createContestNotification(
                                    userId,
                                    title,
                                    content,
                                    contest.getId()
                            );
                            log.info("Sent contest reminder notification to user {} for contest {} (10 minutes before start)", 
                                    userId, contest.getId());
                        } catch (Exception e) {
                            log.error("Error sending contest reminder to user {} for contest {}", 
                                    userId, contest.getId(), e);
                        }
                    }
                    
                    log.info("Processed contest reminder for contest: {} ({} registrations)", 
                            contest.getTitle(), registrations.size());
                }
            }
        } catch (Exception e) {
            log.error("Error in contest notification scheduler", e);
        }
    }
}

