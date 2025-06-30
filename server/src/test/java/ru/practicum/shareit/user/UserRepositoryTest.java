package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(null, "John Doe", "john@example.com");
    }

    @Test
    void testSaveAndFindById() {
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(saved);
    }

    @Test
    void testExistsByEmail() {
        userRepository.save(user);
        boolean exists = userRepository.existsByEmail("john@example.com");
        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByEmailAndIdNot() {
        User anotherUser = new User(null, "Jane Doe", "jane@example.com");
        userRepository.save(user);
        userRepository.save(anotherUser);

        boolean exists = userRepository.existsByEmailAndIdNot("john@example.com", anotherUser.getId());
        assertThat(exists).isTrue();
    }
}
