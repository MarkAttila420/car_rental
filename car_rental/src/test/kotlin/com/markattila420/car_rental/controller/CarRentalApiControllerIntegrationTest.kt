package com.markattila420.car_rental.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.markattila420.car_rental.dto.request.CreateBookingRequest
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
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class CarRentalApiControllerIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var carRepository: CarRepository

    @Autowired
    private lateinit var bookingRepository: BookingRepository

    private val objectMapper: ObjectMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .findAndAddModules()
        .build()

    private lateinit var activeCar1: Car
    private lateinit var activeCar2: Car
    private lateinit var inactiveCar: Car

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()

        bookingRepository.deleteAll()
        carRepository.deleteAll()

        activeCar1 = carRepository.save(
            Car(
                brand = "Toyota",
                model = "Camry",
                dailyRate = BigDecimal("500"),
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
                dailyRate = BigDecimal("600"),
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
                dailyRate = BigDecimal("1000"),
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
    fun `should get all available cars when no dates provided`() {
        mockMvc.perform(get("/api/v1/cars/available"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Available cars retrieved successfully"))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].brand").exists())
            .andExpect(jsonPath("$.data[0].model").exists())
            .andExpect(jsonPath("$.data[0].dailyRate").exists())
    }

    @Test
    fun `should get available cars for specific date range`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

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
                totalAmount = BigDecimal("1500"),
                bookingStatus = BookingStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        )

        mockMvc.perform(
            get("/api/v1/cars/available")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].brand").value("Honda"))
    }

    @Test
    fun `should create booking successfully with valid request`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Booking created successfully"))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.customerName").value("John Doe"))
            .andExpect(jsonPath("$.data.customerEmail").value("john@example.com"))
            .andExpect(jsonPath("$.data.car.brand").value("Toyota"))
            .andExpect(jsonPath("$.data.car.model").value("Camry"))
            .andExpect(jsonPath("$.data.bookingStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.numberOfDays").value(3))

        val bookings = bookingRepository.findAll()
        assert(bookings.size == 1)
        assert(bookings[0].customerName == "John Doe")
    }

    @Test
    fun `should fail to create booking with missing required fields`() {
        val request = """
            {
                "carId": ${activeCar1.id},
                "customerName": "",
                "customerEmail": "invalid-email",
                "customerAddress": "123",
                "customerPhone": "123",
                "startDate": "${LocalDate.now().plusDays(1)}",
                "endDate": "${LocalDate.now().plusDays(3)}"
            }
        """

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should fail to create booking with invalid email format`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "John Doe",
            customerEmail = "invalid-email-format",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should fail to create booking with invalid phone number format`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "123",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should fail to create booking when end date is before start date`() {
        val startDate = LocalDate.now().plusDays(3)
        val endDate = LocalDate.now().plusDays(1)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should fail to create booking when start date is in the past`() {
        val startDate = LocalDate.now().minusDays(1)
        val endDate = LocalDate.now().plusDays(1)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should fail to create booking for non-existent car`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        val request = CreateBookingRequest(
            carId = 99999L,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should fail to create booking for inactive car`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        val request = CreateBookingRequest(
            carId = inactiveCar.id!!,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should fail to create booking for already booked car in date range`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

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
                totalAmount = BigDecimal("1500"),
                bookingStatus = BookingStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        )

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should calculate total amount correctly when creating booking`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(5)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.numberOfDays").value(5))
            .andExpect(jsonPath("$.data.totalAmount").value(2500))

        val bookings = bookingRepository.findAll()
        assert(bookings[0].numberOfDays == 5)
        assert(bookings[0].totalAmount == BigDecimal("2500"))
    }

    @Test
    fun `should create booking with minimum valid customer name`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "Jo",
            customerEmail = "jo@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.customerName").value("Jo"))
    }

    @Test
    fun `should fail to create booking with customer name too short`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(3)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "J",
            customerEmail = "j@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should create booking for same day start and end date`() {
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(1)

        val request = CreateBookingRequest(
            carId = activeCar1.id!!,
            customerName = "John Doe",
            customerEmail = "john@example.com",
            customerAddress = "123 Main Street",
            customerPhone = "+1234567890",
            startDate = startDate,
            endDate = endDate
        )

        mockMvc.perform(
            post("/api/v1/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.numberOfDays").value(1))
            .andExpect(jsonPath("$.data.totalAmount").value(500))
    }
}
