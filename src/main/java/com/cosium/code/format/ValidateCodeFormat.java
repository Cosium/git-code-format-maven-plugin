package com.cosium.code.format;

import com.cosium.code.format.formatter.CodeFormatter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Created on 16/01/18.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "validate-code-format", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ValidateCodeFormat extends AbstractFormatMojo {
  @Override
  protected void process(Path path) throws MojoFailureException {
    if (validate(path)) {
      return;
    }
    throw new MojoFailureException(path + " is not correctly formatted !");
  }

  private boolean validate(Path path) {
    return codeFormatters().forFileExtension(FileExtension.parse(path)).stream()
        .map(formatter -> doValidate(path, formatter))
        .filter(valid -> !valid)
        .findFirst()
        .orElse(true);
  }

  private boolean doValidate(Path path, CodeFormatter formatter) {
    getLog().info("Validating '" + gitBaseDir().relativize(path) + "'");
    try (InputStream content = Files.newInputStream(path)) {
      return formatter.validate(content);
    } catch (IOException e) {
      throw new MavenGitCodeFormatException(e);
    }
  }
}
