package ru.practicum.shareit.item.comment;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

public class CommentTest {
    @Test
    void equalsHashCodeAndGetters() {
        Comment c1 = new Comment();
        c1.setId(5L);
        c1.setText("Nice");
        c1.setCreated(LocalDateTime.now());

        Comment c2 = new Comment();
        c2.setId(5L);
        c2.setText("Nice");
        c2.setCreated(c1.getCreated());

        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());

        c2.setId(6L);
        assertThat(c1).isNotEqualTo(c2);
    }
}
