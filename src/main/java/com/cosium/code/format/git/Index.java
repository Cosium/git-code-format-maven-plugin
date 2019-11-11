package com.cosium.code.format.git;

import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;

import java.io.IOException;

/** @author Réda Housni Alaoui */
public class Index implements AutoCloseable {

  private final DirCache dirCache;

  private Index(Repository repository) throws IOException {
    dirCache = repository.lockDirCache();
  }

  public static Index lock(Repository repository) throws IOException {
    return new Index(repository);
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
