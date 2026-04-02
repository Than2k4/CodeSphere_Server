-- Post moderation and reporting enhancements
ALTER TABLE posts
ADD COLUMN report_count INT NOT NULL DEFAULT 0,
ADD COLUMN last_reported_at TIMESTAMP NULL,
ADD COLUMN moderation_reason_code VARCHAR(50) NULL,
ADD COLUMN moderation_reason_detail VARCHAR(500) NULL,
ADD COLUMN moderated_at TIMESTAMP NULL;

CREATE TABLE post_reports (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_id BIGINT NOT NULL,
  reporter_id BIGINT NOT NULL,
  reason_code VARCHAR(50) NOT NULL,
  reason_detail VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_post_report UNIQUE (post_id, reporter_id),
  CONSTRAINT fk_post_reports_post FOREIGN KEY (post_id) REFERENCES posts(id),
  CONSTRAINT fk_post_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)
);

CREATE INDEX idx_post_report_post ON post_reports(post_id);
CREATE INDEX idx_post_report_created ON post_reports(created_at);
