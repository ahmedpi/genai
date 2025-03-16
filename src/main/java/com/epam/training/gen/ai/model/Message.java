package com.epam.training.gen.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Message {
    private String type;
    private String contents;

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", contents='" + contents + '\'' +
                '}';
    }
}
