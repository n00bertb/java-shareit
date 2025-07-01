package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.model.Booking;

import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

public class BookingTest {
    @Test
    void equalsHashCodeAndGetters() {
        Booking b1 = new Booking();
        b1.setId(1L);
        b1.setStart(LocalDateTime.now());
        b1.setEnd(LocalDateTime.now().plusHours(1));

        Booking b2 = new Booking();
        b2.setId(1L);
        b2.setStart(b1.getStart());
        b2.setEnd(b1.getEnd());

        assertThat(b1).isEqualTo(b2);
        assertThat(b1.hashCode()).isEqualTo(b2.hashCode());

        b2.setId(2L);
        assertThat(b1).isNotEqualTo(b2);
    }
}
