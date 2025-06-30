package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.item.model.Item;

import static org.assertj.core.api.Assertions.*;

public class ItemTest {
    @Test
    void gettersSettersAndEqualsHashCode() {
        Item i1 = new Item();
        i1.setId(10L);
        i1.setName("Book");
        i1.setDescription("For IT");
        i1.setAvailable(true);

        Item i2 = new Item();
        i2.setId(10L);
        i2.setName("Book");
        i2.setDescription("For IT");
        i2.setAvailable(true);

        assertThat(i1.toString()).contains("Book");

        assertThat(i1).isEqualTo(i2);
        assertThat(i1.hashCode()).isEqualTo(i2.hashCode());

        i2.setId(11L);
        assertThat(i1).isNotEqualTo(i2);
    }
}