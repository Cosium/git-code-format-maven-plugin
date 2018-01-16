package com.cosium.code.format;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created on 01/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "on-pre-commit", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class OnPreCommitMojo extends AbstractModulMavenGitCodeFormatMojo {

  /** The file containing the staged files list */
  @Parameter(property = "stagedFilesFile", required = true)
  private String stagedFilesFile;

  protected void doExecute() throws MojoExecutionException {
    try {
      getLog().info("Executing pre-commit hooks");
      onPreCommit();
      getLog().info("Executed pre-commit hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void onPreCommit() throws IOException {
    getLog().debug("Staged files file is '" + stagedFilesFile + "'");

    List<String> content = Files.readAllLines(Paths.get(stagedFilesFile));
    getLog().debug("Staged files content: " + content.toString());

    content
        .stream()
        .map(StringUtils::trim)
        .filter(StringUtils::isNotBlank)
        .map(this::toPath)
        .filter(this::isEligible)
        .forEach(file -> codeFormatter().format(file));
  }

  private Path toPath(String diffPath) {
    Path workTree = gitRepository().getWorkTree().toPath();
    return workTree.resolve(diffPath);
  }

  private boolean isEligible(Path file) {
    if (!file.toAbsolutePath().toString().contains(baseDir().toAbsolutePath().toString())) {
      getLog().debug(file + " does not belong to the current project");
      return false;
    }
    return true;
  }
}
