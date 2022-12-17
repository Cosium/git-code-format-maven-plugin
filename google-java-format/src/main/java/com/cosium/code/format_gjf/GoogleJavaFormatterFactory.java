package com.cosium.code.format_gjf;

import com.cosium.code.format_spi.CodeFormatter;
import com.cosium.code.format_spi.CodeFormatterConfiguration;
import com.cosium.code.format_spi.CodeFormatterFactory;

/**
 * @author Réda Housni Alaoui
 */
public class GoogleJavaFormatterFactory implements CodeFormatterFactory {
  @Override
  public String configurationId() {
    return "googleJavaFormat";
  }

  @Override
  public CodeFormatter build(CodeFormatterConfiguration configuration, String sourceEncoding) {

    return new GoogleJavaFormatter(new GoogleJavaFormatterOptions(configuration), sourceEncoding);
  }
}
