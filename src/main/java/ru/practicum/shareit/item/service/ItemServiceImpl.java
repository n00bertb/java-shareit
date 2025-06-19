package ru.practicum.shareit.item.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.ItemNotFoundException;
import ru.practicum.shareit.exception.UserNotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private final Map<Long, Item> items = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Autowired
    private UserService userService;

    @Override
    public ItemDto createItem(Long userId, ItemDto itemDto) {
        validateItemForCreation(itemDto, userId);

        User owner;
        try {
            owner = UserMapper.toUser(userService.getUserById(userId));
        } catch (IllegalArgumentException e) {
            throw new UserNotFoundException("Владелец не найден");
        }

        Item item = ItemMapper.toItem(itemDto);
        item.setId(idGenerator.getAndIncrement());
        item.setOwner(owner);

        items.put(item.getId(), item);
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) {
        try {
            userService.getUserById(userId);
        } catch (IllegalArgumentException e) {
            throw new UserNotFoundException("Владелец не найден");
        }

        Item existingItem = items.get(itemId);
        if (existingItem == null) {
            throw new ItemNotFoundException("Предмет не найден");
        }

        if (!existingItem.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Только владелец может редактировать предмет");
        }

        if (itemDto.getName() != null) {
            if (itemDto.getName().trim().isEmpty()) {
                throw new ValidationException("Имя предмета не может быть пустым");
            }
            existingItem.setName(itemDto.getName().trim());
        }

        if (itemDto.getDescription() != null) {
            if (itemDto.getDescription().trim().isEmpty()) {
                throw new ValidationException("Описание предмета не может быть пустым");
            }
            existingItem.setDescription(itemDto.getDescription().trim());
        }

        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toItemDto(existingItem);
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        Item item = items.get(itemId);
        if (item == null) {
            throw new ItemNotFoundException("Предмет не найден");
        }

        return ItemMapper.toItemDto(item);
    }

    @Override
    public List<ItemDto> getItemsByOwner(Long userId) {
        try {
            userService.getUserById(userId);
        } catch (IllegalArgumentException e) {
            throw new UserNotFoundException("Владелец не найден");
        }

        return items.values().stream()
                .filter(item -> item.getOwner().getId().equals(userId))
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchText = text.toLowerCase();
        return items.values().stream()
                .filter(Item::getAvailable)
                .filter(item ->
                        item.getName().toLowerCase().contains(searchText) ||
                                item.getDescription().toLowerCase().contains(searchText))
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    private void validateItemForCreation(ItemDto itemDto, Long userId) {
        if (itemDto == null) {
            throw new ValidationException("Предмет не может быть null");
        }

        if (userId == null) {
            throw new UserNotFoundException("Владелец не может быть null");
        }

        if (itemDto.getName() == null || itemDto.getName().trim().isEmpty()) {
            throw new ValidationException("Имя предмета не может быть пустым или null");
        }

        if (itemDto.getDescription() == null || itemDto.getDescription().trim().isEmpty()) {
            throw new ValidationException("Описание предмета не может быть пустым или null");
        }

        if (itemDto.getAvailable() == null) {
            throw new ValidationException("Статус предмета не может быть null");
        }
    }
}
