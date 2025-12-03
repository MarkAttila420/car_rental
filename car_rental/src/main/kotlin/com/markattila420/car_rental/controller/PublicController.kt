package com.markattila420.car_rental.controller

import com.markattila420.car_rental.repository.BookingRepository
import com.markattila420.car_rental.repository.CarRepository
import com.markattila420.car_rental.service.BookingService
import com.markattila420.car_rental.service.CarService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.LocalDate

@Controller
class PublicController(
    private val carService: CarService,
    private val bookingService: BookingService,
    private val carRepository: CarRepository,
    private val bookingRepository: BookingRepository
) {
    
    @GetMapping("/")
    fun homePage(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        model: Model
    ): String {
        val cars = carService.findAvailableCars(startDate, endDate)
        
        model.addAttribute("cars", cars)
        model.addAttribute("startDate", startDate)
        model.addAttribute("endDate", endDate)
        
        return "index"
    }
    
    @GetMapping("/booking/{carId}")
    fun bookingPage(
        @PathVariable carId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        model: Model
    ): String {
        val car = carService.findAllActiveCars().find { it.id == carId }
            ?: throw IllegalArgumentException("Car not found")
        
        model.addAttribute("car", car)
        model.addAttribute("carId", carId)
        model.addAttribute("startDate", startDate)
        model.addAttribute("endDate", endDate)
        
        return "booking"
    }

    @PostMapping("/booking")
    fun submitBooking(
        @RequestParam carId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam name: String,
        @RequestParam email: String,
        @RequestParam address: String,
        @RequestParam phoneNumber: String,
        redirectAttributes: RedirectAttributes
    ): String {
        return try {
            bookingService.createBooking(
                carId = carId,
                customerName = name,
                customerEmail = email,
                customerAddress = address,
                customerPhone = phoneNumber,
                startDate = startDate,
                endDate = endDate
            )
            
            redirectAttributes.addFlashAttribute("successMessage", "Foglalás sikeresen létrehozva!")
            "redirect:/"
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hiba történt a foglalás során: ${e.message}")
            "redirect:/booking/$carId?startDate=$startDate&endDate=$endDate"
        }
    }
    
    @GetMapping("/api/cars/{carId}/availability")
    @ResponseBody
    fun checkCarAvailability(
        @PathVariable carId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): Map<String, Boolean> {
        val car = carRepository.findById(carId).orElse(null)
        
        if (car == null || !car.active) {
            return mapOf("available" to false)
        }
        
        val overlappingBookings = bookingRepository.findOverlappingBookings(carId, startDate, endDate)
        
        return mapOf("available" to overlappingBookings.isEmpty())
    }
    
    @GetMapping("/api/cars/{carId}/unavailable-dates")
    @ResponseBody
    fun getUnavailableDates(
        @PathVariable carId: Long
    ): Map<String, List<String>> {
        val car = carRepository.findById(carId).orElse(null)
        
        if (car == null || !car.active) {
            return mapOf("unavailableDates" to emptyList())
        }
        
        val today = LocalDate.now()
        val futureDate = today.plusYears(1)
        
        val bookings = bookingRepository.findOverlappingBookings(carId, today, futureDate)
        
        val unavailableDates = mutableSetOf<LocalDate>()
        bookings.forEach { booking ->
            var date = booking.startDate
            while (!date.isAfter(booking.endDate)) {
                unavailableDates.add(date)
                date = date.plusDays(1)
            }
        }
        
        return mapOf("unavailableDates" to unavailableDates.map { it.toString() }.sorted())
    }
}
