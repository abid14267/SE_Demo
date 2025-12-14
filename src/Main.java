import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Spreadsheet sheet = new Spreadsheet();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- Spreadsheet Menu ---");
            System.out.println("1. Set cell content");
            System.out.println("2. Get cell numeric value");
            System.out.println("3. Get cell RAW content");
            System.out.println("4. Load spreadsheet (S2V)");
            System.out.println("5. Save spreadsheet (S2V)");
            System.out.println("6. Exit");
            System.out.print("Choose option: ");

            int opt;
            try {
                opt = Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Invalid input.");
                continue;
            }

            try {
                if (opt == 1) {
                    System.out.print("Cell (e.g. A1): ");
                    String ref = sc.nextLine().trim().toUpperCase();

                    System.out.print("Content (number/text or formula starting with '='): ");
                    String content = sc.nextLine();

                    sheet.setCell(ref, content);
                    System.out.println("Cell updated.");
                }
                else if (opt == 2) {
                    System.out.print("Cell (e.g. A1): ");
                    String ref = sc.nextLine().trim().toUpperCase();

                    double v = sheet.evaluateCell(ref);
                    System.out.println("Value = " + v);
                }
                else if (opt == 3) {
                    System.out.print("Cell (e.g. A1): ");
                    String ref = sc.nextLine().trim().toUpperCase();

                    String raw = sheet.getRaw(ref);
                    System.out.println("RAW = " + raw);
                }
                else if (opt == 4) {
                    System.out.print("File path: ");
                    String path = sc.nextLine().trim();

                    sheet = SpreadsheetIO.load(path);
                    System.out.println("Loaded and recomputed.");
                }
                else if (opt == 5) {
                    System.out.print("File path: ");
                    String path = sc.nextLine().trim();

                    SpreadsheetIO.save(sheet, path);
                    System.out.println("Saved.");
                }
                else if (opt == 6) {
                    System.out.println("Bye.");
                    break;
                }
                else {
                    System.out.println("Invalid option.");
                }
            } catch (CircularDependencyException e) {
                System.out.println("ERROR: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }

        sc.close();
    }
}
