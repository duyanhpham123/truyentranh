-- =========================================
-- Dữ liệu mẫu cho comic_web. Chạy sau 01_schema.sql, chỉ chạy một lần.
-- Tên truyện và tác giả đều là hư cấu; ảnh là ảnh placeholder (cần có Internet để hiển thị).
--
-- Tài khoản mẫu:
--   admin@comicweb.example   / Admin@12345    (ADMIN)
--   Các tài khoản còn lại    / Password@123   (USER)
-- Thời gian lưu theo UTC.
-- =========================================

-- Bắt buộc để tiếng Việt không bị lỗi font khi chạy bằng client khác Workbench
SET NAMES utf8mb4;

-- Workbench mặc định bật Safe Updates, chặn các câu UPDATE tính lại toàn bảng bên dưới
SET SQL_SAFE_UPDATES = 0;

USE comic_web;

-- ---------- Users ----------
INSERT INTO users (id, username, email, password, full_name, status, email_verified_at, created_at) VALUES
(1, 'admin',  'admin@comicweb.example',  '$2a$10$a.iYgLssyEesJF6cL3z88.yabo56l9A0DzS3K/An5h9qY7RwZ1fVK', 'Quản trị viên', 'ACTIVE', UTC_TIMESTAMP() - INTERVAL 60 DAY, UTC_TIMESTAMP() - INTERVAL 60 DAY),
(2, 'minhan', 'minhan@comicweb.example', '$2a$10$sHwXL7ZHrZU63RLWQM2GqOPCoJ5F.t.uS06.GneY8SJRuqSU3V1Z.', 'Minh An',       'ACTIVE', UTC_TIMESTAMP() - INTERVAL 30 DAY, UTC_TIMESTAMP() - INTERVAL 30 DAY),
(3, 'thuha',  'thuha@comicweb.example',  '$2a$10$sHwXL7ZHrZU63RLWQM2GqOPCoJ5F.t.uS06.GneY8SJRuqSU3V1Z.', 'Thu Hà',        'ACTIVE', UTC_TIMESTAMP() - INTERVAL 20 DAY, UTC_TIMESTAMP() - INTERVAL 20 DAY),
(4, 'quanglong', 'quanglong@comicweb.example', '$2a$10$sHwXL7ZHrZU63RLWQM2GqOPCoJ5F.t.uS06.GneY8SJRuqSU3V1Z.', 'Quang Long', 'ACTIVE', NULL, UTC_TIMESTAMP() - INTERVAL 3 DAY),
(5, 'spammer', 'spammer@comicweb.example', '$2a$10$sHwXL7ZHrZU63RLWQM2GqOPCoJ5F.t.uS06.GneY8SJRuqSU3V1Z.', 'Tài khoản bị khóa', 'BANNED', UTC_TIMESTAMP() - INTERVAL 10 DAY, UTC_TIMESTAMP() - INTERVAL 10 DAY);

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), (1, 2), (2, 1), (3, 1), (4, 1), (5, 1);

-- ---------- Authors ----------
INSERT INTO authors (id, name, biography) VALUES
(1, 'Lam Vũ',      'Tác giả hư cấu chuyên truyện phiêu lưu giả tưởng.'),
(2, 'Hạ Nhật Minh', 'Tác giả hư cấu chuyên truyện học đường và đời thường.'),
(3, 'Trần Sơn Khê', 'Tác giả hư cấu chuyên truyện hành động võ thuật.'),
(4, 'Mai Thanh',    'Tác giả hư cấu chuyên truyện lãng mạn.'),
(5, 'Bạch Dương',   'Tác giả hư cấu chuyên truyện kinh dị, trinh thám.');

-- ---------- Genres ----------
INSERT INTO genres (id, slug, name, description) VALUES
(1, 'hanh-dong',  'Hành Động',  'Truyện có nhiều pha chiến đấu, kịch tính.'),
(2, 'phieu-luu',  'Phiêu Lưu',  'Những chuyến hành trình đến vùng đất mới.'),
(3, 'gia-tuong',  'Giả Tưởng',  'Thế giới phép thuật và sinh vật kỳ ảo.'),
(4, 'hoc-duong',  'Học Đường',  'Câu chuyện về tuổi học trò.'),
(5, 'lang-man',   'Lãng Mạn',   'Chuyện tình cảm đôi lứa.'),
(6, 'hai-huoc',   'Hài Hước',   'Những tình huống vui nhộn.'),
(7, 'kinh-di',    'Kinh Dị',    'Không khí rùng rợn, căng thẳng.'),
(8, 'trinh-tham', 'Trinh Thám', 'Phá án và giải mã bí ẩn.');

