package org.example;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Main {
    public static void main(String[] args) throws IOException, WriterException {
        List<String> lines = Files.readAllLines(Path.of("C:\\Users\\abba\\IdeaProjects\\convert_files\\input\\input.txt"));
        Map<String, String> userConfigs = getStringStringMap(lines);
        Map<String, Map<String, String>> unionConfigs = new HashMap<>();
        String userName = "";
        for (Map.Entry<String, String> kv : userConfigs.entrySet()) {
            String name = kv.getKey();
            userName = name.split("-")[0];
            unionConfigs.putIfAbsent(userName, new HashMap<>());
            unionConfigs.get(userName).put(name, kv.getValue());
        }


        Path outputDir = Path.of("configs");
        if (!Files.exists(outputDir)) {
            Files.createDirectory(outputDir);
        }
        for (Map.Entry<String, Map<String, String>> kv : unionConfigs.entrySet()) {

            Path userFolder = Path.of(outputDir.toString(), kv.getKey());
            if (!Files.exists(userFolder)) {
                Files.createDirectory(userFolder);
            }
            for (Map.Entry<String, String> ckv : kv.getValue().entrySet()) {
                Path configFileName = Path.of(userFolder.toString(), ckv.getKey() + ".conf");
                Path qrFileName = Path.of(userFolder.toString(), ckv.getKey() + ".png");
                generateQRCode(qrFileName, ckv.getValue(), 500, 500);
                generateConfigFile(configFileName, ckv.getValue());
            }
            String zipFilePath = Path.of(outputDir.toString(), kv.getKey() + ".zip").toString();
            zipFolder(userFolder.toString(), zipFilePath);
            deleteDirectoryStream(userFolder);
        }


    }

    public static void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder()) // Sort in reverse order to delete inner elements first
                .map(Path::toFile)
                .forEach(file -> {
                    if (!file.delete()) {
                        System.err.println("Failed to delete: " + file);
                    }
                });
        Files.deleteIfExists(path);
    }

    private static Map<String, String> getStringStringMap(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> userConfig = new HashMap<>();
        String key = "";
        for (String line : lines) {
            if (line.contains("[Interface]")) {
                if (!sb.isEmpty()) {
                    userConfig.putIfAbsent(key, sb.toString());
                    sb.setLength(0);
                    key = "";
                }
            } else if (line.contains("#")) {
                key = line.substring(1);


            }
            sb.append(line);
            sb.append("\n");
        }
        if (!sb.isEmpty()) {
            userConfig.putIfAbsent(key, sb.toString());
            sb.setLength(0);
        }
        return userConfig;
    }

    public static void generateQRCode(Path name, String data, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", name);
    }

    public static void generateConfigFile(Path name, String data) throws IOException {
        Files.writeString(name, data);
    }

    public static void zipFolder(String sourceFolder, String zipFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        File folder = new File(sourceFolder);
        addFolderToZip(folder, folder.getName(), zos);

        zos.close();
        fos.close();
    }

    private static void addFolderToZip(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                // If it's a directory, create a new ZipEntry for it and recursively call addFolderToZip
                String entryName = parentFolder + File.separator + file.getName() + File.separator;
                zos.putNextEntry(new ZipEntry(entryName));
                addFolderToZip(file, parentFolder + File.separator + file.getName(), zos);
            } else {
                // If it's a file, add it to the zip
                FileInputStream fis = new FileInputStream(file);
                String entryName = parentFolder + File.separator + file.getName();
                zos.putNextEntry(new ZipEntry(entryName));

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
            }
        }
    }
}