package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import ru.practicum.shareit.item.dto.ItemDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class ItemDtoTest {

    @Autowired
    private JacksonTester<ItemDto> jsonJacksonTester;

    @Test
    void testSerializeDeserialize() throws Exception {
        ItemDto dto = new ItemDto(1L, "Name", "Description", true, null);

        String expectedJson = "{\"id\": 1, \"name\": \"Name\", \"description\": \"Description\", \"available\": true}";

        assertThat(jsonJacksonTester.write(dto))
                .isEqualToJson(expectedJson);

        assertThat(jsonJacksonTester.parse(expectedJson))
                .isEqualTo(dto);
    }
}
