package com.cosium.code.format.formatter;

import static java.util.Objects.requireNonNull;

import com.cosium.code.format.FileExtension;
import com.cosium.code.format.MavenGitCodeFormatException;
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
 * Created on 07/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class GoogleJavaFormatter implements CodeFormatter {

  private final GoogleJavaFormatterOptions options;
  private final Formatter formatter;

  public GoogleJavaFormatter(GoogleJavaFormatterOptions options) {
    this.options = requireNonNull(options);
    this.formatter = new Formatter(options.javaFormatterOptions());
  }

  @Override
  public FileExtension fileExtension() {
    return FileExtension.of("java");
  }

  @Override
  public void format(InputStream content, LineRanges lineRanges, OutputStream formattedContent) {
    final String formattedContentToWrite;
    try {
      String unformattedContent = IOUtils.toString(content, "UTF-8");
      formattedContentToWrite = doFormat(unformattedContent, lineRanges);
    } catch (IOException | FormatterException e) {
      throw new MavenGitCodeFormatException(e);
    }

    try {
      IOUtils.write(formattedContentToWrite, formattedContent);
    } catch (IOException e) {
      throw new MavenGitCodeFormatException(e);
    }
  }

  @Override
  public boolean validate(InputStream content) {
    try {
      String unformattedContent = IOUtils.toString(content);
      String formattedContent = doFormat(unformattedContent, LineRanges.all());
      return unformattedContent.equals(formattedContent);
    } catch (IOException | FormatterException e) {
      throw new MavenGitCodeFormatException(e);
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
      formattedContent = ImportOrderer.reorderImports(formattedContent);
    }
    return formattedContent;
  }
}
