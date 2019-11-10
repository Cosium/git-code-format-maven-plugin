package com.cosium.code.format.git;

import com.cosium.code.format.MavenGitCodeFormatException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;

import java.io.IOException;

/** @author Réda Housni Alaoui */
public class Index implements AutoCloseable {

  private final Log log;
  private final DirCache dirCache;

  private Index(Log log, Repository repository) throws IOException {
    this.log = log;
    try {
      dirCache = repository.lockDirCache();
    } catch (LockFailedException e) {
      throw new MavenGitCodeFormatException(
          "Could not lock .git/index.\n"
              + "Make sure to use 'git add . && git commit -m \"Commit message\"' instead of 'git commit -am \"Commit message\"'.\n"
              + "Take a look at https://github.com/Cosium/maven-git-code-format/issues/22#issuecomment-552183050 for more information.",
          e);
    }
  }

  public static Index lock(Log log, Repository repository) throws IOException {
    return new Index(log, repository);
  }

  public DirCacheEditor editor() {
    return dirCache.editor();
  }

  public void write() throws IOException {
    dirCache.write();
  }

  public void commit() {
    dirCache.commit();
  }

  public AbstractTreeIterator treeIterator() {
    return new DirCacheIterator(dirCache);
  }

  @Override
  public void close() {
    dirCache.unlock();
  }
}
