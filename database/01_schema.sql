-- =========================================
-- comic_web: chạy file này trong MySQL Workbench bằng tài khoản root.
-- Sau đó chạy 02_seed.sql để thêm dữ liệu mẫu.
--
-- Nếu đã tạo comic_web theo bản cũ, bỏ comment dòng dưới để tạo lại từ đầu.
-- CẢNH BÁO: lệnh này xóa toàn bộ dữ liệu trong comic_web.
-- =========================================
-- DROP DATABASE IF EXISTS comic_web;

SET NAMES utf8mb4;

-- =========================================
-- 1. CREATE DATABASE
-- =========================================

CREATE DATABASE IF NOT EXISTS comic_web
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE comic_web;


-- =========================================
-- 2. USERS
-- =========================================

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    avatar_url VARCHAR(500),
    status ENUM('ACTIVE', 'INACTIVE', 'BANNED') NOT NULL
        DEFAULT 'ACTIVE',
    email_verified_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_created_at (created_at)
);


-- =========================================
-- 3. ROLES
-- =========================================

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

-- Dữ liệu bắt buộc: app cần 2 role này để đăng ký và phân quyền
INSERT INTO roles (id, name) VALUES (1, 'USER'), (2, 'ADMIN');


-- =========================================
-- 4. USER_ROLES
-- =========================================

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE CASCADE
);


-- =========================================
-- 5. REFRESH_TOKENS (đăng nhập JWT)
-- =========================================

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);


-- =========================================
-- 6. USER_TOKENS (xác thực email, đặt lại mật khẩu)
-- =========================================

CREATE TABLE user_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    type ENUM('VERIFY_EMAIL', 'RESET_PASSWORD') NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);


-- =========================================
-- 7. AUTHORS
-- =========================================

CREATE TABLE authors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    biography TEXT,
    avatar_url VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_authors_name (name)
);


-- =========================================
-- 8. COMICS
-- Các cột *_url nhận object key trên MinIO/S3 (ảnh upload) hoặc URL đầy đủ.
-- =========================================

CREATE TABLE comics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_url VARCHAR(500),
    author_id BIGINT,

    status ENUM(
        'ONGOING',
        'COMPLETED',
        'HIATUS'
    ) NOT NULL DEFAULT 'ONGOING',

    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    views BIGINT NOT NULL DEFAULT 0,
    rating_avg DECIMAL(3,2) NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    last_chapter_at DATETIME NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    FOREIGN KEY (author_id) REFERENCES authors(id)
        ON DELETE SET NULL,

    INDEX idx_comics_title (title),
    INDEX idx_comics_status (status),
    INDEX idx_comics_views (views),
    INDEX idx_comics_last_chapter (last_chapter_at)
);


-- =========================================
-- 9. GENRES
-- =========================================

CREATE TABLE genres (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);


-- =========================================
-- 10. COMIC_GENRES (Many-to-Many)
-- =========================================

CREATE TABLE comic_genres (
    comic_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,

    PRIMARY KEY (comic_id, genre_id),

    FOREIGN KEY (comic_id) REFERENCES comics(id)
        ON DELETE CASCADE,

    -- App chặn xóa thể loại đang có truyện, RESTRICT là lớp bảo vệ cuối
    FOREIGN KEY (genre_id) REFERENCES genres(id)
        ON DELETE RESTRICT
);


-- =========================================
-- 11. COMIC_CHAPTERS
-- =========================================

CREATE TABLE comic_chapters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comic_id BIGINT NOT NULL,

    chapter_number DECIMAL(8,2) NOT NULL,
    chapter_title VARCHAR(255),

    status ENUM('DRAFT', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT',
    views BIGINT NOT NULL DEFAULT 0,

    published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    -- Chapter đã xóa mềm nhận NULL ở đây, nhờ vậy có thể tạo lại chapter cùng số
    active_number DECIMAL(8,2) GENERATED ALWAYS AS
        (IF(deleted_at IS NULL, chapter_number, NULL)) STORED,

    FOREIGN KEY (comic_id) REFERENCES comics(id)
        ON DELETE CASCADE,

    UNIQUE KEY uk_comic_chapter (
        comic_id,
        active_number
    ),

    INDEX idx_chapter_published (published_at)
);


-- =========================================
-- 12. CHAPTER_PAGES
-- =========================================

CREATE TABLE chapter_pages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT NOT NULL,

    page_number INT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    width INT NULL,
    height INT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (chapter_id) REFERENCES comic_chapters(id)
        ON DELETE CASCADE,

    UNIQUE KEY uk_chapter_page (
        chapter_id,
        page_number
    ),

    INDEX idx_chapter_pages_image (image_url(255))
);


-- =========================================
-- 13. COMIC_VIEW_DAILY (thống kê cho Dashboard)
-- =========================================

CREATE TABLE comic_view_daily (
    comic_id BIGINT NOT NULL,
    view_date DATE NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (comic_id, view_date),

    FOREIGN KEY (comic_id) REFERENCES comics(id)
        ON DELETE CASCADE,

    INDEX idx_view_daily_date (view_date)
);


