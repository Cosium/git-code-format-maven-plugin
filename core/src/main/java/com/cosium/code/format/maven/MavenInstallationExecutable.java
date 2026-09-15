package com.cosium.code.format.maven;

import static java.util.Objects.requireNonNull;

import com.cosium.code.format.executable.CommandRunner;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.maven.plugin.logging.Log;

/**
 * An executable of the maven installation, checked by running it.
 *
 * @author Réda Housni Alaoui
 */
class MavenInstallationExecutable implements MavenExecutable {

  private final Supplier<Log> log;
  private final CommandRunner commandRunner;
  private final Path path;

  MavenInstallationExecutable(
      Supplier<Log> log,
      CommandRunner commandRunner,
      boolean debug,
      Path prefix,
      Extension extension) {
    this.log = requireNonNull(log);
    this.commandRunner = requireNonNull(commandRunner);
    requireNonNull(extension);

    String name = "mvn";
    if (debug) {
      name += "Debug";
    }
    if (extension != Extension.NONE) {
      name += "." + extension.value;
    }
    if (prefix != null) {
      path = prefix.resolve(name);
    } else {
      path = Paths.get(name);
    }
  }

  @Override
  public Path path() {
    return path;
  }

  @Override
  public boolean isValid() {
    try {
      commandRunner.run(null, Map.of("MAVEN_DEBUG_OPTS", ""), path.toString(), "--version");
      return true;
    } catch (Exception e) {
      log.get().debug(e.getMessage());
    }
    return false;
  }

  enum Extension {
    NONE(null),
    CMD("cmd");

    private final String value;

    Extension(String value) {
      this.value = value;
    }
  }
}
