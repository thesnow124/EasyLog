package com.github.easylog.util;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class EasyLogVersion {

    private EasyLogVersion() {
    }

    public static String getVersion() {
        return determineSpringBootVersion();
    }

    private static String determineSpringBootVersion() {
        final Package pkg = EasyLogVersion.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        CodeSource codeSource = EasyLogVersion.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return null;
        }
        URL codeSourceLocation = codeSource.getLocation();
        try {
            URLConnection connection = codeSourceLocation.openConnection();
            if (connection instanceof JarURLConnection) {
                return getImplementationVersion(((JarURLConnection) connection).getJarFile());
            }
            try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()))) {
                return getImplementationVersion(jarFile);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private static String getImplementationVersion(JarFile jarFile) throws IOException {
        return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION);
    }

}
