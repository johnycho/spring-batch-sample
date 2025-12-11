package com.example.batch.processor;

import com.example.batch.entity.Product;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class ProductItemProcessor implements ItemProcessor<Product, Product> {

    @Override
    public Product process(Product product) throws Exception {
        // 가격에 10% 할인 적용 (예제)
        if (product.getPrice() != null) {
            product.setPrice(product.getPrice().multiply(new java.math.BigDecimal("0.9")));
        }
        
        System.out.println("Processing product: " + product.getName() + " - Price: " + product.getPrice());
        return product;
    }
}

