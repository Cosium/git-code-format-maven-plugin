package com.cosium.code.format.maven;

import com.cosium.code.format.MavenGitCodeFormatException;
import com.cosium.code.format.executable.CommandRunException;
import com.cosium.code.format.executable.CommandRunner;
import com.cosium.code.format.executable.DefaultCommandRunner;
import org.apache.maven.plugin.logging.Log;
import org.apache.commons.exec.OS;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * Created on 02/11/17.
 *
 * @author Reda.Housni-Alaoui
 * @author Matt.Ruel
 */
public class MavenEnvironment {

  private static final String MAVEN_HOME_PROP = "maven.home";

  private final Supplier<Log> log;
  private final UnaryOperator<String> systemProperties;
  private final CommandRunner commandRunner;

  public MavenEnvironment(Supplier<Log> log) {
    this(log, System::getProperty, new DefaultCommandRunner(log));
  }

  MavenEnvironment(
      Supplier<Log> log, UnaryOperator<String> systemProperties, CommandRunner commandRunner) {
    this.log = log;
    this.systemProperties = requireNonNull(systemProperties);
    this.commandRunner = requireNonNull(commandRunner);
  }

  public Path getMavenExecutable(boolean debug) {
    Path mavenHome = Paths.get(systemProperties.apply(MAVEN_HOME_PROP));
    log.get().debug("maven.home=" + mavenHome);
    Path bin = mavenHome.resolve("bin");
    Path executable;
    String extension = OS.isFamilyWindows() ? ".cmd" : "";
    if (!debug) {
      executable = bin.resolve("mvn" + extension);
    } else {
      executable = bin.resolve("mvnDebug" + extension);
    }

    try {
      commandRunner.run(null, executable.toString(), "--version");
      return executable;
    } catch (CommandRunException e) {
      log.get().debug(e.getMessage());
    }

    Path fallbackExecutable;
    if (!debug) {
      fallbackExecutable = Paths.get("mvn" + extension);
    } else {
      fallbackExecutable = Paths.get("mvnDebug" + extension);
    }

    log.get()
        .info(
            "Could not execute '"
                + executable
                + "'. Falling back to '"
                + fallbackExecutable
                + "'.");
    try {
      commandRunner.run(null, fallbackExecutable.toString(), "--version");
      return fallbackExecutable;
    } catch (CommandRunException e) {
      throw new MavenGitCodeFormatException(e.getMessage(), e);
    }
  }
}
