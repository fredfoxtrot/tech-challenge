package com.upgrade.challenge;

import com.upgrade.challenge.api.ReservationApi;
import com.upgrade.challenge.api.model.ReservationApiModel;
import com.upgrade.challenge.impl.exception.ReservationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class CampsiteReservationConcurrencyTest {

    @Autowired
    private ReservationApi reservationService;

    @Test
    public void testConcurrentReservations() throws InterruptedException {
        int numThreads = 10; // number of concurrent threads
        int numReservations = 100; // number of reservations to attempt in total

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < numReservations; i++) {
            executorService.execute(() -> {
                try {
                    latch.await(); // wait for the signal to start concurrent requests
                    reservationService.reserve(new ReservationApiModel("JohnDoe@email.com", "John Doe", LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ReservationException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        latch.countDown(); // start all concurrent threads simultaneously
        executorService.shutdown();
        executorService.awaitTermination(10000, TimeUnit.MILLISECONDS);

        // verify the result of the concurrent reservations
        Integer expectedReservations = 1; // expected number of successful reservations
        Integer actualReservations = reservationService.numberOfReservationBetweenDates(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3)); // get the actual number of reservations made
        assertEquals(expectedReservations, actualReservations);
    }
}