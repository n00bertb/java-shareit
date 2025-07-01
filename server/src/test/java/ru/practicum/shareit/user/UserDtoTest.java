package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class UserDtoTest {

    @Autowired
    private JacksonTester<UserDto> jsonJacksonTester;

    @Test
    void serializeDeserializeTest() throws Exception {
        UserDto dto = new UserDto(1L, "John", "john@example.com");

        String expectedJson = "{\"id\": 1, \"name\": \"John\", \"email\": \"john@example.com\"}";

        assertThat(jsonJacksonTester.write(dto))
                .isEqualToJson(expectedJson);

        assertThat(jsonJacksonTester.parse(expectedJson))
                .isEqualTo(dto);
    }
}
