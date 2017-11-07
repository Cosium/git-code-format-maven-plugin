package com.cosium.code.format;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

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

  private static final String HOOKS_DIR = "hooks";

  private static final String BASE_PLUGIN_PRE_COMMIT_HOOK = "maven-git-code-format.pre-commit.sh";

  private static final String MAIN_PRE_COMMIT_HOOK = "pre-commit";
  private static final String MAVEN_EXECUTABLE_KEY = "maven.executable";

  private final ExecutableUtils executableUtils = new ExecutableUtils(this::getLog);
  private final MavenUtils mavenUtils = new MavenUtils(this::getLog);

  public void execute() throws MojoExecutionException {
    if (!isExecutionRoot()) {
      getLog().debug("Not in execution root. Do not execute.");
      return;
    }

    try {
      getLog().info("Installing git hooks");
      doExecute();
      getLog().info("Installed git hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void doExecute() throws IOException {
    Path hooksDirectory = prepareHooksDirectory();

    writePluginPreCommitHook(hooksDirectory);

    configureMainPreCommitHook(hooksDirectory);
  }

  private void configureMainPreCommitHook(Path hooksDirectory) throws IOException {
    getLog().debug("Checking plugin pre commit hook file call to the main pre commit hook file");
    Path mainPreCommitHook = hooksDirectory.resolve(MAIN_PRE_COMMIT_HOOK);
    executableUtils.getOrCreateExecutableScript(mainPreCommitHook);
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

  private void writePluginPreCommitHook(Path hooksDirectory) throws IOException {
    getLog().debug("Writing plugin pre commit hook file");
    Path pluginPreCommitHook = hooksDirectory.resolve(pluginPreCommitHookFileName());
    executableUtils.getOrCreateExecutableScript(pluginPreCommitHook);
    try (InputStream inputStream = getClass().getResourceAsStream(BASE_PLUGIN_PRE_COMMIT_HOOK)) {
      String rawContent = IOUtils.toString(inputStream);
      String content = String.format(rawContent, mavenUtils.getMavenExecutable().toAbsolutePath());
      Files.write(pluginPreCommitHook, content.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    getLog().debug("Written plugin pre commit hook file");
  }

  private Path prepareHooksDirectory() {
    getLog().debug("Preparing git hook directory");
    Path hooksDirectory;
    hooksDirectory = getOrCreateHooksDirectory();
    getLog().debug("Prepared git hook directory");
    return hooksDirectory;
  }

  /**
   * Get or creates the git hooks directory
   *
   * @return The git hooks directory
   */
  private Path getOrCreateHooksDirectory() {
    Path hooksDirectory = gitRepository().getDirectory().toPath().resolve(HOOKS_DIR);
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

  private String mainPreCommitHookCall() {
    return "./"
        + baseDir().relativize(getOrCreateHooksDirectory())
        + "/"
        + pluginPreCommitHookFileName();
  }

  private String pluginPreCommitHookFileName() {
    return artifactId() + "." + BASE_PLUGIN_PRE_COMMIT_HOOK;
  }
}
