package com.upgrade.challenge.dao;

import com.upgrade.challenge.dao.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query(value = "SELECT * FROM reservation WHERE end_date >= ?1 AND start_date <= ?2 AND is_cancelled = false", nativeQuery = true)
    List<Reservation> findActiveReservationBetweenDates(LocalDate startDate, LocalDate endDate);
}
