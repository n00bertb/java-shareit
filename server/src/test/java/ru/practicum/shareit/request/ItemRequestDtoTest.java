package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class ItemRequestDtoTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSerializeItemRequestDto() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        ItemRequestDto dto = new ItemRequestDto(1L, "Test request", now, null);

        String json = objectMapper.writeValueAsString(dto);
        String expectedTimePrefix = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        assertThat(json).contains("\"id\":1");
        assertThat(json).contains("\"description\":\"Test request\"");
        assertThat(json).contains(expectedTimePrefix);
    }

    @Test
    public void testDeserializeItemRequestDto() throws Exception {
        String json = String.format("{\"id\":1,\"description\":\"Test request\",\"created\":\"%s\"}",
                LocalDateTime.now().toString());

        ItemRequestDto dto = objectMapper.readValue(json, ItemRequestDto.class);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getDescription()).isEqualTo("Test request");
        assertThat(dto.getCreated()).isNotNull();
    }
}
