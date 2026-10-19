package btndbrt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BudgetTest {

    private static Period samplePeriod() {
        Period period = Period.ofMonth(YearMonth.of(2026, 10));
        Category rent = new Category("Rent", new BigDecimal("800.00"));
        rent.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 1), "October", new BigDecimal("800.00")));
        Category food = new Category("Food", new BigDecimal("400.00"));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 2), "Groceries", new BigDecimal("55.40")));
        food.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 3), "", new BigDecimal("4.60")));
        period.getCategories().add(rent);
        period.getCategories().add(food);
        return period;
    }

    @Test
    public void totalsAddUp() {
        Period period = samplePeriod();
        Category food = period.getCategories().get(1);
        assertEquals(new BigDecimal("60.00"), food.getSpent());
        assertEquals(new BigDecimal("340.00"), food.getRemaining());
        assertEquals(new BigDecimal("1200.00"), period.getTotalBudget());
        assertEquals(new BigDecimal("860.00"), period.getTotalSpent());
        assertEquals(new BigDecimal("340.00"), period.getTotalRemaining());
    }

    @Test
    public void overspendingGivesNegativeRemaining() {
        Category fun = new Category("Fun", new BigDecimal("50"));
        fun.getTransactions().add(new Transaction(LocalDate.of(2026, 10, 5), "Concert", new BigDecimal("80")));
        assertEquals(new BigDecimal("-30"), fun.getRemaining());
    }

    @Test
    public void hasCategoryIgnoresCase() {
        assertTrue(samplePeriod().hasCategory("food"));
    }

    @Test
    public void periodContainsBothEnds() {
        Period october = Period.ofMonth(YearMonth.of(2026, 10));
        assertEquals(LocalDate.of(2026, 10, 31), october.getEnd());
        assertTrue(october.contains(LocalDate.of(2026, 10, 1)));
        assertTrue(october.contains(LocalDate.of(2026, 10, 31)));
        assertFalse(october.contains(LocalDate.of(2026, 11, 1)));
    }

    @Test
    public void nextPeriodFollowsLatestAndCopiesCategories() {
        Budget budget = new Budget();
        budget.addPeriod(samplePeriod());
        Period next = budget.nextPeriod();

        assertEquals(LocalDate.of(2026, 11, 1), next.getStart());
        assertEquals(LocalDate.of(2026, 11, 30), next.getEnd());
        assertEquals(2, next.getCategories().size());
        Category food = next.getCategories().get(1);
        assertEquals(new BigDecimal("400.00"), food.getBudgetAmount());
        assertTrue(food.getTransactions().isEmpty());
        assertEquals(1, budget.getPeriods().size()); // nextPeriod() does not add it
    }

    @Test
    public void nextPeriodWorksForMidMonthStarts() {
        Budget budget = new Budget();
        budget.addPeriod(new Period(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 11, 9)));
        Period next = budget.nextPeriod();
        assertEquals(LocalDate.of(2026, 11, 10), next.getStart());
        assertEquals(LocalDate.of(2026, 12, 9), next.getEnd());
    }

    @Test
    public void findsOverlappingPeriods() {
        Budget budget = new Budget();
        Period october = samplePeriod();
        budget.addPeriod(october);
        assertNotNull(budget.findOverlap(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 30), null));
        assertNull(budget.findOverlap(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null));
        assertNull(budget.findOverlap(october.getStart(), october.getEnd(), october)); // editing itself
    }

    @Test
    public void saveAndLoadKeepsEverything(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("budget.txt");
        Budget budget = new Budget();
        budget.setCurrency("HUF");
        budget.addPeriod(samplePeriod());
        budget.addPeriod(budget.nextPeriod());
        BudgetStorage.save(budget, file);
        Budget loaded = BudgetStorage.load(file);

        assertEquals("HUF", loaded.getCurrency());
        assertEquals(2, loaded.getPeriods().size());
        Period october = loaded.getPeriods().get(0);
        assertEquals(LocalDate.of(2026, 10, 1), october.getStart());
        assertEquals(LocalDate.of(2026, 10, 31), october.getEnd());
        Category food = october.getCategories().get(1);
        assertEquals("Food", food.getName());
        assertEquals(new BigDecimal("400.00"), food.getBudgetAmount());
        assertEquals(2, food.getTransactions().size());
        assertEquals("", food.getTransactions().get(1).getDescription());
        assertEquals(LocalDate.of(2026, 10, 2), food.getTransactions().get(0).getDate());
        assertEquals(new BigDecimal("340.00"), october.getTotalRemaining());
        assertEquals(new BigDecimal("1200.00"), loaded.getPeriods().get(1).getTotalBudget());
    }

    @Test
    public void loadsOldFilesWithoutPeriodsOrAmounts(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("old.txt");
        Files.writeString(file, "CATEGORY\tGroceries\n");
        Budget budget = BudgetStorage.load(file);

        assertEquals("", budget.getCurrency());
        assertEquals(1, budget.getPeriods().size());
        Period period = budget.getPeriods().get(0);
        assertTrue(period.contains(LocalDate.now()));
        Category groceries = period.getCategories().get(0);
        assertEquals("Groceries", groceries.getName());
        assertEquals(0, groceries.getBudgetAmount().signum());
    }

    @Test
    public void loadingMissingFileGivesEmptyBudget(@TempDir Path dir) throws IOException {
        assertTrue(BudgetStorage.load(dir.resolve("nope.txt")).getPeriods().isEmpty());
    }
}
