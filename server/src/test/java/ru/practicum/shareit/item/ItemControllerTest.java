package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.ItemNotFoundException;
import ru.practicum.shareit.exception.UserNotFoundException;
import ru.practicum.shareit.item.comment.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingDto;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    private ItemDto itemDto;
    private ItemWithBookingDto itemWithBookingDto;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        itemDto = new ItemDto();
        itemDto.setId(1L);
        itemDto.setName("Test Item");
        itemDto.setDescription("Test Description");
        itemDto.setAvailable(true);

        itemWithBookingDto = new ItemWithBookingDto();
        itemWithBookingDto.setId(1L);
        itemWithBookingDto.setName("Test Item");
        itemWithBookingDto.setDescription("Test Description");
        itemWithBookingDto.setAvailable(true);
        itemWithBookingDto.setComments(Collections.emptyList());

        commentDto = new CommentDto();
        commentDto.setId(1L);
        commentDto.setText("Great item!");
        commentDto.setAuthorName("John Doe");
        commentDto.setCreated(LocalDateTime.now());
    }

    @Test
    void createItem_ShouldReturnCreatedItem() throws Exception {
        // Given
        when(itemService.createItem(eq(1L), any(ItemDto.class))).thenReturn(itemDto);

        // When & Then
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Item")))
                .andExpect(jsonPath("$.description", is("Test Description")))
                .andExpect(jsonPath("$.available", is(true)));

        verify(itemService).createItem(eq(1L), any(ItemDto.class));
    }

    @Test
    void createItem_WithoutUserHeader_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).createItem(anyLong(), any(ItemDto.class));
    }

    @Test
    void createItem_WithNonExistentUser_ShouldReturnNotFound() throws Exception {
        // Given
        when(itemService.createItem(eq(999L), any(ItemDto.class)))
                .thenThrow(new UserNotFoundException("Собственник не найден"));

        // When & Then
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isNotFound());

        verify(itemService).createItem(eq(999L), any(ItemDto.class));
    }

    @Test
    void updateItem_ShouldReturnUpdatedItem() throws Exception {
        // Given
        ItemDto updateDto = new ItemDto();
        updateDto.setName("Updated Item");
        updateDto.setDescription("Updated Description");
        updateDto.setAvailable(false);

        ItemDto updatedItem = new ItemDto();
        updatedItem.setId(1L);
        updatedItem.setName("Updated Item");
        updatedItem.setDescription("Updated Description");
        updatedItem.setAvailable(false);

        when(itemService.updateItem(eq(1L), eq(1L), any(ItemDto.class))).thenReturn(updatedItem);

        // When & Then
        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Item")))
                .andExpect(jsonPath("$.description", is("Updated Description")))
                .andExpect(jsonPath("$.available", is(false)));

        verify(itemService).updateItem(eq(1L), eq(1L), any(ItemDto.class));
    }

    @Test
    void updateItem_WithAccessDenied_ShouldReturnForbidden() throws Exception {
        // Given
        when(itemService.updateItem(eq(2L), eq(1L), any(ItemDto.class)))
                .thenThrow(new AccessDeniedException("Только собственник может редактировать предмет"));

        // When & Then
        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isForbidden());

        verify(itemService).updateItem(eq(2L), eq(1L), any(ItemDto.class));
    }

    @Test
    void updateItem_WithNonExistentItem_ShouldReturnNotFound() throws Exception {
        // Given
        when(itemService.updateItem(eq(1L), eq(999L), any(ItemDto.class)))
                .thenThrow(new ItemNotFoundException("Предмет не найден"));

        // When & Then
        mockMvc.perform(patch("/items/999")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isNotFound());

        verify(itemService).updateItem(eq(1L), eq(999L), any(ItemDto.class));
    }

    @Test
    void getItem_ShouldReturnItem() throws Exception {
        // Given
        when(itemService.getItemById(1L)).thenReturn(itemWithBookingDto);

        // When & Then
        mockMvc.perform(get("/items/1")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Item")))
                .andExpect(jsonPath("$.description", is("Test Description")))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.comments", hasSize(0)));

        verify(itemService).getItemById(1L);
    }

    @Test
    void getItem_WithNonExistentItem_ShouldReturnNotFound() throws Exception {
        // Given
        when(itemService.getItemById(999L)).thenThrow(new ItemNotFoundException("Предмет не найден"));

        // When & Then
        mockMvc.perform(get("/items/999")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isNotFound());

        verify(itemService).getItemById(999L);
    }

    @Test
    void getItems_ShouldReturnOwnerItems() throws Exception {
        // Given
        List<ItemWithBookingDto> items = Arrays.asList(itemWithBookingDto);
        when(itemService.getItemsByOwner(1L)).thenReturn(items);

        // When & Then
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Test Item")));

        verify(itemService).getItemsByOwner(1L);
    }

    @Test
    void getItems_WithNonExistentUser_ShouldReturnNotFound() throws Exception {
        // Given
        when(itemService.getItemsByOwner(999L))
                .thenThrow(new UserNotFoundException("Собственник не найден"));

        // When & Then
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 999L))
                .andExpect(status().isNotFound());

        verify(itemService).getItemsByOwner(999L);
    }

    @Test
    void searchItems_ShouldReturnMatchingItems() throws Exception {
        // Given
        List<ItemDto> items = Arrays.asList(itemDto);
        when(itemService.searchItems("test")).thenReturn(items);

        // When & Then
        mockMvc.perform(get("/items/search")
                        .param("text", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Test Item")));

        verify(itemService).searchItems("test");
    }

    @Test
    void searchItems_WithEmptyText_ShouldReturnEmptyList() throws Exception {
        // Given
        when(itemService.searchItems("")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/items/search")
                        .param("text", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(itemService).searchItems("");
    }

    @Test
    void addComment_ShouldReturnCreatedComment() throws Exception {
        // Given
        CommentDto inputComment = new CommentDto();
        inputComment.setText("Great item!");

        when(itemService.addComment(eq(1L), eq(1L), any(CommentDto.class))).thenReturn(commentDto);

        // When & Then
        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputComment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.text", is("Great item!")))
                .andExpect(jsonPath("$.authorName", is("John Doe")))
                .andExpect(jsonPath("$.created").exists());

        verify(itemService).addComment(eq(1L), eq(1L), any(CommentDto.class));
    }

    @Test
    void addComment_WithNonExistentItem_ShouldReturnNotFound() throws Exception {
        // Given
        CommentDto inputComment = new CommentDto();
        inputComment.setText("Great item!");

        when(itemService.addComment(eq(1L), eq(999L), any(CommentDto.class)))
                .thenThrow(new ItemNotFoundException("Предмет не найден"));

        // When & Then
        mockMvc.perform(post("/items/999/comment")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputComment)))
                .andExpect(status().isNotFound());

        verify(itemService).addComment(eq(1L), eq(999L), any(CommentDto.class));
    }

    @Test
    void addComment_WithNonExistentUser_ShouldReturnNotFound() throws Exception {
        // Given
        CommentDto inputComment = new CommentDto();
        inputComment.setText("Great item!");

        when(itemService.addComment(eq(999L), eq(1L), any(CommentDto.class)))
                .thenThrow(new UserNotFoundException("Пользователь не найден"));

        // When & Then
        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputComment)))
                .andExpect(status().isNotFound());

        verify(itemService).addComment(eq(999L), eq(1L), any(CommentDto.class));
    }
}
