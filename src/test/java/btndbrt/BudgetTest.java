package btndbrt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BudgetTest {

    private static Budget sampleBudget() {
        Budget budget = new Budget();
        Category rent = new Category("Rent", new BigDecimal("800.00"));
        rent.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 1), "October", new BigDecimal("800.00")));
        Category food = new Category("Food", new BigDecimal("400.00"));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 2), "Groceries", new BigDecimal("55.40")));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 3), "", new BigDecimal("4.60")));
        budget.getCategories().add(rent);
        budget.getCategories().add(food);
        return budget;
    }

    @Test
    public void totalsAddUp() {
        Budget budget = sampleBudget();
        Category food = budget.getCategories().get(1);
        assertEquals(new BigDecimal("60.00"), food.getSpent());
        assertEquals(new BigDecimal("340.00"), food.getRemaining());
        assertEquals(new BigDecimal("1200.00"), budget.getTotalBudget());
        assertEquals(new BigDecimal("860.00"), budget.getTotalSpent());
        assertEquals(new BigDecimal("340.00"), budget.getTotalRemaining());
    }

    @Test
    public void overspendingGivesNegativeRemaining() {
        Category fun = new Category("Fun", new BigDecimal("50"));
        fun.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 5), "Concert", new BigDecimal("80")));
        assertEquals(new BigDecimal("-30"), fun.getRemaining());
    }

    @Test
    public void emptyBudgetIsZero() {
        assertEquals(0, new Budget().getTotalRemaining().signum());
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
        assertEquals(new BigDecimal("400.00"), food.getBudgetAmount());
        assertEquals(2, food.getTransactions().size());
        assertEquals("", food.getTransactions().get(1).getDescription());
        assertEquals(LocalDate.of(2026, 10, 2), food.getTransactions().get(0).getDate());
        assertEquals(new BigDecimal("340.00"), loaded.getTotalRemaining());
    }

    @Test
    public void loadsOldFilesWithoutBudgetAmount(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("old.txt");
        Files.writeString(file, "CATEGORY\tGroceries\n");
        Category groceries = BudgetStorage.load(file).getCategories().get(0);
        assertEquals("Groceries", groceries.getName());
        assertEquals(0, groceries.getBudgetAmount().signum());
    }

    @Test
    public void loadingMissingFileGivesEmptyBudget(@TempDir Path dir) throws IOException {
        assertTrue(BudgetStorage.load(dir.resolve("nope.txt")).getCategories().isEmpty());
    }
}
