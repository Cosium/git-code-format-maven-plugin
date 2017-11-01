package com.cosium.code.format;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Installs git hooks on each initialization. Hooks are always overriden in case changes in:
 *
 * <ul>
 *   <li>maven installation
 *   <li>plugin structure
 * </ul>
 */
@Mojo(name = "install-hooks", defaultPhase = LifecyclePhase.INITIALIZE)
public class InstallHooksMojo extends AbstractMavenGitCodeFormatMojo {

  private static final String SHIBANG = "#!/bin/bash";
  private static final String HOOKS_DIR = "hooks";

  private static final String BASE_PLUGIN_PRE_COMMIT_HOOK = "maven-git-code-format.sh";
  private static final String PLUGIN_PRE_COMMIT_COMMAND_ARGS = "git-code-format:on-pre-commit";

  private static final String MAIN_PRE_COMMIT_HOOK = "pre-commit";
  private static final String MAVEN_HOME_PROP = "maven.home";
  private static final String BIN_MVN = "bin/mvn";

  public void execute() throws MojoExecutionException {
    try {
      getLog().info("Installing git hooks");
      doExecute();
      getLog().info("Installed git hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void doExecute() throws IOException {
    getLog().debug("Preparing git hook directory");
    Path hooksDirectory;
    hooksDirectory = getOrCreateHooksDirectory();
    getLog().debug("Prepared git hook directory");

    getLog().debug("Writing plugin pre commit hook file");
    Path pluginPreCommitHook = hooksDirectory.resolve(pluginPreCommitHook());
    getOrCreateExecutableFile(pluginPreCommitHook);
    Files.write(
        pluginPreCommitHook,
        Arrays.asList(
            SHIBANG, getMavenExecutable().toAbsolutePath() + " " + PLUGIN_PRE_COMMIT_COMMAND_ARGS),
        StandardOpenOption.TRUNCATE_EXISTING);
    getLog().debug("Written plugin pre commit hook file");

    getLog().debug("Checking plugin pre commit hook file call to the main pre commit hook file");
    Path mainPreCommitHook = hooksDirectory.resolve(MAIN_PRE_COMMIT_HOOK);
    getOrCreateExecutableFile(mainPreCommitHook);
    boolean callExists =
        Files.readAllLines(mainPreCommitHook)
            .stream()
            .anyMatch(s -> s.contains(mainPreCommitHookCall()));
    if (callExists) {
      getLog().debug("Call already exists in main pre commit hook");
    } else {
      getLog().debug("No call found in the main pre commit hook.");
      getLog().debug("Appending the call.");
      Files.write(
          mainPreCommitHook,
          Collections.singletonList(mainPreCommitHookCall()),
          StandardOpenOption.APPEND);
      getLog().debug("Appended the call.");
    }
  }

  private Path getMavenExecutable() {
    Path mavenHome = Paths.get(System.getProperty(MAVEN_HOME_PROP));
    Path executable = mavenHome.resolve(BIN_MVN);
    if (!Files.exists(executable)) {
      throw new RuntimeException(
          "Could not find maven executable. " + executable + " does not exist.");
    }
    return executable;
  }

  /**
   * Get or creates the git hooks directory
   *
   * @return The git hooks directory
   */
  private Path getOrCreateHooksDirectory() {
    Repository gitRepository = git().getRepository();

    Path hooksDirectory = gitRepository.getDirectory().toPath().resolve(HOOKS_DIR);
    if (!Files.exists(hooksDirectory)) {
      getLog().debug("Creating directory " + hooksDirectory);
      try {
        Files.createDirectories(hooksDirectory);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      getLog().debug(hooksDirectory + "already exists");
    }
    return hooksDirectory;
  }

  /**
   * Get or creates a file then mark it as executable.
   *
   * @param file The file
   */
  private void getOrCreateExecutableFile(Path file) {
    if (!Files.exists(file)) {
      getLog().debug("Creating " + file);
      try {
        Files.createFile(file);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      getLog().debug(file + " already exists");
    }

    getLog().debug("Marking '" + file + "' as executable");
    Set<PosixFilePermission> permissions;
    try {
      permissions = Files.getPosixFilePermissions(file);
    } catch (UnsupportedOperationException ignored) {
      return;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    permissions.add(PosixFilePermission.OWNER_EXECUTE);
    permissions.add(PosixFilePermission.GROUP_EXECUTE);
    permissions.add(PosixFilePermission.OTHERS_EXECUTE);

    try {
      Files.setPosixFilePermissions(file, permissions);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String mainPreCommitHookCall() {
    return "./" + pluginPreCommitHook();
  }

  private String pluginPreCommitHook() {
    return baseDir().relativize(getOrCreateHooksDirectory())
        + "/"
        + artifactId()
        + "."
        + BASE_PLUGIN_PRE_COMMIT_HOOK;
  }
}
