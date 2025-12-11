package com.example.batch.reader;

import com.example.batch.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;

/**
 * Repository 기반 ItemReader 구현 예제
 * 실제로는 Spring Data Repository를 직접 사용하는 것보다
 * JdbcCursorItemReader나 JdbcPagingItemReader를 사용하는 것이 일반적입니다.
 * 여기서는 개념을 보여주기 위해 구현했습니다.
 */
@RequiredArgsConstructor
public class RepositoryItemReaderImpl<T> implements ItemReader<Product> {

    private final DataSource dataSource;
    private JdbcCursorItemReader<Product> delegate;

    @Override
    public Product read() throws Exception {
        if (delegate == null) {
            delegate = new JdbcCursorItemReaderBuilder<Product>()
                    .name("repositoryItemReader")
                    .dataSource(dataSource)
                    .sql("SELECT id, name, price, category, stock FROM product ORDER BY id")
                    .rowMapper(new BeanPropertyRowMapper<>(Product.class))
                    .build();
            delegate.afterPropertiesSet();
            delegate.open(new ExecutionContext());
        }
        return delegate.read();
    }
}

