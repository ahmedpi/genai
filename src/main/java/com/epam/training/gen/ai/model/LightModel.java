package com.epam.training.gen.ai.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LightModel {

  private int id;
  private String description;
  private boolean isOn;

  public LightModel(int id, String description, boolean isOn) {
    this.id = id;
    this.description = description;
    this.isOn = isOn;
  }

  @Override
  public String toString() {
    return "LightModel{" +
        "id=" + id +
        ", description='" + description + '\'' +
        ", isOn=" + isOn +
        '}';
  }
}
