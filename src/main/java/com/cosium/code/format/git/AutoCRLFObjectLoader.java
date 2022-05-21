package com.cosium.code.format.git;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.CoreConfig.EolStreamType;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.util.io.EolStreamTypeUtil;

/** @author Réda Housni Alaoui */
public class AutoCRLFObjectLoader extends ObjectLoader {

  private final ObjectLoader delegate;
  private final EolStreamType eolStreamType;
  private Long cachedSize;

  public AutoCRLFObjectLoader(ObjectLoader delegate, EolStreamType eolStreamType) {
    this.delegate = requireNonNull(delegate);
    this.eolStreamType = requireNonNull(eolStreamType);
  }

  @Override
  public int getType() {
    return delegate.getType();
  }

  @Override
  public long getSize() {
    // https://github.com/Cosium/git-code-format-maven-plugin/issues/42: It is very important to
    // return the exact transformed content size
    if (cachedSize != null) {
      return cachedSize;
    }
    try (CountingInputStream countingInputStream = new CountingInputStream(openStream())) {
      while (countingInputStream.read() != -1) {
        // Do nothing in the while, we are just moving bytes through CountingInputstream to retrieve
        // the total stream size
      }
      cachedSize = countingInputStream.getByteCount();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return cachedSize;
  }

  @Override
  public boolean isLarge() {
    return delegate.isLarge();
  }

  @Override
  public byte[] getCachedBytes() throws LargeObjectException {
    return convertBytes(delegate.getCachedBytes());
  }

  @Override
  public byte[] getCachedBytes(int sizeLimit)
      throws LargeObjectException, MissingObjectException, IOException {
    return convertBytes(delegate.getCachedBytes(sizeLimit));
  }

  private byte[] convertBytes(byte[] bytes) {
    try {
      return IOUtils.toByteArray(
          EolStreamTypeUtil.wrapInputStream(new ByteArrayInputStream(bytes), eolStreamType));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public ObjectStream openStream() throws MissingObjectException, IOException {
    return new AutoCRLFObjectStream(delegate.openStream(), eolStreamType);
  }

  @Override
  public void copyTo(OutputStream out) throws MissingObjectException, IOException {
    delegate.copyTo(EolStreamTypeUtil.wrapOutputStream(out, eolStreamType));
  }
}