-- =========================================
-- 14. FAVORITES (Follow / Bookmark)
-- =========================================

CREATE TABLE favorites (
    user_id BIGINT NOT NULL,
    comic_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, comic_id),

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (comic_id) REFERENCES comics(id)
        ON DELETE CASCADE,

    -- Dùng khi tìm người theo dõi để gửi thông báo chapter mới
    INDEX idx_favorites_comic (comic_id)
);


-- =========================================
-- 15. READING_HISTORIES
-- Mỗi user một dòng cho mỗi truyện: chapter và trang đang đọc dở.
-- =========================================

CREATE TABLE reading_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    comic_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,

    last_page INT NOT NULL DEFAULT 1,
    last_read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (comic_id) REFERENCES comics(id)
        ON DELETE CASCADE,

    FOREIGN KEY (chapter_id) REFERENCES comic_chapters(id)
        ON DELETE CASCADE,

    UNIQUE KEY uk_user_comic_history (
        user_id,
        comic_id
    ),

    INDEX idx_reading_user_time (
        user_id,
        last_read_at
    )
);


-- =========================================
-- 16. COMMENTS
-- =========================================

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    comic_id BIGINT NOT NULL,
    chapter_id BIGINT NULL,

    content TEXT NOT NULL,

    parent_id BIGINT NULL,
    like_count INT NOT NULL DEFAULT 0,

    -- HIDDEN: admin ẩn, DELETED: chủ bình luận tự xóa
    status ENUM('VISIBLE', 'HIDDEN', 'DELETED') NOT NULL
        DEFAULT 'VISIBLE',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (comic_id) REFERENCES comics(id)
        ON DELETE CASCADE,

    FOREIGN KEY (chapter_id) REFERENCES comic_chapters(id)
        ON DELETE SET NULL,

    FOREIGN KEY (parent_id) REFERENCES comments(id)
        ON DELETE CASCADE,

    INDEX idx_comments_comic_time (
        comic_id,
        created_at
    )
);


-- =========================================
-- 17. COMMENT_LIKES
-- =========================================

CREATE TABLE comment_likes (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (comment_id, user_id),

    FOREIGN KEY (comment_id) REFERENCES comments(id)
        ON DELETE CASCADE,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);


-- =========================================
-- 18. COMMENT_REPORTS
-- =========================================

CREATE TABLE comment_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    comment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,

    reason VARCHAR(500) NOT NULL,
    status ENUM('PENDING', 'RESOLVED', 'DISMISSED') NOT NULL
        DEFAULT 'PENDING',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (comment_id) REFERENCES comments(id)
        ON DELETE CASCADE,

    FOREIGN KEY (reporter_id) REFERENCES users(id)
        ON DELETE CASCADE,

    UNIQUE KEY uk_comment_report_once (
        comment_id,
        reporter_id
    ),

    INDEX idx_comment_reports_status (
        status,
        created_at
    )
);


-- =========================================
-- 19. RATINGS
-- =========================================

CREATE TABLE ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    comic_id BIGINT NOT NULL,

    score TINYINT NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (comic_id) REFERENCES comics(id)
        ON DELETE CASCADE,

    UNIQUE KEY uk_user_comic_rating (
        user_id,
        comic_id
    ),

    INDEX idx_ratings_comic (comic_id),

    CONSTRAINT chk_rating_score
        CHECK (score >= 1 AND score <= 5)
);


-- =========================================
-- 20. NOTIFICATIONS
-- =========================================

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,

    type VARCHAR(30) NOT NULL,
    message VARCHAR(500) NOT NULL,

    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    FOREIGN KEY (chapter_id) REFERENCES comic_chapters(id)
        ON DELETE CASCADE,

    -- Xử lý lại cùng một event cũng không tạo thông báo trùng
    UNIQUE KEY uk_notification_once (
        user_id,
        chapter_id,
        type
    ),

    INDEX idx_notifications_unread (
        user_id,
        read_at
    )
);


-- =========================================
-- 21. AUDIT_LOGS
-- =========================================

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    actor_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NULL,

    before_data JSON NULL,
    after_data JSON NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (actor_id) REFERENCES users(id),

    INDEX idx_audit_created (created_at),
    INDEX idx_audit_action (action, created_at)
);


-- =========================================
-- 22. OUTBOX_EVENTS
-- Hàng đợi tạm trước khi gửi sang RabbitMQ, không có khóa ngoại.
-- =========================================

CREATE TABLE outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    event_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload JSON NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME NULL,

    INDEX idx_outbox_pending (sent_at, id)
);


-- =========================================
-- 23. TÀI KHOẢN RIÊNG CHO APP
-- App không dùng root. Mật khẩu này chỉ dùng trên máy dev, khớp application-dev.yml.
-- =========================================

CREATE USER IF NOT EXISTS 'comic_app'@'%' IDENTIFIED BY 'comic_app_dev';
GRANT SELECT, INSERT, UPDATE, DELETE ON comic_web.* TO 'comic_app'@'%';
FLUSH PRIVILEGES;
