package ru.practicum.shareit.user.service;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.validator.EmailValidator;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final Map<Long, User> users = new HashMap<>();
    private final Set<String> emails = new HashSet<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserDto createUser(UserDto userDto) {
        validateUserData(userDto, true);

        if (emails.contains(userDto.getEmail())) {
            throw new IllegalArgumentException("Email уже существует");
        }

        User user = UserMapper.toUser(userDto);
        user.setId(idGenerator.getAndDecrement());
        user.setEmail(user.getEmail().trim());

        users.put(user.getId(), user);
        emails.add(user.getEmail());

        return UserMapper.toUserDto(user);
    }

    @Override
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = users.get(userId);
        if (existingUser == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        if (userDto.getEmail() != null) {
            EmailValidator.validateEmail(userDto.getEmail());
            String normalizedEmail = userDto.getEmail().trim();

            if (!normalizedEmail.equals(existingUser.getEmail())) {
                if (emails.contains(normalizedEmail)) {
                    throw new IllegalArgumentException("Пользователь с таким email уже существует");
                }
                emails.remove(existingUser.getEmail());
                emails.add(normalizedEmail);
                existingUser.setEmail(normalizedEmail);
            }
        }

        if (userDto.getName() != null) {
            if (userDto.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Имя не может быть пустым");
            }
            existingUser.setName(userDto.getName().trim());
        }

        return UserMapper.toUserDto(existingUser);
    }

    @Override
    public UserDto getUserById(Long userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }

        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return users.values().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long userId) {
        User user = users.remove(userId);
        if (user != null) {
            emails.remove(user.getEmail());
        }
    }

    private void validateUserData(UserDto userDto, boolean isCreation) {
        if (userDto == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }

        if (isCreation || userDto.getName() != null) {
            if (userDto.getName() == null || userDto.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Имя не может быть пустым или null");
            }
        }

        if (isCreation || userDto.getEmail() != null) {
            if (userDto.getEmail() == null || userDto.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email не может быть пустым или null");
            }
            EmailValidator.validateEmail(userDto.getEmail());
        }
    }
}
