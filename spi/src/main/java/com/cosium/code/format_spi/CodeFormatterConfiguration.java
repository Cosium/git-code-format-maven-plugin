package com.cosium.code.format_spi;

import java.util.Optional;

/**
 * @author Réda Housni Alaoui
 */
public interface CodeFormatterConfiguration {

  Optional<String> getValue(String key);
}
