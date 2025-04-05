package com.epam.training.gen.ai.plugin;

import com.microsoft.semantickernel.semanticfunctions.annotations.DefineKernelFunction;
import com.microsoft.semantickernel.semanticfunctions.annotations.KernelFunctionParameter;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AgeCalculatorPlugin {

  @DefineKernelFunction(name = "calculate_age", description = "Calculate age of a person based on user-provided birth dates")
  public int calculateAge(
      @KernelFunctionParameter(name = "birthdate", description = "The birth date") String birthDateString) {

    System.out.println(
        "Using AgeCalculatorPlugin to calculate age for birthdate " + birthDateString);

    DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
    LocalDate birthDate;
    try {
      birthDate = LocalDate.parse(birthDateString, formatter);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "[AgeCalculatorPlugin] Invalid birth date format. Please use yyyy-MM-dd.");
    }

    LocalDate currentDate = LocalDate.now();
    if (currentDate.isBefore(birthDate)) {
      throw new IllegalArgumentException("[AgeCalculatorPlugin] Birthdate cannot be future date.");
    }

    return Period.between(birthDate, currentDate).getYears();
  }
}