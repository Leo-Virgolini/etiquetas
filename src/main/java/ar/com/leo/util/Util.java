package ar.com.leo.util;

import ar.com.leo.Launcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class Util {

    public static String getJarFolder() throws URISyntaxException {
        URI uri = Launcher.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI();

        return Paths.get(uri).getParent().toString();
    }
}
