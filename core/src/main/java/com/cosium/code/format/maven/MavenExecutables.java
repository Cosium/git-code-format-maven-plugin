package com.cosium.code.format.maven;

import com.cosium.code.format.MavenGitCodeFormatException;
import com.cosium.code.format.executable.CommandRunner;
import com.cosium.code.format.executable.DefaultCommandRunner;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

/**
 * @author Réda Housni Alaoui
 * @author Matt.Ruel
 */
public class MavenExecutables {

  private final MavenWrapperExecutables wrapperExecutables;
  private final MavenInstallationExecutables installationExecutables;

  public MavenExecutables(Supplier<Log> log, Path baseDir, Path gitBaseDir) {
    this(log, baseDir, gitBaseDir, System::getProperty, new DefaultCommandRunner(log));
  }

  MavenExecutables(
      Supplier<Log> log,
      Path baseDir,
      Path gitBaseDir,
      UnaryOperator<String> systemProperties,
      CommandRunner commandRunner) {
    this.wrapperExecutables = new MavenWrapperExecutables(log, baseDir, gitBaseDir);
    this.installationExecutables =
        new MavenInstallationExecutables(log, systemProperties, commandRunner);
  }

  /**
   * @param debug True to get the debug flavour. The maven wrapper holds none, so the maven
   *     installation is the only candidate then.
   * @param preferMavenWrapper True to let the maven wrapper of the repository come before the maven
   *     installation, false to let it come last
   * @return The maven executable to run
   */
  public Path select(boolean debug, boolean preferMavenWrapper) {
    Stream<MavenExecutable> wrappers = wrapperExecutables.stream(debug);
    Stream<MavenExecutable> installations = installationExecutables.stream(debug);

    Stream<MavenExecutable> candidates;
    if (preferMavenWrapper) {
      candidates = Stream.concat(wrappers, installations);
    } else {
      candidates = Stream.concat(installations, wrappers);
    }

    return candidates
        .filter(MavenExecutable::isValid)
        .findFirst()
        .map(MavenExecutable::path)
        .orElseThrow(() -> new MavenGitCodeFormatException("No valid maven executable found !"));
  }
}
