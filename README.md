# Spring Batch 샘플 프로젝트

## 프로젝트 구조

```
src/main/java/com/example/batch/
├── BatchApplication.java          # 메인 애플리케이션
├── config/
│   └── BatchConfig.java          # Batch Job/Step 설정
├── entity/                        # 엔티티 클래스
├── repository/                    # JPA Repository
├── reader/                        # ItemReader 구현
├── processor/                     # ItemProcessor 구현
├── writer/                        # ItemWriter 구현
└── controller/                    # Job 실행 컨트롤러
```

## 테스트 ItemReader 종류

### 1. JdbcCursorItemReader
- **용도**: JDBC 커서 기반 데이터 읽기
- **특징**: 대용량 데이터 처리에 적합, 메모리 효율적
- **사용 예**: `jdbcCursorItemReader()` 메서드 참조

### 2. JdbcPagingItemReader
- **용도**: JDBC 페이징 기반 데이터 읽기
- **특징**: 페이지 단위로 데이터를 읽어 메모리 사용량 제어
- **사용 예**: `jdbcPagingItemReader()` 메서드 참조

### 3. FlatFileItemReader
- **용도**: CSV/텍스트 파일 읽기
- **특징**: 파일 기반 데이터 처리
- **사용 예**: `flatFileItemReader()` 메서드 참조
- **파일**: `src/main/resources/products.csv`

### 4. JsonItemReader
- **용도**: JSON 파일 읽기
- **특징**: JSON 형식의 데이터 파일 처리
- **사용 예**: `jsonItemReader()` 메서드 참조
- **파일**: `src/main/resources/customers.json`

### 5. ListItemReader
- **용도**: 메모리 내 리스트 읽기
- **특징**: 간단한 테스트나 작은 데이터셋에 사용
- **사용 예**: `listItemReader()` 메서드 참조

### 6. RepositoryItemReader
- **용도**: Spring Data Repository 기반 읽기
- **특징**: JPA Repository를 사용한 읽기 (개념 예제)
- **사용 예**: `repositoryItemReader()` 메서드 참조

### 7. MultiResourceItemReader (다중 CSV)
- **용도**: 여러 CSV 파일을 순차적으로 읽기
- **특징**: 파일을 나눠 관리할 때 유용
- **사용 예**: `multiResourceItemReader()` 메서드 참조
- **파일**: `products-part1.csv`, `products-part2.csv`

### 8. JpaPagingItemReader
- **용도**: JPA 엔티티를 페이징 방식으로 읽기
- **특징**: 페이징 단위 조회로 메모리 사용 제어
- **사용 예**: `jpaPagingItemReader()` 메서드 참조

### 9. JpaCursorItemReader
- **용도**: JPA EntityManager를 이용해 커서 스타일로 읽기
- **특징**: JPA 기반 스트리밍 처리
- **사용 예**: `jpaCursorItemReader()` 메서드 참조

### 10. HintSettableJpaCursorItemReader
- **용도**: JPA 커서 기반 읽기 + 쿼리 힌트(fetch size, read-only 등) 적용 가능
- **특징**: Hibernate 쿼리 힌트를 활용해 성능(메모리 사용량·속도) 최적화 실험 가능. 기본 `JpaCursorItemReader` 대비 더 세밀한 튜닝이 필요한 경우 사용
- **사용 예**: `hintSettableJpaCursorItemReader()` 메서드 참조

### 11. StaxEventItemReader (XML 스트리밍)
- **용도**: XML을 스트리밍 방식으로 읽기
- **특징**: 큰 XML도 메모리 효율적으로 처리
- **파일**: `customers.xml`

### 12. MappingSqlQuery 스타일 (커스텀 RowMapper)
- **용도**: RowMapper 기반 SQL 결과를 객체로 매핑
- **특징**: SQL+RowMapper로 세밀한 매핑 제어

## 실행 방법

### 1. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 2. Job 실행 (REST API)
애플리케이션 실행 후 다음 엔드포인트로 각 Job을 실행할 수 있습니다:

- `curl -X POST http://localhost:8080/api/jobs/jdbc-cursor` - JdbcCursorItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/jdbc-paging` - JdbcPagingItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/flat-file` - FlatFileItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/json` - JsonItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/list-item` - ListItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/repository-item` - RepositoryItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/multi-resource` - MultiResourceItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/jpa-paging` - JpaPagingItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/jpa-cursor` - JpaCursorItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/hint-jpa-cursor` - HintSettableJpaCursorItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/stax-xml` - StaxEventItemReader 사용
- `curl -X POST http://localhost:8080/api/jobs/mapping-sql` - MappingSqlQuery 스타일 Reader 사용

Swagger UI: http://localhost:8080/swagger-ui.html

### 3. H2 콘솔 접속
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (비어있음)

## 데이터베이스 스키마

- `customer`: 고객 정보 테이블
- `product`: 상품 정보 테이블
- `customer_processed`: 처리된 고객 정보 테이블

## 주요 설정

- **Spring Boot**: 3.2.0
- **Java**: 17
- **데이터베이스**: H2 (인메모리)
- **빌드 도구**: Gradle

## 성능 테스트

각 리더의 성능을 측정하고 비교할 수 있는 테스트 클래스가 포함되어 있습니다.

### 성능 테스트 실행

```bash
# 모든 성능 테스트 실행
./gradlew test --tests ReaderPerformanceTest

# 특정 리더의 성능 테스트만 실행
./gradlew test --tests ReaderPerformanceTest.testJdbcCursorItemReaderPerformance

# 모든 리더 성능 비교 테스트 실행
./gradlew test --tests ReaderPerformanceTest.testAllReadersPerformanceComparison
```

### 측정 항목

성능 테스트는 다음 항목들을 측정합니다:

1. **처리 시간**: 모든 아이템을 읽는데 걸리는 시간 (밀리초)
2. **처리량**: 초당 처리할 수 있는 아이템 수 (items/sec)
3. **메모리 사용량**: 리더 실행 중 사용된 메모리 (MB)
4. **아이템 수**: 읽어온 총 아이템 개수

### 성능 테스트 종류

- **개별 리더 테스트**: 각 리더의 성능을 개별적으로 측정
- **비교 테스트**: 모든 리더의 성능을 한 번에 측정하고 비교
- **반복 측정 테스트**: 동일 리더를 여러 번 실행하여 평균 성능 계산

### 테스트 결과 예시

```
====================================================================================================
성능 측정 결과 요약
====================================================================================================
리더명                      | 처리시간(ms) | 아이템수 | 처리량(items/sec) | 메모리(MB)
----------------------------------------------------------------------------------------------------
JdbcCursorItemReader        | 처리시간:     45 ms | 아이템수:   8 | 처리량:  177.78 items/sec | 메모리:    0.12 MB
JdbcPagingItemReader        | 처리시간:     52 ms | 아이템수:   8 | 처리량:  153.85 items/sec | 메모리:    0.15 MB
...
```

## 참고사항

- 각 Job은 독립적으로 실행 가능합니다
- Job 실행 결과는 콘솔 로그와 H2 데이터베이스에서 확인할 수 있습니다
- `application.yml`에서 `spring.batch.job.enabled: false`로 설정되어 있어 자동 실행되지 않습니다
- 성능 테스트는 테스트 환경(`application-test.yml`)에서 실행되며, 실제 운영 환경과는 다를 수 있습니다
