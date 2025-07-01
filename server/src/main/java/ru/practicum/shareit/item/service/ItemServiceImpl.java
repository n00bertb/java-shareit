package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.status.BookingStatus;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.CommentDto;
import ru.practicum.shareit.item.comment.CommentRepository;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    @Transactional
    public ItemDto createItem(Long userId, ItemDto itemDto) {
        validateItemForCreation(itemDto, userId);

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Владелец не найден"));

        if (itemDto.getRequestId() != null) {
            ItemRequest request = itemRequestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> new RequestNotFoundException(
                            "Запрос с ID " + itemDto.getRequestId() + " не найден"));
        }

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);

        Item savedItem = itemRepository.save(item);
        return ItemMapper.toItemDto(savedItem);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long userId, Long itemId, ItemDto itemDto) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Владелец не найден"));

        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Предмет не найден"));

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

        if (itemDto.getRequestId() != null) {
            existingItem.setRequestId(itemDto.getRequestId());
        }

        Item updatedItem = itemRepository.save(existingItem);
        return ItemMapper.toItemDto(updatedItem);
    }

    @Override
    public ItemWithBookingDto getItemById(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Предмет не найден"));
        List<Comment> comments = commentRepository.findByItemId(itemId);
        List<CommentDto> commentDtos = comments.stream()
                .map(this::toCommentDto)
                .collect(Collectors.toList());

        return toItemWithBookingDto(item, null, null, commentDtos);
    }

    @Override
    public List<ItemWithBookingDto> getItemsByOwner(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Владелец не найден"));

        List<Item> items = itemRepository.findByOwnerId(userId);
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .collect(Collectors.toList());

        List<Booking> bookings = bookingRepository.findByItemIdInAndStatus(itemIds, BookingStatus.APPROVED);
        Map<Long, List<Booking>> bookingsByItemId = bookings.stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));

        List<Comment> comments = commentRepository.findByItemIdIn(itemIds);
        Map<Long, List<Comment>> commentsByItemId = comments.stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId()));

        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    List<Booking> itemBookings = bookingsByItemId.getOrDefault(item.getId(), new ArrayList<>());

                    BookingDto lastBooking = itemBookings.stream()
                            .filter(booking -> booking.getStart().isBefore(now))
                            .max((b1, b2) -> b1.getStart().compareTo(b2.getStart()))
                            .map(BookingMapper::toBookingDto)
                            .orElse(null);

                    BookingDto nextBooking = itemBookings.stream()
                            .filter(booking -> booking.getStart().isAfter(now))
                            .min((b1, b2) -> b1.getStart().compareTo(b2.getStart()))
                            .map(BookingMapper::toBookingDto)
                            .orElse(null);

                    List<CommentDto> itemComments = commentsByItemId.getOrDefault(item.getId(), new ArrayList<>())
                            .stream()
                            .map(this::toCommentDto)
                            .collect(Collectors.toList());

                    return toItemWithBookingDto(item, lastBooking, nextBooking, itemComments);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String searchText = text.toLowerCase();
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Предмет не найден"));

        List<Booking> userBookings = bookingRepository.findByBookerIdAndItemIdAndStatusAndEndBefore(
                userId, itemId, BookingStatus.APPROVED, LocalDateTime.now());

        if (userBookings.isEmpty()) {
            throw new RuntimeException("Вы можете комментировать только те предметы, которые бронировали");
        }

        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        return toCommentDto(savedComment);
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

    private ItemWithBookingDto toItemWithBookingDto(Item item, BookingDto lastBooking,
                                                    BookingDto nextBooking, List<CommentDto> comments) {
        ItemWithBookingDto dto = new ItemWithBookingDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setAvailable(item.getAvailable());
        dto.setLastBooking(lastBooking);
        dto.setNextBooking(nextBooking);
        dto.setComments(comments);
        return dto;
    }

    private CommentDto toCommentDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setText(comment.getText());
        dto.setAuthorName(comment.getAuthor().getName());
        dto.setCreated(comment.getCreated());
        return dto;
    }
}
