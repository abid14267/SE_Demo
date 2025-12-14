import java.util.*;

public class Spreadsheet {

    private final Map<String, Cell> cells = new HashMap<>();

    private static String norm(String ref) {
        if (ref == null) throw new IllegalArgumentException("Null cell ref");
        ref = ref.trim().toUpperCase();
        if (ref.isEmpty()) throw new IllegalArgumentException("Empty cell ref");
        return ref;
    }

    public Cell getCell(String ref) {
        ref = norm(ref);
        return cells.computeIfAbsent(ref, k -> new Cell());
    }

    public void setCell(String ref, String content) {
        ref = norm(ref);

        Cell cell = getCell(ref);
        String old = cell.getRaw();

        cell.setRaw(content);

        // Validate: if formula introduces a cycle, rollback
        try {
            if (cell.getType() == CellType.FORMULA) {
                evaluateCell(ref);
            }
        } catch (RuntimeException e) {
            cell.setRaw(old);
            throw e;
        }
    }

    public double evaluateCell(String ref) {
        ref = norm(ref);
        return evaluateCellInternal(ref, new HashSet<>());
    }

    // IMPORTANT: used by FormulaEvaluator to keep ONE visiting set across the whole chain
    double evaluateCellInternal(String ref, Set<String> visiting) {
        ref = ref.trim().toUpperCase();

        if (visiting.contains(ref)) {
            throw new CircularDependencyException("Circular dependency at " + ref);
        }

        visiting.add(ref);
        try {
            Cell c = getCell(ref);

            switch (c.getType()) {
                case EMPTY:
                    return 0.0;

                case NUMBER:
                    return c.getCachedNumber();

                case TEXT:
                    return 0.0; // keep it simple

                case FORMULA:
                    double v = FormulaEvaluator.evaluate(c.getRaw().substring(1), this, visiting);
                    c.setCachedNumber(v);
                    return v;

                default:
                    return 0.0;
            }
        } finally {
            // ✅ CRITICAL: remove after finishing evaluation
            visiting.remove(ref);
        }
    }


    public String getRaw(String ref) {
        ref = ref.trim().toUpperCase();
        Cell c = cells.get(ref);      // direct map access
        return (c == null) ? "" : c.getRaw();
    }


    public Set<String> allNonEmptyCells() {
        Set<String> s = new HashSet<>();
        for (Map.Entry<String, Cell> e : cells.entrySet()) {
            if (!e.getValue().getRaw().isEmpty()) s.add(e.getKey());
        }
        return s;
    }

    public void recomputeAll() {
        for (String ref : allNonEmptyCells()) {
            evaluateCell(ref);
        }
    }
}
