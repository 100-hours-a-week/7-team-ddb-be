# 돌핀(Dolphin) 백엔드 👋

## 프로젝트 소개 🐬
돌핀(Dolphin)은 사용자 중심의 커뮤니티 플랫폼으로, 위치 기반 정보 공유와 소통을 통해 더 나은 사용자 경험을 제공합니다. 본 프로젝트는 Spring Boot 기반의 백엔드 아키텍처를 사용하며, 모듈화된 도메인 중심 설계와 최신 기술을 적용하여 안정적이고 유지보수가 용이한 애플리케이션을 구현하고 있습니다.

## 팀 위키
- [팀 위키 보러가기](https://github.com/100-hours-a-week/7-team-ddb-wiki/wiki)
- [팀 칸반보드 보러가기](https://github.com/orgs/100-hours-a-week/projects/139)

## 목차 📑
- [기술 스택 🛠️](#기술-스택-)
- [프로젝트 구조 📂](#프로젝트-구조-)
- [설치 및 실행 방법 💻](#설치-및-실행-방법-)
- [개발 가이드 📝](#개발-가이드-)
- [주요 기능 ✨](#주요-기능-)
- [프로젝트 관리 📊](#프로젝트-관리-)
- [팀원 👨‍💻👩‍💻](#팀원-)

## 기술 스택 🛠️

### Core
- **언어**: Java 17 (LTS)
- **프레임워크**: Spring Boot 3.4.4
- **빌드 도구**: Gradle
- **API 문서화**: Swagger/OpenAPI 3.0

### 데이터베이스 및 캐싱
- **RDBMS**: PostgreSQL 15.5 with PostGIS 3.3.3
- **ORM**: Spring Data JPA 3.4.4, Hibernate Spatial 6.6.11.Final
- **캐싱**: Redis
- **쿼리 도구**: QueryDSL 5.0.0

### 인증 및 보안
- **인증**: OAuth 2.0 (카카오)
- **보안**: Spring Security 6.2.x
- **토큰**: JJWT 0.12.6

### 테스트 및 품질 관리
- **테스트 프레임워크**: JUnit 5, Mockito, AssertJ
- **통합 테스트**: Testcontainers
- **API 테스트**: MockMvc, TestRestTemplate

### 인프라 및 배포
- **컨테이너화**: Docker, Docker Compose
- **CI/CD**: GitHub Actions
- **클라우드**: AWS (EC2, S3, RDS, ElastiCache)
- **메시지 브로커**: RabbitMQ

### 기타 도구
- **로깅**: Logback
- **파일 저장소**: AWS S3
- **위치 기반 서비스**: PostGIS, GeoHash

## 프로젝트 구조 📂
```
com.dolpin/
├── DolpinApplication.java
├── common/
│   ├── config/
│   ├── exception/
│   ├── security/
│   ├── util/
│   └── aop/
├── domain/
│   ├── place/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── domain/
│   │   ├── dto/
│   │   └── exception/
│   ├── user/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── domain/
│   │   ├── dto/
│   │   └── exception/
│   ├── moment/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── domain/
│   │   ├── dto/
│   │   └── exception/
│   └── comment/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── domain/
│       ├── dto/
│       └── exception/
├── global/
│   ├── cache/
│   ├── event/
│   ├── search/
│   ├── storage/
│   └── monitoring/
├── infra/
│   ├── aws/
│   ├── redis/
│   └── postgis/
└── external/
├── oauth/
├── s3/
└── imageprocessing/
```
## 설치 및 실행 방법 💻

### 사전 요구사항
- Java JDK 17 이상
- Docker 및 Docker Compose
- PostgreSQL 15.5 with PostGIS 3.3.3 extension
- Redis

### 설치

```bash
# 저장소 클론
git clone https://github.com/100-hours-a-week/dolphin-backend.git
cd dolphin-backend

# Gradle Wrapper로 프로젝트 빌드
./gradlew clean build

# Docker 컨테이너로 필요한 서비스 실행
docker-compose -f docker-compose-dev.yml up -d
개발 서버 실행
bash# 개발 프로필로 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 또는 IDE에서 DolpinApplication 클래스 실행
# 이때 VM Option에 -Dspring.profiles.active=dev 추가
빌드 및 프로덕션 실행
bash# 프로덕션 빌드
./gradlew clean build

# JAR 파일 실행
java -jar -Dspring.profiles.active=prod build/libs/dolphin-0.0.1-SNAPSHOT.jar

# 또는 Docker 이미지 빌드 및 실행
docker build -t dolphin-backend:latest .
docker run -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=prod" dolphin-backend:latest
```
## 개발 가이드 📝

### 코딩 컨벤션
- **네이밍 규칙**:
    - 클래스: PascalCase (예: PlaceController)
    - 메서드 및 변수: camelCase (예: findPlaceById)
    - 상수: UPPER_SNAKE_CASE (예: MAX_RESULTS_PER_PAGE)
    - 패키지: 모두 소문자 (예: com.dolpin.domain.place)

- **코드 포맷팅**:
    - 인덴트: 4 스페이스
    - 최대 줄 길이: 120자
    - Google Java Style Guide 준수

- **Java 17 기능 활용**:
    - Record 클래스를 DTO로 활용하여 코드량 감소
    - 패턴 매칭 for instanceof 활용
    - Switch Expression 사용
    - Sealed Classes 적절히 활용

### 아키텍처 가이드라인
- **계층 분리**: Controller → Service → Repository 구조 준수
- **DTO 사용**: 계층 간 데이터 전송에는 반드시 DTO 사용
- **예외 처리**: 도메인별 커스텀 예외 클래스 정의 및 GlobalExceptionHandler로 처리
- **트랜잭션 관리**: @Transactional 어노테이션으로 일관성 보장

### 테스트
- **단위 테스트**: 서비스 레이어 로직에 대한 단위 테스트 필수 (목표 커버리지: 90%+)
- **통합 테스트**: Testcontainers를 활용한 주요 사용자 시나리오 검증
- **API 테스트**: 모든 API 엔드포인트에 대한 기본 CRUD 테스트
- **테스트 네이밍**: methodName_scenario_expectedBehavior 형식 사용

## 주요 기능 ✨

### 사용자 도메인
- 카카오 OAuth2.0 기반 소셜 로그인
- 사용자 프로필 관리 및 조회
- 개인정보 및 위치정보 동의 관리
- JWT 기반 인증 및 토큰 갱신

### 장소 도메인
- PostGIS 기반 위치 검색 (ST_DWithin, ST_Distance 활용)
- 장소 상세 정보 조회 및 캐싱
- 북마크 저장 및 관리
- 카테고리별 장소 필터링

### 모먼트(기록) 도메인
- 장소별 기록 작성 및 조회
- S3 연동 이미지 업로드 및 관리
- 개인/공개 기록 설정
- 사용자별 기록 목록 조회

### 댓글 도메인
- 모먼트에 대한 댓글 작성 및 조회
- 댓글 삭제 기능
- 댓글 목록 페이지네이션
- 작성자 정보 표시

## 프로젝트 관리 📊

### 이슈 관리
- GitHub Issues를 활용한 이슈 추적
- 기능 단위 브랜치 관리 (feature/*, bugfix/*)
- 코드 리뷰 프로세스를 통한 품질 관리

### 배포 프로세스
- GitHub Actions를 통한 CI/CD 파이프라인
- 개발(dev), 스테이징(staging), 프로덕션(prod) 환경 분리
- 테스트 자동화 및 품질 게이트 적용

### 문서화
- Swagger/OpenAPI를 활용한 API 문서 자동화
- ERD 및 데이터베이스 스키마 문서 관리
- 기술 문서 및 설계 문서 GitHub Wiki 관리

## 팀원 👨‍💻👩‍💻

| 이름 | 역할 | 주요 업무 |
|------------------|------|------------|
| suzy.kang (강수지) | 프론트엔드/팀장 | • 프론트엔드 개발<br>• UI/UX 설계<br>• 사용자 인터페이스 구현<br>• 애자일 스크럼 관리<br>• 칸반보드 및 GitHub 위키 관리<br>• 디스코드 봇 설정 및 자동화 시스템 구축 |
| eric.choi (최진우) | 백엔드 | • 백엔드 시스템 설계 및 개발<br>• API 설계 및 구현<br>• 데이터베이스 모델링 |
| juny.lee (이현준) | 인공지능 | • AI 모델 설계 및 개발<br>• 모델 학습 및 최적화<br>• 데이터 분석 및 처리 |
| jensen.hwang (황찬희) | 인공지능 | • AI 모델 설계 및 개발<br>• 추천 시스템 구현<br>• 성능 분석 및 개선 |
| peter.kang (강동석) | 클라우드 | • 클라우드 인프라 구축<br>• 시스템 아키텍처 설계<br>• 서버 관리 및 모니터링 |
| lily.shin (신지영) | 클라우드 | • 클라우드 서비스 구현<br>• 배포 자동화<br>• 보안 및 성능 최적화 |
