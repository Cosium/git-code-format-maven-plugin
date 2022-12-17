package com.cosium.code.format;

import com.cosium.code.format.formatter.GoogleJavaFormatterOptions;
import org.apache.maven.plugins.annotations.Parameter;

/** @author Réda Housni Alaoui */
public class MavenGoogleJavaFormatOptions {

  @Parameter private boolean aosp;
  @Parameter private boolean fixImportsOnly;
  @Parameter private boolean skipSortingImports;
  @Parameter private boolean skipRemovingUnusedImports;

  public GoogleJavaFormatterOptions toFormatterOptions() {
    return new GoogleJavaFormatterOptions(
        aosp, fixImportsOnly, skipSortingImports, skipRemovingUnusedImports);
  }

  public void setAosp(boolean aosp) {
    this.aosp = aosp;
  }

  public void setFixImportsOnly(boolean fixImportsOnly) {
    this.fixImportsOnly = fixImportsOnly;
  }

  public void setSkipSortingImports(boolean skipSortingImports) {
    this.skipSortingImports = skipSortingImports;
  }

  public void setSkipRemovingUnusedImports(boolean skipRemovingUnusedImports) {
    this.skipRemovingUnusedImports = skipRemovingUnusedImports;
  }
}
