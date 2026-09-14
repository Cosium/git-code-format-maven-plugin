package com.cosium.code.format.git;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jgit.lib.Constants.ATTR_FILTER_TYPE_CLEAN;
import static org.eclipse.jgit.lib.Constants.ATTR_FILTER_TYPE_SMUDGE;
import static org.eclipse.jgit.lib.Constants.OBJ_BLOB;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk.OperationType;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.util.io.EolStreamTypeUtil;

/**
 * Reports the formatting performed on the git index back to the working tree.
 *
 * <p>The formatting is applied as a three way merge between the content which was staged, the
 * content of the working tree and the content which is now staged. A plain patch application cannot
 * be used here: a partially staged file has a working tree content which differs from the staged
 * one, so a patch computed between two index states has no reason to apply to it.
 *
 * @author Réda Housni Alaoui
 */
class WorkingTree {

  private static final List<String> CONTENT_FILTER_TYPES =
      Arrays.asList(ATTR_FILTER_TYPE_CLEAN, ATTR_FILTER_TYPE_SMUDGE);

  private final Log log;
  private final Repository repository;

  WorkingTree(Log log, Repository repository) {
    this.log = requireNonNull(log);
    this.repository = requireNonNull(repository);
  }

  /**
   * @param dirCacheIterator an iterator over the freshly formatted index. A new one is required for
   *     each call as it is consumed by the underlying tree walk.
   */
  void applyFormatting(DirCacheIterator dirCacheIterator, FormattedFile formattedFile)
      throws IOException {
    String path = formattedFile.path();
    Path file = repository.getWorkTree().toPath().resolve(path);
    if (!Files.isRegularFile(file)) {
      log.debug("'" + path + "' has no working tree file to update");
      return;
    }

    Conversion conversion = detectConversion(dirCacheIterator, path);
    if (conversion == null) {
      return;
    }
    log.debug("'" + path + "' conversion is " + conversion);

    byte[] workingTreeContent = read(file, conversion.checkIn);

    if (isBlob(workingTreeContent, formattedFile.unformattedObjectId())) {
      // The file is fully staged: the working tree holds exactly what has just been formatted.
      // Stream the formatted blob into it instead of loading it, as nothing has to be merged.
      copyBlob(formattedFile.formattedObjectId(), file, conversion.checkOut);
      return;
    }

    // Only a partially staged file needs the three contents in memory at once: MergeAlgorithm
    // works on RawText, which is a random access sequence of lines and cannot be streamed.
    byte[] merged =
        merge(
            path,
            readBlob(formattedFile.unformattedObjectId()),
            workingTreeContent,
            readBlob(formattedFile.formattedObjectId()));
    if (merged == null) {
      return;
    }
    if (Arrays.equals(merged, workingTreeContent)) {
      log.debug("Working tree content of '" + path + "' is already up to date");
      return;
    }

    write(file, conversion.checkOut, merged);
  }

  /**
   * @return how the content of the file at the given path is converted, or null if the file must
   *     be left alone because a content filter is configured on it.
   */
  private Conversion detectConversion(DirCacheIterator dirCacheIterator, String path)
      throws IOException {
    try (TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.setOperationType(OperationType.CHECKIN_OP);
      FileTreeIterator workingTreeIterator = new FileTreeIterator(repository);
      treeWalk.addTree(workingTreeIterator);
      workingTreeIterator.setDirCacheIterator(treeWalk, treeWalk.addTree(dirCacheIterator));
      treeWalk.setFilter(PathFilterGroup.createFromStrings(path));
      treeWalk.setRecursive(true);

      if (!treeWalk.next()) {
        // The file is not walked, so no conversion applies to it.
        return new Conversion(null, null);
      }

      // A clean filter turns the working tree content of a file into the content git stores in
      // the index. The smudge filter performs the opposite conversion when the file is written
      // back to the working tree. Both are declared through the 'filter' gitattribute and are
      // arbitrary external commands, git-lfs being the best known one: it cleans a large file
      // into a small pointer and smudges that pointer back into the large file.
      // When such a filter is configured, the bytes on disk are not the bytes of the blob, so the
      // formatted blob cannot be written to the working tree without running the filter. Leave
      // the working tree alone rather than write a wrongly converted content to it.
      for (String filterType : CONTENT_FILTER_TYPES) {
        String filterCommand = treeWalk.getFilterCommand(filterType);
        if (filterCommand == null) {
          continue;
        }
        log.warn(
            "'"
                + path
                + "' is subject to the "
                + filterType
                + " filter '"
                + filterCommand
                + "'. Its staged content was formatted, its working tree content was left as you"
                + " wrote it. Run the 'format-code' goal to format it.");
        return null;
      }

      return new Conversion(
          treeWalk.getEolStreamType(OperationType.CHECKIN_OP),
          treeWalk.getEolStreamType(OperationType.CHECKOUT_OP));
    }
  }

