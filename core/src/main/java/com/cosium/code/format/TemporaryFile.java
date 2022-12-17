package com.cosium.code.format;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.logging.Log;

/**
 * @author Réda Housni Alaoui
 */
public class TemporaryFile implements Closeable {
  private final Log log;

  private final Path file;

  private TemporaryFile(Log log, String virtualName) {
    this.log = log;
    try {
      this.file = Files.createTempFile(null, null);
    } catch (IOException e) {
      throw new MavenGitCodeFormatException(e);
    }
    log.debug("Temporary file virtually named '" + virtualName + "' is viewable at '" + file + "'");
  }

  public static TemporaryFile create(Log log, String virtualName) {
    return new TemporaryFile(log, virtualName);
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
    if (log.isDebugEnabled()) {
      return;
    }
    Files.delete(file);
  }
}
