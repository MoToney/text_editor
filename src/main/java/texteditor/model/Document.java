package texteditor.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Document {

    public static void printFile(String file) {
        Path filePath = Path.of(file);
        try {
            List<String> lines = Files.readAllLines(filePath);

            for (String line : lines) {
                System.out.println(line);
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public static void writeFile(String file) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Now type");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            String line;
            while (true) {
                line = scanner.nextLine();
                if (line.equalsIgnoreCase("DONE")) {
                    break;
                }
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Input sucessfully written to test file");
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } finally {
            printFile(file);
            scanner.close();
        }
    }

    public static void main(String[] args) throws Exception {
        String fl = "src/main/resources/test.txt";
        printFile(fl);
        writeFile(fl);

    }


}
