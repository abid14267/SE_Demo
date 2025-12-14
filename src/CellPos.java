public class CellPos {
    public final int col; // 0-based
    public final int row; // 1-based

    public CellPos(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public static CellPos parse(String ref) {
        ref = ref.trim().toUpperCase();

        int i = 0;
        while (i < ref.length() && Character.isLetter(ref.charAt(i))) i++;
        if (i == 0 || i >= ref.length()) throw new IllegalArgumentException("Bad cell ref: " + ref);

        String colS = ref.substring(0, i);
        String rowS = ref.substring(i);

        int row = Integer.parseInt(rowS);
        int col = colToIndex(colS);
        return new CellPos(col, row);
    }

    public static int colToIndex(String col) {
        col = col.toUpperCase();
        int x = 0;
        for (int i = 0; i < col.length(); i++) {
            x = x * 26 + (col.charAt(i) - 'A' + 1);
        }
        return x - 1;
    }

    public static String indexToCol(int idx) {
        StringBuilder sb = new StringBuilder();
        int x = idx + 1;
        while (x > 0) {
            int rem = (x - 1) % 26;
            sb.append((char)('A' + rem));
            x = (x - 1) / 26;
        }
        return sb.reverse().toString();
    }

    public static String toRef(int col, int row) {
        return indexToCol(col) + row;
    }
}
