package io.jenkins.plugins.jfrog;

import java.io.IOException;

/**
 * @author gail
 */
public class OsUtils {
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    public static boolean isUnix() {
        return (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix"));
    }

    public static String getOsDetails() throws IOException {
        // Windows
        if (isWindows()) {
            return "windows-amd64";
        }
        // Mac
        if (isMac()) {
            if (OS_ARCH.contains("arm64")) {
                return "mac-arm64";
            }
            return "mac-386";
        }
        // Unix
        switch (OS_ARCH) {
            case ("i386"):
            case ("i486"):
            case ("i586"):
            case ("i686"):
            case ("i786"):
            case ("x86"):
                return "linux-386";
            case ("amd64"):
            case ("x86_64"):
            case ("x64"):
                return "linux-amd64";
            case ("arm"):
            case ("armv7l"):
                return "linux-arm";
            case ("aarch64"):
                return "linux-arm64";
            case ("s390x"):
                return "linux-s390x";
            case ("ppc64"):
                return "linux-ppc64";
            case ("ppc64le"):
                return "linux-ppc64le";
        }
        throw new IOException("Unsupported operating system: " + OS_ARCH);
    }
}
