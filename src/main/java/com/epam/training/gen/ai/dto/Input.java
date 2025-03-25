package com.epam.training.gen.ai.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class Input {

  private String role;
  private String text;
}
