package com.upgrade.challenge.controller;

import com.upgrade.challenge.api.ReservationApi;
import com.upgrade.challenge.impl.exception.ReservationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CampsiteController.class)
class CampsiteControllerTest {

    private static final Long RESERVATION_ID = 1L;
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String FAILED_ERROR_MESSAGE = "Failed to create reservation";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationApi reservationApi;

    private String validReservation;

    @BeforeEach
    void setUp() {
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);
        validReservation = String.format(
                """
                        {"email": "john.doe@examplecom",
                          "fullName": "John Doe",
                          "startDate": "%s",
                          "endDate": "%s"
                        }""", startDate, endDate);
    }

    @Test
    void checkCampsiteAvailability_whenAvailabilityExists_shouldReturnOk() throws Exception {
        when(reservationApi.getAvailabilityDays(any(), any())).thenReturn(List.of(LocalDate.now().plusDays(2).toString()));
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);
        mockMvc.perform(get(String.format("/api/v1/campsite/availability?startDate=%s&endDate=%s", startDate, endDate)).contentType("application/json")).andExpect(status().isOk());
    }

    @Test
    void checkCampsiteAvailability_whenAvailabilityNotExists_shouldReturnBadRequest() throws Exception {
        when(reservationApi.getAvailabilityDays(any(), any())).thenReturn(Collections.emptyList());
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = LocalDate.now().plusDays(5);
        MvcResult result = mockMvc.perform(get(String.format("/api/v1/campsite/availability?startDate=%s&endDate=%s", startDate, endDate)).contentType("application/json")).andReturn();
        Assertions.assertEquals(String.format("{\"daysAvailableList\":[],\"errorMessage\":\"No availability from %s to %s\"}", startDate, endDate), result.getResponse().getContentAsString());
        Assertions.assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    void checkCampsiteAvailability_whenEndDateIsBeforeStartDate_shouldReturnBadRequest() throws Exception {
        when(reservationApi.getAvailabilityDays(any(), any())).thenReturn(Collections.emptyList());
        LocalDate endDate = LocalDate.now().plusDays(2);
        LocalDate startDate = LocalDate.now().plusDays(5);
        MvcResult result = mockMvc.perform(get(String.format("/api/v1/campsite/availability?startDate=%s&endDate=%s", startDate, endDate)).contentType("application/json")).andReturn();
        Assertions.assertEquals("{\"daysAvailableList\":[],\"errorMessage\":\"End date cannot be before Start Date\"}", result.getResponse().getContentAsString());
        Assertions.assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    void createReservation_whenValidAndAvailable_shouldReturnOk() throws Exception {
        when(reservationApi.reserve(any())).thenReturn(Optional.of(RESERVATION_ID));

        MvcResult result = mockMvc.perform(post("/api/v1/campsite/reservation").contentType("application/json")
                .content(validReservation)).andReturn();
        Assertions.assertEquals(String.format("Reservation created with booking ID: %s", RESERVATION_ID), result.getResponse().getContentAsString());
        Assertions.assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    void createReservation_whenNoReservationIdReturned_shouldReturnBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/campsite/reservation").contentType("application/json")
                .content(validReservation)).andReturn();
        Assertions.assertEquals(FAILED_ERROR_MESSAGE, result.getResponse().getContentAsString());
        Assertions.assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    void createReservation_whenNoReservationSend_shouldReturnBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/campsite/reservation").contentType("application/json")
                .content("")).andReturn();
        Assertions.assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    void updateReservation_whenReservationValid_shouldReturnOk() throws Exception {
        when(reservationApi.updateReservation(any(), any())).thenReturn(Optional.of(RESERVATION_ID));

        MvcResult result = mockMvc.perform(put(String.format("/api/v1/campsite/reservation/%s", RESERVATION_ID)).contentType("application/json")
                .content(validReservation)).andReturn();
        Assertions.assertEquals("Reservation updated successfully!", result.getResponse().getContentAsString());
        Assertions.assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    void updateReservation_whenThrowReservationException_shouldReturnBadRequest() throws Exception {
        when(reservationApi.updateReservation(any(), any())).thenThrow(new ReservationException(ERROR_MESSAGE));

        MvcResult result = mockMvc.perform(put(String.format("/api/v1/campsite/reservation/%s", RESERVATION_ID)).contentType("application/json")
                .content(validReservation)).andReturn();
        Assertions.assertEquals(ERROR_MESSAGE, result.getResponse().getContentAsString());
        Assertions.assertEquals(400, result.getResponse().getStatus());
    }

    @Test
    void cancelReservation_whenCancellationIsSuccess_shouldReturnOk() throws Exception {
        MvcResult result = mockMvc.perform(delete(String.format("/api/v1/campsite/reservation/%s", RESERVATION_ID)).contentType("application/json")
        ).andReturn();
        Assertions.assertEquals("Reservation canceled successfully.", result.getResponse().getContentAsString());
        Assertions.assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    void cancelReservation_whenThrowReservationException_shouldReturnBadRequest() throws Exception {
        doThrow(new ReservationException(ERROR_MESSAGE)).when(reservationApi).cancelReservation(any());

        MvcResult result = mockMvc.perform(delete(String.format("/api/v1/campsite/reservation/%s", RESERVATION_ID)).contentType("application/json")
        ).andReturn();
        Assertions.assertEquals(ERROR_MESSAGE, result.getResponse().getContentAsString());
        Assertions.assertEquals(400, result.getResponse().getStatus());
    }
}