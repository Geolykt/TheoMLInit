package de.geolykt.theomlinit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class TheoMLInit {

    private static final String SL_VERSION = "20221221";
    private static final String SL_JAR_REMOTE = "https://geolykt.de/maven/de/geolykt/starloader/launcher/" + SL_VERSION + "/launcher-" + SL_VERSION + "-all.jar";
    private static final String SL_JAR_LOCAL = "launcher-" + SL_VERSION + "-all.jar";

    public static void main(String[] args) throws MalformedURLException {
        if (args.length != 0) {
            Path p = Path.of(SL_JAR_LOCAL);
            if (Files.notExists(p)) {
                System.err.println("[TheoMLInit] I was unable to find the file " + p.toAbsolutePath() + ". Do note that the Installer itself should not be called with arguments, as those are required for unrelated hacky workarounds.");
                return;
            }
            try {
                String[] lines = {
                        "{",
                        "  \"__comment\": \"This is an automatically generated file. It has no effect on runtime.\",",
                        "  \"classPath\": [",
                        "    \"" + p + "\",",
                        "    \"TheoTown66.lby\"",
                        "  ]",
                        "}"
                };
                Files.write(Path.of("config.json"), Arrays.asList(lines), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            URL[] urls = new URL[1];
            urls[0] = p.toAbsolutePath().toUri().toURL();
            try {
                System.out.println("[TheoMLInit] Preparing classloaders");
                @SuppressWarnings("resource") // The Classloader should live as long as need be
                URLClassLoader urlCl = new URLClassLoader("TheoMLInit", urls, TheoMLInit.class.getClassLoader());
                Class<?> c = urlCl.loadClass("de.geolykt.starloader.launcher.CLILauncher");
                System.out.println("[TheoMLInit] Invoking starloader main...");
                c.getMethod("main", String[].class).invoke(null, new Object[] {args});
                System.out.println("[TheoMLInit] Exited main method");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        Path gameDir;
        if (System.getProperty("game_dir") != null) {
            gameDir = Paths.get(System.getProperty("game_dir"));
        } else {
            gameDir = Utils.getGameDir(Utils.STEAM_APPNAME).toPath();
        }
        if (gameDir == null) {
            System.err.println("I was unable to locate your Game directory. You may wish to pass it explicitly through the \"game_dir\" system property.");
            return;
        } else {
            System.out.println("Located Theotown directory at: " + gameDir);
        }

        Path theotown64 = gameDir.resolve("TheoTown64");
        if (Files.notExists(theotown64)) {
            System.err.println("I was unable to locate the TheoTown64 executable (expected to be at " + theotown64.toAbsolutePath() + "). Are you sure that you are running the installer under linux? Windows and mac are not supported.");
            return;
        }

        System.out.println("Transforming " + theotown64.toAbsolutePath());

        try {
            byte[] data = Files.readAllBytes(theotown64);
            boolean found = false;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == 'T' && data[i + 1] == 'h') {
                    String s = new String(data, i, "TheoTown66.lby".length(), StandardCharsets.US_ASCII);
                    if (s.equals("TheoTown66.lby")) {
                        byte[] replaceBytes = "TheoMLInit.jar".getBytes(StandardCharsets.US_ASCII);
                        System.arraycopy(replaceBytes, 0, data, i, replaceBytes.length);
                        found = true;
                        break;
                    }
                }
            }
            URL self = TheoMLInit.class.getProtectionDomain().getCodeSource().getLocation();
            Path theoMLInit = gameDir.resolve("TheoMLInit.jar");
            System.out.println("Copying " + self + " to " + theoMLInit);
            Files.copy(self.openStream(), theoMLInit, StandardCopyOption.REPLACE_EXISTING);
            Path starloader = gameDir.resolve(SL_JAR_LOCAL);
            URL starloaderRemote = new URL(SL_JAR_REMOTE);
            System.out.println("Copying " + starloaderRemote + " to " + starloader);
            Files.copy(starloaderRemote.openStream(), starloader, StandardCopyOption.REPLACE_EXISTING);
            if (!found) {
                System.err.println("I was unable to find the String \"TheoTown66.lby\" in the executable file located at " + theotown64.toAbsolutePath() + ". Perhaps you already ran this application?");
                return;
            }
            Files.write(theotown64, data);
            System.out.println("Done.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
