package com.example.batch.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileDataInitializer implements CommandLineRunner {

  @Override
  public void run(String... args) {
    try {
      Path dir = Paths.get("data");
      Files.createDirectories(dir);

      Path csvPath      = dir.resolve("products-100k.csv");
      Path csvPart1Path = dir.resolve("products-part1-100k.csv");
      Path csvPart2Path = dir.resolve("products-part2-100k.csv");
      Path jsonPath     = dir.resolve("customers-100k.json");
      Path xmlPath      = dir.resolve("customers-100k.xml");

      if (!Files.exists(csvPath)) {
        generateCsv(csvPath, 100_000);
      }
      if (!Files.exists(csvPart1Path)) {
        generateCsvRange(csvPart1Path, 1, 50_000);
      }
      if (!Files.exists(csvPart2Path)) {
        generateCsvRange(csvPart2Path, 50_001, 100_000);
      }
      if (!Files.exists(jsonPath)) {
        generateJson(jsonPath, 100_000);
      }
      if (!Files.exists(xmlPath)) {
        generateXml(xmlPath, 100_000);
      }
    } catch (IOException e) {
      throw new RuntimeException("대용량 파일 생성 중 오류 발생", e);
    }
  }

  private void generateCsv(Path path, int count) throws IOException {
    try (var writer = Files.newBufferedWriter(path)) {
      writer.write("name,price,category,stock");
      writer.newLine();
      for (int i = 1; i <= count; i++) {
        String line = String.format(
            "Product-%05d,%d,%s,%d",
            i,
            1000 + i,
            "Category-" + (i % 10),
            (i * 7) % 1000
        );
        writer.write(line);
        writer.newLine();
      }
    }
  }

  private void generateCsvRange(Path path, int startInclusive, int endInclusive) throws IOException {
    try (var writer = Files.newBufferedWriter(path)) {
      writer.write("name,price,category,stock");
      writer.newLine();
      for (int i = startInclusive; i <= endInclusive; i++) {
        String line = String.format(
            "Product-%05d,%d,%s,%d",
            i,
            1000 + i,
            "Category-" + (i % 10),
            (i * 7) % 1000
        );
        writer.write(line);
        writer.newLine();
      }
    }
  }

  private void generateJson(Path path, int count) throws IOException {
    try (var writer = Files.newBufferedWriter(path)) {
      writer.write("[");
      writer.newLine();
      for (int i = 1; i <= count; i++) {
        String json = String.format(
            "  {\"id\": %d, \"firstName\": \"first%05d\", \"lastName\": \"last%05d\", \"email\": \"user%05d@example.com\", \"age\": %d}",
            i,
            i,
            i,
            i,
            (i % 60) + 20
        );
        writer.write(json);
        if (i < count) {
          writer.write(",");
        }
        writer.newLine();
      }
      writer.write("]");
      writer.newLine();
    }
  }

  private void generateXml(Path path, int count) throws IOException {
    try (var writer = Files.newBufferedWriter(path)) {
      writer.write("<customers>");
      writer.newLine();
      for (int i = 1; i <= count; i++) {
        writer.write("  <customer>");
        writer.newLine();
        writer.write("    <id>" + i + "</id>");
        writer.newLine();
        writer.write("    <firstName>first" + String.format("%05d", i) + "</firstName>");
        writer.newLine();
        writer.write("    <lastName>last" + String.format("%05d", i) + "</lastName>");
        writer.newLine();
        writer.write("    <email>user" + String.format("%05d", i) + "@example.com</email>");
        writer.newLine();
        writer.write("    <age>" + ((i % 60) + 20) + "</age>");
        writer.newLine();
        writer.write("  </customer>");
        writer.newLine();
      }
      writer.write("</customers>");
      writer.newLine();
    }
  }
}