package com.cosium.code.format;

import com.cosium.code.format.git.GitStagedFiles;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * @author Réda Housni Alaoui
 */
@Mojo(name = "on-pre-commit", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class OnPreCommitMojo extends AbstractModuleMavenGitCodeFormatMojo {

  protected void doExecute() throws MojoExecutionException {
    try {
      getLog().info("Executing pre-commit hooks");
      onPreCommit();
      getLog().info("Executed pre-commit hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void onPreCommit() throws IOException, GitAPIException {
    GitStagedFiles.read(getLog(), gitRepository(), this::isFormattable).format(codeFormatters());
  }

  private boolean isFormattable(Path path) {
    return sourceDirs().stream().anyMatch(path::startsWith);
  }
}
