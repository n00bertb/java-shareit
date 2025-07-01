package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.status.BookingStatus;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoTest {

    @Autowired
    private JacksonTester<BookingDto> json;

    @Autowired
    private JacksonTester<BookingCreateDto> createJson;

    @Autowired
    private JacksonTester<BookItemRequestDto> requestJson;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Test
    void testBookingDtoSerialization() throws Exception {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

        UserDto booker = new UserDto();
        booker.setId(1L);
        booker.setName("John Doe");
        booker.setEmail("john@example.com");

        ItemDto item = new ItemDto();
        item.setId(1L);
        item.setName("Drill");
        item.setDescription("Power drill");
        item.setAvailable(true);

        BookingDto bookingDto = new BookingDto(1L, start, end, item, booker, BookingStatus.WAITING);

        JsonContent<BookingDto> result = json.write(bookingDto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.start")
                .isEqualTo(start.format(FORMATTER));
        assertThat(result).extractingJsonPathStringValue("$.end")
                .isEqualTo(end.format(FORMATTER));
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("WAITING");
        assertThat(result).extractingJsonPathNumberValue("$.booker.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.booker.name").isEqualTo("John Doe");
        assertThat(result).extractingJsonPathNumberValue("$.item.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.item.name").isEqualTo("Drill");
    }

    @Test
    void testBookingDtoDeserialization() throws Exception {
        String jsonContent = "{"
                + "\"id\":1,"
                + "\"start\":\"2024-01-01T10:00:00\","
                + "\"end\":\"2024-01-01T12:00:00\","
                + "\"item\":{"
                + "    \"id\":1,"
                + "    \"name\":\"Drill\","
                + "    \"description\":\"Power drill\","
                + "    \"available\":true"
                + "},"
                + "\"booker\":{"
                + "    \"id\":1,"
                + "    \"name\":\"John Doe\","
                + "    \"email\":\"john@example.com\""
                + "},"
                + "\"status\":\"WAITING\""
                + "}";

        BookingDto result = json.parse(jsonContent).getObject();

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0, 0));
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(result.getBooker().getId()).isEqualTo(1L);
        assertThat(result.getBooker().getName()).isEqualTo("John Doe");
        assertThat(result.getItem().getId()).isEqualTo(1L);
        assertThat(result.getItem().getName()).isEqualTo("Drill");
    }

    @Test
    void testBookingCreateDtoSerialization() throws Exception {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        BookingCreateDto createDto = new BookingCreateDto(start, end, 1L);

        JsonContent<BookingCreateDto> result = createJson.write(createDto);

        assertThat(result).extractingJsonPathStringValue("$.start")
                .isEqualTo(start.format(FORMATTER));
        assertThat(result).extractingJsonPathStringValue("$.end")
                .isEqualTo(end.format(FORMATTER));
        assertThat(result).extractingJsonPathNumberValue("$.itemId").isEqualTo(1);
    }

    @Test
    void testBookingCreateDtoDeserialization() throws Exception {
        String jsonContent = "{"
                + "\"start\":\"2024-01-01T10:00:00\","
                + "\"end\":\"2024-01-01T12:00:00\","
                + "\"itemId\":1"
                + "}";

        BookingCreateDto result = createJson.parse(jsonContent).getObject();

        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0, 0));
        assertThat(result.getItemId()).isEqualTo(1L);
    }

    @Test
    void testBookItemRequestDtoValidation() throws Exception {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        BookItemRequestDto requestDto = new BookItemRequestDto(1L, start, end);

        JsonContent<BookItemRequestDto> result = requestJson.write(requestDto);

        assertThat(result).extractingJsonPathNumberValue("$.itemId").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.start")
                .isEqualTo(start.format(FORMATTER));
        assertThat(result).extractingJsonPathStringValue("$.end")
                .isEqualTo(end.format(FORMATTER));
    }

    @Test
    void testBookItemRequestDtoDeserialization() throws Exception {
        String jsonContent = "{"
                + "\"itemId\":1,"
                + "\"start\":\"2024-01-01T10:00:00\","
                + "\"end\":\"2024-01-01T12:00:00\""
                + "}";

        BookItemRequestDto result = requestJson.parse(jsonContent).getObject();

        assertThat(result.getItemId()).isEqualTo(1L);
        assertThat(result.getStart()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0, 0));
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0, 0));
    }
}
