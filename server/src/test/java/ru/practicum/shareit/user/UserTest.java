package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.user.model.User;

import static org.assertj.core.api.Assertions.*;

public class UserTest {
    @Test
    void gettersAndSetters() {
        User u = new User();
        u.setId(3L);
        u.setName("Alex");
        u.setEmail("alex@example.com");

        assertThat(u.getId()).isEqualTo(3L);
        assertThat(u.getName()).isEqualTo("Alex");
        assertThat(u.getEmail()).isEqualTo("alex@example.com");
    }
}