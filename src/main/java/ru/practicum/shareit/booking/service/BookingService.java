package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.status.BookingStatus;

import java.util.List;

public interface BookingService {

    BookingDto createBooking(Long userId, BookingCreateDto bookingDto);

    BookingDto updateBookingStatus(Long userId, Long bookingId, Boolean approved);

    BookingDto getBookingById(Long userId, Long bookingId);

    List<BookingDto> getBookingsByUser(Long userId, BookingStatus status);

    List<BookingDto> getBookingsByOwner(Long userId, BookingStatus status);
}
