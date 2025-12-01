package com.markattila420.car_rental.controller

import com.markattila420.car_rental.entity.Booking
import com.markattila420.car_rental.entity.BookingStatus
import com.markattila420.car_rental.entity.Car
import com.markattila420.car_rental.repository.BookingRepository
import com.markattila420.car_rental.repository.CarRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class PublicControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var carRepository: CarRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    private lateinit var activeCar1: Car
    private lateinit var activeCar2: Car
    private lateinit var inactiveCar: Car

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()

        // Clean up repositories
        bookingRepository.deleteAll()
        carRepository.deleteAll()

        // Create test cars
        activeCar1 = carRepository.save(
            Car(
                brand = "Toyota",
                model = "Camry",
                dailyRate = BigDecimal("50.00"),
                imagePath = "/images/camry.jpg",
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        activeCar2 = carRepository.save(
            Car(
                brand = "Honda",
                model = "Accord",
                dailyRate = BigDecimal("60.00"),
                imagePath = "/images/accord.jpg",
                active = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        inactiveCar = carRepository.save(
            Car(
                brand = "Ford",
                model = "Mustang",
                dailyRate = BigDecimal("100.00"),
                imagePath = "/images/mustang.jpg",
                active = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
    }

    @AfterEach
    fun cleanup() {
        bookingRepository.deleteAll()
        carRepository.deleteAll()
    }

    @Test
    fun `should load home page with all active cars when no dates provided`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("index"))
            .andExpect(model().attributeExists("cars"))
            .andExpect(model().attribute("startDate", null as LocalDate?))
            .andExpect(model().attribute("endDate", null as LocalDate?))
    }

    @Test
    fun `should load home page with available cars for date range`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        // Create a booking for activeCar1
        bookingRepository.save(
            Booking(
                car = activeCar1,
                customerName = "Test User",
                customerEmail = "test@example.com",
                customerAddress = "123 Test St",
                customerPhone = "+1234567890",
                startDate = startDate,
                endDate = endDate,
                numberOfDays = 3,
                totalAmount = BigDecimal("150.00"),
                bookingStatus = BookingStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        )

        mockMvc.perform(
            get("/")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
        )
            .andExpect(status().isOk)
            .andExpect(view().name("index"))
            .andExpect(model().attributeExists("cars"))
            .andExpect(model().attribute("startDate", startDate))
            .andExpect(model().attribute("endDate", endDate))
    }

    @Test
    fun `should load booking page for active car`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        mockMvc.perform(
            get("/booking/{carId}", activeCar1.id)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
        )
            .andExpect(status().isOk)
            .andExpect(view().name("booking"))
            .andExpect(model().attributeExists("car"))
            .andExpect(model().attribute("carId", activeCar1.id))
            .andExpect(model().attribute("startDate", startDate))
            .andExpect(model().attribute("endDate", endDate))
    }

    @Test
    fun `should throw exception when loading booking page for non-existent car`() {
        try {
            mockMvc.perform(get("/booking/{carId}", 99999L))
            assert(false) { "Expected IllegalArgumentException to be thrown" }
        } catch (e: Exception) {
            assert(e.cause is IllegalArgumentException)
        }
    }

    @Test
    fun `should throw exception when loading booking page for inactive car`() {
        try {
            mockMvc.perform(get("/booking/{carId}", inactiveCar.id))
            assert(false) { "Expected IllegalArgumentException to be thrown" }
        } catch (e: Exception) {
            assert(e.cause is IllegalArgumentException)
        }
    }

    @Test
    fun `should submit booking successfully`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        mockMvc.perform(
            post("/booking")
                .param("carId", activeCar1.id.toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("name", "John Doe")
                .param("email", "john@example.com")
                .param("address", "123 Main St")
                .param("phoneNumber", "+1234567890")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attributeExists("successMessage"))

        // Verify booking was created
        val bookings = bookingRepository.findAll()
        assert(bookings.size == 1)
        assert(bookings[0].customerName == "John Doe")
        assert(bookings[0].car.id == activeCar1.id)
    }

    @Test
    fun `should fail to submit booking for overlapping dates`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        // Create existing booking
        bookingRepository.save(
            Booking(
                car = activeCar1,
                customerName = "Existing User",
                customerEmail = "existing@example.com",
                customerAddress = "123 Test St",
                customerPhone = "+1111111111",
                startDate = startDate,
                endDate = endDate,
                numberOfDays = 3,
                totalAmount = BigDecimal("150.00"),
                bookingStatus = BookingStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        )

        // Try to create overlapping booking
        mockMvc.perform(
            post("/booking")
                .param("carId", activeCar1.id.toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("name", "John Doe")
                .param("email", "john@example.com")
                .param("address", "123 Main St")
                .param("phoneNumber", "+1234567890")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrlPattern("/booking/${activeCar1.id}?startDate=*"))
            .andExpect(flash().attributeExists("errorMessage"))
    }

    @Test
    fun `should fail to submit booking for inactive car`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        mockMvc.perform(
            post("/booking")
                .param("carId", inactiveCar.id.toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("name", "John Doe")
                .param("email", "john@example.com")
                .param("address", "123 Main St")
                .param("phoneNumber", "+1234567890")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(flash().attributeExists("errorMessage"))
    }

    @Test
    fun `should check car availability - available car`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        mockMvc.perform(
            get("/api/cars/{carId}/availability", activeCar1.id)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.available").value(true))
    }

    @Test
    fun `should check car availability - unavailable car with booking`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        // Create booking
        bookingRepository.save(
            Booking(
                car = activeCar1,
                customerName = "Test User",
                customerEmail = "test@example.com",
                customerAddress = "123 Test St",
                customerPhone = "+1234567890",
                startDate = startDate,
                endDate = endDate,
                numberOfDays = 3,
                totalAmount = BigDecimal("150.00"),
                bookingStatus = BookingStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        )

        mockMvc.perform(
            get("/api/cars/{carId}/availability", activeCar1.id)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.available").value(false))
    }

    @Test
    fun `should check car availability - inactive car`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        mockMvc.perform(
            get("/api/cars/{carId}/availability", inactiveCar.id)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.available").value(false))
    }

    @Test
    fun `should check car availability - non-existent car`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        mockMvc.perform(
            get("/api/cars/{carId}/availability", 99999L)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.available").value(false))
    }

    @Test
    fun `should get unavailable dates for car with no bookings`() {
        mockMvc.perform(get("/api/cars/{carId}/unavailable-dates", activeCar1.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.unavailableDates").isArray)
            .andExpect(jsonPath("$.unavailableDates").isEmpty)
    }

    @Test
    fun `should get unavailable dates for car with bookings`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        // Create booking
        bookingRepository.save(
            Booking(
                car = activeCar1,
                customerName = "Test User",
                customerEmail = "test@example.com",
                customerAddress = "123 Test St",
                customerPhone = "+1234567890",
                startDate = startDate,
                endDate = endDate,
                numberOfDays = 3,
                totalAmount = BigDecimal("150.00"),
                bookingStatus = BookingStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        )

        mockMvc.perform(get("/api/cars/{carId}/unavailable-dates", activeCar1.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.unavailableDates").isArray)
            .andExpect(jsonPath("$.unavailableDates").isNotEmpty)
            .andExpect(jsonPath("$.unavailableDates[0]").value(startDate.toString()))
    }

    @Test
    fun `should get empty unavailable dates for inactive car`() {
        mockMvc.perform(get("/api/cars/{carId}/unavailable-dates", inactiveCar.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.unavailableDates").isArray)
            .andExpect(jsonPath("$.unavailableDates").isEmpty)
    }

    @Test
    fun `should get empty unavailable dates for non-existent car`() {
        mockMvc.perform(get("/api/cars/{carId}/unavailable-dates", 99999L))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.unavailableDates").isArray)
            .andExpect(jsonPath("$.unavailableDates").isEmpty)
    }

    @Test
    fun `should calculate total amount correctly when creating booking`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(5) // 5 days

        mockMvc.perform(
            post("/booking")
                .param("carId", activeCar1.id.toString())
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("name", "John Doe")
                .param("email", "john@example.com")
                .param("address", "123 Main St")
                .param("phoneNumber", "+1234567890")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        // Verify booking calculation
        val bookings = bookingRepository.findAll()
        assert(bookings.size == 1)
        assert(bookings[0].numberOfDays == 5)
        assert(bookings[0].totalAmount == BigDecimal("250.00")) // 50 * 5
    }
}
