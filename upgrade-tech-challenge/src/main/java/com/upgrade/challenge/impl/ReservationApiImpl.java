package com.upgrade.challenge.impl;

import com.upgrade.challenge.api.ReservationApi;
import com.upgrade.challenge.api.model.ReservationApiModel;
import com.upgrade.challenge.dao.ReservationRepository;
import com.upgrade.challenge.dao.model.Reservation;
import com.upgrade.challenge.impl.exception.ReservationException;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ReservationApiImpl implements ReservationApi {

    @Autowired
    ReservationRepository repository;

    private final Lock reservationLock = new ReentrantLock();

    @Override
    @Nonnull
    public List<String> getAvailabilityDays(@NonNull final LocalDate startDate, @NonNull final LocalDate endDate) {
        List<Reservation> reservationList = repository.findActiveReservationBetweenDates(startDate, endDate);

        Set<String> reservationMap = createReservationMap(reservationList);
        List<String> availableDateList = new ArrayList<>(Collections.emptyList());

        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            // generate list of days, from start - end, if the date is exists on reservationList, then exclude that
            if (!reservationMap.contains(date.toString())) {
                availableDateList.add(date.toString());
            }
        }

        return availableDateList;
    }

    @Override
    public Optional<Long> reserve(@NonNull final ReservationApiModel reservation) throws ReservationException {
        try {
            reservationLock.lock(); // get lock for sync access

            // check if the intended days are valid
            if (!isReservationValid(reservation)) {
                return Optional.empty();
                // check if dates available
            } else if (!isAvailable(reservation.getStartDate(), reservation.getEndDate())) {
                return Optional.empty();
            } else {
                Reservation newReservation = repository.save(adaptReservationApiModelToReservationDaoModel(reservation));
                return Optional.of(newReservation.getId());
            }
        } finally {
            reservationLock.unlock(); // release the lock
        }
    }

    @Override
    public void cancelReservation(@NonNull final Long id) throws ReservationException {
        Reservation existingReservation = repository.findById(id).orElseThrow(() ->
                new ReservationException(String.format("Unable to find reservation with Id : %s", id)));

        // Cannot cancel previously cancelled reservation
        if (existingReservation.isCancelled()) {
            throw new ReservationException(String.format("Reservation with ID : %s, has been cancelled previously!", id));
        }

        existingReservation.setCancelled(true);
        repository.save(existingReservation);
    }

    @Override
    public Optional<Long> updateReservation(@NonNull final Long id, @NonNull final ReservationApiModel newReservation) throws ReservationException {

        // ensure the new reservation is valid
        if (!isReservationValid(newReservation)) {
            return Optional.empty();
        }

        Reservation existingReservation = repository.findById(id).orElseThrow(() ->
                new ReservationException(String.format("Unable to find reservation with Id : %s", id)));

        // ensure the existing reservation is not cancelled yet
        if (existingReservation.isCancelled()) {
            throw new ReservationException("Unable to update a cancelled reservation!");
        }

        // make sure the new reservation dates are available
        if (!isAvailable(newReservation.getStartDate(), newReservation.getEndDate())) {
            throw new ReservationException("New reservation dates are not available anymore!");
        }

        // cancel the existing reservation
        cancelReservation(existingReservation.getId());

        // save the new reservation, return the id
        return Optional.of(repository.save(adaptReservationApiModelToReservationDaoModel(newReservation, existingReservation.getId())).getId());
    }

    @Override
    public Boolean isAvailable(@NonNull final LocalDate startDate, @NonNull final LocalDate endDate) {
        List<Reservation> reservationList = repository.findActiveReservationBetweenDates(startDate, endDate);
        Set<String> reservationMap = createReservationMap(reservationList);
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            if (reservationMap.contains(date.toString())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Boolean isReservationValid(@NonNull final ReservationApiModel reservation) throws ReservationException {
        // ensure email is not empty
        if (null == reservation.getEmail() || reservation.getEmail().isEmpty()) {
            throw new ReservationException("Reservation email cannot be empty!");
        }

        // ensure name is not empty
        if (null == reservation.getFullName() || reservation.getFullName().isEmpty()) {
            throw new ReservationException("Reservation fullName cannot be empty!");
        }

        // ensure start date and end date exists
        if (null == reservation.getStartDate() || null == reservation.getEndDate()) {
            throw new ReservationException("Start Date and/or End Date cannot be empty!");
        }

        final LocalDate startDate = reservation.getStartDate();
        final LocalDate endDate = reservation.getEndDate();

        // check startDate < endDate
        if (endDate.isBefore(startDate)) {
            throw new ReservationException("End Date is before Start Date!");
        }

        // check startDate == endDate
        if (endDate.isEqual(startDate)) {
            throw new ReservationException("End Date cannot be the same as Start Date!");
        }

        // check startDate has to be at least tomorrow
        if (startDate.isBefore(LocalDate.now().plusDays(1))) {
            throw new ReservationException("The campsite can be reserved minimum 1 day(s) from now!");
        }

        // check reservation max 3 days
        if (startDate.plusDays(3).isBefore(endDate)) {
            throw new ReservationException("Reservation exceeded 3 consecutive days!");
        }
        return true;
    }

    @Override
    public Integer numberOfReservationBetweenDates(@NonNull final LocalDate startDate, @NonNull final LocalDate endDate) {
        return repository.findActiveReservationBetweenDates(startDate, endDate).size();
    }

    @Nonnull
    private Set<String> createReservationMap(@NonNull final List<Reservation> reservationList) {
        Set<String> result = new HashSet<>();
        for (Reservation reservation : reservationList) {
            for (LocalDate date = reservation.getStartDate(); date.isBefore(reservation.getEndDate()); date = date.plusDays(1)) {
                result.add(date.toString());
            }
        }
        return result;
    }

    @Nonnull
    private Reservation adaptReservationApiModelToReservationDaoModel(@Nonnull final ReservationApiModel reservationApiModel) {
        Reservation reservationDaoModel = new Reservation();
        reservationDaoModel.setEmail(reservationApiModel.getEmail());
        reservationDaoModel.setFullName(reservationApiModel.getFullName());
        reservationDaoModel.setStartDate(reservationApiModel.getStartDate());
        reservationDaoModel.setEndDate(reservationApiModel.getEndDate());
        return reservationDaoModel;
    }

    @Nonnull
    private Reservation adaptReservationApiModelToReservationDaoModel(@Nonnull final ReservationApiModel reservationApiModel, @Nonnull final Long id) {
        Reservation reservationDaoModel = new Reservation();
        reservationDaoModel.setId(id);
        reservationDaoModel.setEmail(reservationApiModel.getEmail());
        reservationDaoModel.setFullName(reservationApiModel.getFullName());
        reservationDaoModel.setStartDate(reservationApiModel.getStartDate());
        reservationDaoModel.setEndDate(reservationApiModel.getEndDate());
        return reservationDaoModel;
    }
}