-- ---------- Comics ----------
INSERT INTO comics (id, slug, title, description, cover_url, author_id, status, is_hidden, created_at) VALUES
(1,  'hanh-trinh-den-dao-may',   'Hành Trình Đến Đảo Mây',   'Cậu bé thợ rèn lên đường tìm hòn đảo bay trong truyền thuyết.', 'https://placehold.co/400x600/png?text=Dao+May',    1, 'ONGOING',   FALSE, UTC_TIMESTAMP() - INTERVAL 50 DAY),
(2,  'kiem-su-cuoi-cung',        'Kiếm Sư Cuối Cùng',        'Người cuối cùng của môn phái đi tìm kẻ đã hủy diệt sư môn.',    'https://placehold.co/400x600/png?text=Kiem+Su',    3, 'ONGOING',   FALSE, UTC_TIMESTAMP() - INTERVAL 45 DAY),
(3,  'lop-hoc-sau-gio-tan-truong', 'Lớp Học Sau Giờ Tan Trường', 'Nhóm bạn lớp 11 lập câu lạc bộ để cứu ngôi trường sắp đóng cửa.', 'https://placehold.co/400x600/png?text=Lop+Hoc', 2, 'COMPLETED', FALSE, UTC_TIMESTAMP() - INTERVAL 40 DAY),
(4,  'mua-he-o-pho-bien',        'Mùa Hè Ở Phố Biển',        'Chuyện tình nhẹ nhàng của hai người trẻ ở một thị trấn ven biển.', 'https://placehold.co/400x600/png?text=Pho+Bien', 4, 'ONGOING',   FALSE, UTC_TIMESTAMP() - INTERVAL 35 DAY),
(5,  'can-nha-so-13',            'Căn Nhà Số 13',            'Một gia đình chuyển đến căn nhà mà không ai dám ở quá ba đêm.', 'https://placehold.co/400x600/png?text=Nha+13',     5, 'HIATUS',    FALSE, UTC_TIMESTAMP() - INTERVAL 30 DAY),
(6,  'tham-tu-gio-nghi-trua',    'Thám Tử Giờ Nghỉ Trưa',    'Nhân viên văn phòng phá các vụ án chỉ trong giờ nghỉ trưa.',    'https://placehold.co/400x600/png?text=Tham+Tu',    5, 'ONGOING',   FALSE, UTC_TIMESTAMP() - INTERVAL 25 DAY),
(7,  'phu-thuy-tap-su',          'Phù Thủy Tập Sự',          'Cô bé tập sự làm hỏng mọi câu thần chú, theo cách rất buồn cười.', 'https://placehold.co/400x600/png?text=Phu+Thuy', 1, 'ONGOING',   FALSE, UTC_TIMESTAMP() - INTERVAL 20 DAY),
(8,  'vo-duong-giua-pho',        'Võ Đường Giữa Phố',        'Võ đường nhỏ giữa thành phố lớn chống lại làn sóng giải tỏa.',  'https://placehold.co/400x600/png?text=Vo+Duong',   3, 'ONGOING',   FALSE, UTC_TIMESTAMP() - INTERVAL 15 DAY),
(9,  'nhat-ky-ban-cung-ban',     'Nhật Ký Bạn Cùng Bàn',     'Những mẩu chuyện vui của hai người bạn cùng bàn.',              'https://placehold.co/400x600/png?text=Nhat+Ky',    2, 'COMPLETED', FALSE, UTC_TIMESTAMP() - INTERVAL 12 DAY),
(10, 'hoa-no-tren-tuyet',        'Hoa Nở Trên Tuyết',        'Chuyện tình vượt qua định kiến ở vùng núi phía Bắc.',           'https://placehold.co/400x600/png?text=Hoa+Tuyet',  4, 'ONGOING',   FALSE, UTC_TIMESTAMP() - INTERVAL 9 DAY),
-- Truyện đang ẩn: độc giả không thấy, admin vẫn thấy
(11, 'ban-thao-chua-duyet',      'Bản Thảo Chưa Duyệt',      'Truyện đang chờ duyệt nội dung.',                               'https://placehold.co/400x600/png?text=Ban+Thao',   1, 'ONGOING',   TRUE,  UTC_TIMESTAMP() - INTERVAL 2 DAY),
-- Truyện mới, chưa có chapter nào publish
(12, 'vung-dat-chua-dat-ten',    'Vùng Đất Chưa Đặt Tên',    'Truyện mới, chapter đầu tiên đang ở dạng nháp.',                'https://placehold.co/400x600/png?text=Vung+Dat',   1, 'ONGOING',   FALSE, UTC_TIMESTAMP() - INTERVAL 1 DAY);

