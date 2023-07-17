package com.upgrade.challenge.api;

import com.upgrade.challenge.api.model.ReservationApiModel;
import com.upgrade.challenge.dao.model.Reservation;
import com.upgrade.challenge.impl.exception.ReservationException;
import lombok.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationApi {
    List<String> getAvailabilityDays(@NonNull final LocalDate startDate,@NonNull final LocalDate endDate);

    Optional<Long> reserve(@NonNull final ReservationApiModel reservation) throws ReservationException;

    void cancelReservation(@NonNull final Long id) throws ReservationException;

    Optional<Long> updateReservation(@NonNull final Long id, @NonNull final ReservationApiModel newReservation) throws ReservationException;

    Boolean isAvailable(@NonNull final LocalDate startDate, @NonNull final LocalDate endDate);

    Boolean isReservationValid(@NonNull final ReservationApiModel reservation) throws ReservationException;

    Integer numberOfReservationBetweenDates(@NonNull final LocalDate startDate,@NonNull final LocalDate endDate);
}
