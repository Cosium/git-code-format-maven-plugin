package com.cosium.code.format.git;

import com.cosium.code.format.MavenGitCodeFormatException;
import com.cosium.code.format.TemporaryFile;
import com.cosium.code.format.formatter.CodeFormatters;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/** @author Réda Housni Alaoui */
public class GitStagedFiles {

  private final Log log;
  private final Repository repository;
  private final Set<String> filePaths;

  private GitStagedFiles(Log log, Repository repository, Set<String> filePaths) {
    this.log = requireNonNull(log);
    this.repository = requireNonNull(repository);
    this.filePaths = Collections.unmodifiableSet(filePaths);
  }

  public static GitStagedFiles read(Log log, Repository repository, Predicate<Path> fileFilter)
      throws GitAPIException {
    Status gitStatus = new Git(repository).status().call();
    Path workTree = repository.getWorkTree().toPath();
    Set<String> filePaths =
        Stream.concat(gitStatus.getChanged().stream(), gitStatus.getAdded().stream())
            .filter(relativePath -> fileFilter.test(workTree.resolve(relativePath)))
            .collect(Collectors.toSet());
    log.debug("Staged files: " + filePaths.toString());
    return new GitStagedFiles(log, repository, filePaths);
  }

  public void format(CodeFormatters formatters) throws IOException {
    Git git = new Git(repository);
    DirCache dirCache = repository.lockDirCache();
    try (TemporaryFile temporaryDiffFile =
        TemporaryFile.create(log, "diff-between-unformatted-and-formatted-files")) {
      DirCacheEditor dirCacheEditor = dirCache.editor();
      filePaths.stream()
          .map(path -> new GitIndexEntry(log, repository, path))
          .map(indexEntry -> indexEntry.entryFormatter(formatters))
          .forEach(dirCacheEditor::add);
      dirCacheEditor.finish();

      dirCache.write();

      try (OutputStream diffOutput = temporaryDiffFile.newOutputStream()) {
        git.diff()
            .setOutputStream(diffOutput)
            .setOldTree(treeIterator(repository.readDirCache()))
            .setNewTree(treeIterator(dirCache))
            .call();
      }

      try (InputStream diffInput = temporaryDiffFile.newInputStream()) {
        git.apply().setPatch(diffInput).call();
      }

      dirCache.commit();
    } catch (GitAPIException e) {
      throw new MavenGitCodeFormatException(e);
    } finally {
      dirCache.unlock();
    }
  }

  private AbstractTreeIterator treeIterator(DirCache dirCache) {
    return new DirCacheIterator(dirCache);
  }
}
