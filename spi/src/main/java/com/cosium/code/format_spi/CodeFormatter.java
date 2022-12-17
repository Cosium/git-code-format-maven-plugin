package com.cosium.code.format_spi;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Réda Housni Alaoui
 */
public interface CodeFormatter {

  /**
   * @return The file extension supported by this formatter.
   */
  FileExtension fileExtension();

  /**
   * Formats a content.
   *
   * <p>The formatter SHOULD strive to format only lines part of the provided line ranges.
   *
   * @param content The content to format.
   * @param lineRanges The line ranges to format.
   * @param formattedContent The {@link OutputStream} to write the formatted content to.
   */
  void format(InputStream content, LineRanges lineRanges, OutputStream formattedContent);

  /**
   * @return true if the provided content is correctly formatted. false otherwise.
   */
  boolean validate(InputStream content);
}
