import java.util.*;

public class FormulaEvaluator {

    private static final Set<String> FN = new HashSet<>(Arrays.asList(
            "SUMA", "MIN", "MAX", "PROMEDIO"
    ));

    public static double evaluate(String expr, Spreadsheet sheet, Set<String> visiting) {
        if (expr == null) throw new IllegalArgumentException("Null expression");
        expr = expr.replace(" ", "");

        expr = reduceFunctions(expr, sheet, visiting);


        List<Token> tokens = tokenizeArithmetic(expr);
        List<Token> rpn = ShuntingYard.toRPN(tokens);
        return evalRPN(rpn, sheet, visiting);
    }

    // ==========================================================
    // 1) Function reduction: turn MAX(...), SUMA(...), etc into numbers
    // ==========================================================

    private static String reduceFunctions(String expr, Spreadsheet sheet, Set<String> visiting) {
        StringBuilder out = new StringBuilder();
        int i = 0;

        while (i < expr.length()) {
            char ch = expr.charAt(i);

            if (Character.isLetter(ch)) {
                // read letters
                int j = i;
                while (j < expr.length() && Character.isLetter(expr.charAt(j))) j++;
                String word = expr.substring(i, j);

                // function if word is known function and next char is '('
                if (FN.contains(word) && j < expr.length() && expr.charAt(j) == '(') {
                    int open = j;
                    int close = findMatchingParen(expr, open);
                    String inside = expr.substring(open + 1, close);

                    double val = evalFunction(word, inside, sheet, visiting);
                    out.append(toCompactNumber(val));

                    i = close + 1;
                    continue;
                }

                // not a function name => keep as it is (likely part of cell ref like A1, AA12)
                out.append(word);
                i = j;
                continue;
            }

            out.append(ch);
            i++;
        }

        String reduced = out.toString();


        if (containsFunctionCall(reduced) && !reduced.equals(expr)) {
            return reduceFunctions(reduced, sheet, visiting);
        }
        return reduced;
    }

    private static boolean containsFunctionCall(String s) {
        for (String f : FN) {
            if (s.contains(f + "(")) return true;
        }
        return false;
    }

    private static int findMatchingParen(String s, int openIdx) {
        int depth = 1;
        for (int k = openIdx + 1; k < s.length(); k++) {
            if (s.charAt(k) == '(') depth++;
            else if (s.charAt(k) == ')') depth--;
            if (depth == 0) return k;
        }
        throw new IllegalArgumentException("Missing ')' in expression");
    }

    private static double evalFunction(String name, String inside, Spreadsheet sheet, Set<String> visiting) {
        List<String> args = splitArgsTopLevel(inside);
        List<Double> values = new ArrayList<>();

        for (String a : args) {
            a = a.trim();
            if (a.isEmpty()) continue;

            if (isRange(a)) {
                for (String ref : expandRangeRefs(a)) {
                    addCellForFunction(name, ref, values, sheet, visiting);
                }
            } else if (isCellRef(a)) {
                addCellForFunction(name, a, values, sheet, visiting);
            } else {
                // constant/expression/nested function result -> always a numeric value
                values.add(evaluate(a, sheet, visiting));
            }
        }

        return Functions.apply(name, values);
    }

    private static void addCellForFunction(
            String funcName, String ref, List<Double> values,
            Spreadsheet sheet, Set<String> visiting) {

        Cell c = sheet.getCell(ref); // access type without evaluating first

        // For PROMEDIO: ignore empty/text cells completely
        if (funcName.equals("PROMEDIO")) {
            if (c.getType() == CellType.EMPTY || c.getType() == CellType.TEXT) {
                return; // skip
            }
            values.add(sheet.evaluateCellInternal(ref, visiting));
            return;
        }

        // For SUMA: empty/text -> treat as 0
        if (funcName.equals("SUMA")) {
            if (c.getType() == CellType.EMPTY || c.getType() == CellType.TEXT) {
                values.add(0.0);
            } else {
                values.add(sheet.evaluateCellInternal(ref, visiting));
            }
            return;
        }


        if (funcName.equals("MIN") || funcName.equals("MAX")) {
            if (c.getType() == CellType.EMPTY || c.getType() == CellType.TEXT) {
                return; // skip
            }
            values.add(sheet.evaluateCellInternal(ref, visiting));
            return;
        }

        // fallback
        values.add(sheet.evaluateCellInternal(ref, visiting));
    }

    private static List<String> expandRangeRefs(String range) {
        String[] p = range.split(":");
        CellPos c1 = CellPos.parse(p[0]);
        CellPos c2 = CellPos.parse(p[1]);

        int colStart = Math.min(c1.col, c2.col);
        int colEnd   = Math.max(c1.col, c2.col);
        int rowStart = Math.min(c1.row, c2.row);
        int rowEnd   = Math.max(c1.row, c2.row);

        List<String> refs = new ArrayList<>();
        for (int col = colStart; col <= colEnd; col++) {
            for (int row = rowStart; row <= rowEnd; row++) {
                refs.add(CellPos.toRef(col, row));
            }
        }
        return refs;
    }

