package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private Long id;

    @NotBlank(message = "Имя предмета не может быть пустым")
    private String name;

    @NotBlank(message = "Описание предмета не может быть пустым")
    private String description;

    @NotNull(message = "Статус не может быть null")
    private Boolean available;
}
