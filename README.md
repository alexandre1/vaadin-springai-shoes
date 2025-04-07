## Using Spring AI to extract structured data from images in Java

This example shows how to use the [Spring AI](https://docs.spring.io/spring-ai/reference/) to extract structured data from images in Java.
It has a [Vaadin Flow](https://vaadin.com/) UI to upload an image and extract structured data from it.

It provide also a shoes recognition view-

## Requirements

- Java 17+
- Open AI API key in OPENAI_API_KEY environment variable (or update `application.properties` with the key)

## Running the application

You can run the application by executing the `main` method in the `com.example.application.Application` class.
Alternatively, you can run the `mvn` command in the project root directory.

To compile the application run mvn clean package and then mvn spring-boot:run 
