# CodeSphere Backend

Backend của hệ thống **CodeSphere** - Website mạng xã hội hỗ trợ luyện tập lập trình tích hợp AI.

## Công nghệ

- Java 21
- Spring Boot
- Spring Security
- JWT Authentication
- OAuth2 (Google)
- Spring Data JPA
- MySQL
- Docker
- WebSocket
- OpenAI API
- Maven

## Chức năng

- Xác thực và phân quyền người dùng
- Quản lý người dùng
- Quản lý bài toán
- Chấm bài tự động
- Quản lý Contest
- Mạng xã hội (bài viết, bình luận, thông báo)
- Chat thời gian thực
- AI hỗ trợ lập trình
- Dashboard quản trị
- RESTful API

## Cài đặt

```bash
git clone https://github.com/your-username/codesphere-backend.git
cd codesphere-backend
```

Cấu hình thông tin database trong `application.yml`, sau đó chạy:

```bash
mvn clean install
mvn spring-boot:run
```

## Yêu cầu

- Java 21
- Maven 3.9+
- MySQL 8+
- Docker

## License

Dự án được phát triển phục vụ mục đích học tập và nghiên cứu.
