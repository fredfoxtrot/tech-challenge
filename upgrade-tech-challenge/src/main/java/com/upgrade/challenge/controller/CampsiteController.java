package com.upgrade.challenge.controller;

import com.upgrade.challenge.api.ReservationApi;
import com.upgrade.challenge.api.model.AvailabilityResponse;
import com.upgrade.challenge.api.model.ReservationApiModel;
import com.upgrade.challenge.impl.exception.ReservationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/campsite")
public class CampsiteController {

    @Autowired
    private ReservationApi reservationApi;

    private Logger logger = LoggerFactory.getLogger(CampsiteController.class);


    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> checkCampsiteAvailability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().plusDays(1); // minimum 1 day ahead of arrival
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(1); // default availability range: 1 month
        }

        // error if endDate is before startDate
        if (endDate.isBefore(startDate)) {
            return new ResponseEntity<>(new AvailabilityResponse(Collections.emptyList(), Optional.of("End date cannot be before Start Date")), HttpStatus.BAD_REQUEST);
        }

        // max 2 months spread of availability - to limit user upper bound
        if (startDate.plusMonths(2).isBefore(endDate)) {
            endDate = startDate.plusMonths(2);
        }

        // ensure startDate is at least tomorrow
        if (startDate.isBefore(LocalDate.now().plusDays(1))) {
            startDate = LocalDate.now().plusDays(1);
        }

        System.out.println(startDate + " " + endDate);
        List<String> daysAvailableList = reservationApi.getAvailabilityDays(startDate, endDate);

        if (daysAvailableList.isEmpty()) {
            return new ResponseEntity<>(new AvailabilityResponse(Collections.emptyList(), Optional.of(String.format("No availability from %s to %s", startDate, endDate))), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(new AvailabilityResponse(daysAvailableList, Optional.empty()), HttpStatus.OK);
    }

    @PostMapping("/reservation")
    public ResponseEntity<?> createReservation(@RequestBody ReservationApiModel reservation) {
        try {
            Optional<Long> reservationId = reservationApi.reserve(reservation);
            return reservationId.map(aLong -> new ResponseEntity<>(String.format("Reservation created with booking ID: %s", aLong), HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>("Failed to create reservation", HttpStatus.BAD_REQUEST));
        } catch (ReservationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/reservation/{reservationId}")
    public ResponseEntity<?> updateReservation(
            @PathVariable Long reservationId,
            @RequestBody ReservationApiModel updatedReservation) {

        try {
            reservationApi.updateReservation(reservationId, updatedReservation);
        } catch (ReservationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok("Reservation updated successfully!");
    }

    @DeleteMapping("/reservation/{bookingId}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long bookingId) {

        try {
            reservationApi.cancelReservation(bookingId);
        } catch (ReservationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok("Reservation canceled successfully.");
    }
}