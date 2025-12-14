import java.io.File;

public class TestSpreadsheet {


        private static int ok = 0;
        private static int bad = 0;

        public static void main(String[] args) throws Exception {

            Spreadsheet s = new Spreadsheet();

            header("1) LONG DEPENDENCY CHAIN");
            s.setCell("A1", "1");
            for (int i = 2; i <= 25; i++) {
                s.setCell("A" + i, "=A" + (i - 1) + "+1");   // A25 should become 25
            }
            assertEq("A25", 25.0, s.evaluateCell("A25"));

            header("2) MULTIPLE UPDATES PROPAGATION");
            s.setCell("A1", "100");
            assertEq("A25 after A1=100", 124.0, s.evaluateCell("A25"));

            s.setCell("A1", "-10");
            assertEq("A25 after A1=-10", 14.0, s.evaluateCell("A25"));

            header("3) OVERLAPPING RANGES");
            // Fill B1..B6
            s.setCell("B1", "1");
            s.setCell("B2", "2");
            s.setCell("B3", "3");
            s.setCell("B4", "4");
            s.setCell("B5", "5");
            s.setCell("B6", "6");

            s.setCell("C1", "=SUMA(B1:B6)");        // 21
            s.setCell("C2", "=SUMA(B2:B5)");        // 14
            s.setCell("C3", "=SUMA(B1:B3)+SUMA(B4:B6)"); // 6 + 15 = 21
            assertEq("C1", 21.0, s.evaluateCell("C1"));
            assertEq("C2", 14.0, s.evaluateCell("C2"));
            assertEq("C3", 21.0, s.evaluateCell("C3"));

            header("4) NESTED FUNCTIONS INSIDE ARITHMETIC");
            s.setCell("D1", "=MAX(1;2;3)*10 + MIN(8;4;6)");
            // max=3 => 30, min=4 => 34
            assertEq("D1", 34.0, s.evaluateCell("D1"));

            s.setCell("D2", "=SUMA(1;2;MAX(3;9;4)) - PROMEDIO(10;20)");
            // SUMA = 1+2+9=12, PROMEDIO=15 => -3
            assertEq("D2", -3.0, s.evaluateCell("D2"));

            header("5) MULTI-COLUMN RANGE + FUNCTION COMBO");
            // block E1:F3 = 1..6
            s.setCell("E1","1"); s.setCell("F1","2");
            s.setCell("E2","3"); s.setCell("F2","4");
            s.setCell("E3","5"); s.setCell("F3","6");

            s.setCell("G1", "=SUMA(E1:F3)");         // 21
            s.setCell("G2", "=MAX(E1:F3)");          // 6
            s.setCell("G3", "=MIN(E1:F3)");          // 1
            s.setCell("G4", "=PROMEDIO(E1:F3)");     // 3.5
            assertEq("G1", 21.0, s.evaluateCell("G1"));
            assertEq("G2", 6.0, s.evaluateCell("G2"));
            assertEq("G3", 1.0, s.evaluateCell("G3"));
            assertClose("G4", 3.5, s.evaluateCell("G4"), 1e-9);

            header("6) EMPTY-ONLY RANGES (EDGE CASE)");
            // H1:H3 not set at all (empty)
            s.setCell("I1", "=SUMA(H1:H3)");
            s.setCell("I2", "=PROMEDIO(H1:H3)");
            assertEq("SUMA(empty range)=0", 0.0, s.evaluateCell("I1"));
            assertEq("PROMEDIO(empty range)=0", 0.0, s.evaluateCell("I2"));

            header("7) AA/BA COLUMN CHECK");
            s.setCell("AA1", "7");
            s.setCell("BA1", "8");
            s.setCell("BB1", "=AA1+BA1");            // 15
            assertEq("BB1", 15.0, s.evaluateCell("BB1"));

            header("8) SAVE/LOAD INTEGRITY (MANY CELLS)");
            String file = "stress_edges.s2v";
            SpreadsheetIO.save(s, file);
            Spreadsheet t = SpreadsheetIO.load(file);

            assertEq("Loaded A25", 14.0, t.evaluateCell("A25"));
            assertEq("Loaded C1", 21.0, t.evaluateCell("C1"));
            assertEq("Loaded D1", 34.0, t.evaluateCell("D1"));
            assertEq("Loaded G1", 21.0, t.evaluateCell("G1"));
            assertClose("Loaded G4", 3.5, t.evaluateCell("G4"), 1e-9);
            assertEq("Loaded I2", 0.0, t.evaluateCell("I2"));

            new File(file).delete();

            header("9) CYCLE DETECTION STILL WORKS");
            try {
                s.setCell("Z1", "=Z2+1");
                s.setCell("Z2", "=Z3+1");
                s.setCell("Z3", "=Z1+1");
                s.evaluateCell("Z1");
                fail("Indirect cycle should throw");
            } catch (Exception e) {
                pass("Indirect cycle throws ✔");
            }

            // Summary
            System.out.println("\n==============================");
            System.out.println("PASSED: " + ok);
            System.out.println("FAILED: " + bad);
            System.out.println("==============================");
            if (bad > 0) throw new RuntimeException("Some tests failed.");
        }

        // ---------------- helpers ----------------
        private static void header(String t) {
            System.out.println("\n===== " + t + " =====");
        }

        private static void assertEq(String name, double expected, double actual) {
            if (Math.abs(expected - actual) < 1e-9) pass(name + " ✓ (" + actual + ")");
            else fail(name + " ✗ expected " + expected + " but got " + actual);
        }

        private static void assertClose(String name, double expected, double actual, double eps) {
            if (Math.abs(expected - actual) <= eps) pass(name + " ✓ (" + actual + ")");
            else fail(name + " ✗ expected " + expected + " but got " + actual);
        }

        private static void pass(String msg) {
            ok++;
            System.out.println("[PASS] " + msg);
        }

        private static void fail(String msg) {
            bad++;
            System.out.println("[FAIL] " + msg);
        }
    }
