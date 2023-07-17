package com.upgrade.challenge.api.model;

import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class ReservationApiModel {

    private String email;

    private String fullName;

    private LocalDate startDate;

    private LocalDate endDate;
}
