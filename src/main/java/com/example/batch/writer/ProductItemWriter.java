package com.example.batch.writer;

import com.example.batch.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class ProductItemWriter implements ItemWriter<Product> {

    private final JdbcTemplate jdbcTemplate;

    public ProductItemWriter(javax.sql.DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void write(Chunk<? extends Product> chunk) throws Exception {
        String sql = "UPDATE product SET price = ? WHERE name = ?";
        
        for (Product item : chunk.getItems()) {
            jdbcTemplate.update(sql, item.getPrice(), item.getName());
            System.out.println("Updated product: " + item.getName() + " - New price: " + item.getPrice());
        }
    }
}

