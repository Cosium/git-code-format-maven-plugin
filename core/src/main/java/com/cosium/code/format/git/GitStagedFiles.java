package com.cosium.code.format.git;

import static java.util.Objects.requireNonNull;

import com.cosium.code.format.formatter.CodeFormatters;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.lib.Repository;

/**
 * @author Réda Housni Alaoui
 */
public class GitStagedFiles {

  private final Log log;
  private final Repository repository;
  private final Set<String> filePaths;
  private final Set<String> deletedFilePaths;

  private GitStagedFiles(
      Log log, Repository repository, Set<String> filePaths, Set<String> deletedFilePaths) {
    this.log = requireNonNull(log);
    this.repository = requireNonNull(repository);
    this.filePaths = Collections.unmodifiableSet(filePaths);
    this.deletedFilePaths = Collections.unmodifiableSet(deletedFilePaths);
  }

  public static GitStagedFiles read(Log log, Repository repository, Predicate<Path> fileFilter)
      throws GitAPIException {
    Status gitStatus = new Git(repository).status().call();
    Path workTree = repository.getWorkTree().toPath();
    Predicate<String> filter = relativePath -> fileFilter.test(workTree.resolve(relativePath));
    Set<String> filePaths =
        Stream.concat(gitStatus.getChanged().stream(), gitStatus.getAdded().stream())
            .filter(filter)
            .collect(Collectors.toSet());
    Set<String> deletedFilePaths =
        gitStatus.getRemoved().stream().filter(filter).collect(Collectors.toSet());
    log.debug("Staged files: " + filePaths);
    log.debug("Staged deleted files: " + deletedFilePaths);
    return new GitStagedFiles(log, repository, filePaths, deletedFilePaths);
  }

  /**
   * @return true if nothing is staged, in other words if committing now would record a commit
   *     holding no change
   */
  public boolean isEmpty() {
    return filePaths.isEmpty() && deletedFilePaths.isEmpty();
  }

  /** Formats the staged files, the deleted ones aside as they hold nothing left to format. */
  public void format(CodeFormatters formatters) throws IOException {
    if (filePaths.isEmpty()) {
      log.debug("No staged files to format");
      return;
    }

    List<FormattedFile> formattedFiles = new ArrayList<>();

    try (Index index = Index.lock(repository)) {
      DirCacheEditor dirCacheEditor = index.editor();
      filePaths.stream()
          .map(path -> new GitIndexEntry(log, repository, path))
          .map(indexEntry -> indexEntry.entryFormatter(formatters, formattedFiles::add))
          .forEach(dirCacheEditor::add);
      dirCacheEditor.finish();

      WorkingTree workingTree = new WorkingTree(log, repository);
      DirCacheEditor statDirCacheEditor = index.editor();
      for (FormattedFile formattedFile : formattedFiles) {
        if (!workingTree.applyFormatting(index.treeIterator(), formattedFile)) {
          continue;
        }
        // The working tree file holds the content which is now staged. Record its stat data, so
        // that git can tell the file is up to date without having to read it back.
        statDirCacheEditor.add(
            new GitIndexEntry(log, repository, formattedFile.path()).workingTreeStatRecorder());
      }
      statDirCacheEditor.finish();

      index.write();
      index.commit();
    }
  }
}
