package samples;

public class PriceCalculator {

    public int total(int price, int quantity) {
        return price * quantity;
    }

    public int applyDiscount(int total, int discount) {
        return total - discount;
    }

    public boolean qualifiesForDiscount(int total) {
        if (total >= 100) {
            return true;
        }
        return false;
    }
}
