package org.starcoin.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class FileUtil {

    public static void writeFile(String filename, byte[] data) {
        writeFile(new File(filename), data);
    }

    public static void writeFile(File file, byte[] data) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(data);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readFile(String filename) {
        return readFile(new File(filename));
    }

    public static byte[] readFile(File file) {
        try {
            int len = (int) file.length();
            byte[] data = new byte[len];
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            in.read(data);
            in.close();

            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteDir(File dir) {
        try {
            Files.walk(dir.toPath())
                    .map(Path::toFile)
                    .sorted(Comparator.reverseOrder())
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
