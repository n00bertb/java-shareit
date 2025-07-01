package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.comment.CommentDto;
import ru.practicum.shareit.item.dto.ItemWithBookingDto;

import java.util.List;

public interface ItemService {

    ItemDto createItem(Long userId, ItemDto itemDto);

    ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto);

    List<ItemDto> searchItems(String text);

    ItemWithBookingDto getItemById(Long itemId);

    List<ItemWithBookingDto> getItemsByOwner(Long userId);

    CommentDto addComment(Long userId, Long itemId, CommentDto commentDto);
}
