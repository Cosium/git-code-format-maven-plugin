package com.cosium.code.format;

import com.cosium.code.format.formatter.CodeFormatters;
import com.cosium.code.format_spi.CodeFormatter;
import com.cosium.code.format_spi.FileExtension;
import com.cosium.code.format_spi.LineRanges;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * @author Réda Housni Alaoui
 */
@Mojo(name = "format-code", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class FormatCodeMojo extends AbstractFormatMojo {

  @Override
  protected void process(CodeFormatters codeFormatters, Path path) {
    codeFormatters
        .forFileExtension(FileExtension.parse(path))
        .forEach(formatter -> format(path, formatter));
  }

  private void format(Path path, CodeFormatter formatter) {
    Path relativePath = gitBaseDir().relativize(path);
    getLog().debug("Formatting '" + relativePath + "'");

    try (TemporaryFile temporaryFormattedFile =
        TemporaryFile.create(getLog(), path + ".formatted")) {
      try (InputStream content = Files.newInputStream(path);
          OutputStream formattedContent = temporaryFormattedFile.newOutputStream()) {
        formatter.format(content, LineRanges.all(), formattedContent);
      }

      try (InputStream formattedContent = temporaryFormattedFile.newInputStream();
          OutputStream unformattedContent = Files.newOutputStream(path)) {
        IOUtils.copy(formattedContent, unformattedContent);
      }
    } catch (IOException | RuntimeException e) {
      throw new MavenGitCodeFormatException(
          String.format("Failed to format '%s': %s", relativePath, e.getMessage()), e);
    }

    getLog().debug("Formatted '" + gitBaseDir().relativize(path) + "'");
  }
}
