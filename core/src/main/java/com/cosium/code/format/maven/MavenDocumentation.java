package com.cosium.code.format.maven;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;

/**
 * @author Réda Housni Alaoui
 */
public class MavenDocumentation {

  private final MavenWrapperExecutables wrapperExecutables;
  private final PluginDescriptor pluginDescriptor;
  private final Path executionRootDirectory;

  public MavenDocumentation(
      Supplier<Log> log,
      PluginDescriptor pluginDescriptor,
      Path baseDir,
      Path gitBaseDir,
      Path executionRootDirectory) {
    this.wrapperExecutables = new MavenWrapperExecutables(log, baseDir, gitBaseDir);
    this.pluginDescriptor = requireNonNull(pluginDescriptor);
    this.executionRootDirectory = requireNonNull(executionRootDirectory);
  }

  /**
   * @return The command formatting all the files, to be run from the execution root directory
   */
  public String createFormatCodeCommand() {
    return "%s %s:format-code".formatted(createMavenCommand(), pluginDescriptor.getGoalPrefix());
  }

  private String createMavenCommand() {
    return wrapperExecutables.stream(false)
        .filter(MavenExecutable::isValid)
        .findFirst()
        .map(MavenExecutable::path)
        .map(this::createWrapperCommand)
        .orElse("mvn");
  }

  private String createWrapperCommand(Path wrapperPath) {
    Path directory = executionRootDirectory.toAbsolutePath().normalize();
    if (!wrapperPath.startsWith(directory)) {
      return wrapperPath.toString();
    }
    return "." + File.separator + directory.relativize(wrapperPath);
  }
}
