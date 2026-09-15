package com.cosium.code.format.maven;

import java.nio.file.Path;

/**
 * A maven executable candidate. Each flavour brings its own notion of validity, since running a
 * candidate to check it is not always affordable.
 *
 * @author Réda Housni Alaoui
 */
interface MavenExecutable {

  Path path();

  boolean isValid();
}
