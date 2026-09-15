package com.cosium.code.format;

import com.cosium.code.format.git.GitStagedFiles;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * @author Réda Housni Alaoui
 */
@Mojo(name = "on-pre-commit", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class OnPreCommitMojo extends AbstractModuleMavenGitCodeFormatMojo {

  /**
   * True to fail the commit when the formatting reverts all the staged changes, leaving nothing to
   * commit. Git checks that a commit holds something before it runs the hook and never looks again,
   * so it records an empty commit instead of refusing it. Failing the commit is the only thing a
   * hook can do about it, and it applies to '--allow-empty' as well: a hook receives no argument,
   * and the environment git gives it is the same whether or not the option was passed.
   */
  @Parameter(property = "gcf.failOnEmptyCommit", defaultValue = "false")
  private boolean failOnEmptyCommit;

  protected void doExecute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Executing pre-commit hooks");
    onPreCommit();
    getLog().info("Executed pre-commit hooks");
  }

  private void onPreCommit() throws MojoExecutionException, MojoFailureException {
    boolean raiseEmptyCommitError;
    try {
      Repository repository = gitRepository();
      // Left to false when the option is off, sparing the scan of the repository it costs.
      boolean doFailOnEmptyCommit = failOnEmptyCommit && !isNextCommitEmpty(repository);

      GitStagedFiles.read(getLog(), repository, this::isFormattable)
          .format(collectCodeFormatters());

      raiseEmptyCommitError = doFailOnEmptyCommit && isNextCommitEmpty(repository);
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }

    if (!raiseEmptyCommitError) {
      return;
    }
    throw new MojoFailureException(
        "The formatting reverted all the staged changes: there is nothing left to commit. Failing"
            + " the commit as requested by the 'failOnEmptyCommit' option.");
  }

  /**
   * @return true if committing now would record a commit holding no change
   */
  private boolean isNextCommitEmpty(Repository repository) throws IOException, GitAPIException {
    if (repository.readMergeHeads() != null) {
      // Git lets a merge commit hold no change, as the merge itself is the information. Its own
      // empty commit check is skipped in that situation, so do not stand in its way either.
      return false;
    }
    return GitStagedFiles.read(getLog(), repository, path -> true).isEmpty();
  }

  private boolean isFormattable(Path path) {
    return sourceDirs().stream().anyMatch(path::startsWith);
  }
}
