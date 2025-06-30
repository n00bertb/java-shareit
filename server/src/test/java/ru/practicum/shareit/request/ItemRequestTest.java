package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.request.model.ItemRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

public class ItemRequestTest {
    @Test
    void gettersAndSetters() {
        ItemRequest r = new ItemRequest();
        r.setId(7L);
        r.setDescription("Need a book");
        r.setCreated(LocalDateTime.of(2025,6,1,12,0));

        assertThat(r.getId()).isEqualTo(7L);
        assertThat(r.getDescription()).isEqualTo("Need a book");
        assertThat(r.getCreated()).isEqualTo(LocalDateTime.of(2025,6,1,12,0));
    }
}
