package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.status.BookingStatus;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.ItemNotFoundException;
import ru.practicum.shareit.exception.UserNotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.comment.CommentDto;
import ru.practicum.shareit.item.comment.CommentRepository;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {"db.name=test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private User owner;
    private User booker;
    private ItemRequest itemRequest;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@example.com");
        owner = userRepository.save(owner);

        booker = new User();
        booker.setName("Booker");
        booker.setEmail("booker@example.com");
        booker = userRepository.save(booker);

        itemRequest = new ItemRequest();
        itemRequest.setDescription("Test request");
        itemRequest.setRequester(booker);
        itemRequest.setCreated(LocalDateTime.now());
        itemRequest = itemRequestRepository.save(itemRequest);
    }

    @Test
    void createItem_ShouldCreateItemSuccessfully() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        ItemDto result = itemService.createItem(owner.getId(), itemDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Item");
        assertThat(result.getDescription()).isEqualTo("Test Description");
        assertThat(result.getAvailable()).isTrue();

        Item savedItem = itemRepository.findById(result.getId()).orElse(null);
        assertThat(savedItem).isNotNull();
        assertThat(savedItem.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void createItem_WithRequestId_ShouldCreateItemWithRequest() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);
        itemDto.setRequestId(itemRequest.getId());

        ItemDto result = itemService.createItem(owner.getId(), itemDto);

        assertThat(result.getRequestId()).isEqualTo(itemRequest.getId());

        Item savedItem = itemRepository.findById(result.getId()).orElse(null);
        assertThat(savedItem).isNotNull();
        assertThat(savedItem.getRequestId()).isEqualTo(itemRequest.getId());
    }

    @Test
    void createItem_WithInvalidUser_ShouldThrowException() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        assertThatThrownBy(() -> itemService.createItem(999L, itemDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Собственник не найден");
    }

    @Test
    void createItem_WithInvalidData_ShouldThrowException() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        assertThatThrownBy(() -> itemService.createItem(owner.getId(), itemDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Название предмета не может быть пустым или null");
    }

    @Test
    void updateItem_ShouldUpdateItemSuccessfully() {
        Item item = createTestItem();

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Name");
        updateDto.setDescription("Updated Description");
        updateDto.setAvailable(false);

        ItemDto result = itemService.updateItem(owner.getId(), item.getId(), updateDto);

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        assertThat(result.getAvailable()).isFalse();

        Item updatedItem = itemRepository.findById(item.getId()).orElse(null);
        assertThat(updatedItem).isNotNull();
        assertThat(updatedItem.getName()).isEqualTo("Updated Name");
    }

    @Test
    void getItemById_ShouldReturnItemWithDetails() {
        Item item = createTestItem();
        Comment comment = createTestComment(item, booker);

        ItemWithBookingDto result = itemService.getItemById(item.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(item.getId());
        assertThat(result.getName()).isEqualTo(item.getName());
        assertThat(result.getComments()).hasSize(1);
        assertThat(result.getComments().get(0).getText()).isEqualTo(comment.getText());
        assertThat(result.getComments().get(0).getAuthorName()).isEqualTo(booker.getName());
    }

    @Test
    void getItemById_WithNonExistentId_ShouldThrowException() {
        assertThatThrownBy(() -> itemService.getItemById(999L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("Предмет не найден");
    }

    @Test
    void getItemsByOwner_ShouldReturnItemsWithBookings() {
        Item item = createTestItem();
        Booking pastBooking = createTestBooking(item, booker,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
        Booking futureBooking = createTestBooking(item, booker,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        List<ItemWithBookingDto> result = itemService.getItemsByOwner(owner.getId());

        assertThat(result).hasSize(1);
        ItemWithBookingDto itemDto = result.get(0);
        assertThat(itemDto.getId()).isEqualTo(item.getId());
        assertThat(itemDto.getLastBooking()).isNotNull();
        assertThat(itemDto.getLastBooking().getId()).isEqualTo(pastBooking.getId());
        assertThat(itemDto.getNextBooking()).isNotNull();
        assertThat(itemDto.getNextBooking().getId()).isEqualTo(futureBooking.getId());
    }

    @Test
    void searchItems_ShouldReturnMatchingItems() {
        createTestItemWithName("Bicycle");
        createTestItemWithName("Car");
        createTestItemWithDescription();

        List<ItemDto> result = itemService.searchItems("bicycle");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ItemDto::getName)
                .containsExactlyInAnyOrder("Bicycle", "Another Item");
    }

    @Test
    void searchItems_WithEmptyText_ShouldReturnEmptyList() {
        createTestItem();

        List<ItemDto> result = itemService.searchItems("");

        assertThat(result).isEmpty();
    }

    @Test
    void addComment_ShouldCreateCommentSuccessfully() {
        Item item = createTestItem();
        Booking completedBooking = createTestBooking(item, booker,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));

        CommentDto commentDto = new CommentDto();
        commentDto.setText("Great item!");

        CommentDto result = itemService.addComment(booker.getId(), item.getId(), commentDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getText()).isEqualTo("Great item!");
        assertThat(result.getAuthorName()).isEqualTo(booker.getName());
        assertThat(result.getCreated()).isNotNull();

        Comment savedComment = commentRepository.findById(result.getId()).orElse(null);
        assertThat(savedComment).isNotNull();
        assertThat(savedComment.getAuthor().getId()).isEqualTo(booker.getId());
        assertThat(savedComment.getItem().getId()).isEqualTo(item.getId());
    }

    @Test
    void addComment_WithoutBooking_ShouldThrowException() {
        Item item = createTestItem();
        CommentDto commentDto = new CommentDto();
        commentDto.setText("Great item!");

        assertThatThrownBy(() -> itemService.addComment(booker.getId(), item.getId(), commentDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Вы можете комментировать только те предметы, которые бронировали");
    }

    // Тесты для проверки доступа при обновлении предмета
    @Test
    void updateItem_WithNonOwnerUser_ShouldThrowAccessDeniedException() {
        Item item = createTestItem();

        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Name");

        assertThatThrownBy(() -> itemService.updateItem(booker.getId(), item.getId(), updateDto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Только собственник может редактировать предмет");
    }

    // Тесты для валидации при обновлении предмета
    @Test
    void updateItem_WithEmptyName_ShouldThrowValidationException() {
        Item item = createTestItem();

        ItemDto updateDto = new ItemDto();
        updateDto.setName("   "); // Пустое название с пробелами

        assertThatThrownBy(() -> itemService.updateItem(owner.getId(), item.getId(), updateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Название предмета не может быть пустым");
    }

    @Test
    void updateItem_WithEmptyDescription_ShouldThrowValidationException() {
        Item item = createTestItem();

        ItemDto updateDto = new ItemDto();
        updateDto.setDescription("   "); // Пустое описание с пробелами

        assertThatThrownBy(() -> itemService.updateItem(owner.getId(), item.getId(), updateDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Описание предмета не может быть пустым");
    }

    @Test
    void updateItem_WithRequestId_ShouldUpdateRequestId() {
        Item item = createTestItem();

        ItemDto updateDto = new ItemDto();
        updateDto.setRequestId(itemRequest.getId());

        ItemDto result = itemService.updateItem(owner.getId(), item.getId(), updateDto);

        assertThat(result.getRequestId()).isEqualTo(itemRequest.getId());

        Item updatedItem = itemRepository.findById(item.getId()).orElse(null);
        assertThat(updatedItem).isNotNull();
        assertThat(updatedItem.getRequestId()).isEqualTo(itemRequest.getId());
    }

    // Тесты для валидации при создании предмета
    @Test
    void createItem_WithNullItemDto_ShouldThrowValidationException() {
        assertThatThrownBy(() -> itemService.createItem(owner.getId(), null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Предмет не может быть null");
    }

    @Test
    void createItem_WithNullUserId_ShouldThrowUserNotFoundException() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        assertThatThrownBy(() -> itemService.createItem(null, itemDto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Собственник не может быть null");
    }

    @Test
    void createItem_WithNullName_ShouldThrowValidationException() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName(null);
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        assertThatThrownBy(() -> itemService.createItem(owner.getId(), itemDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Название предмета не может быть пустым или null");
    }

    @Test
    void createItem_WithNullDescription_ShouldThrowValidationException() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription(null);
        itemDto.setAvailable(true);

        assertThatThrownBy(() -> itemService.createItem(owner.getId(), itemDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Описание предмета не может быть пустым или null");
    }

    @Test
    void createItem_WithEmptyDescription_ShouldThrowValidationException() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("   "); // Пустое описание с пробелами
        itemDto.setAvailable(true);

        assertThatThrownBy(() -> itemService.createItem(owner.getId(), itemDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Описание предмета не может быть пустым или null");
    }

    @Test
    void createItem_WithNullAvailable_ShouldThrowValidationException() {
        ItemDto itemDto = new ItemDto();
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(null);

        assertThatThrownBy(() -> itemService.createItem(owner.getId(), itemDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Статус доступности предмета не может быть null");
    }

    private Item createTestItem() {
        Item item = new Item();
        item.setName("Test Item");
        item.setDescription("Test Description");
        item.setAvailable(true);
        item.setOwner(owner);
        return itemRepository.save(item);
    }

    private void createTestItemWithName(String name) {
        Item item = new Item();
        item.setName(name);
        item.setDescription("Test Description");
        item.setAvailable(true);
        item.setOwner(owner);
        itemRepository.save(item);
    }

    private void createTestItemWithDescription() {
        Item item = new Item();
        item.setName("Another Item");
        item.setDescription("A nice bicycle for rent");
        item.setAvailable(true);
        item.setOwner(owner);
        itemRepository.save(item);
    }

    private Comment createTestComment(Item item, User author) {
        Comment comment = new Comment();
        comment.setText("Test comment");
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    private Booking createTestBooking(Item item, User booker, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setStart(start);
        booking.setEnd(end);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.APPROVED);
        return bookingRepository.save(booking);
    }
}