package com.cosium.code.format;

import com.google.common.io.Files;
import java.nio.file.Path;
import java.util.Objects;

/** @author Réda Housni Alaoui */
public class FileExtension {

  private final String value;

  private FileExtension(String value) {
    this.value = value;
  }

  public static FileExtension parse(Path path) {
    return new FileExtension(Files.getFileExtension(path.getFileName().toString()));
  }

  public static FileExtension parse(String path) {
    return new FileExtension(Files.getFileExtension(path));
  }

  public static FileExtension of(String value) {
    return new FileExtension(value);
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FileExtension that = (FileExtension) o;

    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return value != null ? value.hashCode() : 0;
  }
}
