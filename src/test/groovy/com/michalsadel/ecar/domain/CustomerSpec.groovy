package com.michalsadel.ecar.domain

import com.michalsadel.ecar.dto.CustomerDto
import com.michalsadel.ecar.dto.CustomerTypeDto
import spock.lang.Specification
import spock.lang.Unroll

abstract class CustomerSpec extends Specification implements ServiceSpec {
    abstract TestConfiguration config()
    private PriceEntryPoint priceService
    private CustomerEntryPoint customerService

    def setup() {
        def config = config()
        priceService = config.priceService()
        customerService = config.customerService()
    }

    final int nonExistingCustomer = 0

    @Unroll
    def "should be no charge between #started and #finished if there is no price definition available in system"(){
        expect:
            customerService.charge(started, finished, nonExistingCustomer) == 0
        where:
            started <<  [dateTime("2019-01-01T03:00"), dateTime("2019-01-01T03:00"), dateTime("2019-01-02T03:00")]
            finished << [dateTime("2019-01-01T03:30"), dateTime("2019-01-02T04:00"), dateTime("2019-01-01T03:00")]
    }

    @Unroll
    def "should be charged #charge EUR between #started and #finished if price effects all day long with 0.5EUR per minute"(){
        given: "system has one price defined of 0.5 EUR per minute all day long"
            priceService.add(createDefaultPrice(0.5))
        expect:
            customerService.charge(started, finished, nonExistingCustomer) == charge
        where:
            started <<  [dateTime("2019-01-01T03:00"), dateTime("2019-01-01T03:00"), dateTime("2019-01-02T03:00")]
            finished << [dateTime("2019-01-01T03:30"), dateTime("2019-01-02T04:00"), dateTime("2019-01-01T03:00")]
            charge << [15, 750, 0]
    }

    @Unroll
    def "should be charged #charge EUR between #started and #finished if price effects between 3:30AM and 4:00AM with 1EUR per minute"(){
        given: "system has one price defined of 1 EUR per minute between 3:30AM and 4:00AM"
            priceService.add(createPrice("03:30", "04:00"))
        expect:
            customerService.charge(started, finished, nonExistingCustomer) == charge
        where:
            started <<  [dateTime("2019-01-01T03:00"), dateTime("2019-01-01T03:00"), dateTime("2019-01-02T03:00")]
            finished << [dateTime("2019-01-01T03:30"), dateTime("2019-01-02T04:00"), dateTime("2019-01-01T03:00")]
            charge << [0, 60, 0]
    }

    @Unroll
    def "should be charged #charge EUR between #started and #finished if price effects between 3:00AM and 3:30AM with 2EUR per minute and between 3:30AM and 4:00AM with 1EUR per minute"(){
        given: "system have two prices 1 EUR per minute between 3:30AM and 4:00AM and 2 EUR per minute between 3:00AM and 3:30AM"
            priceService.add(createPrice("03:30", "04:00"))
            priceService.add(createPrice("03:00", "03:30", 2.0))
        expect:
            customerService.charge(started, finished, nonExistingCustomer) == charge
        where:
            started <<  [dateTime("2019-01-01T03:00"), dateTime("2019-01-01T03:00"), dateTime("2019-01-02T03:00")]
            finished << [dateTime("2019-01-01T03:30"), dateTime("2019-01-02T04:00"), dateTime("2019-01-01T03:00")]
            charge << [60, 180, 0]
    }

    @Unroll
    def "should be charged #charge EUR between #started and #finished if price effects between 3:00AM and 3:30AM with 2EUR per minute and between 3:30AM and 4:00AM with 1EUR per minute and there is default price of 0.5 EUR in any other time"(){
        given: "system have three prices 1 EUR per minute between 3:30AM and 4:00AM and 2 EUR per minute between 3:00AM and 3:30AM and default one 0.5 EUR in any other time"
            priceService.add(createPrice("03:30", "04:00"))
            priceService.add(createPrice("03:00", "03:30", 2.0))
            priceService.add(createDefaultPrice(0.5))
        expect:
            customerService.charge(started, finished, nonExistingCustomer) == charge
        where:
            started <<  [dateTime("2019-01-01T03:00"), dateTime("2019-01-01T03:00"), dateTime("2019-01-02T03:00"), dateTime("2019-01-01T02:50")]
            finished << [dateTime("2019-01-01T03:30"), dateTime("2019-01-02T04:00"), dateTime("2019-01-01T03:00"), dateTime("2019-01-02T04:10")]
            charge << [60, 870, 0, 880]
    }

    def "system should apply 10% discount for a VIP customer"(){
        given: "system have one default price of 0.5 EUR all day long"
            priceService.add(createDefaultPrice(0.5))
        and: "system have one standard customer"
            def standardCustomer = customerService.add(CustomerDto.builder().customerType(CustomerTypeDto.DEFAULT).build())
        and: "system have one VIP customer"
            def vipCustomer = customerService.add(CustomerDto.builder().customerType(CustomerTypeDto.VIP).build())
        and: "charging started at midnight"
            def startedAt = dateTime("2019-01-01T00:00")
        and: "charging finished at midnight the next day"
            def finishedAt = dateTime("2019-01-02T00:00")
        and: "system calculates normal price for a normal customer"
            def normalCharge = customerService.charge(startedAt, finishedAt, standardCustomer.getId())
        when: "system calculates normal price for a VIP customer"
            def discountedCharge = customerService.charge(startedAt, finishedAt, vipCustomer.getId())
        then: "charge for a VIP customer should be 10% lower than charge of a normal customer"
            discountedCharge == normalCharge - (0.1 * normalCharge)
    }

    def "should be no charge when all prices have been removed"(){
        given: "have to prices defined in the system"
            priceService.add(createDefaultPrice())
            priceService.add(createPrice("00:00", "01:00"))
        when: "delete all prices"
            priceService.removeAll()
        then: "no charge should be calculated"
            customerService.charge(dateTime("2019-01-01T00:00"), dateTime("2019-01-01T23:00"), nonExistingCustomer) == 0
    }
}
