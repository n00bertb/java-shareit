package ru.practicum.shareit.item.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private User author;
    private Item item;

    @BeforeEach
    void setUp() {
        author = new User(null, "Author", "author@example.com");
        author = userRepository.save(author);

        item = new Item(null, "Item", "Desc", true, author, null);
        item = itemRepository.save(item);
    }

    @Test
    void testFindByItemId() {
        Comment comment = new Comment();
        comment.setText("Great item!");
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        commentRepository.save(comment);

        List<Comment> comments = commentRepository.findByItemId(item.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getText()).isEqualTo("Great item!");
    }
}