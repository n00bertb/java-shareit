package ru.practicum.shareit.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.status.BookingStatus;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    private User owner;
    private User booker;
    private Item item;
    private Booking pastBooking;
    private Booking currentBooking;
    private Booking futureBooking;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@test.com");
        owner = entityManager.persistAndFlush(owner);

        booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@test.com");
        booker = entityManager.persistAndFlush(booker);

        item = new Item();
        item.setName("Test Item");
        item.setDescription("Test Description");
        item.setAvailable(true);
        item.setOwner(owner);
        item = entityManager.persistAndFlush(item);

        LocalDateTime now = LocalDateTime.now();

        pastBooking = new Booking();
        pastBooking.setStart(now.minusHours(3));
        pastBooking.setEnd(now.minusHours(1));
        pastBooking.setItem(item);
        pastBooking.setBooker(booker);
        pastBooking.setStatus(BookingStatus.APPROVED);
        pastBooking = entityManager.persistAndFlush(pastBooking);

        currentBooking = new Booking();
        currentBooking.setStart(now.minusHours(1));
        currentBooking.setEnd(now.plusHours(1));
        currentBooking.setItem(item);
        currentBooking.setBooker(booker);
        currentBooking.setStatus(BookingStatus.APPROVED);
        currentBooking = entityManager.persistAndFlush(currentBooking);

        futureBooking = new Booking();
        futureBooking.setStart(now.plusHours(1));
        futureBooking.setEnd(now.plusHours(3));
        futureBooking.setItem(item);
        futureBooking.setBooker(booker);
        futureBooking.setStatus(BookingStatus.WAITING);
        futureBooking = entityManager.persistAndFlush(futureBooking);
    }

    @Test
    void findByBookerId_ShouldReturnAllBookingsForBooker() {
        List<Booking> result = bookingRepository.findByBookerId(booker.getId(),
                Sort.by(Sort.Direction.DESC, "start"));

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(futureBooking, currentBooking, pastBooking);
    }

    @Test
    void findByBookerIdAndEndIsBefore_ShouldReturnPastBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> result = bookingRepository.findByBookerIdAndEndIsBefore(booker.getId(), now,
                Sort.by(Sort.Direction.DESC, "start"));

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(pastBooking);
    }

    @Test
    void findByBookerIdAndStartIsAfter_ShouldReturnFutureBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> result = bookingRepository.findByBookerIdAndStartIsAfter(booker.getId(), now,
                Sort.by(Sort.Direction.DESC, "start"));

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(futureBooking);
    }

    @Test
    void findByBookerIdAndCurrent_ShouldReturnCurrentBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> result = bookingRepository.findByBookerIdAndCurrent(booker.getId(), now,
                Sort.by(Sort.Direction.DESC, "start"));

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(currentBooking);
    }

    @Test
    void findByBookerIdAndStatus_ShouldReturnBookingsWithSpecificStatus() {
        List<Booking> waitingBookings = bookingRepository.findByBookerIdAndStatus(booker.getId(),
                BookingStatus.WAITING, Sort.by(Sort.Direction.DESC, "start"));
        List<Booking> approvedBookings = bookingRepository.findByBookerIdAndStatus(booker.getId(),
                BookingStatus.APPROVED, Sort.by(Sort.Direction.DESC, "start"));

        assertThat(waitingBookings).hasSize(1);
        assertThat(waitingBookings).containsExactly(futureBooking);

        assertThat(approvedBookings).hasSize(2);
        assertThat(approvedBookings).containsExactly(currentBooking, pastBooking);
    }

    @Test
    void findByItemOwnerId_ShouldReturnAllBookingsForOwner() {
        List<Booking> result = bookingRepository.findByItemOwnerId(owner.getId(),
                Sort.by(Sort.Direction.DESC, "start"));

        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(futureBooking, currentBooking, pastBooking);
    }

    @Test
    void findByItemOwnerIdAndEndIsBefore_ShouldReturnPastBookingsForOwner() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> result = bookingRepository.findByItemOwnerIdAndEndIsBefore(owner.getId(), now,
                Sort.by(Sort.Direction.DESC, "start"));

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(pastBooking);
    }

    @Test
    void findByItemOwnerIdAndStartIsAfter_ShouldReturnFutureBookingsForOwner() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> result = bookingRepository.findByItemOwnerIdAndStartIsAfter(owner.getId(), now,
                Sort.by(Sort.Direction.DESC, "start"));

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(futureBooking);
    }

    @Test
    void findByItemOwnerIdAndCurrent_ShouldReturnCurrentBookingsForOwner() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> result = bookingRepository.findByItemOwnerIdAndCurrent(owner.getId(), now,
                Sort.by(Sort.Direction.DESC, "start"));

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(currentBooking);
    }

    @Test
    void findByItemOwnerIdAndStatus_ShouldReturnBookingsWithSpecificStatusForOwner() {
        List<Booking> waitingBookings = bookingRepository.findByItemOwnerIdAndStatus(owner.getId(),
                BookingStatus.WAITING, Sort.by(Sort.Direction.DESC, "start"));

        assertThat(waitingBookings).hasSize(1);
        assertThat(waitingBookings).containsExactly(futureBooking);
    }

    @Test
    void findByItemIdInAndStatus_ShouldReturnBookingsForItemsWithStatus() {
        List<Booking> result = bookingRepository.findByItemIdInAndStatus(
                List.of(item.getId()), BookingStatus.APPROVED);

        assertThat(result).hasSize(2);
        assertThat(result).contains(pastBooking, currentBooking);
    }

    @Test
    void findByBookerIdAndItemIdAndStatusAndEndBefore_ShouldReturnSpecificBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> result = bookingRepository.findByBookerIdAndItemIdAndStatusAndEndBefore(
                booker.getId(), item.getId(), BookingStatus.APPROVED, now);

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(pastBooking);
    }
}
