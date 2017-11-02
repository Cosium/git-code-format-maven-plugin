package com.cosium.code.format;

import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Created on 02/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class ExecutableUtils {

    private static final String SHIBANG = "#!/bin/bash";

    private final Supplier<Log> log;

    public ExecutableUtils(Supplier<Log> log){
        requireNonNull(log);
        this.log = log;
    }

    /**
     * Get or creates a file then mark it as executable.
     *
     * @param file The file
     */
    public void getOrCreateExecutableScript(Path file) {
        if (!Files.exists(file)) {
            log.get().debug("Creating " + file);
            try {
                Files.createFile(file);
                Files.write(file, Collections.singleton(SHIBANG), StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log.get().debug(file + " already exists");
        }

        log.get().debug("Marking '" + file + "' as executable");
        Set<PosixFilePermission> permissions;
        try {
            permissions = Files.getPosixFilePermissions(file);
        } catch (UnsupportedOperationException ignored) {
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        permissions.add(PosixFilePermission.GROUP_EXECUTE);
        permissions.add(PosixFilePermission.OTHERS_EXECUTE);

        try {
            Files.setPosixFilePermissions(file, permissions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
