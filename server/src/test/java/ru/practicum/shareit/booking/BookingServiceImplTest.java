package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.status.BookingStatus;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = "db.name=test")
@Transactional
class BookingServiceImplTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@test.com");
        owner = userRepository.save(owner);

        booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@test.com");
        booker = userRepository.save(booker);

        item = new Item();
        item.setName("Test Item");
        item.setDescription("Test Description");
        item.setAvailable(true);
        item.setOwner(owner);
        item = itemRepository.save(item);
    }

    @Test
    void createBooking_ValidData_ShouldCreateBooking() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        BookingCreateDto bookingDto = new BookingCreateDto(start, end, item.getId());

        BookingDto result = bookingService.createBooking(booker.getId(), bookingDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStart()).isEqualTo(start);
        assertThat(result.getEnd()).isEqualTo(end);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(result.getBooker().getId()).isEqualTo(booker.getId());
        assertThat(result.getItem().getId()).isEqualTo(item.getId());

        Booking savedBooking = bookingRepository.findById(result.getId()).orElse(null);
        assertThat(savedBooking).isNotNull();
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void createBooking_OwnerTriesToBookOwnItem_ShouldThrowException() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        BookingCreateDto bookingDto = new BookingCreateDto(start, end, item.getId());

        assertThrows(AccessDeniedException.class,
                () -> bookingService.createBooking(owner.getId(), bookingDto));
    }

    @Test
    void createBooking_ItemNotAvailable_ShouldThrowException() {
        item.setAvailable(false);
        itemRepository.save(item);

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        BookingCreateDto bookingDto = new BookingCreateDto(start, end, item.getId());

        assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(booker.getId(), bookingDto));
    }

    @Test
    void createBooking_InvalidDates_ShouldThrowException() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = LocalDateTime.now().plusHours(1); // end before start
        BookingCreateDto bookingDto = new BookingCreateDto(start, end, item.getId());

        assertThrows(ValidationException.class,
                () -> bookingService.createBooking(booker.getId(), bookingDto));
    }

    @Test
    void updateBookingStatus_ValidApproval_ShouldUpdateStatus() {
        Booking booking = createTestBooking();
        booking = bookingRepository.save(booking);

        BookingDto result = bookingService.updateBookingStatus(owner.getId(), booking.getId(), true);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);

        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElse(null);
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void updateBookingStatus_ValidRejection_ShouldUpdateStatus() {
        Booking booking = createTestBooking();
        booking = bookingRepository.save(booking);

        BookingDto result = bookingService.updateBookingStatus(owner.getId(), booking.getId(), false);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void updateBookingStatus_NotOwner_ShouldThrowException() {
        Booking booking = createTestBooking();
        booking = bookingRepository.save(booking);

        Booking finalBooking = booking;
        assertThrows(AccessDeniedException.class,
                () -> bookingService.updateBookingStatus(booker.getId(), finalBooking.getId(), true));
    }

    @Test
    void getBookingById_ValidBookerAccess_ShouldReturnBooking() {
        Booking booking = createTestBooking();
        booking = bookingRepository.save(booking);

        BookingDto result = bookingService.getBookingById(booker.getId(), booking.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(booking.getId());
    }

    @Test
    void getBookingById_ValidOwnerAccess_ShouldReturnBooking() {
        Booking booking = createTestBooking();
        booking = bookingRepository.save(booking);

        BookingDto result = bookingService.getBookingById(owner.getId(), booking.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(booking.getId());
    }

    @Test
    void getBookingById_UnauthorizedAccess_ShouldThrowException() {
        Booking booking = createTestBooking();
        booking = bookingRepository.save(booking);

        User anotherUser = new User();
        anotherUser.setName("Another");
        anotherUser.setEmail("another@test.com");
        anotherUser = userRepository.save(anotherUser);

        User finalAnotherUser = anotherUser;
        Booking finalBooking = booking;
        assertThrows(AccessDeniedException.class,
                () -> bookingService.getBookingById(finalAnotherUser.getId(), finalBooking.getId()));
    }

    @Test
    void getBookingsByUser_AllStatus_ShouldReturnAllBookings() {
        Booking booking1 = createTestBooking();
        Booking booking2 = createTestBooking();
        booking2.setStart(LocalDateTime.now().plusHours(3));
        booking2.setEnd(LocalDateTime.now().plusHours(4));

        bookingRepository.save(booking1);
        bookingRepository.save(booking2);

        List<BookingDto> result = bookingService.getBookingsByUser(booker.getId(), BookingStatus.ALL);

        assertThat(result).hasSize(2);
    }

    @Test
    void getBookingsByUser_WaitingStatus_ShouldReturnWaitingBookings() {
        Booking booking = createTestBooking();
        booking = bookingRepository.save(booking);

        List<BookingDto> result = bookingService.getBookingsByUser(booker.getId(), BookingStatus.WAITING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    void getBookingsByOwner_AllStatus_ShouldReturnAllBookings() {
        Booking booking = createTestBooking();
        bookingRepository.save(booking);

        List<BookingDto> result = bookingService.getBookingsByOwner(owner.getId(), BookingStatus.ALL);

        assertThat(result).hasSize(1);
    }

    private Booking createTestBooking() {
        Booking booking = new Booking();
        booking.setStart(LocalDateTime.now().plusHours(1));
        booking.setEnd(LocalDateTime.now().plusHours(2));
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);
        return booking;
    }
}
