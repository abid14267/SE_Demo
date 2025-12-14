public class Token {
    public enum Type { NUMBER, CELL, OPERATOR, LPAREN, RPAREN }

    public final Type type;
    public final String text;

    public Token(Type type, String text) {
        this.type = type;
        this.text = text;
    }
}
