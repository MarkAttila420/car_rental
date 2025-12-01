package com.markattila420.car_rental.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.markattila420.car_rental.entity.Booking
import com.markattila420.car_rental.entity.BookingStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class BookingResponse(
    val id: Long,
    val car: CarDto,
    val customerName: String,
    val customerEmail: String,
    val customerAddress: String,
    val customerPhone: String,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate,
    val numberOfDays: Int,
    val totalAmount: BigDecimal,
    val bookingStatus: BookingStatus,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime
) {
    companion object {
        fun fromEntity(booking: Booking): BookingResponse {
            return BookingResponse(
                id = booking.id!!,
                car = CarDto.fromEntity(booking.car),
                customerName = booking.customerName,
                customerEmail = booking.customerEmail,
                customerAddress = booking.customerAddress,
                customerPhone = booking.customerPhone,
                startDate = booking.startDate,
                endDate = booking.endDate,
                numberOfDays = booking.numberOfDays,
                totalAmount = booking.totalAmount,
                bookingStatus = booking.bookingStatus,
                createdAt = booking.createdAt
            )
        }
    }
}
