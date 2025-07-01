package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ItemRequestRepositoryTest {

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindUserRequests() {
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        User savedUser = userRepository.save(user);

        ItemRequest request1 = new ItemRequest();
        request1.setDescription("Request 1");
        request1.setCreated(LocalDateTime.now());
        request1.setRequester(savedUser);

        ItemRequest request2 = new ItemRequest();
        request2.setDescription("Request 2");
        request2.setCreated(LocalDateTime.now().plusDays(1));
        request2.setRequester(savedUser);

        itemRequestRepository.save(request1);
        itemRequestRepository.save(request2);

        List<ItemRequest> foundRequests = itemRequestRepository.findByRequesterId(
                savedUser.getId(),
                Sort.by("created").descending());

        assertThat(foundRequests).hasSize(2);
        assertThat(foundRequests.get(0).getDescription()).isEqualTo("Request 2");
        assertThat(foundRequests.get(1).getDescription()).isEqualTo("Request 1");
    }

    @Test
    public void testFindOtherUsersRequests() {
        User user1 = new User();
        user1.setName("John Doe");
        user1.setEmail("john@example.com");
        User savedUser1 = userRepository.save(user1);

        ItemRequest request1 = new ItemRequest();
        request1.setDescription("User1 Request");
        request1.setCreated(LocalDateTime.now());
        request1.setRequester(savedUser1);
        itemRequestRepository.save(request1);

        User user2 = new User();
        user2.setName("Jane Smith");
        user2.setEmail("jane@example.com");
        User savedUser2 = userRepository.save(user2);

        ItemRequest request2 = new ItemRequest();
        request2.setDescription("User2 Request");
        request2.setCreated(LocalDateTime.now().plusDays(1));
        request2.setRequester(savedUser2);
        itemRequestRepository.save(request2);

        List<ItemRequest> foundRequests = itemRequestRepository.findByRequesterIdNot(
                savedUser1.getId(),
                Sort.by("created").descending());

        assertThat(foundRequests).hasSize(1);
        assertThat(foundRequests.get(0).getDescription()).isEqualTo("User2 Request");
    }
}
