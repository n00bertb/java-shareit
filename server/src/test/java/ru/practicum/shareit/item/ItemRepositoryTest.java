package ru.practicum.shareit.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(null, "User", "user@example.com");
        user = userRepository.save(user);
    }

    @Test
    void testFindByOwnerId() {
        Item item = new Item(null, "Spoon", "Silver spoon", true, user, null);
        itemRepository.save(item);

        List<Item> items = itemRepository.findByOwnerId(user.getId());

        assertThat(items).hasSize(1);
        assertThat(items.get(0)).isEqualTo(item);
    }

    @Test
    void testSearch() {
        Item item1 = new Item(null, "Spoon", "Silver spoon", true, user, null);
        Item item2 = new Item(null, "Fork", "Stainless steel fork", true, user, null);
        Item item3 = new Item(null, "Plate", "Ceramic plate", true, user, null);

        itemRepository.save(item1);
        itemRepository.save(item2);
        itemRepository.save(item3);

        List<Item> result = itemRepository.search("spoon");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(item1);
    }
}
