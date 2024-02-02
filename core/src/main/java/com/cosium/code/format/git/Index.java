package com.cosium.code.format.git;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;

/**
 * @author Réda Housni Alaoui
 */
public class Index implements AutoCloseable {

  private static final Map<File, Lock> GLOBAL_LOCK_BY_REPOSITORY_DIRECTORY =
      new ConcurrentHashMap<>();

  private final Lock globalLock;
  private final DirCache dirCache;

  private Index(Repository repository) throws IOException {
    globalLock =
        GLOBAL_LOCK_BY_REPOSITORY_DIRECTORY.computeIfAbsent(
            repository.getDirectory(), repositoryDirectory -> new ReentrantLock());
    globalLock.lock();
    try {
      dirCache = repository.lockDirCache();
    } catch (RuntimeException e) {
      globalLock.unlock();
      throw e;
    }
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
    try {
      dirCache.unlock();
    } finally {
      globalLock.unlock();
    }
  }
}
