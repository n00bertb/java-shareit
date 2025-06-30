package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
public class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void createShouldSaveAndReturnUserDto() {
        UserDto dto = new UserDto(null, "Alex", "alex@example.com");
        UserDto created = userService.createUser(dto);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Alex");
        assertThat(created.getEmail()).isEqualTo("alex@example.com");

        Optional<User> fromDb = userRepository.findById(created.getId());
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getName()).isEqualTo("Alex");
    }

    @Test
    void getByIdShouldReturnUserDto() {
        User user = new User();
        user.setName("Alex");
        user.setEmail("alex@example.com");
        user = userRepository.save(user);

        UserDto dto = userService.getUserById(user.getId());
        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getName()).isEqualTo("Alex");
    }

    @Test
    void getAllShouldReturnAllUsers() {
        User user1 = new User();
        user1.setName("User 1");
        user1.setEmail("user1@example.com");
        User user2 = new User();
        user2.setName("User 2");
        user2.setEmail("user2@example.com");
        userRepository.saveAll(List.of(user1, user2));

        List<UserDto> all = userService.getAllUsers();
        assertThat(all).hasSize(2)
                .extracting(UserDto::getName)
                .containsExactlyInAnyOrder("User 1", "User 2");
    }
}
