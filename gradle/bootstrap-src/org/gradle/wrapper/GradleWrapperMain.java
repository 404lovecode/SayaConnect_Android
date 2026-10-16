package org.gradle.wrapper;

/** Minimal, transparent offline-built launcher for projects where the official wrapper JAR
 * could not be fetched at packaging time. Downloads only the official Gradle distribution
 * and checks the SHA-256 published at https://gradle.org/release-checksums/.
 * Replace with the official wrapper by running `gradle wrapper --gradle-version 8.10.2`.
 */
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

public final class GradleWrapperMain {
  private static final String URL = "https://services.gradle.org/distributions/gradle-8.10.2-bin.zip";
  private static final String SHA = "31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26";
  public static void main(String[] args) throws Exception {
    Path base = Paths.get(System.getProperty("user.home"), ".gradle", "saya-bootstrap");
    Path gradle = base.resolve("gradle-8.10.2");
    Path exec = gradle.resolve("bin").resolve(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "gradle.bat" : "gradle");
    if (!Files.exists(exec)) {
      Files.createDirectories(base);
      Path zipPath = base.resolve("gradle.zip");
      System.out.println("Fetching verified Gradle 8.10.2 from services.gradle.org …");
      URLConnection c = new URI(URL).toURL().openConnection();
      c.setConnectTimeout(20000); c.setReadTimeout(60000);
      try (InputStream in = c.getInputStream()) { Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING); }
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      try (InputStream in = Files.newInputStream(zipPath)) {
        byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
      }
      StringBuilder hash = new StringBuilder();
      for (byte x : md.digest()) hash.append(String.format("%02x", x & 255));
      if (!SHA.equals(hash.toString())) throw new SecurityException("Gradle SHA-256 mismatch. Aborting.");
      try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
          Path dest = base.resolve(entry.getName()).normalize();
          if (!dest.startsWith(base)) throw new SecurityException("Invalid ZIP entry");
          if (entry.isDirectory()) Files.createDirectories(dest);
          else {
            Files.createDirectories(dest.getParent());
            Files.copy(zip, dest, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
      Files.deleteIfExists(zipPath);
      exec.toFile().setExecutable(true);
    }
    List<String> command = new ArrayList<>();
    if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
      command.add("cmd.exe"); command.add("/c"); command.add(exec.toString());
    } else command.add(exec.toString());
    command.addAll(Arrays.asList(args));
    ProcessBuilder process = new ProcessBuilder(command).inheritIO();
    int exit = process.start().waitFor();
    System.exit(exit);
  }
}
