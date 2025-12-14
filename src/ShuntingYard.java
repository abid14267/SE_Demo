import java.util.*;

public class ShuntingYard {

    private static int prec(String op) {
        if (op.equals("+") || op.equals("-")) return 1;
        if (op.equals("*") || op.equals("/")) return 2;
        return -1;
    }

    public static List<Token> toRPN(List<Token> tokens) {
        List<Token> out = new ArrayList<>();
        Deque<Token> stack = new ArrayDeque<>();

        for (Token t : tokens) {
            switch (t.type) {
                case NUMBER:
                case CELL:
                    out.add(t);
                    break;

                case OPERATOR:
                    while (!stack.isEmpty()
                            && stack.peek().type == Token.Type.OPERATOR
                            && prec(stack.peek().text) >= prec(t.text)) {
                        out.add(stack.pop());
                    }
                    stack.push(t);
                    break;

                case LPAREN:
                    stack.push(t);
                    break;

                case RPAREN:
                    while (!stack.isEmpty() && stack.peek().type != Token.Type.LPAREN) {
                        out.add(stack.pop());
                    }
                    if (stack.isEmpty() || stack.peek().type != Token.Type.LPAREN) {
                        throw new IllegalArgumentException("Mismatched parentheses");
                    }
                    stack.pop(); // pop '('
                    break;
            }
        }

        while (!stack.isEmpty()) {
            Token top = stack.pop();
            if (top.type == Token.Type.LPAREN || top.type == Token.Type.RPAREN) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            out.add(top);
        }

        return out;
    }
}
