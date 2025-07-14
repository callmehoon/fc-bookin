# 📚 YESorNO.24 - Spring Boot 온라인 서점 프로젝트

## 프로젝트 소개 Introduction
Spring Boot를 활용한 온라인 서점 시스템으로, 사용자와 관리자를 위한 종합적인 도서 쇼핑몰 기능을 제공합니다.

### 주요 특징
- 🔐 **사용자/관리자 분리된 인증 시스템**
- 📖 **3단계 계층형 카테고리 구조**
- 🛒 **실시간 재고 연동 장바구니**
- 💳 **KakaoPay API 연동 결제 시스템**
- 📱 **반응형 웹 디자인**
- ⚡ **캐싱 기반 성능 최적화**

## 📋 목차
- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [API 명세서](#api-명세서)
- [설치 및 실행](#설치-및-실행)
- [데이터베이스 설정](#데이터베이스-설정)
- [환경 설정](#환경-설정)
- [트러블슈팅](#트러블슈팅)

## 🎯 프로젝트 개요

### ERD (Entity Relationship Diagram)
![YESorNO.24 ERD](src/main/resources/static/layout/YESorNO.24_ERD.png)

### 아키텍처
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Presentation  │    │    Business     │    │      Data       │
│   (Controller)  │◄──►│   (Service)     │◄──►│  (Repository)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
│                                                                │
│  - @Controller        │  - @Service          │  - @Repository  │
│  - REST API           │  - 비즈니스 로직      │  - JPA/Hibernate│
│  - Thymeleaf          │  - 트랜잭션 관리      │  - MySQL 연동    │
└────────────────────────────────────────────────────────────────┘
```

## 🛠 기술 스택

### Backend
- **Java 17** - 최신 LTS 버전
- **Spring Boot 3.3.1** - 웹 애플리케이션 프레임워크
- **Spring Data JPA** - ORM 및 데이터 액세스
- **Spring Web** - RESTful API 개발
- **Hibernate** - ORM 구현체
- **MySQL 8.0** - 관계형 데이터베이스

### Frontend
- **Thymeleaf** - 서버 사이드 템플릿 엔진
- **HTML5/CSS3** - 마크업 및 스타일링
- **JavaScript ES6+** - 클라이언트 사이드 로직
- **부트스트랩 스타일** - 반응형 디자인

### 외부 API
- **KakaoPay API** - 결제 시스템
- **JavaMail API** - 이메일 발송 (아이디/비밀번호 찾기)

### 개발 도구
- **IntelliJ IDEA** - 통합 개발 환경
- **Maven** - 의존성 관리 및 빌드 도구
- **Git & GitHub** - 버전 관리
- **MySQL** - 데이터베이스 관리

## ✨ 주요 기능

### 👤 사용자 기능

#### 🔐 **회원 관리**
- ✅ 회원가입 (실명인증, 이메일 중복검사)
- ✅ 로그인/로그아웃 (30분 세션 관리)
- ✅ 아이디/비밀번호 찾기 (이메일 발송)
- ✅ 회원정보 수정
- ✅ 휴면계정 관리 (스케줄러 기반)

#### 📖 **상품 조회**
- ✅ 메인 페이지 (베스트셀러, 신상품, 인기검색어)
- ✅ 카테고리별 상품 브라우징 (대분류 > 중분류 > 소분류)
- ✅ 상품 검색 (일반검색, 상세검색)
- ✅ 상품 상세 정보 (재고, 리뷰, 평점)
- ✅ 최근 본 상품 (로컬 스토리지 기반)

#### 🛒 **장바구니**
- ✅ 상품 추가/수정/삭제
- ✅ 실시간 재고 확인 (API 캐싱)
- ✅ 수량 조절 (재고량 초과 방지)
- ✅ 선택 상품 주문
- ✅ 페이지네이션 (10개씩)

#### 💳 **주문/결제**
- ✅ 주문서 작성 (배송지, 수량 확인)
- ✅ KakaoPay 결제 연동
- ✅ 주문 상태 추적 (7단계)
- ✅ 주문 취소
- ✅ 주문 내역 조회

#### ⭐ **리뷰 시스템**
- ✅ 리뷰 작성/수정/삭제
- ✅ 평점 시스템 (1-5점)
- ✅ 리뷰 페이지네이션
- ✅ Soft Delete 지원

### 👨‍💼 관리자 기능

#### 📦 **상품 관리**
- ✅ 상품 등록/수정/삭제
- ✅ 재고 관리 (입고/출고 이력)
- ✅ 상품 상태 변경 (판매중/품절/절판/입고예정)
- ✅ 카테고리 관리
- ✅ 상품 검색/필터링 (상세검색)

#### 📋 **주문 관리**
- ✅ 주문 내역 조회
- ✅ 주문 상태 변경
- ✅ 기간별 주문 검색
- ✅ 주문 상세 정보 확인

#### 👥 **회원 관리**
- ✅ 회원 정보 조회
- ✅ 회원 검색 (다중 조건)
- ✅ 회원별 주문 내역 확인
- ✅ 회원 상태 관리

#### 🔒 **관리자 권한 제어**
- ✅ 관리자 전용 페이지 접근 제한
- ✅ 일반 사용자 기능 차단 (장바구니, 마이페이지)
- ✅ URL 직접 접속 차단

## 🌐 API 명세서

### 📋 **인증 관련 API**
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| GET | `/login` | 로그인 페이지 | - | HTML |
| POST | `/login` | 로그인 처리 | `LoginRequest` | Redirect |
| POST | `/logout` | 로그아웃 | - | Redirect |
| GET | `/register` | 회원가입 페이지 | - | HTML |
| POST | `/api/register` | 회원가입 처리 | `RegisterRequest` | `RegisterResponse` |
| POST | `/findId` | 아이디 찾기 | `name, email` | String |
| POST | `/findPassword` | 비밀번호 찾기 | `idForUser, email` | String |

### 📖 **상품 관련 API**
| Method | Endpoint | Description | Parameters | Response |
|--------|----------|-------------|------------|----------|
| GET | `/main` | 메인 페이지 | - | HTML + 베스트셀러/신상품 |
| GET | `/product/detail/{isbn}` | 상품 상세 | `isbn`, `page` | `Product` + 리뷰 |
| GET | `/product/category/low/{categoryId}` | 카테고리별 상품 | `categoryId`, `page`, `size`, `sort` | `ProductListPageResponse` |
| GET | `/product/search` | 일반 검색 | `q`, `page`, `size`, `sort` | `ProductListPageResponse` |
| GET | `/product/search-advanced` | 상세 검색 | `title`, `author`, `publisher` | `ProductListPageResponse` |
| GET | `/api/popular-keywords` | 인기 검색어 Top 10 | - | `List<ProductSimpleResponse>` |
| GET | `/api/bestseller` | 베스트셀러 | `period` (week/month/year) | `List<ProductSimpleResponse>` |
| GET | `/api/newproducts` | 신상품 Top 10 | - | `List<ProductSimpleResponse>` |

### 🛒 **장바구니 API**
| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| GET | `/cart` | 장바구니 페이지 | - | HTML |
| POST | `/cart/add` | 상품 추가 | `isbn`, `quantity` | String |
| POST | `/cart/add-bulk` | 일괄 추가 | `List<String> isbns` | String |
| POST | `/cart/check` | 중복 상품 확인 | `List<String> isbns` | `Map<String, Object>` |
| POST | `/cart/update` | 수량 변경 | `isbn`, `quantity` | String |
| POST | `/cart/delete` | 상품 삭제 | `isbn` | String |
| GET | `/cart/list` | 장바구니 목록 | - | `List<CartItemResponse>` |
| GET | `/product/api/stock/{isbn}` | 재고 확인 | `isbn` | `Map<String, Integer>` |

### 💳 **주문/결제 API**
| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| GET | `/order` | 주문 페이지 | - | HTML |
| POST | `/api/order/create` | 주문 생성 | `OrderRequest` | `Map<String, Object>` |
| POST | `/api/payment/attempt` | 결제 시도 | `PaymentRequest` | KakaoPay Response |
| POST | `/api/payment/complete` | 결제 완료 | `paymentId`, `pgToken` | Redirect |
| POST | `/api/payment/fail` | 결제 실패 | `paymentId` | Redirect |
| GET | `/order/summary` | 주문 요약 | `orderId` | HTML |
| POST | `/api/orders/cancel` | 주문 취소 | `orderId`, `idForAdmin` | String |

### 👤 **마이페이지 API**
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| GET | `/mypage` | 마이페이지 | - | HTML + 사용자 정보 |
| POST | `/api/user/update` | 회원정보 수정 | `UserUpdateRequest` | String |
| GET | `/api/user/orders` | 주문 내역 | `page`, `size` | `List<OrderHistoryResponse>` |

### ⭐ **리뷰 API**
| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| POST | `/api/review/write` | 리뷰 작성 | `ReviewRequest` | String |
| POST | `/api/review/edit` | 리뷰 수정 | `ReviewRequest` | String |
| POST | `/api/review/delete` | 리뷰 삭제 | `reviewId` | String |

### 👨‍💼 **관리자 API**

#### 상품 관리
| Method | Endpoint | Description | Request | Response |
|--------|----------|-------------|---------|----------|
| GET | `/admin/product-inquiry` | 상품 조회 페이지 | 필터 파라미터들 | HTML |
| POST | `/admin/api/products/{isbn}/stock-in` | 입고 처리 | `quantity` | String |
| POST | `/admin/api/products/{isbn}/status` | 상태 변경 | `status` | String |
| POST | `/admin/api/products/{isbn}/price` | 가격 변경 | `price` | String |
| POST | `/admin/api/product/register` | 상품 등록 | `ProductRequest` | String |
| GET | `/admin/api/categories` | 카테고리 목록 | - | `CategoryTreeResponse` |

#### 주문 관리
| Method | Endpoint | Description | Parameters | Response |
|--------|----------|-------------|------------|----------|
| GET | `/admin/order-inquiry` | 주문 조회 페이지 | 검색 조건들 | HTML |
| GET | `/admin/api/orders` | 주문 목록 API | `page`, 필터 조건들 | `AdminOrderPageResponse` |
| POST | `/admin/api/orders/{orderId}/status` | 주문 상태 변경 | `idForAdmin`, `status` | String |

#### 회원 관리
| Method | Endpoint | Description | Parameters | Response |
|--------|----------|-------------|------------|----------|
| GET | `/admin/user-inquiry` | 회원 조회 페이지 | 검색 조건들 | HTML |
| GET | `/admin/api/users` | 회원 목록 API | `page`, 필터 조건들 | `Page<User>` |
| GET | `/admin/api/users/{idForAdmin}/orders` | 특정 회원 주문 내역 | `idForAdmin` | `List<AdminOrderHistoryResponse>` |

## 🚀 설치 및 실행

### 사전 요구사항
- **Java 17** 이상
- **Maven 3.6** 이상
- **MySQL 8.0** 이상
- **Git**

### 1. 프로젝트 클론
```bash
git clone https://github.com/your-username/springboot-bookstore.git
cd springboot-bookstore
```

### 2. 데이터베이스 설정
MySQL에서 새 데이터베이스 생성:
```sql
CREATE DATABASE bookstore_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'bookstore_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON bookstore_db.* TO 'bookstore_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 애플리케이션 설정 파일 생성
`src/main/resources/application.properties` 파일을 생성하고 다음 내용을 입력하세요:

```properties
# 데이터베이스 설정
spring.datasource.url=jdbc:mysql://localhost:3306/bookstore_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=bookstore_user
spring.datasource.password=your_database_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Seoul

# 로깅 설정
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# 파일 업로드 설정
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# 이메일 설정 (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

# KakaoPay 설정
kakaopay.admin-key=your_kakaopay_admin_key
kakaopay.cid=your_cid
kakaopay.ready-url=https://kapi.kakao.com/v1/payment/ready
kakaopay.approve-url=https://kapi.kakao.com/v1/payment/approve
kakaopay.cancel-url=https://kapi.kakao.com/v1/payment/cancel
kakaopay.fail-url=https://kapi.kakao.com/v1/payment/fail

# 서버 설정
server.port=8080
server.servlet.context-path=/
server.servlet.encoding.charset=UTF-8
server.servlet.encoding.enabled=true
server.servlet.encoding.force=true

# Thymeleaf 설정
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
```

### 4. 애플리케이션 실행
```bash
mvn clean install
mvn spring-boot:run
```

또는 IDE에서 `SpringbootAssignmentApplication.java` 실행

### 5. 접속 확인
브라우저에서 `http://localhost:8080` 접속

## 🗄 데이터베이스 설정

### 샘플 데이터 구성
프로젝트 실행 후 다음 순서로 데이터를 구성하세요:

1. **관리자 계정 생성**
```sql
INSERT INTO admin (admin_id, admin_pwd, admin_name, admin_status) 
VALUES ('admin', 'admin123', '시스템 관리자', 'ACTIVE');
```

2. **사용자 등급 설정**
```sql
INSERT INTO user_grade (user_grade_id, user_grade_name, grade_criteria_start_price, grade_criteria_end_price) VALUES
('BRONZE', '브론즈', 0, 99999),
('SILVER', '실버', 100000, 299999),
('GOLD', '골드', 300000, 499999),
('PLATINUM', '플래티넘', 500000, 999999);
```

3. **카테고리 구조 생성**
```sql
-- 대분류
INSERT INTO top_category (top_category_name) VALUES
('문학'), ('인문'), ('사회과학'), ('자연과학'), ('기술공학');

-- 중분류 (예시)
INSERT INTO middle_category (top_category, mid_category_name) VALUES
(1, '한국문학'), (1, '외국문학'), (2, '철학'), (2, '역사');

-- 소분류 (예시)  
INSERT INTO low_category (mid_category, low_category_name) VALUES
(1, '소설'), (1, '시'), (2, '소설'), (3, '동양철학');
```

4. **결제 방법 설정**
```sql
INSERT INTO payment_method (payment_method_id, payment_method_name, is_active) VALUES
('KAKAO_PAY', '카카오페이', true),
('BANK_TRANSFER', '계좌이체', true);
```

### 데이터베이스 백업/복원
프로젝트에는 데이터베이스 구조와 샘플 데이터를 포함한 SQL 파일이 제공됩니다:
- `database/full_export.sql` - 테이블 구조 및 샘플 데이터

#### 데이터베이스 복원 방법
```bash
  # 실제 운영 데이터로 복원 (추천)
  mysql -u bookstore_user -p bookstore_db < database/full_export.sql
```

## ⚙️ 환경 설정

### 필수 설정 항목

#### 1. KakaoPay API 설정
1. [Kakao Developers](https://developers.kakao.com) 에서 앱 생성
2. 결제 서비스 활성화
3. Admin Key 발급
4. `application.properties`에 설정

#### 2. 이메일 설정 (Gmail 기준)
1. Gmail 계정에서 앱 비밀번호 생성
2. `application.properties`에 이메일 정보 설정

#### 3. 보안 설정 (운영 환경)
```properties
# 운영 환경에서는 반드시 변경
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=WARN
```

### 선택 설정 항목

#### 1. 이미지 업로드 경로
```properties
file.upload.path=/path/to/upload/directory
```

#### 2. 세션 설정
```properties
server.servlet.session.timeout=30m
server.servlet.session.cookie.max-age=1800
```

## 🚨 트러블슈팅

주요 트러블슈팅 내용은 별도 문서를 참조하세요:
- [📋 TROUBLESHOOTING.md](TROUBLESHOOTING.md) - 상세 트러블슈팅 가이드

### 자주 발생하는 문제

#### 1. 데이터베이스 연결 실패
```
Could not create connection to database server
```
**해결방법:**
- MySQL 서버 실행 상태 확인
- 데이터베이스 계정 권한 확인
- 포트 번호 확인 (기본: 3306)

#### 2. KakaoPay API 오류
```
카카오페이 결제 준비 실패
```
**해결방법:**
- Admin Key 확인
- CID 확인
- 네트워크 연결 상태 확인

#### 3. 이메일 발송 실패
```
Failed to send email
```
**해결방법:**
- Gmail 앱 비밀번호 확인
- SMTP 포트 확인 (587)
- 방화벽 설정 확인

## 🧪 테스트 계정

### 관리자 계정
- **ID**: `admin_1`
- **Password**: `admin1!`

### 일반 사용자 계정 (샘플 데이터 로드 후)
- **ID**: `fastcampus`
- **Password**: `fast123!`

## 📂 프로젝트 구조
```
src/
├── main/
│   ├── java/bookstore_ai_project/
│   │   ├── config/          # 설정 클래스
│   │   ├── controller/      # 컨트롤러 클래스
│   │   ├── dto/             # 데이터 전송 객체
│   │   ├── entity/          # JPA 엔티티
│   │   ├── repository/      # 데이터 액세스 레이어
│   │   ├── service/         # 비즈니스 로직
│   │   └── scheduler/       # 스케줄러 (휴면계정 관리)
│   └── resources/
│       ├── static/          # 정적 자원 (CSS, JS, Images)
│       ├── templates/       # Thymeleaf 템플릿
│       └── application.properties
└── test/                    # 테스트 코드
```

## 🔧 성능 최적화

### 캐싱 전략
- 재고 조회 API 캐싱 (30초)
- 인기 검색어 캐싱 (1시간)
- 카테고리 구조 캐싱 (세션별)

### 데이터베이스 최적화
- 지연 로딩 (Lazy Loading) 전략
- 복합키 인덱스 활용
- 쿼리 최적화 (N+1 문제 해결)

## 🚀 배포 정보

### 개발 환경
- **Java**: OpenJDK 17
- **Spring Boot**: 3.3.1
- **MySQL**: 8.0.33
- **Port**: 8080

### 운영 환경 고려사항
- SSL 인증서 적용 필요
- 데이터베이스 성능 튜닝
- 로드 밸런싱 구성
- 보안 강화 (비밀번호 암호화 등)

## 📈 추가 개발 계획

### 단기 계획
- [ ] 비밀번호 암호화 (BCrypt)
- [ ] Spring Security 적용
- [ ] Redis 캐싱 시스템
- [ ] 이미지 업로드 기능

### 장기 계획
- [ ] 쿠폰/할인 시스템
- [ ] 추천 시스템 (협업 필터링)
- [ ] 모바일 앱 API
- [ ] 실시간 알림 시스템

## 🤝 기여하기
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 라이선스
이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

---

**YESorNO.24** - Spring Boot로 구현한 종합 온라인 서점 시스템