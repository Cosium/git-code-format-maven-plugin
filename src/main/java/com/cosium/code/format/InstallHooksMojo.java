package com.cosium.code.format;

import com.cosium.code.format.executable.ExecutableManager;
import com.cosium.code.format.utils.MavenUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Path;

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

  private static final String BASE_PLUGIN_PRE_COMMIT_HOOK = "maven-git-code-format.pre-commit.sh";

  private static final String MAIN_PRE_COMMIT_HOOK = "pre-commit";

  private final ExecutableManager executableManager = new ExecutableManager(this::getLog);
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
    executableManager
        .getOrCreateExecutableScript(hooksDirectory.resolve(MAIN_PRE_COMMIT_HOOK))
        .appendCommandCall(mainPreCommitHookCall());
  }

  private void writePluginPreCommitHook(Path hooksDirectory) throws IOException {
    getLog().debug("Writing plugin pre commit hook file");
    executableManager
        .getOrCreateExecutableScript(hooksDirectory.resolve(pluginPreCommitHookFileName()))
        .truncateWithTemplate(
            () -> getClass().getResourceAsStream(BASE_PLUGIN_PRE_COMMIT_HOOK),
            mavenUtils.getMavenExecutable().toAbsolutePath());
    getLog().debug("Written plugin pre commit hook file");
  }

  private Path prepareHooksDirectory() {
    getLog().debug("Preparing git hook directory");
    Path hooksDirectory;
    hooksDirectory = getOrCreateHooksDirectory();
    getLog().debug("Prepared git hook directory");
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
