package samples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceCalculatorWeakTest {

    @Test
    void returnsTotalForQuantity() {
        // Arrange
        PriceCalculator calculator = new PriceCalculator();

        // Act
        int total = calculator.total(12, 3);

        // Assert
        assertEquals(36, total);
    }
}