  /**
   * @return the merged content, or null if the formatting conflicts with the unstaged changes.
   */
  private byte[] merge(String path, byte[] base, byte[] ours, byte[] theirs) throws IOException {
    MergeResult<RawText> mergeResult =
        new MergeAlgorithm()
            .merge(
                RawTextComparator.DEFAULT,
                new RawText(base),
                new RawText(ours),
                new RawText(theirs));

    if (mergeResult.containsConflicts()) {
      log.warn(
          "The formatting of '"
              + path
              + "' could not be merged into its unstaged changes. Its staged content was"
              + " formatted, its working tree content was left as you wrote it. Nothing has to be"
              + " done: committing the remaining changes will format them as well, or run the"
              + " 'format-code' goal to format them right away.");
      return null;
    }

    ByteArrayOutputStream mergedContent = new ByteArrayOutputStream();
    new MergeFormatter()
        .formatMerge(
            mergedContent,
            mergeResult,
            Arrays.asList("BASE", "OURS", "THEIRS"),
            StandardCharsets.UTF_8);
    return mergedContent.toByteArray();
  }

  /** @return true if the given content is exactly the blob designated by the given id. */
  private boolean isBlob(byte[] content, ObjectId objectId) {
    try (ObjectInserter.Formatter formatter = new ObjectInserter.Formatter()) {
      return formatter.idFor(OBJ_BLOB, content).equals(objectId);
    }
  }

  private byte[] readBlob(ObjectId objectId) throws IOException {
    return repository.getObjectDatabase().open(objectId, OBJ_BLOB).getBytes();
  }

  private void copyBlob(ObjectId objectId, Path file, EolStreamType checkOutStreamType)
      throws IOException {
    try (OutputStream output =
        EolStreamTypeUtil.wrapOutputStream(Files.newOutputStream(file), checkOutStreamType)) {
      repository.getObjectDatabase().open(objectId, OBJ_BLOB).copyTo(output);
    }
  }

  private byte[] read(Path file, EolStreamType checkInStreamType) throws IOException {
    try (InputStream content =
        EolStreamTypeUtil.wrapInputStream(Files.newInputStream(file), checkInStreamType)) {
      return IOUtils.toByteArray(content);
    }
  }

  private void write(Path file, EolStreamType checkOutStreamType, byte[] content)
      throws IOException {
    try (OutputStream output =
        EolStreamTypeUtil.wrapOutputStream(Files.newOutputStream(file), checkOutStreamType)) {
      output.write(content);
    }
  }

  /** How the content of a blob and the content of its working tree file relate to each other. */
  private static class Conversion {

    private final EolStreamType checkIn;
    private final EolStreamType checkOut;

    Conversion(EolStreamType checkIn, EolStreamType checkOut) {
      this.checkIn = orDirect(checkIn);
      this.checkOut = orDirect(checkOut);
    }

    private static EolStreamType orDirect(EolStreamType eolStreamType) {
      if (eolStreamType == null) {
        return EolStreamType.DIRECT;
      }
      return eolStreamType;
    }

    @Override
    public String toString() {
      return "check-in '" + checkIn + "', check-out '" + checkOut + "'";
    }
  }
}
