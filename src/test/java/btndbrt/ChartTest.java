package btndbrt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ChartTest {

    private static Category category(String name, String... spent) {
        Category c = new Category(name, new BigDecimal("100"));
        for (String amount : spent) {
            c.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 1), "", new BigDecimal(amount)));
        }
        return c;
    }

    @Test
    public void periodDayCounting() {
        Period october = Period.ofMonth(YearMonth.of(2026, 10));
        assertEquals(31, october.getDayCount());
        assertEquals(18, october.dayIndex(LocalDate.of(2026, 10, 19)));
        assertEquals(13, october.daysLeft(LocalDate.of(2026, 10, 19)));
        assertEquals(1, october.daysLeft(LocalDate.of(2026, 10, 31)));
    }

    @Test
    public void pieSlicesAreSortedAndSkipEmptyCategories() {
        Period period = Period.ofMonth(YearMonth.of(2026, 10));
        period.getCategories().add(category("Food", "10"));
        period.getCategories().add(category("Rent", "50"));
        period.getCategories().add(category("Fun")); // nothing spent

        List<PieChartPanel.Slice> slices = PieChartPanel.buildSlices(period);
        assertEquals(2, slices.size());
        assertEquals("Rent", slices.get(0).name());
        assertEquals(ChartColors.CATEGORIES[1], slices.get(0).color()); // color follows the category
    }

    @Test
    public void smallSlicesAreGroupedAsOther() {
        Period period = Period.ofMonth(YearMonth.of(2026, 10));
        for (int i = 1; i <= 8; i++) {
            period.getCategories().add(category("C" + i, String.valueOf(i * 10)));
        }
        List<PieChartPanel.Slice> slices = PieChartPanel.buildSlices(period);
        assertEquals(PieChartPanel.MAX_SLICES, slices.size());
        PieChartPanel.Slice other = slices.get(slices.size() - 1);
        assertEquals("Other", other.name());
        assertEquals(new BigDecimal("60"), other.amount()); // C1 + C2 + C3 = 10 + 20 + 30
    }

    @Test
    public void cumulativeSpendingAddsUpDayByDay() {
        Period period = new Period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 4));
        Category food = new Category("Food", new BigDecimal("100"));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 1), "", new BigDecimal("5")));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 3), "", new BigDecimal("10")));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 3), "", new BigDecimal("2.5")));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 11, 1), "", new BigDecimal("99"))); // outside

        double[] result = LineChartPanel.cumulativeSpending(period, List.of(food));
        assertArrayEquals(new double[] {5, 5, 17.5, 17.5}, result, 0.001);
    }

    @Test
    public void niceStepsAreRound() {
        assertEquals(20000, LineChartPanel.niceStep(13750), 0.001);
        assertEquals(50, LineChartPanel.niceStep(31), 0.001);
        assertEquals(1, LineChartPanel.niceStep(0.8), 0.001);
    }
}
