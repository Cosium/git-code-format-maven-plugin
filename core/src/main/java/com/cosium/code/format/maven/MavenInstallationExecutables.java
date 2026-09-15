package com.cosium.code.format.maven;

import static java.util.Objects.requireNonNull;

import com.cosium.code.format.executable.CommandRunner;
import com.cosium.code.format.maven.MavenInstallationExecutable.Extension;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.apache.commons.exec.OS;
import org.apache.maven.plugin.logging.Log;

/**
 * The executable candidates of the maven installation, those of the home directory coming before
 * those the path may hold.
 *
 * @author Réda Housni Alaoui
 * @author Matt.Ruel
 */
class MavenInstallationExecutables {

  private static final String MAVEN_HOME_PROP = "maven.home";

  private final Supplier<Log> log;
  private final UnaryOperator<String> systemProperties;
  private final CommandRunner commandRunner;

  MavenInstallationExecutables(
      Supplier<Log> log, UnaryOperator<String> systemProperties, CommandRunner commandRunner) {
    this.log = requireNonNull(log);
    this.systemProperties = requireNonNull(systemProperties);
    this.commandRunner = requireNonNull(commandRunner);
  }

  /**
   * @param debug True to get the debug flavour
   * @return The maven installation candidates
   */
  Stream<MavenExecutable> stream(boolean debug) {
    Path mavenHome = Paths.get(systemProperties.apply(MAVEN_HOME_PROP));
    log.get().debug("maven.home=" + mavenHome);
    Path mavenBinDirectory = mavenHome.resolve("bin");

    List<MavenExecutable> shellExecutables =
        List.of(
            new MavenInstallationExecutable(
                log, commandRunner, debug, mavenBinDirectory, Extension.NONE),
            new MavenInstallationExecutable(log, commandRunner, debug, null, Extension.NONE));
    List<MavenExecutable> batchExecutables =
        List.of(
            new MavenInstallationExecutable(
                log, commandRunner, debug, mavenBinDirectory, Extension.CMD),
            new MavenInstallationExecutable(log, commandRunner, debug, null, Extension.CMD));

    if (OS.isFamilyWindows()) {
      return Stream.concat(batchExecutables.stream(), shellExecutables.stream());
    }
    return Stream.concat(shellExecutables.stream(), batchExecutables.stream());
  }
}
