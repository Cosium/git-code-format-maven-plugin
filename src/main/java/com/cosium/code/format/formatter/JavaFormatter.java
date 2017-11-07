package com.cosium.code.format.formatter;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

/**
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class JavaFormatter implements CodeFormatter {

  private static final String JAVA_EXTENSION = ".java";

  private final Supplier<Log> log;

  public JavaFormatter(Supplier<Log> log) {
    this.log = log;
  }

  @Override
  public void format(Path file) {
    if (!file.toString().endsWith(JAVA_EXTENSION)) {
      log.get().debug(file + " is not a java file");
      return;
    }

    log.get().info("Formatting '" + file + "'");
    final String formattedContent;
    try (InputStream inputStream = Files.newInputStream(file)) {
      formattedContent = new Formatter().formatSource(IOUtils.toString(inputStream));
    } catch (IOException | FormatterException e) {
      throw new RuntimeException(e);
    }
    try (OutputStream outputStream =
        Files.newOutputStream(file, StandardOpenOption.TRUNCATE_EXISTING)) {
      IOUtils.write(formattedContent, outputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.get().info("Formatted '" + file + "'");
  }
}
