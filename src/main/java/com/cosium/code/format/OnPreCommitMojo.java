package com.cosium.code.format;

import com.cosium.code.format.git.GitStagedFiles;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Created on 01/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
@Mojo(name = "on-pre-commit", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class OnPreCommitMojo extends AbstractModulMavenGitCodeFormatMojo {

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
    GitStagedFiles.read(getLog(), gitRepository(), path -> path.startsWith(baseDir()))
        .format(codeFormatters());
  }
}
