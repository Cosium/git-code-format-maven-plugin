package com.cosium.code.format;

import com.cosium.code.format.formatter.CodeFormatter;
import com.cosium.code.format.formatter.LineRanges;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "format-code", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class FormatCodeMojo extends AbstractFormatMojo {

  @Override
  protected void process(Path path) {
    codeFormatters()
        .forFileExtension(FileExtension.parse(path))
        .forEach(formatter -> format(path, formatter));
  }

  private void format(Path path, CodeFormatter formatter) {
    getLog().debug("Formatting '" + gitBaseDir().relativize(path) + "'");

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
    } catch (IOException e) {
      throw new MavenGitCodeFormatException(e);
    }

    getLog().debug("Formatted '" + gitBaseDir().relativize(path) + "'");
  }
}
