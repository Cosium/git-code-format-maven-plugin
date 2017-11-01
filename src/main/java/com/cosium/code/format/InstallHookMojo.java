package com.cosium.code.format;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

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
 * Installs pre commit hook on each initialization. The hook is always overriden to update eventual
 * changes in:
 *
 * <ul>
 *   <li>maven installation
 *   <li>plugin structure
 * </ul>
 */
@Mojo(name = "install-hook", defaultPhase = LifecyclePhase.INITIALIZE)
public class InstallHookMojo extends AbstractMojo {

  private static final String SHIBANG = "#!/bin/bash";
  private static final String HOOKS_DIR = "hooks";

  private static final String PLUGIN_PRE_COMMIT_HOOK = "maven-git-code-format.sh";
  private static final String PLUGIN_PRE_COMMIT_COMMAND_ARGS = "gcf:on-pre-commit";

  private static final String MAIN_PRE_COMMIT_HOOK = "pre-commit";
  private static final String MAIN_PRE_COMMIT_HOOK_CALL = "./" + PLUGIN_PRE_COMMIT_HOOK;
  public static final String MAVEN_HOME_PROP = "maven.home";

  @Parameter(readonly = true, defaultValue = "${project}")
  private MavenProject currentProject;

  public void execute() throws MojoExecutionException {
    try {
      doExecute();
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
    Path pluginPreCommitHook = hooksDirectory.resolve(PLUGIN_PRE_COMMIT_HOOK);
    getOrCreateExecutableFile(pluginPreCommitHook);
    Files.write(
        pluginPreCommitHook,
        Arrays.asList(
            SHIBANG,
            getMavenExecutable().toAbsolutePath() + " " + PLUGIN_PRE_COMMIT_COMMAND_ARGS),
        StandardOpenOption.TRUNCATE_EXISTING);
    getLog().debug("Written plugin pre commit hook file");

    getLog().debug("Checking plugin pre commit hook file call to the main pre commit hook file");
    Path mainPreCommitHook = hooksDirectory.resolve(MAIN_PRE_COMMIT_HOOK);
    getOrCreateExecutableFile(mainPreCommitHook);
    boolean callExists =
        Files.readAllLines(mainPreCommitHook)
            .stream()
            .anyMatch(s -> s.contains(MAIN_PRE_COMMIT_HOOK_CALL));
    if (callExists) {
      getLog().debug("Call already exists in main pre commit hook");
    } else {
      getLog().debug("No call found in the main pre commit hook.");
      getLog().debug("Appending the call.");
      Files.write(
          mainPreCommitHook,
          Collections.singletonList(MAIN_PRE_COMMIT_HOOK_CALL),
          StandardOpenOption.APPEND);
      getLog().debug("Appended the call.");
    }
  }

  private Path getMavenExecutable() {
    Path mavenHome = Paths.get(System.getProperty(MAVEN_HOME_PROP));
    Path executable = mavenHome.resolve("bin/mvn");
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
    Repository gitRepository;
    try {
      gitRepository = new FileRepositoryBuilder().findGitDir(currentProject.getBasedir()).build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

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
}
