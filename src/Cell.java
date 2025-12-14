public class Cell {
    private String raw = "";
    private CellType type = CellType.EMPTY;
    private double cachedNumber = 0.0;

    public void setRaw(String content) {
        raw = (content == null) ? "" : content.trim();

        if (raw.isEmpty()) {
            type = CellType.EMPTY;
            cachedNumber = 0.0;
            return;
        }

        if (raw.startsWith("=")) {
            type = CellType.FORMULA;
            return;
        }

        try {
            cachedNumber = Double.parseDouble(raw);
            type = CellType.NUMBER;
        } catch (Exception e) {
            type = CellType.TEXT;
        }
    }

    public String getRaw() {
        return raw;
    }

    public CellType getType() {
        return type;
    }

    public double getCachedNumber() {
        return cachedNumber;
    }

    public void setCachedNumber(double v) {
        cachedNumber = v;
    }
}
