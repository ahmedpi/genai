package com.epam.training.gen.ai.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.units.qual.N;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Book {

  private Long id;                // Unique identifier for the book
  private String title;           // Title of the book
  private String author;          // Author of the book
  private LocalDate publishedDate; // Date the book was published
  private String isbn;            // ISBN number of the book
  private Double price;           // Price of the book

  @Override
  public String toString() {
    return "Book{" +
        "id=" + id +
        ", title='" + title + '\'' +
        ", author='" + author + '\'' +
        ", publishedDate=" + publishedDate +
        ", isbn='" + isbn + '\'' +
        ", price=" + price +
        '}';
  }
}
