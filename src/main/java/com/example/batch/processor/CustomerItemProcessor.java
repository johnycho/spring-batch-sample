package com.example.batch.processor;

import com.example.batch.entity.Customer;
import com.example.batch.entity.CustomerProcessed;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class CustomerItemProcessor implements ItemProcessor<Customer, CustomerProcessed> {

    @Override
    public CustomerProcessed process(Customer customer) throws Exception {
        CustomerProcessed processed = new CustomerProcessed();
        processed.setCustomerId(customer.getId());
        processed.setFullName(customer.getFirstName() + " " + customer.getLastName());
        processed.setEmail(customer.getEmail());
        processed.setAge(customer.getAge());
        processed.setProcessedAt(java.time.LocalDateTime.now());
        
        System.out.println("Processing customer: " + processed.getFullName());
        return processed;
    }
}

