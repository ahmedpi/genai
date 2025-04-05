package com.epam.training.gen.ai.plugin;

import com.epam.training.gen.ai.model.LightModel;
import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AgeCalculator {

  @DefineKernelFunction(name = "calculate_age", description = "Calculate age of a person from birthdate")
  public int calculateAge(
      @KernelFunctionParameter(name = "birthdate", description = "The birth date of the person") String birthDateString) {

    System.out.println(
        "Using AgeCalculator plugin to calculate the age of person born on " + birthDateString);

    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
    LocalDate birthDate;
    try {
      birthDate = LocalDate.parse(birthDateString, formatter);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "[AgeCalculator] Invalid birth date format. Please use yyyy-MM-dd.");
    }

    LocalDate currentDate = LocalDate.now();
    if (currentDate.isBefore(birthDate)) {
      throw new IllegalArgumentException("[AgeCalculator] Birthdate cannot be future date.");
    }

    return Period.between(birthDate, currentDate).getYears();
  }
}