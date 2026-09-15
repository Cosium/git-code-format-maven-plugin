package com.cosium.code.format.maven;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.apache.maven.plugin.logging.Log;

/**
 * A maven wrapper, checked by inspecting the file. Running it would download a whole maven
 * distribution, which no hook installation has to pay for.
 *
 * @author Réda Housni Alaoui
 */
class MavenWrapperExecutable implements MavenExecutable {

  private final Supplier<Log> log;
  private final Path path;

  MavenWrapperExecutable(Supplier<Log> log, Path path) {
    this.log = requireNonNull(log);
    this.path = requireNonNull(path);
  }

  @Override
  public Path path() {
    return path;
  }

  @Override
  public boolean isValid() {
    if (!Files.isRegularFile(path)) {
      return false;
    }
    if (!Files.isExecutable(path)) {
      log.get().debug(path + " is not executable");
      return false;
    }
    log.get().debug("Found maven wrapper " + path);
    return true;
  }
}
