package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.validator.EmailValidator;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        validateUserData(userDto, true);

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("Email уже существует");
        }

        User user = UserMapper.toUser(userDto);
        User savedUser = userRepository.save(user);
        return UserMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long userId, UserDto userDto) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (userDto.getEmail() != null && !userDto.getEmail().equals(existingUser.getEmail())) {
            EmailValidator.validateEmail(userDto.getEmail());
            String normalizedEmail = userDto.getEmail().trim();

            if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
                throw new IllegalArgumentException("Пользователь с таким email уже существует");
            }
            existingUser.setEmail(normalizedEmail);
        }

        if (userDto.getName() != null) {
            if (userDto.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Имя не может быть пустым");
            }
            existingUser.setName(userDto.getName().trim());
        }
        User updatedUser = userRepository.save(existingUser);
        return UserMapper.toUserDto(existingUser);
    }

    @Override
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        return UserMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
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
