# Backend Truyện Tranh

Spring Boot 4.1, Java 21, MySQL 8.0, Redis, RabbitMQ, MinIO. Danh sách API và thiết kế xem ở `../../images/API.md`.

## Cấu trúc

```
src/main/java/project/truyentranh
├── config       Cấu hình chung: thuộc tính app, Swagger, RabbitMQ
├── controller   Nhận request, gọi service (Admin* là các API quản trị)
├── dto
│   ├── request  Dữ liệu client gửi lên
│   └── response Dữ liệu trả về
├── event        Gửi và nhận event qua RabbitMQ
├── exception    ApiException và xử lý lỗi chung
├── mapper       Chuyển entity sang DTO
├── model        Entity và enum, map với bảng trong MySQL
├── repository   Truy vấn database
├── security     Cấu hình JWT, phân quyền, lấy user hiện tại
├── service      Xử lý nghiệp vụ
└── util         Hàm dùng chung
```

## Chạy trên máy dev

Cần JDK 21, MySQL 8.0 và Docker Desktop. Maven đã có sẵn qua `mvnw`.

1. Mở MySQL Workbench, đăng nhập root, chạy lần lượt `../../database/01_schema.sql` rồi `../../database/02_seed.sql`. Script tạo database `comic_web`, user `comic_app` cho app, và dữ liệu mẫu.
2. Ở thư mục `truyen`, chạy Redis, RabbitMQ, MinIO, Mailpit:

```bash
docker compose up -d
```

3. Trong thư mục `backend`, chạy app (hoặc import vào Eclipse bằng Import > Existing Maven Projects rồi chạy `TruyentranhApiApplication`):

```bash
./mvnw spring-boot:run
```

Tài khoản mẫu: `admin@comicweb.example` / `Admin@12345` (admin), `minhan@comicweb.example` / `Password@123` (độc giả).

| Dịch vụ       | Địa chỉ                               |
| ------------- | ------------------------------------- |
| API           | http://localhost:8080/api/v1          |
| Swagger       | http://localhost:8080/swagger-ui.html |
| MySQL         | localhost:3306, database `comic_web`  |
| Mailpit       | http://localhost:8025                 |
| RabbitMQ      | http://localhost:15672                |
| MinIO console | http://localhost:9001                 |

Khi sửa cấu trúc bảng, sửa trong `database/01_schema.sql` và entity tương ứng trong `model`. App kiểm tra khớp nhau lúc khởi động (`ddl-auto: validate`) và không tự đổi database.

Chạy cả backend trong Docker thay cho bước 3 (ở thư mục `truyen`):

```bash
docker compose --profile app up -d --build
```

## Chạy test

```bash
./mvnw test
```

Test dùng Testcontainers nên cần Docker đang chạy. Test tự tạo MySQL riêng từ các file trong `database/`, không đụng vào `comic_web` trên máy.

## Triển khai production

Đặt `SPRING_PROFILES_ACTIVE=prod` và các biến môi trường mà `application.yml` yêu cầu: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `MAIL_HOST`, `MAIL_FROM`, `FRONTEND_URL`, `JWT_SECRET` (ít nhất 32 byte), `STORAGE_ENDPOINT`, `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY`, `STORAGE_BUCKET`, `STORAGE_PUBLIC_BASE_URL`.

- `DB_URL` phải có `connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` vì DB lưu giờ UTC.
- Đổi mật khẩu của user `comic_app` trong script SQL.
- Các job định kỳ (gom view, gửi outbox, dọn dữ liệu) chưa có khóa phân tán, nên hiện chỉ chạy một instance.
