package com.cosium.code.format.executable;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Created on 08/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public interface Executable {

  Executable truncateWithTemplate(Supplier<InputStream> template, Object... values)
      throws IOException;

  /**
   * Appends a command call to the executable
   *
   * @param commandCall
   * @return
   * @throws IOException
   */
  Executable appendCommandCall(String commandCall) throws IOException;
}
