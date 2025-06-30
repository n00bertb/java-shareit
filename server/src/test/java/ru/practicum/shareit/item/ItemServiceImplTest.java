package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.status.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
//import ru.practicum.shareit.item.comment.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.comment.Comment;
import ru.practicum.shareit.item.dto.ItemWithBookingDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.comment.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
public class ItemServiceImplTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ItemRequestRepository requestRepository;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setup() {
        owner = new User();
        owner.setName("Owner");
        owner.setEmail("o@ex.com");
        owner = userRepository.save(owner);

        booker = new User();
        booker.setName("Booker");
        booker.setEmail("b@ex.com");
        booker = userRepository.save(booker);

        item = new Item();
        item.setName("Book");
        item.setDescription("Drama");
        item.setAvailable(true);
        item.setOwner(owner);
        item = itemRepository.save(item);
    }

    @Test
    void createShouldAssignOwnerAndOptionalRequestId() {
        // создание вещи без id
        ItemDto dto = new ItemDto(null, "Book", "For IT", true,null);
        ItemDto created = itemService.createItem(owner.getId(),dto);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getRequestId()).isNull();

        // создание вещи с id
        ItemRequest req = new ItemRequest();
        req.setDescription("Need a book for Java");
        req.setRequester(booker);
        req.setCreated(LocalDateTime.now());
        req = requestRepository.save(req);

        ItemDto dto2 = new ItemDto(null, "Apple iPhone", "256 GB", true, req.getId());
        ItemDto created2 = itemService.createItem(owner.getId(),dto2);
        assertThat(created2.getRequestId()).isEqualTo(req.getId());
    }

    @Test
    void getAllByUserShouldIncludeBookingsAndComments() {
        LocalDateTime now = LocalDateTime.now();

        Booking past = new Booking();
        past.setItem(item);
        past.setBooker(booker);
        past.setStart(now.minusDays(2));
        past.setEnd(now.minusDays(1));
        past.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(past);

        Comment comment = new Comment();
        comment.setText("Great book");
        comment.setItem(item);
        comment.setAuthor(booker);
        comment.setCreated(now);
        commentRepository.save(comment);

        List<ItemWithBookingDto> result = itemService.getItemsByOwner(owner.getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastBooking()).isNotNull();
        assertThat(result.get(0).getComments()).extracting("text").containsExactly("Great book");
    }


    /*
    @Test
    void addCommentShouldSucceedOnlyAfterPastBooking() {
        LocalDateTime now = LocalDateTime.now();

        Booking b = new Booking();
        b.setItem(item);
        b.setBooker(booker);
        b.setStart(now.minusDays(2));
        b.setEnd(now.minusDays(1));
        b.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(b);

        CommentDto reqDto = new CommentDto(null, "Great book", "Anton", null);
        CommentDto comment = itemService.addComment(booker.getId(), item.getId(), reqDto);
        assertThat(comment.getText()).isEqualTo("Great book");
        assertThat(comment.getAuthorName()).isEqualTo(booker.getName());

        // без завершённого бронирования
        CommentDto badDto = new CommentDto(null, "Bad book", "Anton", null);
        assertThrows(ResponseStatusException.class,
                () -> itemService.addComment(owner.getId(), item.getId(), badDto)
        );
    }
     */

    @Test
    void getByIdShouldReturnComments() {
        Comment c = new Comment();
        c.setText("Great book");
        c.setItem(item);
        c.setAuthor(booker);
        c.setCreated(LocalDateTime.now());
        commentRepository.save(c);

        ItemWithBookingDto dto = itemService.getItemById(item.getId());
        assertThat(dto.getComments()).hasSize(1);
    }

    @Test
    void searchEmpty() {
        assertThat(itemService.searchItems(null)).isEmpty();
        assertThat(itemService.searchItems("   ")).isEmpty();
    }

    @Test
    void searchNoMatches() {
        assertThat(itemService.searchItems("cup")).isEmpty();
    }
}