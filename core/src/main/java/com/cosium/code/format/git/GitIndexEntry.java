package com.cosium.code.format.git;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import com.cosium.code.format.MavenGitCodeFormatException;
import com.cosium.code.format.TemporaryFile;
import com.cosium.code.format.formatter.CodeFormatters;
import com.cosium.code.format_spi.CodeFormatter;
import com.cosium.code.format_spi.FileExtension;
import com.cosium.code.format_spi.LineRanges;
import com.google.common.collect.Range;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCacheEditor.PathEdit;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.NullOutputStream;

/**
 * @author Réda Housni Alaoui
 */
class GitIndexEntry {

  private final Log log;
  private final Repository repository;
  private final String path;

  GitIndexEntry(Log log, Repository repository, String path) {
    this.log = requireNonNull(log);
    this.repository = requireNonNull(repository);
    this.path = requireNonNull(path);
  }

  PathEdit entryFormatter(CodeFormatters formatters) {
    return new EntryFormatter(log, repository, formatters, path);
  }

  private static class EntryFormatter extends PathEdit {

    private final Log log;
    private final Repository repository;
    private final CodeFormatters formatters;

    EntryFormatter(Log log, Repository repository, CodeFormatters formatters, String entryPath) {
      super(entryPath);
      this.log = log;
      this.repository = requireNonNull(repository);
      this.formatters = requireNonNull(formatters);
    }

    @Override
    public void apply(DirCacheEntry dirCacheEntry) {
      formatters
          .forFileExtension(FileExtension.parse(dirCacheEntry.getPathString()))
          .forEach(formatter -> doFormat(dirCacheEntry, formatter));
    }

    private void doFormat(DirCacheEntry dirCacheEntry, CodeFormatter formatter) {
      LineRanges lineRanges = computeLineRanges(dirCacheEntry);
      if (lineRanges.isAll()) {
        log.info("Formatting '" + dirCacheEntry.getPathString() + "'");
      } else {
        log.info("Formatting lines " + lineRanges + " of '" + dirCacheEntry.getPathString() + "'");
      }

      try (TemporaryFile temporaryFormattedFile =
          TemporaryFile.create(log, dirCacheEntry.getPathString() + ".formatted")) {
        ObjectId unformattedObjectId = dirCacheEntry.getObjectId();
        log.debug("Unformatted object id is '" + unformattedObjectId + "'");
        ObjectDatabase objectDatabase = repository.getObjectDatabase();
        ObjectLoader objectLoader = objectDatabase.open(unformattedObjectId);

        logObjectContent(objectLoader, dirCacheEntry.getPathString() + ".unformatted");

        try (InputStream content = objectLoader.openStream();
            OutputStream formattedContent = temporaryFormattedFile.newOutputStream()) {
          formatter.format(content, lineRanges, formattedContent);
        }

        long formattedSize = temporaryFormattedFile.size();
        ObjectId formattedObjectId;
        try (InputStream formattedContent = temporaryFormattedFile.newInputStream();
            ObjectInserter objectInserter = objectDatabase.newInserter()) {
          formattedObjectId = objectInserter.insert(OBJ_BLOB, formattedSize, formattedContent);
          objectInserter.flush();
        }

        log.debug("Formatted size is " + formattedSize);
        dirCacheEntry.setLength(formattedSize);
        log.debug("Formatted object id is '" + formattedObjectId + "'");
        dirCacheEntry.setObjectId(formattedObjectId);
      } catch (IOException e) {
        throw new MavenGitCodeFormatException(e);
      }

      if (lineRanges.isAll()) {
        log.info("Formatted '" + dirCacheEntry.getPathString() + "'");
      } else {
        log.info("Formatted lines " + lineRanges + " of '" + dirCacheEntry.getPathString() + "'");
      }
    }

    private void logObjectContent(ObjectLoader objectLoader, String virtualName)
        throws IOException {
      if (!log.isDebugEnabled()) {
        return;
      }

      try (InputStream input = objectLoader.openStream();
          OutputStream output = TemporaryFile.create(log, virtualName).newOutputStream()) {
        IOUtils.copy(input, output);
      }
    }

    private LineRanges computeLineRanges(DirCacheEntry dirCacheEntry) {
      try (Git git = new Git(repository)) {
        boolean partiallyStaged;
        try {
          partiallyStaged =
              !git.status().addPath(dirCacheEntry.getPathString()).call().getModified().isEmpty();
        } catch (GitAPIException e) {
          throw new MavenGitCodeFormatException(e);
        }

        if (!partiallyStaged) {
          return LineRanges.all();
        }

        try {
          return git
              .diff()
              .setPathFilter(PathFilter.create(dirCacheEntry.getPathString()))
              .setCached(true)
              .call()
              .stream()
              .map(this::computeLineRanges)
              .reduce(LineRanges::concat)
              .orElse(LineRanges.all());

        } catch (GitAPIException e) {
          throw new MavenGitCodeFormatException(e);
        }
      }
    }

    private LineRanges computeLineRanges(DiffEntry diffEntry) {
      try (DiffFormatter diffFormatter = new DiffFormatter(NullOutputStream.INSTANCE)) {
        diffFormatter.setRepository(repository);

        try {
          FileHeader fileHeader = diffFormatter.toFileHeader(diffEntry);
          return fileHeader.getHunks().stream()
              .map(HunkHeader.class::cast)
              .map(this::computeLineRanges)
              .reduce(LineRanges::concat)
              .orElse(LineRanges.all());
        } catch (IOException e) {
          throw new MavenGitCodeFormatException(e);
        }
      }
    }

    private LineRanges computeLineRanges(HunkHeader hunkHeader) {
      Set<Range<Integer>> ranges =
          hunkHeader.toEditList().stream()
              .flatMap(
                  edit ->
                      Stream.of(
                          Range.closedOpen(edit.getBeginA(), edit.getEndA()),
                          Range.closedOpen(edit.getBeginB(), edit.getEndB())))
              .filter(((Predicate<Range<Integer>>) Range::isEmpty).negate())
              .collect(Collectors.toSet());
      return LineRanges.of(ranges);
    }
  }
}