INSERT INTO comic_genres (comic_id, genre_id) VALUES
(1, 2), (1, 3), (2, 1), (2, 2), (3, 4), (3, 6), (4, 5), (5, 7), (6, 8), (6, 6),
(7, 3), (7, 6), (8, 1), (9, 4), (9, 6), (10, 5), (11, 3), (12, 2);

-- ---------- Chapters đã publish: số chapter theo từng truyện, chapter mới nhất cách đây (comic_id) ngày ----------
INSERT INTO comic_chapters (comic_id, chapter_number, chapter_title, status, views, published_at, created_at)
WITH RECURSIVE counts (comic_id, total) AS (
    SELECT 1, 6 UNION ALL SELECT 2, 5 UNION ALL SELECT 3, 4 UNION ALL SELECT 4, 3 UNION ALL SELECT 5, 3
    UNION ALL SELECT 6, 4 UNION ALL SELECT 7, 3 UNION ALL SELECT 8, 2 UNION ALL SELECT 9, 3 UNION ALL SELECT 10, 2
    UNION ALL SELECT 11, 1
),
nums (n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM nums WHERE n < 6
)
SELECT c.comic_id, n.n, CONCAT('Chương ', n.n), 'PUBLISHED', 0,
       UTC_TIMESTAMP() - INTERVAL (c.comic_id + (c.total - n.n) * 7) DAY,
       UTC_TIMESTAMP() - INTERVAL (c.comic_id + (c.total - n.n) * 7) DAY
FROM counts c JOIN nums n ON n.n <= c.total
ORDER BY c.comic_id, n.n;

-- Chapter phụ số lẻ và chapter nháp
INSERT INTO comic_chapters (comic_id, chapter_number, chapter_title, status, published_at, created_at) VALUES
(1, 2.50, 'Ngoại truyện: Lò rèn cũ', 'PUBLISHED', UTC_TIMESTAMP() - INTERVAL 20 DAY, UTC_TIMESTAMP() - INTERVAL 20 DAY),
(1, 7.00, 'Bản nháp chương 7', 'DRAFT', NULL, UTC_TIMESTAMP()),
(12, 1.00, 'Bản nháp chương 1', 'DRAFT', NULL, UTC_TIMESTAMP());

-- ---------- Pages: mỗi chapter 5 trang ----------
INSERT INTO chapter_pages (chapter_id, page_number, image_url, width, height)
WITH RECURSIVE pages (n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM pages WHERE n < 5
)
SELECT ch.id, p.n,
       CONCAT('https://placehold.co/800x1200/png?text=C', ch.comic_id, '-', ROUND(ch.chapter_number, 1), '-P', p.n),
       800, 1200
FROM comic_chapters ch JOIN pages p
ORDER BY ch.id, p.n;

-- ---------- Thống kê view 14 ngày gần nhất ----------
INSERT INTO comic_view_daily (comic_id, view_date, view_count)
WITH RECURSIVE days (d) AS (
    SELECT 0 UNION ALL SELECT d + 1 FROM days WHERE d < 13
)
SELECT c.id, UTC_DATE() - INTERVAL days.d DAY, (130 - c.id * 10) + ((c.id * 7 + days.d * 11) % 40)
FROM comics c JOIN days
WHERE c.id <= 10;

UPDATE comics c
SET c.views = (SELECT COALESCE(SUM(d.view_count), 0) FROM comic_view_daily d WHERE d.comic_id = c.id),
    c.last_chapter_at = (SELECT MAX(ch.published_at) FROM comic_chapters ch WHERE ch.comic_id = c.id AND ch.status = 'PUBLISHED');

