import java.util.*;

public class Functions {

    public static double apply(String name, List<Double> values) {

        // handle empty argument list safely
        if (values == null || values.isEmpty()) {
            switch (name) {
                case "SUMA":      return 0.0;
                case "PROMEDIO":  return 0.0;
                case "MIN":       throw new IllegalArgumentException("MIN needs at least one numeric argument");
                case "MAX":       throw new IllegalArgumentException("MAX needs at least one numeric argument");
                default:          throw new IllegalArgumentException("Unknown function: " + name);
            }
        }

        switch (name) {
            case "SUMA": {
                double sum = 0;
                for (double v : values) sum += v;
                return sum;
            }
            case "MIN": {
                double m = values.get(0);
                for (double v : values) m = Math.min(m, v);
                return m;
            }
            case "MAX": {
                double m = values.get(0);
                for (double v : values) m = Math.max(m, v);
                return m;
            }
            case "PROMEDIO": {
                double sum = 0;
                for (double v : values) sum += v;
                return sum / values.size();
            }
            default:
                throw new IllegalArgumentException("Unknown function: " + name);
        }
    }
}
