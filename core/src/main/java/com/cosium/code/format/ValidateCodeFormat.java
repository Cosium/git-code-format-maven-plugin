package com.cosium.code.format;

import com.cosium.code.format.formatter.CodeFormatters;
import com.cosium.code.format_spi.CodeFormatter;
import com.cosium.code.format_spi.FileExtension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * @author Réda Housni Alaoui
 */
@Mojo(name = "validate-code-format", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ValidateCodeFormat extends AbstractFormatMojo {
  @Override
  protected void process(CodeFormatters codeFormatters, Path path) throws MojoFailureException {
    if (validate(codeFormatters, path)) {
      return;
    }
    throw new MojoFailureException(path + " is not correctly formatted !");
  }

  private boolean validate(CodeFormatters codeFormatters, Path path) {
    return codeFormatters.forFileExtension(FileExtension.parse(path)).stream()
        .map(formatter -> doValidate(path, formatter))
        .filter(valid -> !valid)
        .findFirst()
        .orElse(true);
  }

  private boolean doValidate(Path path, CodeFormatter formatter) {
    Path relativePath = gitBaseDir().relativize(path);
    getLog().debug("Validating '" + relativePath + "'");
    try (InputStream content = Files.newInputStream(path)) {
      return formatter.validate(content);
    } catch (IOException | RuntimeException e) {
      throw new MavenGitCodeFormatException(
          String.format("Failed to validate '%s': %s", relativePath, e.getMessage()), e);
    }
  }
}
