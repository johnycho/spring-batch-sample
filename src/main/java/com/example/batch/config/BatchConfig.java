package com.example.batch.config;

import com.example.batch.entity.Customer;
import com.example.batch.entity.CustomerProcessed;
import com.example.batch.entity.Product;
import com.example.batch.processor.CustomerItemProcessor;
import com.example.batch.processor.ProductItemProcessor;
import com.example.batch.reader.*;
import com.example.batch.writer.CustomerItemWriter;
import com.example.batch.writer.ProductItemWriter;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernateHints;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.H2PagingQueryProvider;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.builder.JsonItemReaderBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final DataSource dataSource;
  private final EntityManagerFactory entityManagerFactory;
  private final CustomerItemProcessor customerItemProcessor;
  private final ProductItemProcessor productItemProcessor;

  // ========== ItemReader 샘플들 ==========

  /**
   * 1. JdbcCursorItemReader - JDBC 커서 기반 읽기
   * 대용량 데이터 처리에 적합, 메모리 효율적
   */
  @Bean
  public ItemReader<Customer> jdbcCursorItemReader() {
    return new JdbcCursorItemReaderBuilder<Customer>()
        .name("jdbcCursorItemReader")
        .dataSource(dataSource)
        .sql("SELECT id, first_name, last_name, email, age, created_at FROM customer ORDER BY id")
        .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
        .build();
  }

  /**
   * 2. JdbcPagingItemReader - JDBC 페이징 기반 읽기
   * 페이지 단위로 데이터를 읽어 메모리 사용량 제어
   */
  @Bean
  public ItemReader<Customer> jdbcPagingItemReader() {
    Map<String, Order> sortKeys = new HashMap<>();
    sortKeys.put("id", Order.ASCENDING);

    H2PagingQueryProvider queryProvider = new H2PagingQueryProvider();
    queryProvider.setSelectClause("id, first_name, last_name, email, age, created_at");
    queryProvider.setFromClause("FROM customer");
    queryProvider.setSortKeys(sortKeys);

    return new JdbcPagingItemReaderBuilder<Customer>()
        .name("jdbcPagingItemReader")
        .dataSource(dataSource)
        .queryProvider(queryProvider)
        .pageSize(3) // 페이지 크기 설정
        .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
        .build();
  }

  /**
   * 3. FlatFileItemReader - CSV/텍스트 파일 읽기
   * 파일 기반 데이터 처리에 사용
   */
  @Bean
  public ItemReader<Product> flatFileItemReader() {
    return new FlatFileItemReaderBuilder<Product>()
        .name("flatFileItemReader")
        .resource(new FileSystemResource("data/products-100k.csv"))
        .delimited()
        .names("name", "price", "category", "stock")
        .targetType(Product.class)
        .linesToSkip(1) // 헤더 라인 스킵
        .strict(false) // 파일이 없어도 오류 발생하지 않음
        .build();
  }

  /**
   * 4. JsonItemReader - JSON 파일 읽기
   * JSON 형식의 데이터 파일 처리
   */
  @Bean
  public ItemReader<Customer> jsonItemReader() {
    JacksonJsonObjectReader<Customer> jsonObjectReader = new JacksonJsonObjectReader<>(Customer.class);
    jsonObjectReader.setMapper(new com.fasterxml.jackson.databind.ObjectMapper());

    return new JsonItemReaderBuilder<Customer>()
        .name("jsonItemReader")
        .resource(new FileSystemResource("data/customers-100k.json"))
        .jsonObjectReader(jsonObjectReader)
        .build();
  }

  /**
   * 5. ListItemReader - 메모리 내 리스트 읽기
   * 간단한 테스트나 작은 데이터셋에 사용
   */
  @Bean
  public ItemReader<Product> listItemReader() {
    return new ListItemReader<>(Arrays.asList(
        new Product(null, "태블릿", new java.math.BigDecimal("500000"), "전자제품", 20),
        new Product(null, "스마트폰", new java.math.BigDecimal("800000"), "전자제품", 100),
        new Product(null, "이어폰", new java.math.BigDecimal("150000"), "전자제품", 300)
    ));
  }

  /**
   * 6. RepositoryItemReader - Spring Data Repository 기반 읽기
   * JPA Repository를 사용한 읽기 (별도 클래스로 구현)
   */
  @Bean
  public ItemReader<Product> repositoryItemReader() {
    return new RepositoryItemReaderImpl<>(dataSource);
  }

  /**
   * 7. MultiResourceItemReader - 다중 CSV 파일 순차 읽기
   */
  @Bean
  public ItemReader<Product> multiResourceItemReader() {
    Resource[] resources = new Resource[]{
        new FileSystemResource("data/products-part1-100k.csv"),
        new FileSystemResource("data/products-part2-100k.csv")
    };

    return new MultiResourceItemReaderBuilder<Product>()
        .name("multiResourceItemReader")
        .resources(resources)
        .delegate(multiResourceProductReader())
        .build();
  }

  @Bean
  public FlatFileItemReader<Product> multiResourceProductReader() {
    return new FlatFileItemReaderBuilder<Product>()
        .name("multiResourceProductReader")
        .delimited()
        .names("name", "price", "category", "stock")
        .targetType(Product.class)
        .linesToSkip(1)
        .strict(false)
        .build();
  }

  /**
   * 8. JpaPagingItemReader - JPA 페이징 기반 읽기
   */
  @Bean
  public ItemReader<Customer> jpaPagingItemReader() {
    return new JpaPagingItemReaderBuilder<Customer>()
        .name("jpaPagingItemReader")
        .entityManagerFactory(entityManagerFactory)
        .pageSize(2)
        .queryString("SELECT c FROM Customer c ORDER BY c.id")
        .build();
  }

  /**
   * 9. JpaCursorItemReader - JPA 커서 기반 읽기
   */
  @Bean
  public ItemReader<Customer> jpaCursorItemReader() {
    return new JpaCursorItemReaderBuilder<Customer>()
        .name("jpaCursorItemReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("SELECT c FROM Customer c ORDER BY c.id")
        .build();
  }

  /**
   * 10. HintSettableJpaCursorItemReader - JPA 커서 기반 읽기 + 힌트 설정 가능
   * JPA 쿼리 힌트(fetch size, read-only 등)를 세팅해서 성능 튜닝에 활용하는 리더
   */
  @Bean
  public ItemReader<Customer> hintSettableJpaCursorItemReader() {
    HintSettableJpaCursorItemReader<Customer> reader = new HintSettableJpaCursorItemReader<>();
    reader.setEntityManagerFactory(entityManagerFactory);
    reader.setQueryString("SELECT c FROM Customer c ORDER BY c.id");
    reader.setHintValues(Map.of(HibernateHints.HINT_FETCH_SIZE, 2, HibernateHints.HINT_READ_ONLY, true));
    return reader;
  }

  /**
   * 11. StaxEventItemReader - XML 스트리밍 읽기
   */
  @Bean
  public ItemReader<Customer> staxEventItemReader(Jaxb2Marshaller customerMarshaller) {
    return new StaxEventItemReaderBuilder<Customer>()
        .name("staxEventItemReader")
        .resource(new FileSystemResource("data/customers-100k.xml"))
        .addFragmentRootElements("customer")
        .unmarshaller(customerMarshaller)
        .build();
  }

  @Bean
  public Jaxb2Marshaller customerMarshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(Customer.class);
    return marshaller;
  }

  /**
   * 12. MappingSqlQuery 스타일 - 커스텀 RowMapper 기반 JDBC 읽기
   */
  @Bean
  public ItemReader<Customer> mappingSqlQueryItemReader() {
    RowMapper<Customer> rowMapper = (rs, rowNum) -> {
      Customer c = new Customer();
      c.setId(rs.getLong("id"));
      c.setFirstName(rs.getString("first_name"));
      c.setLastName(rs.getString("last_name"));
      c.setEmail(rs.getString("email"));
      c.setAge(rs.getInt("age"));
      c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
      return c;
    };

    return new JdbcCursorItemReaderBuilder<Customer>()
        .name("mappingSqlQueryItemReader")
        .dataSource(dataSource)
        .sql("SELECT id, first_name, last_name, email, age, created_at FROM customer ORDER BY id")
        .rowMapper(rowMapper)
        .build();
  }

  // ========== ItemProcessor ==========
  // CustomerItemProcessor와 ProductItemProcessor는 @Component로 이미 빈으로 등록되어 있음

  // ========== ItemWriter ==========
  @Bean
  public ItemWriter<CustomerProcessed> customerItemWriter() {
    return new CustomerItemWriter(dataSource);
  }

  @Bean
  public ItemWriter<Product> productItemWriter() {
    return new ProductItemWriter(dataSource);
  }

  // ========== Steps ==========

  @Bean
  public Step jdbcCursorStep() {
    return new StepBuilder("jdbcCursorStep", jobRepository)
        .<Customer, CustomerProcessed>chunk(3, transactionManager)
        .reader(jdbcCursorItemReader())
        .processor(customerItemProcessor)
        .writer(customerItemWriter())
        .build();
  }

  @Bean
  public Step jdbcPagingStep() {
    return new StepBuilder("jdbcPagingStep", jobRepository)
        .<Customer, CustomerProcessed>chunk(2, transactionManager)
        .reader(jdbcPagingItemReader())
        .processor(customerItemProcessor)
        .writer(customerItemWriter())
        .build();
  }

  @Bean
  public Step flatFileStep() {
    return new StepBuilder("flatFileStep", jobRepository)
        .<Product, Product>chunk(2, transactionManager)
        .reader(flatFileItemReader())
        .processor(productItemProcessor)
        .writer(productItemWriter())
        .build();
  }

  @Bean
  public Step jsonStep() {
    return new StepBuilder("jsonStep", jobRepository)
        .<Customer, CustomerProcessed>chunk(2, transactionManager)
        .reader(jsonItemReader())
        .processor(customerItemProcessor)
        .writer(customerItemWriter())
        .build();
  }

  @Bean
  public Step listItemStep() {
    return new StepBuilder("listItemStep", jobRepository)
        .<Product, Product>chunk(2, transactionManager)
        .reader(listItemReader())
        .processor(productItemProcessor)
        .writer(productItemWriter())
        .build();
  }

  @Bean
  public Step repositoryItemStep() {
    return new StepBuilder("repositoryItemStep", jobRepository)
        .<Product, Product>chunk(2, transactionManager)
        .reader(repositoryItemReader())
        .processor(productItemProcessor)
        .writer(productItemWriter())
        .build();
  }

  @Bean
  public Step multiResourceStep() {
    return new StepBuilder("multiResourceStep", jobRepository)
        .<Product, Product>chunk(2, transactionManager)
        .reader(multiResourceItemReader())
        .processor(productItemProcessor)
        .writer(productItemWriter())
        .build();
  }

  @Bean
  public Step jpaPagingStep() {
    return new StepBuilder("jpaPagingStep", jobRepository)
        .<Customer, CustomerProcessed>chunk(2, transactionManager)
        .reader(jpaPagingItemReader())
        .processor(customerItemProcessor)
        .writer(customerItemWriter())
        .build();
  }

  @Bean
  public Step jpaCursorStep() {
    return new StepBuilder("jpaCursorStep", jobRepository)
        .<Customer, CustomerProcessed>chunk(2, transactionManager)
        .reader(jpaCursorItemReader())
        .processor(customerItemProcessor)
        .writer(customerItemWriter())
        .build();
  }

  @Bean
  public Step hintSettableJpaCursorStep() {
    return new StepBuilder("hintSettableJpaCursorStep", jobRepository)
        .<Customer, CustomerProcessed>chunk(2, transactionManager)
        .reader(hintSettableJpaCursorItemReader())
        .processor(customerItemProcessor)
        .writer(customerItemWriter())
        .build();
  }

  @Bean
  public Step staxEventStep() {
    return new StepBuilder("staxEventStep", jobRepository)
        .<Customer, CustomerProcessed>chunk(2, transactionManager)
        .reader(staxEventItemReader(customerMarshaller()))
        .processor(customerItemProcessor)
        .writer(customerItemWriter())
        .build();
  }

  @Bean
  public Step mappingSqlQueryStep() {
    return new StepBuilder("mappingSqlQueryStep", jobRepository)
        .<Customer, CustomerProcessed>chunk(2, transactionManager)
        .reader(mappingSqlQueryItemReader())
        .processor(customerItemProcessor)
        .writer(customerItemWriter())
        .build();
  }

  // ========== Jobs ==========

  @Bean
  public Job jdbcCursorJob() {
    return new JobBuilder("jdbcCursorJob", jobRepository)
        .start(jdbcCursorStep())
        .build();
  }

  @Bean
  public Job jdbcPagingJob() {
    return new JobBuilder("jdbcPagingJob", jobRepository)
        .start(jdbcPagingStep())
        .build();
  }

  @Bean
  public Job flatFileJob() {
    return new JobBuilder("flatFileJob", jobRepository)
        .start(flatFileStep())
        .build();
  }

  @Bean
  public Job jsonJob() {
    return new JobBuilder("jsonJob", jobRepository)
        .start(jsonStep())
        .build();
  }

  @Bean
  public Job listItemJob() {
    return new JobBuilder("listItemJob", jobRepository)
        .start(listItemStep())
        .build();
  }

  @Bean
  public Job repositoryItemJob() {
    return new JobBuilder("repositoryItemJob", jobRepository)
        .start(repositoryItemStep())
        .build();
  }

  @Bean
  public Job multiResourceJob() {
    return new JobBuilder("multiResourceJob", jobRepository)
        .start(multiResourceStep())
        .build();
  }

  @Bean
  public Job jpaPagingJob() {
    return new JobBuilder("jpaPagingJob", jobRepository)
        .start(jpaPagingStep())
        .build();
  }

  @Bean
  public Job jpaCursorJob() {
    return new JobBuilder("jpaCursorJob", jobRepository)
        .start(jpaCursorStep())
        .build();
  }

  @Bean
  public Job hintSettableJpaCursorJob() {
    return new JobBuilder("hintSettableJpaCursorJob", jobRepository)
        .start(hintSettableJpaCursorStep())
        .build();
  }

  @Bean
  public Job staxEventJob() {
    return new JobBuilder("staxEventJob", jobRepository)
        .start(staxEventStep())
        .build();
  }

  @Bean
  public Job mappingSqlQueryJob() {
    return new JobBuilder("mappingSqlQueryJob", jobRepository)
        .start(mappingSqlQueryStep())
        .build();
  }
}