package com.cosium.code.format.git;

import static java.util.Objects.requireNonNull;

import org.eclipse.jgit.lib.ObjectId;

/**
 * A staged file which has just been formatted in the git index.
 *
 * @author Réda Housni Alaoui
 */
class FormattedFile {

  private final String path;
  private final ObjectId unformattedObjectId;
  private final ObjectId formattedObjectId;

  FormattedFile(String path, ObjectId unformattedObjectId, ObjectId formattedObjectId) {
    this.path = requireNonNull(path);
    this.unformattedObjectId = requireNonNull(unformattedObjectId);
    this.formattedObjectId = requireNonNull(formattedObjectId);
  }

  String path() {
    return path;
  }

  /** The content which was staged before the formatting took place. */
  ObjectId unformattedObjectId() {
    return unformattedObjectId;
  }

  /** The content which is now staged. */
  ObjectId formattedObjectId() {
    return formattedObjectId;
  }
}