    private static double evaluateArgument(String a, Spreadsheet sheet, Set<String> visiting) {
        // If argument is a plain cell reference, evaluate it directly:
        if (isCellRef(a)) {
            return sheet.evaluateCellInternal(a, visiting);
        }
        // Otherwise treat it as a full expression:
        return evaluate(a, sheet, visiting);
    }

    private static List<String> splitArgsTopLevel(String s) {
        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;

            if (c == ';' && depth == 0) {
                res.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        res.add(cur.toString());
        return res;
    }

    // ==========================================================
    // 2) Ranges and cell refs
    // ==========================================================

    private static boolean isCellRef(String s) {
        int i = 0;
        while (i < s.length() && Character.isLetter(s.charAt(i))) i++;
        if (i == 0 || i == s.length()) return false;

        int j = i;
        while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
        return j == s.length();
    }

    private static boolean isRange(String s) {
        int k = s.indexOf(':');
        if (k < 0) return false;
        String a = s.substring(0, k);
        String b = s.substring(k + 1);
        return isCellRef(a) && isCellRef(b);
    }

    private static List<Double> expandRangeValues(String range, Spreadsheet sheet, Set<String> visiting) {
        String[] p = range.split(":");
        CellPos c1 = CellPos.parse(p[0]);
        CellPos c2 = CellPos.parse(p[1]);

        int colStart = Math.min(c1.col, c2.col);
        int colEnd   = Math.max(c1.col, c2.col);
        int rowStart = Math.min(c1.row, c2.row);
        int rowEnd   = Math.max(c1.row, c2.row);

        List<Double> vals = new ArrayList<>();
        for (int col = colStart; col <= colEnd; col++) {
            for (int row = rowStart; row <= rowEnd; row++) {
                String ref = CellPos.toRef(col, row);
                vals.add(sheet.evaluateCellInternal(ref, visiting));
            }
        }
        return vals;
    }

    // ==========================================================
    // 3) Arithmetic tokenization
    // ==========================================================

    private static List<Token> tokenizeArithmetic(String expr) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        Token prev = null;

        while (i < expr.length()) {
            char c = expr.charAt(i);

            // number
            if (Character.isDigit(c) || c == '.') {
                int j = i + 1;
                while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) j++;
                tokens.add(new Token(Token.Type.NUMBER, expr.substring(i, j)));
                prev = tokens.get(tokens.size() - 1);
                i = j;
                continue;
            }

            // cell ref: letters + digits
            if (Character.isLetter(c)) {
                int j = i;
                while (j < expr.length() && Character.isLetter(expr.charAt(j))) j++;
                int k = j;
                while (k < expr.length() && Character.isDigit(expr.charAt(k))) k++;

                String ref = expr.substring(i, k);
                if (!isCellRef(ref)) {
                    throw new IllegalArgumentException("Invalid token: " + ref);
                }
                tokens.add(new Token(Token.Type.CELL, ref));
                prev = tokens.get(tokens.size() - 1);
                i = k;
                continue;
            }

            // parentheses
            if (c == '(') {
                tokens.add(new Token(Token.Type.LPAREN, "("));
                prev = tokens.get(tokens.size() - 1);
                i++;
                continue;
            }
            if (c == ')') {
                tokens.add(new Token(Token.Type.RPAREN, ")"));
                prev = tokens.get(tokens.size() - 1);
                i++;
                continue;
            }

            // operators (+-*/). unary +/- => insert 0
            if ("+-*/".indexOf(c) >= 0) {
                boolean unary = (prev == null)
                        || prev.type == Token.Type.OPERATOR
                        || prev.type == Token.Type.LPAREN;

                if (unary && (c == '+' || c == '-')) {
                    tokens.add(new Token(Token.Type.NUMBER, "0"));
                }

                tokens.add(new Token(Token.Type.OPERATOR, String.valueOf(c)));
                prev = tokens.get(tokens.size() - 1);
                i++;
                continue;
            }

            throw new IllegalArgumentException("Invalid character: " + c);
        }

        return tokens;
    }

    private static double evalRPN(List<Token> rpn, Spreadsheet sheet, Set<String> visiting) {
        Deque<Double> st = new ArrayDeque<>();

        for (Token t : rpn) {
            switch (t.type) {
                case NUMBER:
                    st.push(Double.parseDouble(t.text));
                    break;
                case CELL:
                    st.push(sheet.evaluateCellInternal(t.text, visiting));
                    break;
                case OPERATOR:
                    if (st.size() < 2) throw new IllegalArgumentException("Syntax error");
                    double b = st.pop();
                    double a = st.pop();
                    st.push(apply(a, b, t.text));
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected token in RPN: " + t.type);
            }
        }

        if (st.size() != 1) throw new IllegalArgumentException("Bad expression");
        return st.pop();
    }

    private static double apply(double a, double b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return a / b;
            default: throw new IllegalArgumentException("Unknown operator: " + op);
        }
    }

    private static String toCompactNumber(double v) {
        long L = (long) v;
        if (Math.abs(v - L) < 1e-12) return Long.toString(L);
        return Double.toString(v);
    }
}
