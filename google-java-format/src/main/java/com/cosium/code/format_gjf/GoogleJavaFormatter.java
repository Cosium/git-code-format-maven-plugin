package com.cosium.code.format_gjf;

import static java.util.Objects.requireNonNull;

import com.cosium.code.format_spi.CodeFormatter;
import com.cosium.code.format_spi.FileExtension;
import com.cosium.code.format_spi.LineRanges;
import com.google.common.collect.RangeSet;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.googlejavaformat.java.ImportOrderer;
import com.google.googlejavaformat.java.RemoveUnusedImports;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;

/**
 * @author Réda Housni Alaoui
 */
class GoogleJavaFormatter implements CodeFormatter {

  private final GoogleJavaFormatterOptions options;
  private final Formatter formatter;
  private final String sourceEncoding;

  public GoogleJavaFormatter(GoogleJavaFormatterOptions options, String sourceEncoding) {
    this.options = requireNonNull(options);
    this.formatter = new Formatter(options.javaFormatterOptions());
    this.sourceEncoding = sourceEncoding;
  }

  @Override
  public FileExtension fileExtension() {
    return FileExtension.of("java");
  }

  @Override
  public void format(InputStream content, LineRanges lineRanges, OutputStream formattedContent) {
    final String formattedContentToWrite;
    try {
      String unformattedContent = IOUtils.toString(content, sourceEncoding);
      formattedContentToWrite = doFormat(unformattedContent, lineRanges);
    } catch (IOException | FormatterException e) {
      throw new GoogleJavaFormatException(e);
    }

    try {
      IOUtils.write(formattedContentToWrite, formattedContent, sourceEncoding);
    } catch (IOException e) {
      throw new GoogleJavaFormatException(e);
    }
  }

  @Override
  public boolean validate(InputStream content) {
    try {
      String unformattedContent = IOUtils.toString(content, sourceEncoding);
      String formattedContent = doFormat(unformattedContent, LineRanges.all());
      return unformattedContent.equals(formattedContent);
    } catch (IOException | FormatterException e) {
      throw new GoogleJavaFormatException(e);
    }
  }

  private String doFormat(String unformattedContent, LineRanges lineRanges)
      throws FormatterException {
    if (options.isFixImportsOnly()) {
      if (!lineRanges.isAll()) {
        return unformattedContent;
      }
      return fixImports(unformattedContent);
    }
    if (lineRanges.isAll()) {
      return fixImports(formatter.formatSource(unformattedContent));
    }

    RangeSet<Integer> charRangeSet =
        Formatter.lineRangesToCharRanges(unformattedContent, lineRanges.rangeSet());
    return formatter.formatSource(unformattedContent, charRangeSet.asRanges());
  }

  private String fixImports(final String unformattedContent) throws FormatterException {
    String formattedContent = unformattedContent;
    if (!options.isSkipRemovingUnusedImports()) {
      formattedContent = RemoveUnusedImports.removeUnusedImports(formattedContent);
    }
    if (!options.isSkipSortingImports()) {
      formattedContent = ImportOrderer.reorderImports(formattedContent, options.javaFormatterOptions().style());
    }
    return formattedContent;
  }
}
