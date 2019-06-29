package com.cosium.code.format.formatter;

import static java.util.Objects.requireNonNull;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.ImportOrderer;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class GoogleJavaFormatter implements CodeFormatter {

  private static final String JAVA_EXTENSION = ".java";

  private final Log log;
  private final GoogleJavaFormatterOptions options;
  private final Formatter formatter;

  public GoogleJavaFormatter(Log log, GoogleJavaFormatterOptions options) {
    this.log = requireNonNull(log);
    this.options = requireNonNull(options);
    this.formatter = new Formatter(options.javaFormatterOptions());
  }

  @Override
  public void format(Path file) {
    if (!isJavaFile(file)) {
      log.debug(file + " is not a java file");
      return;
    }

    if (!Files.exists(file)) {
      log.debug(file + " doesn't exist");
      return;
    }

    log.info("Formatting '" + file + "'");

    final String formattedContent;
    try (InputStream inputStream = Files.newInputStream(file)) {
      String unformattedContent = IOUtils.toString(inputStream, "UTF-8");
      formattedContent = doFormat(unformattedContent);
    } catch (IOException | FormatterException e) {
      throw new RuntimeException(e);
    }
    try (OutputStream outputStream =
        Files.newOutputStream(file, StandardOpenOption.TRUNCATE_EXISTING)) {
      IOUtils.write(formattedContent, outputStream, "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("Formatted '" + file + "'");
  }

  @Override
  public boolean validate(Path file) {
    if (!isJavaFile(file)) {
      log.debug(file + " is not a java file");
      return true;
    }

    log.info("Validating '" + file + "'");
    try (InputStream inputStream = Files.newInputStream(file)) {
      String unformattedContent = IOUtils.toString(inputStream);
      String formattedContent = doFormat(unformattedContent);
      return unformattedContent.equals(formattedContent);
    } catch (IOException | FormatterException e) {
      throw new RuntimeException(e);
    }
  }

  private String doFormat(String unformattedContent) throws FormatterException {
    if (options.isFixImportsOnly()) {
      return fixImports(unformattedContent);
    }
    return fixImports(formatter.formatSource(unformattedContent));
  }

  private String fixImports(final String unformattedContent) throws FormatterException {
    String formattedContent = unformattedContent;
    if (!options.isSkipRemovingUnusedImports()) {
      formattedContent = RemoveUnusedImports.removeUnusedImports(formattedContent);
    }
    if (!options.isSkipSortingImports()) {
      formattedContent = ImportOrderer.reorderImports(formattedContent);
    }
    return formattedContent;
  }

  private boolean isJavaFile(Path file) {
    return file.toString().endsWith(JAVA_EXTENSION);
  }
}
