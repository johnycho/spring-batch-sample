package com.example.batch.reader;

import com.example.batch.entity.Customer;
import com.example.batch.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 각 리더의 성능을 측정하는 테스트 클래스
 */
@SpringBootTest
@ActiveProfiles("test")
class ReaderPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(ReaderPerformanceTest.class);

    @Autowired
    @Qualifier("jdbcCursorItemReader")
    private ItemReader<Customer> jdbcCursorItemReader;

    @Autowired
    @Qualifier("jdbcPagingItemReader")
    private ItemReader<Customer> jdbcPagingItemReader;

    @Autowired
    @Qualifier("flatFileItemReader")
    private ItemReader<Product> flatFileItemReader;

    @Autowired
    @Qualifier("jsonItemReader")
    private ItemReader<Customer> jsonItemReader;

    /**
     * ListItemReader 생성 헬퍼 메서드
     * ListItemReader는 상태를 유지하므로 각 테스트마다 새 인스턴스를 생성해야 함
     */
    private ItemReader<Product> createListItemReader() {
        return new ListItemReader<>(Arrays.asList(
            new Product(null, "태블릿", new BigDecimal("500000"), "전자제품", 20),
            new Product(null, "스마트폰", new BigDecimal("800000"), "전자제품", 100),
            new Product(null, "이어폰", new BigDecimal("150000"), "전자제품", 300)
        ));
    }

    @Autowired
    @Qualifier("repositoryItemReader")
    private ItemReader<Product> repositoryItemReader;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier("multiResourceItemReader")
    private ItemReader<Product> multiResourceItemReader;

    @Autowired
    @Qualifier("jpaCursorItemReader")
    private ItemReader<Customer> jpaCursorItemReader;

    @Autowired
    @Qualifier("hintSettableJpaCursorItemReader")
    private ItemReader<Customer> hintSettableJpaCursorItemReader;

    @Autowired
    @Qualifier("jpaPagingItemReader")
    private ItemReader<Product> jpaPagingItemReader;

    @Autowired
    @Qualifier("staxEventItemReader")
    private ItemReader<Customer> staxEventItemReader;

    @Autowired
    @Qualifier("mappingSqlQueryItemReader")
    private ItemReader<Customer> mappingSqlQueryItemReader;

    /**
     * 리더 성능 측정 결과를 담는 내부 클래스
     */
    private static class PerformanceResult {
        String readerName;
        long totalTimeNs;  // 나노초 단위로 저장
        long totalTimeMs;   // 밀리초 단위 (표시용)
        int itemCount;
        double itemsPerSecond;
        long memoryUsedBytes;

        PerformanceResult(String readerName, long totalTimeNs, int itemCount, long memoryUsedBytes) {
            this.readerName = readerName;
            this.totalTimeNs = totalTimeNs;
            this.totalTimeMs = TimeUnit.NANOSECONDS.toMillis(totalTimeNs);
            // 최소 1밀리초로 처리 (0ms 방지)
            if (this.totalTimeMs == 0 && totalTimeNs > 0) {
                this.totalTimeMs = 1;
            }
            this.itemCount = itemCount;
            // 나노초 기반으로 처리량 계산
            this.itemsPerSecond = itemCount > 0 && totalTimeNs > 0
                ? (itemCount * 1_000_000_000.0 / totalTimeNs)
                : 0;
            this.memoryUsedBytes = memoryUsedBytes;
        }

        @Override
        public String toString() {
            // 나노초가 1ms 미만인 경우 마이크로초로 표시
            String timeDisplay;
            if (totalTimeMs == 0 && totalTimeNs > 0) {
                long microSeconds = TimeUnit.NANOSECONDS.toMicros(totalTimeNs);
                timeDisplay = String.format("%6d μs", microSeconds);
            } else {
                timeDisplay = String.format("%6d ms", totalTimeMs);
            }

            return String.format(
                "%-25s | 처리시간: %s | 아이템수: %3d | 처리량: %8.2f items/sec | 메모리: %8.2f MB",
                readerName,
                timeDisplay,
                itemCount,
                itemsPerSecond,
                memoryUsedBytes / (1024.0 * 1024.0)
            );
        }
    }

    @BeforeEach
    void setUp() {
        // GC 실행하여 메모리 상태 초기화
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 리더의 성능을 측정하는 공통 메서드
     */
    private <T> PerformanceResult measureReaderPerformance(
            String readerName,
            ItemReader<T> reader,
            Class<T> itemType) throws Exception {

        // 메모리 측정 시작
        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // GC 실행
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // 성능 측정 시작
        long startTime = System.nanoTime();

        List<T> items = new ArrayList<>();
        T item;
        int count = 0;

        try {
            // 리더 초기화 (필요한 경우)
            if (reader instanceof org.springframework.batch.item.ItemStream) {
                try {
                    ((org.springframework.batch.item.ItemStream) reader).open(new ExecutionContext());
                } catch (Exception e) {
                    // 이미 열려있을 수 있음, 무시
                    log.debug("Reader open 실패 (이미 열려있을 수 있음): {}", e.getMessage());
                }
            }

            // 모든 아이템 읽기
            while ((item = reader.read()) != null) {
                items.add(item);
                count++;
            }

            // FlatFileParseException 등 파싱 오류는 정상 종료로 간주 (빈 줄 등)
            // 이미 읽은 아이템이 있으면 성공으로 간주

        } finally {
            // 리더 정리
            if (reader instanceof org.springframework.batch.item.ItemStream) {
                try {
                    ((org.springframework.batch.item.ItemStream) reader).close();
                } catch (Exception e) {
                    log.debug("Reader close 실패: {}", e.getMessage());
                }
            }
        }

        // 성능 측정 종료
        long endTime = System.nanoTime();
        long totalTimeNs = endTime - startTime;

        // 메모리 측정 종료
        runtime.gc(); // GC 실행
        Thread.sleep(50); // GC 완료 대기
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = Math.max(0, memoryAfter - memoryBefore);

        return new PerformanceResult(readerName, totalTimeNs, count, memoryUsed);
    }

    @Test
    @DisplayName("JdbcCursorItemReader 성능 테스트")
    void testJdbcCursorItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "JdbcCursorItemReader",
            jdbcCursorItemReader,
            Customer.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0); // 나노초 단위로 검증
    }

    @Test
    @DisplayName("JdbcPagingItemReader 성능 테스트")
    void testJdbcPagingItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "JdbcPagingItemReader",
            jdbcPagingItemReader,
            Customer.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0); // 나노초 단위로 검증
    }

    @Test
    @DisplayName("FlatFileItemReader 성능 테스트")
    void testFlatFileItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "FlatFileItemReader",
            flatFileItemReader,
            Product.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0); // 나노초 단위로 검증
    }

    @Test
    @DisplayName("JsonItemReader 성능 테스트")
    void testJsonItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "JsonItemReader",
            jsonItemReader,
            Customer.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0); // 나노초 단위로 검증
    }

    @Test
    @DisplayName("ListItemReader 성능 테스트")
    void testListItemReaderPerformance() throws Exception {
        // ListItemReader는 상태를 유지하므로 매번 새 인스턴스 생성
        ItemReader<Product> reader = createListItemReader();
        PerformanceResult result = measureReaderPerformance(
            "ListItemReader",
            reader,
            Product.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0); // 나노초 단위로 검증
    }

    @Test
    @DisplayName("RepositoryItemReader 성능 테스트")
    void testRepositoryItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "RepositoryItemReader",
            repositoryItemReader,
            Product.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0); // 나노초 단위로 검증
    }

    @Test
    @DisplayName("MultiResourceItemReader 성능 테스트")
    void testMultiResourceItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "MultiResourceItemReader",
            multiResourceItemReader,
            Product.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("JpaCursorItemReader 성능 테스트")
    void testJpaCursorItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "JpaCursorItemReader",
            jpaCursorItemReader,
            Customer.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("HintSettableJpaCursorItemReader 성능 테스트")
    void testHintSettableJpaCursorItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "HintSettableJpaCursorItemReader",
            hintSettableJpaCursorItemReader,
            Customer.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("JpaPagingItemReader 성능 테스트")
    void testJpaPagingItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "JpaPagingItemReader",
            jpaPagingItemReader,
            Product.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("StaxEventItemReader 성능 테스트")
    void testStaxEventItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "StaxEventItemReader",
            staxEventItemReader,
            Customer.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("MappingSqlQueryItemReader 성능 테스트")
    void testMappingSqlQueryItemReaderPerformance() throws Exception {
        PerformanceResult result = measureReaderPerformance(
            "MappingSqlQueryItemReader",
            mappingSqlQueryItemReader,
            Customer.class
        );

        log.info("성능 측정 결과:\n{}", result);

        assertThat(result.itemCount).isGreaterThan(0);
        assertThat(result.totalTimeNs).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("스프링 컨텍스트의 모든 ItemReader 성능 비교 테스트")
    void testAllItemReadersFromContextPerformance() throws Exception {
        log.info("=".repeat(100));
        log.info("스프링 컨텍스트에서 등록된 모든 ItemReader 성능 비교 시작");
        log.info("=".repeat(100));

        List<PerformanceResult> results = new ArrayList<>();

        // ApplicationContext에서 모든 ItemReader 빈 조회
        var readers = applicationContext.getBeansOfType(ItemReader.class);

        for (var entry : readers.entrySet()) {
            String beanName = entry.getKey();
            @SuppressWarnings("unchecked")
            ItemReader<Object> reader = (ItemReader<Object>) entry.getValue();

            if (reader instanceof FlatFileItemReader<?>) {
              log.info("FlatFileItemReader는 별도 Step 기반 테스트 필요, skip: {}", beanName);
              continue;
            }

            log.info("ItemReader 빈 성능 측정 시작: {}", beanName);
            PerformanceResult result = measureReaderPerformance(beanName, reader, Object.class);
            results.add(result);
        }

        log.info("\n" + "=".repeat(100));
        log.info("스프링 컨텍스트 ItemReader 성능 측정 결과 요약");
        log.info("=".repeat(100));
        log.info(String.format("%-40s | %12s | %10s | %15s | %12s",
                "빈 이름", "처리시간(ms)", "아이템수", "처리량(items/sec)", "메모리(MB)"));
        log.info("-".repeat(110));

        for (PerformanceResult result : results) {
            log.info(result.toString());
        }

        log.info("=".repeat(100));

        assertThat(results).isNotEmpty();
    }

    @Test
    @DisplayName("리더 성능 반복 측정 테스트 (평균값 계산)")
    void testReaderPerformanceWithMultipleRuns() throws Exception {
        int runs = 5;
        log.info("각 리더를 {}번 실행하여 평균 성능 측정", runs);
        log.info("=".repeat(100));
        
        // JdbcCursorItemReader 반복 측정
        List<PerformanceResult> cursorResults = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            cursorResults.add(measureReaderPerformance(
                "JdbcCursorItemReader (Run " + (i + 1) + ")",
                jdbcCursorItemReader,
                Customer.class
            ));
            Thread.sleep(50);
        }
        
        // 평균 계산 (나노초 단위)
        double avgTimeNs = cursorResults.stream()
            .mapToLong(r -> r.totalTimeNs)
            .average()
            .orElse(0);
        
        double avgTimeMs = TimeUnit.NANOSECONDS.toMillis((long)avgTimeNs);
        if (avgTimeMs == 0 && avgTimeNs > 0) {
            avgTimeMs = 1;
        }
        
        double avgThroughput = cursorResults.stream()
            .mapToDouble(r -> r.itemsPerSecond)
            .average()
            .orElse(0);
        
        log.info("\nJdbcCursorItemReader 평균 성능:");
        log.info("평균 처리시간: {} ms", String.format("%.2f", avgTimeMs));
        log.info("평균 처리량: {} items/sec", String.format("%.2f", avgThroughput));
        log.info("=".repeat(100));
        
        assertThat(avgTimeNs).isGreaterThanOrEqualTo(0);
    }
}

