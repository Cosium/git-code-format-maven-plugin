package com.cosium.code.format.formatter;

import com.cosium.code.format.FileExtension;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public interface CodeFormatter {

  FileExtension fileExtension();

  void format(InputStream content, LineRanges lineRanges, OutputStream formattedContent);

  boolean validate(InputStream content);
}
