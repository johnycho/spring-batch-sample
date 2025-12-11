package com.example.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public class H2DataInitializer implements CommandLineRunner {

  private final DataSource dataSource;

  @Override
  public void run(String... args) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    Integer productCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM product",
        Integer.class
    );
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM customer",
        Integer.class
    );

    // 이미 customer / product 각각 10만 건 이상 있으면 패스
    boolean customerReady = (count != null && count >= 100_000);
    boolean productReady = (productCount != null && productCount >= 100_000);
    if (customerReady && productReady) {
      return;
    }

    // 샘플이라 싹 지우고 새로 채움
    jdbcTemplate.update("DELETE FROM customer");
    jdbcTemplate.update("DELETE FROM product");


    // H2 기준: SYSTEM_RANGE 로 1 ~ 100000 더미 생성
    jdbcTemplate.update(
        "INSERT INTO customer (id, first_name, last_name, email, age, created_at) " +
            "SELECT x, " +
            "       CONCAT('first', x), " +
            "       CONCAT('last', x), " +
            "       CONCAT('user', x, '@example.com'), " +
            "       MOD(x, 60) + 20, " +
            "       CURRENT_TIMESTAMP " +
            "FROM SYSTEM_RANGE(1, 100000)"
    );

    // product 더미 데이터 1 ~ 100000 생성
    jdbcTemplate.update(
        "INSERT INTO product (id, name, price, category, stock) " +
            "SELECT x, " +
            "       CONCAT('Product-', LPAD(x, 5, '0')), " +
            "       1000 + x, " +
            "       CONCAT('Category-', MOD(x, 10)), " +
            "       MOD(x * 7, 1000) " +
            "FROM SYSTEM_RANGE(1, 100000)"
    );
  }

}