package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(UserServiceImpl.class)
@ActiveProfiles("test")
class UserServiceImplTest {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        testUser1 = new User(null, "Иван Иванов", "ivan@example.com");
        testUser2 = new User(null, "Петр Петров", "petr@example.com");
    }

    @Test
    void createUser_ShouldSaveUserToDatabase() {
        UserDto userDto = new UserDto(null, "Новый Пользователь", "new@example.com");

        UserDto createdUser = userService.createUser(userDto);

        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getName()).isEqualTo("Новый Пользователь");
        assertThat(createdUser.getEmail()).isEqualTo("new@example.com");

        User savedUser = entityManager.find(User.class, createdUser.getId());
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Новый Пользователь");
        assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void createUser_WithExistingEmail_ShouldThrowException() {
        entityManager.persistAndFlush(testUser1);
        UserDto userDto = new UserDto(null, "Другой Пользователь", "ivan@example.com");

        assertThatThrownBy(() -> userService.createUser(userDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email уже существует");

        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(1);
    }

    @Test
    @Transactional
    void updateUser_WithWhitespaceInEmail_ShouldNormalizeEmail() {
        User savedUser = entityManager.persistAndFlush(testUser1);
        String emailWithWhitespace = "  new.email@example.com  ";
        String expectedNormalizedEmail = emailWithWhitespace.trim();
        UserDto updateDto = new UserDto(null, null, emailWithWhitespace);

        UserDto updatedUser = userService.updateUser(savedUser.getId(), updateDto);
        entityManager.flush();

        assertThat(updatedUser.getEmail()).isEqualTo(expectedNormalizedEmail);
        assertThat(updatedUser.getName()).isEqualTo(testUser1.getName());

        entityManager.clear();
        User userFromDb = entityManager.find(User.class, savedUser.getId());

        if (userFromDb != null && userFromDb.getEmail() != null) {
            System.out.println("DB email length: " + userFromDb.getEmail().length());
            System.out.println("Emails equal: " + expectedNormalizedEmail.equals(userFromDb.getEmail()));

            // Проверяем каждый символ
            String dbEmail = userFromDb.getEmail();
            if (!expectedNormalizedEmail.equals(dbEmail)) {
                System.out.println("Character comparison:");
                int minLength = Math.min(expectedNormalizedEmail.length(), dbEmail.length());
                for (int i = 0; i < minLength; i++) {
                    char expected = expectedNormalizedEmail.charAt(i);
                    char actual = dbEmail.charAt(i);
                    if (expected != actual) {
                        System.out.printf("Diff at position %d: expected '%c' (%d), actual '%c' (%d)%n",
                                i, expected, (int)expected, actual, (int)actual);
                    }
                }
            }
        }

        assertThat(userFromDb).isNotNull();
        assertThat(userFromDb.getEmail()).isEqualTo(expectedNormalizedEmail);
        assertThat(userFromDb.getName()).isEqualTo(testUser1.getName());
    }

    @Test
    @Transactional
    void updateUser_PartialUpdate_ShouldUpdateOnlyProvidedFields() {
        User savedUser = entityManager.persistAndFlush(testUser1);
        String newName = "Только Новое Имя";
        UserDto updateDto = new UserDto(null, newName, null);

        UserDto updatedUser = userService.updateUser(savedUser.getId(), updateDto);
        entityManager.flush();

        assertThat(updatedUser.getName()).isEqualTo(newName.trim());
        assertThat(updatedUser.getEmail()).isEqualTo(testUser1.getEmail());

        entityManager.clear();
        User userFromDb = entityManager.find(User.class, savedUser.getId());

        assertThat(userFromDb.getName()).isEqualTo(newName.trim());
        assertThat(userFromDb.getEmail()).isEqualTo(testUser1.getEmail());
    }

    @Test
    void updateUser_WithExistingEmail_ShouldThrowException() {
        User user1 = entityManager.persistAndFlush(testUser1);
        User user2 = entityManager.persistAndFlush(testUser2);

        UserDto updateDto = new UserDto(null, "Обновленное Имя", testUser1.getEmail());

        assertThatThrownBy(() -> userService.updateUser(user2.getId(), updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь с таким email уже существует");

        entityManager.clear();
        User unchangedUser = entityManager.find(User.class, user2.getId());
        assertThat(unchangedUser.getName()).isEqualTo(testUser2.getName());
        assertThat(unchangedUser.getEmail()).isEqualTo(testUser2.getEmail());
    }

    @Test
    void updateUser_NonExistentUser_ShouldThrowException() {
        Long nonExistentId = 999L;
        UserDto updateDto = new UserDto(null, "Новое Имя", "new@example.com");

        assertThatThrownBy(() -> userService.updateUser(nonExistentId, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь не найден");
    }

    @Test
    void getUserById_ShouldReturnUserFromDatabase() {
        User savedUser = entityManager.persistAndFlush(testUser1);

        UserDto foundUser = userService.getUserById(savedUser.getId());

        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.getName()).isEqualTo(testUser1.getName());
        assertThat(foundUser.getEmail()).isEqualTo(testUser1.getEmail());
    }

    @Test
    void getUserById_NonExistentUser_ShouldThrowException() {
        Long nonExistentId = 999L;

        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь не найден");
    }

    @Test
    void getAllUsers_ShouldReturnAllUsersFromDatabase() {
        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);

        List<UserDto> allUsers = userService.getAllUsers();

        assertThat(allUsers).hasSize(2);

        assertThat(allUsers).extracting("name")
                .containsExactlyInAnyOrder("Иван Иванов", "Петр Петров");

        assertThat(allUsers).extracting("email")
                .containsExactlyInAnyOrder("ivan@example.com", "petr@example.com");
    }

    @Test
    void getAllUsers_EmptyDatabase_ShouldReturnEmptyList() {
        List<UserDto> allUsers = userService.getAllUsers();

        assertThat(allUsers).isEmpty();
    }

    @Test
    @Transactional
    void deleteUser_ShouldRemoveUserFromDatabase() {
        User savedUser = entityManager.persistAndFlush(testUser1);
        Long userId = savedUser.getId();

        assertThat(entityManager.find(User.class, userId)).isNotNull();

        userService.deleteUser(userId);
        entityManager.flush();
        entityManager.clear();

        User deletedUser = entityManager.find(User.class, userId);

        System.out.println("User ID: " + userId);
        System.out.println("Deleted user: " + deletedUser);
        System.out.println("Repository exists check: " + userRepository.existsById(userId));

        assertThat(deletedUser).isNull();

        assertThat(userRepository.existsById(userId)).isFalse();
    }

    @Test
    void deleteUser_NonExistentUser_ShouldThrowException() {
        Long nonExistentId = 999L;

        assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь не найден");
    }

    @Test
    @Transactional
    void deleteUser_WithMultipleUsers_ShouldDeleteOnlySpecifiedUser() {
        User user1 = entityManager.persistAndFlush(testUser1);
        User user2 = entityManager.persistAndFlush(testUser2);

        userService.deleteUser(user1.getId());

        entityManager.flush();
        entityManager.clear();

        User deletedUser = entityManager.find(User.class, user1.getId());
        assertThat(deletedUser).isNull();

        User remainingUser = entityManager.find(User.class, user2.getId());
        assertThat(remainingUser).isNotNull();
        assertThat(remainingUser.getName()).isEqualTo(testUser2.getName());
        assertThat(remainingUser.getEmail()).isEqualTo(testUser2.getEmail());
    }
}
