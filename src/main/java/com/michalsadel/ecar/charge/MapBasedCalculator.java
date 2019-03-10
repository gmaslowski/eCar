package com.michalsadel.ecar.charge;

import com.michalsadel.ecar.price.dto.*;
import org.slf4j.*;

import java.math.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.util.Objects.*;

class MapBasedCalculator implements ChargeCalculator {
    private static final Logger log = LoggerFactory.getLogger(MapBasedCalculator.class);

    private int minuteFromStart(PriceDto priceDto) {
        requireNonNull(priceDto);
        requireNonNull(priceDto.getEffectedIn());
        return priceDto.getEffectedIn().getStartsAt().get(ChronoField.MINUTE_OF_DAY);
    }

    private int minuteFromEnd(PriceDto priceDto) {
        requireNonNull(priceDto);
        requireNonNull(priceDto.getEffectedIn());
        return priceDto.getEffectedIn().getFinishesAt().get(ChronoField.MINUTE_OF_DAY);
    }

    private List<BigDecimal> createPriceMap(List<PriceDto> priceList) {
        priceList.sort(Comparator.comparing(this::minuteFromStart));
        final List<BigDecimal> mapping = LongStream.range(0, Duration.between(LocalTime.MIDNIGHT, LocalTime.MAX).toMinutes())
                .boxed()
                .map(aLong -> BigDecimal.ZERO)
                .collect(Collectors.toList());
        for (PriceDto price : priceList) {
            for (int i = minuteFromStart(price); i < minuteFromEnd(price); i++) {
                mapping.set(i, price.getPerMinute());
            }
        }
        return Collections.unmodifiableList(mapping);
    }

    @Override
    public BigDecimal calculate(LocalDateTime startsAt, LocalDateTime finishesAt, List<PriceDto> prices) {
        log.info("Calculated using {}", getClass().getSimpleName());
        final List<BigDecimal> priceMap = createPriceMap(prices);
        LongFunction<BigDecimal> mapper = minute -> priceMap.get((startsAt.get(ChronoField.MINUTE_OF_DAY) + (int) minute) % (int) ChronoField.MINUTE_OF_DAY.range().getMaximum());
        return LongStream.range(0, Duration.between(startsAt, finishesAt).toMinutes())
                .mapToObj(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
