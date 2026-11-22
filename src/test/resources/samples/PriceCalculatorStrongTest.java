package samples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceCalculatorStrongTest {

    @Test
    void returnsTotalForQuantity() {
        // Arrange
        PriceCalculator calculator = new PriceCalculator();

        // Act
        int total = calculator.total(12, 3);

        // Assert
        assertEquals(36, total);
    }

    @Test
    void subtractsDiscountFromTotal() {
        // Arrange
        PriceCalculator calculator = new PriceCalculator();

        // Act
        int discountedTotal = calculator.applyDiscount(120, 15);

        // Assert
        assertEquals(105, discountedTotal);
    }

    @ParameterizedTest
    @CsvSource({"99, false", "100, true"})
    void checksDiscountAtBoundary(int total, boolean expected) {
        // Arrange
        PriceCalculator calculator = new PriceCalculator();

        // Act
        boolean qualifies = calculator.qualifiesForDiscount(total);

        // Assert
        assertEquals(expected, qualifies);
    }
}
