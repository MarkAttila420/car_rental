package com.markattila420.car_rental.repository

import com.markattila420.car_rental.entity.Booking
import com.markattila420.car_rental.entity.BookingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface BookingRepository : JpaRepository<Booking, Long> {
    
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.car.id = :carId 
        AND b.bookingStatus != 'CANCELLED'
        AND (
            (b.startDate <= :endDate AND b.endDate >= :startDate)
        )
    """)
    fun findOverlappingBookings(
        @Param("carId") carId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Booking>
}