-- Chia lượt xem của truyện cho các chapter đã publish
UPDATE comic_chapters ch
JOIN comics c ON c.id = ch.comic_id
SET ch.views = c.views DIV (SELECT COUNT(*) FROM (SELECT * FROM comic_chapters) x WHERE x.comic_id = ch.comic_id AND x.status = 'PUBLISHED')
WHERE ch.status = 'PUBLISHED';

-- ---------- Favorites ----------
INSERT INTO favorites (user_id, comic_id) VALUES
(2, 1), (2, 2), (2, 6), (3, 1), (3, 3), (3, 4), (4, 7);

-- ---------- Reading histories ----------
INSERT INTO reading_histories (user_id, comic_id, chapter_id, last_page, last_read_at)
SELECT h.user_id, h.comic_id, ch.id, h.last_page, UTC_TIMESTAMP() - INTERVAL h.hours_ago HOUR
FROM (
    SELECT 2 user_id, 1 comic_id, 3.00 chapter_number, 4 last_page, 2 hours_ago
    UNION ALL SELECT 2, 2, 5.00, 1, 30
    UNION ALL SELECT 3, 3, 4.00, 5, 5
    UNION ALL SELECT 3, 4, 2.00, 2, 50
    UNION ALL SELECT 4, 7, 1.00, 3, 10
) h
JOIN comic_chapters ch ON ch.comic_id = h.comic_id AND ch.chapter_number = h.chapter_number;

-- ---------- Ratings ----------
INSERT INTO ratings (user_id, comic_id, score) VALUES
(2, 1, 5), (3, 1, 4), (1, 1, 5), (2, 2, 4), (3, 3, 5), (1, 3, 4), (3, 4, 3), (2, 6, 5), (4, 7, 4);

UPDATE comics c
SET c.rating_avg = COALESCE((SELECT AVG(r.score) FROM ratings r WHERE r.comic_id = c.id), 0),
    c.rating_count = (SELECT COUNT(*) FROM ratings r WHERE r.comic_id = c.id);

-- ---------- Comments ----------
INSERT INTO comments (id, user_id, comic_id, chapter_id, content, parent_id, like_count, status, created_at) VALUES
(1, 2, 1, NULL, 'Nét vẽ đẹp, cốt truyện cuốn thật sự!', NULL, 2, 'VISIBLE', UTC_TIMESTAMP() - INTERVAL 5 DAY),
(2, 3, 1, NULL, 'Đồng ý, chương đầu đã thấy hay rồi.', 1, 1, 'VISIBLE', UTC_TIMESTAMP() - INTERVAL 4 DAY),
(3, 3, 3, NULL, 'Cái kết của truyện này làm mình khóc mất.', NULL, 0, 'VISIBLE', UTC_TIMESTAMP() - INTERVAL 3 DAY),
(4, 5, 1, NULL, 'Vào trang của tôi xem truyện miễn phí!!!', NULL, 0, 'VISIBLE', UTC_TIMESTAMP() - INTERVAL 2 DAY),
(5, 2, 2, NULL, 'Mong tác giả ra chương mới sớm.', NULL, 1, 'VISIBLE', UTC_TIMESTAMP() - INTERVAL 1 DAY),
(6, 2, 6, NULL, 'Bình luận này đã bị chủ tài khoản xóa.', NULL, 0, 'DELETED', UTC_TIMESTAMP() - INTERVAL 1 DAY);

INSERT INTO comment_likes (comment_id, user_id) VALUES
(1, 3), (1, 1), (2, 2), (5, 3);

INSERT INTO comment_reports (comment_id, reporter_id, reason, status) VALUES
(4, 2, 'Spam quảng cáo', 'PENDING'),
(4, 3, 'Chứa liên kết lạ', 'PENDING');

-- ---------- Notifications: chapter mới nhất của truyện đang theo dõi ----------
INSERT INTO notifications (user_id, chapter_id, type, message, read_at, created_at)
SELECT f.user_id, ch.id, 'NEW_CHAPTER',
       CONCAT(c.title, ' đã có chapter ', TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM ch.chapter_number))),
       IF(f.user_id = 3, UTC_TIMESTAMP(), NULL), ch.published_at
FROM favorites f
JOIN comics c ON c.id = f.comic_id
JOIN comic_chapters ch ON ch.comic_id = c.id AND ch.published_at = c.last_chapter_at AND ch.status = 'PUBLISHED';
