package com.example.batch.writer;

import com.example.batch.entity.CustomerProcessed;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class CustomerItemWriter implements ItemWriter<CustomerProcessed> {

    private final JdbcTemplate jdbcTemplate;

    public CustomerItemWriter(javax.sql.DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void write(Chunk<? extends CustomerProcessed> chunk) throws Exception {
        String sql = "INSERT INTO customer_processed (customer_id, full_name, email, age, processed_at) VALUES (?, ?, ?, ?, ?)";
        
        for (CustomerProcessed item : chunk.getItems()) {
            jdbcTemplate.update(sql,
                    item.getCustomerId(),
                    item.getFullName(),
                    item.getEmail(),
                    item.getAge(),
                    item.getProcessedAt());
            System.out.println("Written: " + item.getFullName());
        }
    }
}

