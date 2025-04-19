# Dynamic Model Chat Service

This is a Spring Boot application that interacts with Azure OpenAI services to dynamically process
requests using different models.

---

## Task 3 - Comparison of results for the same prompt using different models

**gpt-4o**
![img.png](img.png)

**DeepSeek-R1-Distill-Llama-70B-FP8**
![img_1.png](img_1.png)

**amazon.titan-tg1-large**
![img_2.png](img_2.png)

## PromptExecutionSettings

Setting the maximum number of tokens limits the length of the output.
![img_3.png](img_3.png)

## Task 4 - Call function from custom plugin

### AgeCalculatorPlugin

Calculates and display ages based on user-provided birth dates
![img_6.png](img_6.png)

### SearchUrlPlugin

The function getWikipediaSearchUrl takes a search query
as input and generates a URL for searching that query on Wikipedia.
{ "prompt", "Europe" }
![img_4.png](img_4.png)

{ "query", "cute kittens on vespas" }
![img_7.png](img_7.png)

### Currency Converter Plugin

![img_5.png](img_5.png)
