package com.markattila420.car_rental.controller

import com.markattila420.car_rental.service.CarService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Controller
class PublicController(
    private val carService: CarService
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
}
