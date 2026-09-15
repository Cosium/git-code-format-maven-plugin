package com.cosium.code.format.maven;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.exec.OS;
import org.apache.maven.plugin.logging.Log;

/**
 * The maven wrapper candidates of a repository, ordered by proximity to the base directory.
 *
 * @author Réda Housni Alaoui
 */
class MavenWrapperExecutables {

  private static final String MAVEN_WRAPPER = "mvnw";
  private static final String WINDOWS_MAVEN_WRAPPER = "mvnw.cmd";

  private final Supplier<Log> log;
  private final Path baseDir;
  private final Path gitBaseDir;

  MavenWrapperExecutables(Supplier<Log> log, Path baseDir, Path gitBaseDir) {
    this.log = requireNonNull(log);
    this.baseDir = requireNonNull(baseDir);
    this.gitBaseDir = requireNonNull(gitBaseDir);
  }

  /**
   * @param debug True to get the debug flavour. The wrapper holds none, so no candidate is produced
   *     then.
   * @return The wrapper candidates, from the base directory up to the git base directory included
   */
  Stream<MavenExecutable> stream(boolean debug) {
    if (debug) {
      log.get().debug("Debug is enabled. The maven wrapper holds no debug flavour.");
      return Stream.empty();
    }

    Path lastDirectory = gitBaseDir.toAbsolutePath().normalize();
    List<MavenExecutable> executableCandidates = new ArrayList<>();
    Path directory = baseDir.toAbsolutePath().normalize();
    while (directory != null && directory.startsWith(lastDirectory)) {
      for (String wrapperName : wrapperNames()) {
        executableCandidates.add(new MavenWrapperExecutable(log, directory.resolve(wrapperName)));
      }
      directory = directory.getParent();
    }
    return executableCandidates.stream();
  }

  /**
   * The hook is a bash script, so the bash flavour of the wrapper comes first. Windows git ships
   * the bash running it, but the wrapper may hold the executable bit on no flavour but the batch
   * one.
   */
  private List<String> wrapperNames() {
    if (OS.isFamilyWindows()) {
      return List.of(MAVEN_WRAPPER, WINDOWS_MAVEN_WRAPPER);
    }
    return List.of(MAVEN_WRAPPER);
  }
}
