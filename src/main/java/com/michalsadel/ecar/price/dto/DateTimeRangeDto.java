package com.michalsadel.ecar.price.dto;

import lombok.*;

import javax.validation.constraints.*;
import java.time.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DateTimeRangeDto {
    @NotNull
    private LocalDateTime start;
    @NotNull
    private LocalDateTime finish;
}
