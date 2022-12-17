package com.cosium.code.format.maven;

import static java.util.Objects.requireNonNull;

import com.cosium.code.format.MavenGitCodeFormatException;
import com.cosium.code.format.executable.CommandRunner;
import com.cosium.code.format.executable.DefaultCommandRunner;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.apache.commons.exec.OS;
import org.apache.maven.plugin.logging.Log;

/**
 * @author Réda Housni Alaoui
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
    Path mavenBinDirectory = mavenHome.resolve("bin");

    List<List<Executable>> executableCandidates =
        Arrays.asList(
            Arrays.asList(
                new Executable(debug, mavenBinDirectory, Extension.NONE),
                new Executable(debug, null, Extension.NONE)),
            Arrays.asList(
                new Executable(debug, mavenBinDirectory, Extension.CMD),
                new Executable(debug, null, Extension.CMD)));

    if (OS.isFamilyWindows()) {
      Collections.reverse(executableCandidates);
    }

    return executableCandidates.stream()
        .flatMap(Collection::stream)
        .filter(Executable::isValid)
        .findFirst()
        .map(Executable::path)
        .orElseThrow(() -> new MavenGitCodeFormatException("No valid maven executable found !"));
  }

  private class Executable {

    private final Path path;

    private Executable(boolean debug, Path prefix, Extension extension) {
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

    Path path() {
      return path;
    }

    boolean isValid() {
      try {
        commandRunner.run(
            null, Collections.singletonMap("MAVEN_DEBUG_OPTS", ""), path.toString(), "--version");
        return true;
      } catch (Exception e) {
        log.get().debug(e.getMessage());
      }
      return false;
    }
  }

  private enum Extension {
    NONE(null),
    CMD("cmd");
    private final String value;

    Extension(String value) {
      this.value = value;
    }
  }
}
