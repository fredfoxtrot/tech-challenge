package com.upgrade.challenge.impl;

import com.upgrade.challenge.api.ReservationApi;
import com.upgrade.challenge.api.model.ReservationApiModel;
import com.upgrade.challenge.dao.ReservationRepository;
import com.upgrade.challenge.dao.model.Reservation;
import com.upgrade.challenge.impl.exception.ReservationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReservationApiImplTest {

    private static final Long RESERVATION_ID = 1L;
    private static final Long CANCELLED_RESERVATION_ID = 5L;
    @MockBean
    ReservationRepository repository;

    @Autowired
    ReservationApi subject;

    private Reservation reservation1;
    private Reservation reservation2;
    private Reservation reservation3;
    private Reservation reservation4;
    private Reservation cancelledReservation;

    private ReservationApiModel reservationApiModel;
    private ReservationApiModel unavailableReservationApiModel;

    @BeforeEach
    void setUp() {
        reservation1 = new Reservation(1L, "test1@email.com", "John Doe1", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), false);
        reservation2 = new Reservation(2L, "test2@email.com", "John Doe2", LocalDate.now().plusDays(5), LocalDate.now().plusDays(6), false);
        reservation3 = new Reservation(3L, "test3@email.com", "John Doe3", LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), false);
        reservation4 = new Reservation(4L, "test4@email.com", "John Doe4", LocalDate.now().plusDays(12), LocalDate.now().plusDays(13), false);
        cancelledReservation = new Reservation(5L, "test5@email.com", "John Doe5", LocalDate.now().plusDays(20), LocalDate.now().plusDays(23), true);

        reservationApiModel = new ReservationApiModel("test5@email.com", "John Doe5", LocalDate.now().plusDays(20), LocalDate.now().plusDays(23));
        unavailableReservationApiModel = new ReservationApiModel("test5@email.com", "John Doe5", LocalDate.now().plusDays(5), LocalDate.now().plusDays(6));

        List<Reservation> reservationList = Arrays.asList(reservation1, reservation2, reservation3, reservation4);

        when(repository.findActiveReservationBetweenDates(any(), any())).thenReturn(reservationList);
    }


    @Test
    void getAvailabilityDays_whenQueryReturnAvailabilities_shouldReturnListOfDates() {
        List<String> result = subject.getAvailabilityDays(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));
        assertEquals(result, Arrays.asList(LocalDate.now().plusDays(2).toString(), LocalDate.now().plusDays(3).toString()));
    }

    @Test
    void getAvailabilityDays_whenQueryDoesNotHaveAvailabilities_shouldReturnEmptyList() {
        List<String> result = subject.getAvailabilityDays(LocalDate.now().plusDays(10), LocalDate.now().plusDays(12));
        assertEquals(result, Collections.emptyList());
    }

    @Test
    void reserve_whenSuccessfullyReserve_shouldReturnReservationId() throws ReservationException {
        when(repository.save(any())).thenReturn(cancelledReservation);

        Optional<Long> reservationId = subject.reserve(reservationApiModel);
        assertTrue(reservationId.isPresent());
        assertEquals(cancelledReservation.getId(), reservationId.get());
        verify(repository, times(1)).save(any());
    }

    @Test
    void cancelReservation_whenIdNotFound_shouldThrowReservationException() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        ReservationException exception = Assertions.assertThrows(ReservationException.class, () -> subject.cancelReservation(RESERVATION_ID));
        assertEquals(String.format("Unable to find reservation with Id : %s", RESERVATION_ID), exception.getMessage());
        verify(repository, times(0)).save(any());
    }

    @Test
    void cancelReservation_whenIsCancelledAlready_shouldThrowReservationException() {
        when(repository.findById(anyLong()))
                .thenReturn(Optional.of(new Reservation(RESERVATION_ID, "test1@email.com", "John Doe1", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), true)));

        ReservationException exception = Assertions.assertThrows(ReservationException.class, () -> subject.cancelReservation(RESERVATION_ID));
        assertEquals(String.format("Reservation with ID : %s, has been cancelled previously!", RESERVATION_ID), exception.getMessage());
        verify(repository, times(0)).save(any());
    }

    @Test
    void cancelReservation_whenCancelledSuccessfully_shouldSaveChanges() throws ReservationException {
        when(repository.findById(anyLong()))
                .thenReturn(Optional.of(new Reservation(RESERVATION_ID, "test1@email.com", "John Doe1", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), false)));

        subject.cancelReservation(RESERVATION_ID);
        verify(repository, times(1)).save(any());
    }

    @Test
    void updateReservation_whenReservationExceptionThrown_shouldNotUpdateReservation() {
        Assertions.assertThrows(ReservationException.class, () -> subject.updateReservation(RESERVATION_ID, new ReservationApiModel(null, null, null, null)));
        verify(repository, times(0)).save(any());
    }

    @Test
    void updateReservation_whenReservationNotExists_shouldNotUpdateReservation() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        ReservationException exception = Assertions.assertThrows(ReservationException.class, () -> subject.updateReservation(RESERVATION_ID, reservationApiModel));
        assertEquals( String.format("Unable to find reservation with Id : %s", RESERVATION_ID), exception.getMessage());
        verify(repository, times(0)).save(any());
    }

    @Test
    void updateReservation_whenReservationIsCancelled_shouldNotUpdateReservation() {
        when(repository.findById(any())).thenReturn(Optional.ofNullable(cancelledReservation));

        ReservationException exception = Assertions.assertThrows(ReservationException.class, () -> subject.updateReservation(CANCELLED_RESERVATION_ID, reservationApiModel));
        assertEquals("Unable to update a cancelled reservation!", exception.getMessage());
        verify(repository, times(0)).save(any());
    }

    @Test
    void updateReservation_whenReservationDatesNotAvailable_shouldNotUpdateReservation() {
        when(repository.findById(any())).thenReturn(Optional.ofNullable(reservation1));

        ReservationException exception = Assertions.assertThrows(ReservationException.class, () -> subject.updateReservation(RESERVATION_ID, unavailableReservationApiModel));
        assertEquals("New reservation dates are not available anymore!", exception.getMessage());
        verify(repository, times(0)).save(any());
    }

    @Test
    void updateReservation_whenUpdateSuccessful_shouldCancelOldReservationAndSaveNewReservation() throws ReservationException {
        when(repository.findById(any())).thenReturn(Optional.ofNullable(reservation1));
        when(repository.save(any())).thenReturn(reservation1);

        Optional<Long> updatedReservation  = subject.updateReservation(RESERVATION_ID, reservationApiModel);
        assertTrue(updatedReservation.isPresent());
        assertEquals(RESERVATION_ID, updatedReservation.get());
        verify(repository, times(2)).save(any()); // 1 for cancelling, 1 to actually save the change
    }

    @Test
    void isAvailable_whenDatesAvailable_shouldReturnTrue() {
        // cancelled reservation
        assertTrue(subject.isAvailable(LocalDate.now().plusDays(20), LocalDate.now().plusDays(23)));

        // expected days to still available
        assertTrue(subject.isAvailable(LocalDate.now().plusDays(2), LocalDate.now().plusDays(5)));
        assertTrue(subject.isAvailable(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10)));
        assertTrue(subject.isAvailable(LocalDate.now().plusDays(12), LocalDate.now().plusDays(12)));
        assertTrue(subject.isAvailable(LocalDate.now().plusDays(13), LocalDate.now().plusDays(14)));
    }

    @Test
    void isAvailable_whenDatesNotAvailable_shouldReturnTrue() {
        // reserved dates
        assertFalse(subject.isAvailable(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)));
        assertFalse(subject.isAvailable(LocalDate.now().plusDays(5), LocalDate.now().plusDays(6)));
        assertFalse(subject.isAvailable(LocalDate.now().plusDays(10), LocalDate.now().plusDays(12)));
        assertFalse(subject.isAvailable(LocalDate.now().plusDays(12), LocalDate.now().plusDays(13)));
    }

    @Test
    void isReservationValid_whenReservationInvalid_shouldThrowException() {
        // email empty
        assertThrows(ReservationException.class, () -> subject.isReservationValid(new ReservationApiModel("", "John Test", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3))));

        // name empty
        assertThrows(ReservationException.class, () -> subject.isReservationValid(new ReservationApiModel("email@email.com", "", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3))));

        // start date not exist
        assertThrows(ReservationException.class, () -> subject.isReservationValid(new ReservationApiModel("email@email.com", "John Test", null, LocalDate.now().plusDays(3))));

        // end date not exist
        assertThrows(ReservationException.class, () -> subject.isReservationValid(new ReservationApiModel("email@email.com", "John Test", LocalDate.now().plusDays(1), null)));

        // end date before start date
        assertThrows(ReservationException.class, () -> subject.isReservationValid(new ReservationApiModel("email@email.com", "John Test", LocalDate.now().plusDays(3), LocalDate.now().plusDays(1))));

        // end date == start date
        assertThrows(ReservationException.class, () -> subject.isReservationValid(new ReservationApiModel("email@email.com", "John Test", LocalDate.now().plusDays(1), LocalDate.now().plusDays(1))));

        // start date not at least tomorrow
        assertThrows(ReservationException.class, () -> subject.isReservationValid(new ReservationApiModel("email@email.com", "John Test", LocalDate.now(), LocalDate.now().plusDays(3))));

        // reservation max 3 days
        assertThrows(ReservationException.class, () -> subject.isReservationValid(new ReservationApiModel("email@email.com", "John Test", LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))));
    }

    @Test
    void isReservationValid_whenReservationValid_shouldReturnTrue() throws ReservationException {
        assertTrue(subject.isReservationValid(new ReservationApiModel("email@email.com", "John Test", LocalDate.now().plusDays(1), LocalDate.now().plusDays(4))));
    }
}