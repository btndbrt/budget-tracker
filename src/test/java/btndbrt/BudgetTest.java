package btndbrt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BudgetTest {

    private static Budget sampleBudget() {
        Budget budget = new Budget();
        Category salary = new Category("Salary");
        salary.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 1), "October", new BigDecimal("2000.00")));
        Category food = new Category("Food");
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 2), "Groceries", new BigDecimal("-55.40")));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 3), "", new BigDecimal("-4.60")));
        budget.getCategories().add(salary);
        budget.getCategories().add(food);
        return budget;
    }

    @Test
    public void totalsAddUp() {
        Budget budget = sampleBudget();
        assertEquals(new BigDecimal("-60.00"), budget.getCategories().get(1).getTotal());
        assertEquals(new BigDecimal("1940.00"), budget.getNetTotal());
    }

    @Test
    public void emptyBudgetIsZero() {
        assertEquals(0, new Budget().getNetTotal().signum());
    }

    @Test
    public void hasCategoryIgnoresCase() {
        assertTrue(sampleBudget().hasCategory("food"));
    }

    @Test
    public void saveAndLoadKeepsEverything(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("budget.txt");
        BudgetStorage.save(sampleBudget(), file);
        Budget loaded = BudgetStorage.load(file);

        assertEquals(2, loaded.getCategories().size());
        Category food = loaded.getCategories().get(1);
        assertEquals("Food", food.getName());
        assertEquals(2, food.getTransactions().size());
        assertEquals("", food.getTransactions().get(1).getDescription());
        assertEquals(LocalDate.of(2026, 10, 2), food.getTransactions().get(0).getDate());
        assertEquals(new BigDecimal("1940.00"), loaded.getNetTotal());
    }

    @Test
    public void loadingMissingFileGivesEmptyBudget(@TempDir Path dir) throws IOException {
        assertTrue(BudgetStorage.load(dir.resolve("nope.txt")).getCategories().isEmpty());
    }
}
