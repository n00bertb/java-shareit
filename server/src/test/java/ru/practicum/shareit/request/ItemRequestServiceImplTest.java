package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
public class ItemRequestServiceImplTest {

    @Autowired
    private ItemRequestService requestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRequestRepository requestRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User requester;
    private User other;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        requester = new User();
        requester.setName("User");
        requester.setEmail("user@example.com");
        requester = userRepository.save(requester);

        other = new User();
        other.setName("Other");
        other.setEmail("other@example.com");
        other = userRepository.save(other);
    }

    @Test
    void createShouldSaveAndReturnDtoWithEmptyItems() {
        ItemRequestDto createDto = new ItemRequestDto(null,"Need a book",null,null);
        ItemRequestDto dto = requestService.createItemRequest(requester.getId(), createDto);

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getDescription()).isEqualTo("Need a book");
        assertThat(dto.getItems()).isEmpty();
        assertThat(dto.getCreated()).isNotNull();
    }

    @Test
    void findByUserShouldReturnOwnRequestsWithItems() {
        // создаём запросы
        ItemRequest r1 = new ItemRequest();
        r1.setDescription("First");
        r1.setRequester(requester);
        r1.setCreated(LocalDateTime.now());
        r1 = requestRepository.save(r1);

        ItemRequest r2 = new ItemRequest();
        r2.setDescription("Second");
        r2.setRequester(requester);
        r2.setCreated(LocalDateTime.now().plusMinutes(1));
        r2 = requestRepository.save(r2);

        // создание вещи для первого запроса
        Item item = new Item();
        item.setName("Book");
        item.setDescription("Need a book for Java");
        item.setAvailable(true);
        item.setOwner(other);
        item.setRequestId(r1.getId());
        itemRepository.save(item);

        List<ItemRequestDto> list = requestService.getUserRequests(requester.getId());

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(r2.getId());
        assertThat(list.get(1).getId()).isEqualTo(r1.getId());
        assertThat(list.get(1).getItems())
                .extracting(r -> r.getId())
                .containsExactly(item.getId());
    }

    @Test
    void findByIdShouldReturnDtoWithItems() {
        ItemRequest r = new ItemRequest();
        r.setDescription("Need a book for Java");
        r.setRequester(requester);
        r.setCreated(LocalDateTime.now());
        r = requestRepository.save(r);

        Item item = new Item();
        item.setName("Book");
        item.setDescription("For IT");
        item.setAvailable(true);
        item.setOwner(other);
        item.setRequestId(r.getId());
        itemRepository.save(item);

        ItemRequestDto dto = requestService.getRequestById(r.getId(), requester.getId());
        assertThat(dto.getId()).isEqualTo(r.getId());
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getId()).isEqualTo(item.getId());
    }
}
