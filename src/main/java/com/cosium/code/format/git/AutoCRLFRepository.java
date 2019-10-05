package com.cosium.code.format.git;

import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.ObjectReader;

import java.io.File;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

/** @author Réda Housni Alaoui */
public class AutoCRLFRepository extends FileRepository {

  private final EolStreamType eolStreamType;

  public AutoCRLFRepository(File gitDir, EolStreamType eolStreamType) throws IOException {
    super(gitDir);
    this.eolStreamType = requireNonNull(eolStreamType);
  }

  @Override
  public ObjectReader newObjectReader() {
    return new AutoCRLFObjectReader(super.newObjectReader(), eolStreamType);
  }
}
