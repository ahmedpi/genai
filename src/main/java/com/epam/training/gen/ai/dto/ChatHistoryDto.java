package com.epam.training.gen.ai.dto;

import com.epam.training.gen.ai.model.Message;
import lombok.Data;

import java.util.List;

@Data
public class ChatHistoryDto {
    private List<Message> messages;
}
