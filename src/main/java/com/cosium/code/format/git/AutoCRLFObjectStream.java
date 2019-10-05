package com.cosium.code.format.git;

import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.util.io.EolStreamTypeUtil;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

/** @author Réda Housni Alaoui */
public class AutoCRLFObjectStream extends ObjectStream {

  private final ObjectStream delegate;
  private final InputStream autoCRLFInputStream;

  public AutoCRLFObjectStream(ObjectStream delegate, EolStreamType eolStreamType) {
    this.delegate = requireNonNull(delegate);
    this.autoCRLFInputStream =
        requireNonNull(EolStreamTypeUtil.wrapInputStream(delegate, eolStreamType));
  }

  @Override
  public int getType() {
    return delegate.getType();
  }

  @Override
  public long getSize() {
    return delegate.getSize();
  }

  @Override
  public int read() throws IOException {
    return autoCRLFInputStream.read();
  }
}
