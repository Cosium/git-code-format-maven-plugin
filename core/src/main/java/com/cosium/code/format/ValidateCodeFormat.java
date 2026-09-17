package com.cosium.code.format;

import com.cosium.code.format.formatter.CodeFormatters;
import com.cosium.code.format.maven.MavenDocumentation;
import com.cosium.code.format_spi.CodeFormatter;
import com.cosium.code.format_spi.FileExtension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Réda Housni Alaoui
 */
@Mojo(name = "validate-code-format", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ValidateCodeFormat extends AbstractFormatMojo {

  @Parameter(readonly = true, defaultValue = "${plugin}")
  private PluginDescriptor pluginDescriptor;

  @Parameter(readonly = true, defaultValue = "${session.executionRootDirectory}")
  private String executionRootDirectory;

  @Override
  protected void process(CodeFormatters codeFormatters, Path path) throws MojoFailureException {
    if (validate(codeFormatters, path)) {
      return;
    }
    throw new MojoFailureException(
        path
            + " is not correctly formatted !"
            + System.lineSeparator()
            + "Run '"
            + mavenDocumentation().createFormatCodeCommand()
            + "' to format all the files.");
  }

  private MavenDocumentation mavenDocumentation() {
    return new MavenDocumentation(
        this::getLog, pluginDescriptor, baseDir(), gitBaseDir(), Paths.get(executionRootDirectory));
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
