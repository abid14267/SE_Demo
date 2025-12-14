import java.io.*;
import java.util.*;

public class SpreadsheetIO {

    public static void save(Spreadsheet sheet, String filename) throws IOException {

        Set<String> used = sheet.allNonEmptyCells();

        // if nothing used, create empty file
        if (used.isEmpty()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
                bw.write("");
            }
            return;
        }

        int maxRow = 0;
        int maxCol = 0;

        for (String ref : used) {
            CellPos p = CellPos.parse(ref);
            maxRow = Math.max(maxRow, p.row);
            maxCol = Math.max(maxCol, p.col);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (int row = 1; row <= maxRow; row++) {
                StringBuilder line = new StringBuilder();

                for (int col = 0; col <= maxCol; col++) {
                    String ref = CellPos.toRef(col, row);


                    String raw = sheet.getRaw(ref);
                    if (raw == null) raw = "";
                    if (raw.startsWith("=")) raw = raw.replace(';', ',');

                    line.append(raw);

                    if (col < maxCol) line.append(';');
                }

                bw.write(line.toString());
                bw.newLine();
            }
        }
    }

    public static Spreadsheet load(String filename) throws IOException {
        Spreadsheet sheet = new Spreadsheet();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int row = 1;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";", -1);
                for (int col = 0; col < parts.length; col++) {
                    String ref = CellPos.toRef(col, row);
                    String raw = parts[col];

                    if (raw == null) raw = "";
                    if (raw.startsWith("=")) raw = raw.replace(',', ';');

                    if (!raw.isEmpty()) sheet.setCell(ref, raw);
                }
                row++;
            }
        }

        sheet.recomputeAll();
        return sheet;
    }
}
