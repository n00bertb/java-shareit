package ru.practicum.shareit.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.RequestNotFoundException;
import ru.practicum.shareit.exception.UserNotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class ItemRequestServiceImplTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User requester;
    private User otherUser;
    private ItemRequest itemRequest1;
    private ItemRequest itemRequest2;
    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setName("Requester");
        requester.setEmail("requester@test.com");
        requester = userRepository.save(requester);

        otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setEmail("other@test.com");
        otherUser = userRepository.save(otherUser);

        itemRequest1 = new ItemRequest();
        itemRequest1.setDescription("Need a drill");
        itemRequest1.setCreated(LocalDateTime.now().minusDays(2));
        itemRequest1.setRequester(requester);
        itemRequest1 = itemRequestRepository.save(itemRequest1);

        itemRequest2 = new ItemRequest();
        itemRequest2.setDescription("Need a ladder");
        itemRequest2.setCreated(LocalDateTime.now().minusDays(1));
        itemRequest2.setRequester(otherUser);
        itemRequest2 = itemRequestRepository.save(itemRequest2);

        item1 = new Item();
        item1.setName("Electric Drill");
        item1.setDescription("Powerful drill");
        item1.setAvailable(true);
        item1.setOwner(otherUser);
        item1.setRequestId(itemRequest1.getId());
        item1 = itemRepository.save(item1);

        item2 = new Item();
        item2.setName("Step Ladder");
        item2.setDescription("3-step ladder");
        item2.setAvailable(true);
        item2.setOwner(requester);
        item2.setRequestId(itemRequest2.getId());
        item2 = itemRepository.save(item2);
    }

    @Test
    void createItemRequest_ShouldCreateAndReturnItemRequest() {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setDescription("Need a hammer");

        ItemRequestDto result = itemRequestService.createItemRequest(requester.getId(), requestDto);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Need a hammer", result.getDescription());
        assertNotNull(result.getCreated());
        assertTrue(result.getItems().isEmpty());

        // Проверяем, что запрос действительно сохранился в базе данных
        ItemRequest savedRequest = itemRequestRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedRequest);
        assertEquals("Need a hammer", savedRequest.getDescription());
        assertEquals(requester.getId(), savedRequest.getRequester().getId());
    }

    @Test
    void createItemRequest_WithNonExistentUser_ShouldThrowUserNotFoundException() {
        ItemRequestDto requestDto = new ItemRequestDto();
        requestDto.setDescription("Need something");

        assertThrows(UserNotFoundException.class,
                () -> itemRequestService.createItemRequest(999L, requestDto));
    }

    @Test
    void getUserRequests_ShouldReturnUserRequestsWithItems() {
        List<ItemRequestDto> result = itemRequestService.getUserRequests(requester.getId());

        assertNotNull(result);
        assertEquals(1, result.size());

        ItemRequestDto requestDto = result.get(0);
        assertEquals(itemRequest1.getId(), requestDto.getId());
        assertEquals("Need a drill", requestDto.getDescription());
        assertEquals(1, requestDto.getItems().size());
        assertEquals("Electric Drill", requestDto.getItems().get(0).getName());
    }

    @Test
    void getUserRequests_WithNonExistentUser_ShouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class,
                () -> itemRequestService.getUserRequests(999L));
    }

    @Test
    void getUserRequests_WithNoRequests_ShouldReturnEmptyList() {
        User userWithoutRequests = new User();
        userWithoutRequests.setName("No Requests User");
        userWithoutRequests.setEmail("noreq@test.com");
        userWithoutRequests = userRepository.save(userWithoutRequests);

        List<ItemRequestDto> result = itemRequestService.getUserRequests(userWithoutRequests.getId());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllRequests_ShouldReturnOtherUsersRequestsWithItems() {
        List<ItemRequestDto> result = itemRequestService.getAllRequests(requester.getId(), 0, 20);

        assertNotNull(result);
        assertEquals(1, result.size());

        ItemRequestDto requestDto = result.get(0);
        assertEquals(itemRequest2.getId(), requestDto.getId());
        assertEquals("Need a ladder", requestDto.getDescription());
        assertEquals(1, requestDto.getItems().size());
        assertEquals("Step Ladder", requestDto.getItems().get(0).getName());
    }

    @Test
    void getAllRequests_WithNonExistentUser_ShouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class,
                () -> itemRequestService.getAllRequests(999L, 0, 20));
    }

    @Test
    void getAllRequests_ShouldExcludeCurrentUserRequests() {
        ItemRequest additionalRequest = new ItemRequest();
        additionalRequest.setDescription("Another request");
        additionalRequest.setCreated(LocalDateTime.now());
        additionalRequest.setRequester(requester);
        itemRequestRepository.save(additionalRequest);

        List<ItemRequestDto> result = itemRequestService.getAllRequests(requester.getId(), 0, 20);

        assertEquals(1, result.size()); // Должен вернуть только запрос от otherUser
        assertEquals(itemRequest2.getId(), result.get(0).getId());
    }

    @Test
    void getRequestById_WithRequesterUser_ShouldReturnRequestWithItems() {
        ItemRequestDto result = itemRequestService.getRequestById(itemRequest1.getId(), requester.getId());

        assertNotNull(result);
        assertEquals(itemRequest1.getId(), result.getId());
        assertEquals("Need a drill", result.getDescription());
        assertEquals(1, result.getItems().size());
        assertEquals("Electric Drill", result.getItems().get(0).getName());
    }

    @Test
    void getRequestById_WithOtherUser_ShouldReturnRequestWithItems() {
        ItemRequestDto result = itemRequestService.getRequestById(itemRequest1.getId(), otherUser.getId());

        assertNotNull(result);
        assertEquals(itemRequest1.getId(), result.getId());
        assertEquals("Need a drill", result.getDescription());
        assertEquals(1, result.getItems().size());
        assertEquals("Electric Drill", result.getItems().get(0).getName());
    }

    @Test
    void getRequestById_WithNonExistentRequest_ShouldThrowRequestNotFoundException() {
        assertThrows(RequestNotFoundException.class,
                () -> itemRequestService.getRequestById(999L, requester.getId()));
    }

    @Test
    void getRequestById_WithNonExistentUser_ShouldThrowUserNotFoundException() {
        assertThrows(UserNotFoundException.class,
                () -> itemRequestService.getRequestById(itemRequest2.getId(), 999L));
    }

    @Test
    void getRequestById_WithNoItems_ShouldReturnRequestWithEmptyItemsList() {
        ItemRequest requestWithoutItems = new ItemRequest();
        requestWithoutItems.setDescription("Request without items");
        requestWithoutItems.setCreated(LocalDateTime.now());
        requestWithoutItems.setRequester(requester);
        requestWithoutItems = itemRequestRepository.save(requestWithoutItems);

        ItemRequestDto result = itemRequestService.getRequestById(requestWithoutItems.getId(), requester.getId());

        assertNotNull(result);
        assertEquals(requestWithoutItems.getId(), result.getId());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void getUserRequests_ShouldReturnRequestsInDescendingOrderByCreated() {
        ItemRequest newerRequest = new ItemRequest();
        newerRequest.setDescription("Newer request");
        newerRequest.setCreated(LocalDateTime.now());
        newerRequest.setRequester(requester);
        itemRequestRepository.save(newerRequest);

        List<ItemRequestDto> result = itemRequestService.getUserRequests(requester.getId());

        assertEquals(2, result.size());
        assertTrue(result.get(0).getCreated().isAfter(result.get(1).getCreated()));
    }

    @Test
    void getAllRequests_ShouldReturnRequestsInDescendingOrderByCreated() {
        ItemRequest newerRequest = new ItemRequest();
        newerRequest.setDescription("Newer request from other user");
        newerRequest.setCreated(LocalDateTime.now());
        newerRequest.setRequester(otherUser);
        itemRequestRepository.save(newerRequest);

        List<ItemRequestDto> result = itemRequestService.getAllRequests(requester.getId(), 0, 20);

        assertEquals(2, result.size());
        assertTrue(result.get(0).getCreated().isAfter(result.get(1).getCreated()));
    }
}
