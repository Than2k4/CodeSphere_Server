package com.hcmute.codesphere_server.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Migration script to fix notification type column
 * Changes ENUM or small VARCHAR to VARCHAR(50) to support all NotificationType values
 */
@Slf4j
@Component
@Order(1)
public class NotificationTypeMigration {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void migrate() {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            // Check current column type
            String columnType = jdbcTemplate.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = 'notifications' " +
                "AND COLUMN_NAME = 'type'",
                String.class
            );
            
            log.info("Current notification.type column type: {}", columnType);
            
            // If it's ENUM or VARCHAR with size < 50, alter it
            if (columnType != null && 
                (columnType.toUpperCase().startsWith("ENUM") || 
                 (columnType.toUpperCase().startsWith("VARCHAR") && 
                  !columnType.contains("50")))) {
                
                log.info("Migrating notification.type column to VARCHAR(50)...");
                jdbcTemplate.execute(
                    "ALTER TABLE notifications " +
                    "MODIFY COLUMN type VARCHAR(50) NOT NULL"
                );
                log.info("Successfully migrated notification.type column to VARCHAR(50)");
            } else {
                log.info("Notification.type column is already VARCHAR(50) or compatible, skipping migration");
            }
        } catch (Exception e) {
            log.error("Error migrating notification.type column: {}", e.getMessage());
            // Don't throw exception to allow app to start even if migration fails
            // User can manually run: ALTER TABLE notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;
        }
    }
}
