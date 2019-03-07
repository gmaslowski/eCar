package com.michalsadel.ecar.domain;

import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jdbc.*;
import org.springframework.context.annotation.*;

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
class ChargeServiceConfiguration {
    @Bean
    PriceFactory priceFactory() {
        return new DefaultPriceFactory();
    }

    @Bean
    PriceRepository priceRepository() {
        return new FakePriceRepository();
    }

    @Bean
    CustomerRepository customerRepository() {
        return new FakeCustomerRepository();
    }

    @Bean
    PriceValidator priceValidator(PriceRepository priceRepository){
        return new PriceValidator(priceRepository);
    }

    @Bean
    ChargeService chargingStationCalculator(PriceRepository priceRepository, CustomerRepository customerRepository, PriceFactory priceFactory, PriceValidator priceValidator) {
        return new ChargeService(priceRepository, customerRepository, priceFactory, priceValidator);
    }

}
