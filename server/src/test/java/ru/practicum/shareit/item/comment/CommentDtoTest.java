package ru.practicum.shareit.item.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.item.comment.CommentDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class CommentDtoTest {

    @Autowired
    private JacksonTester<CommentDto> jsonJacksonTester;

    @Test
    void testSerializeDeserialize() throws Exception {
        LocalDateTime fixedTime = LocalDateTime.of(2025, 6, 29, 9, 51, 29, 161745700);

        CommentDto dto = new CommentDto(1L, "Text", "Author", fixedTime);

        String json = jsonJacksonTester.write(dto).getJson();

        CommentDto parsedDto = jsonJacksonTester.parseObject(json);

        assertThat(parsedDto)
                .usingRecursiveComparison()
                .isEqualTo(dto);
    }
}
