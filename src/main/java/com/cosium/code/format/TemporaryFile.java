package com.cosium.code.format;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** @author Réda Housni Alaoui */
public class TemporaryFile implements Closeable {

  private final Path file;

  private TemporaryFile() {
    try {
      this.file = Files.createTempFile(null, null);
    } catch (IOException e) {
      throw new MavenGitCodeFormatException(e);
    }
  }

  public static TemporaryFile create() {
    return new TemporaryFile();
  }

  public OutputStream newOutputStream() throws IOException {
    return Files.newOutputStream(file);
  }

  public InputStream newInputStream() throws IOException {
    return Files.newInputStream(file);
  }

  public long size() throws IOException {
    return Files.size(file);
  }

  @Override
  public void close() throws IOException {
    Files.delete(file);
  }
}
