package com.example.batch.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class FileDataCleaner implements DisposableBean {

  @Override
  public void destroy() throws Exception {
    Path dataDir = Paths.get("data");
    if (Files.exists(dataDir)) {
      try (var paths = Files.walk(dataDir)) {
        paths.sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
      }
    }
  }
}