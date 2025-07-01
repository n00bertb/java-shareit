package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.status.BookingStatus;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.UserNotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Test
    void createBooking_ValidRequest_ShouldReturnCreatedBooking() throws Exception {
        Long userId = 1L;
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        BookingCreateDto createDto = new BookingCreateDto(start, end, 1L);

        BookingDto responseDto = createBookingDto(1L, start, end, BookingStatus.WAITING);

        when(bookingService.createBooking(eq(userId), any(BookingCreateDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("WAITING")))
                .andExpect(jsonPath("$.item.id", is(1)))
                .andExpect(jsonPath("$.booker.id", is(1)));
    }

    @Test
    void getBooking_ValidRequest_ShouldReturnBooking() throws Exception {
        Long userId = 1L;
        Long bookingId = 1L;
        BookingDto bookingDto = createBookingDto(bookingId,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                BookingStatus.WAITING);

        when(bookingService.getBookingById(userId, bookingId)).thenReturn(bookingDto);

        mockMvc.perform(get("/bookings/{bookingId}", bookingId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("WAITING")));
    }

    @Test
    void getBookings_ValidRequest_ShouldReturnBookingsList() throws Exception {
        Long userId = 1L;
        List<BookingDto> bookings = Arrays.asList(
                createBookingDto(1L, LocalDateTime.now().plusHours(1),
                        LocalDateTime.now().plusHours(2), BookingStatus.WAITING),
                createBookingDto(2L, LocalDateTime.now().plusHours(3),
                        LocalDateTime.now().plusHours(4), BookingStatus.APPROVED)
        );

        when(bookingService.getBookingsByUser(userId, BookingStatus.ALL)).thenReturn(bookings);

        mockMvc.perform(get("/bookings")
                        .header(USER_ID_HEADER, userId)
                        .param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    void getBookingsByOwner_ValidRequest_ShouldReturnBookingsList() throws Exception {
        Long userId = 1L;
        List<BookingDto> bookings = Arrays.asList(
                createBookingDto(1L, LocalDateTime.now().plusHours(1),
                        LocalDateTime.now().plusHours(2), BookingStatus.WAITING)
        );

        when(bookingService.getBookingsByOwner(userId, BookingStatus.ALL)).thenReturn(bookings);

        mockMvc.perform(get("/bookings/owner")
                        .header(USER_ID_HEADER, userId)
                        .param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));
    }

    @Test
    void updateBookingStatus_ValidApproval_ShouldReturnUpdatedBooking() throws Exception {
        Long userId = 1L;
        Long bookingId = 1L;
        BookingDto updatedBooking = createBookingDto(bookingId,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                BookingStatus.APPROVED);

        when(bookingService.updateBookingStatus(userId, bookingId, true))
                .thenReturn(updatedBooking);

        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .header(USER_ID_HEADER, userId)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    void updateBookingStatus_AccessDenied_ShouldReturnForbidden() throws Exception {
        Long userId = 1L;
        Long bookingId = 1L;

        when(bookingService.updateBookingStatus(userId, bookingId, true))
                .thenThrow(new AccessDeniedException("Доступ запрещен"));

        mockMvc.perform(patch("/bookings/{bookingId}", bookingId)
                        .header(USER_ID_HEADER, userId)
                        .param("approved", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBooking_UserNotFound_ShouldReturnNotFound() throws Exception {
        Long userId = 999L;
        BookingCreateDto createDto = new BookingCreateDto(
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                1L);

        when(bookingService.createBooking(eq(userId), any(BookingCreateDto.class)))
                .thenThrow(new UserNotFoundException("Пользователь не найден"));

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isNotFound());
    }

    private BookingDto createBookingDto(Long id, LocalDateTime start, LocalDateTime end, BookingStatus status) {
        UserDto booker = new UserDto();
        booker.setId(1L);
        booker.setName("Booker");
        booker.setEmail("booker@test.com");

        ItemDto item = new ItemDto();
        item.setId(1L);
        item.setName("Test Item");
        item.setDescription("Test Description");
        item.setAvailable(true);

        return new BookingDto(id, start, end, item, booker, status);
    }
}
