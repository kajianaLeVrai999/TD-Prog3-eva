import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class Main {

    public static void main(String[] args) {

        DataRetriever dr = new DataRetriever();

        Instant t = LocalDateTime
                .parse("2024-01-06T12:00:00")
                .atZone(ZoneId.systemDefault())
                .toInstant();

        for (int i = 1; i <= 5; i++) {

            Ingredient ingredient = dr.findIngredientById(i);

            double stockOO = ingredient.getStockValueAt(t).getQuantity();
            double stockDB = dr.getStockValueAt(i, t);

            System.out.println("Ingredient " + i);
            System.out.println("OO  : " + stockOO);
            System.out.println("DB  : " + stockDB);
            System.out.println("----------------------");
        }
    }
}
