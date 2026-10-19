package btndbrt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * One budget period, usually a month (e.g. 2026-10-01 to 2026-10-31).
 * Each period has its own categories, amounts and transactions, so old periods stay as history.
 */
public class Period {

    private LocalDate start;
    private LocalDate end;
    private final List<Category> categories = new ArrayList<>();

    public Period(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    /** A period covering a whole calendar month. */
    public static Period ofMonth(YearMonth month) {
        return new Period(month.atDay(1), month.atEndOfMonth());
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    /** True if the date is between start and end (both included). */
    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /** Number of days in the period, start and end included. */
    public int getDayCount() {
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    /** Position of the date in the period: 0 for the start day, 1 for the next day, and so on. */
    public int dayIndex(LocalDate date) {
        return (int) ChronoUnit.DAYS.between(start, date);
    }

    /** Days left until the end of the period, counting the given day too. */
    public int daysLeft(LocalDate date) {
        return (int) ChronoUnit.DAYS.between(date, end) + 1;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public boolean hasCategory(String name) {
        for (Category c : categories) {
            if (c.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** The net budget of this period: all category amounts added together. */
    public BigDecimal getTotalBudget() {
        BigDecimal total = BigDecimal.ZERO;
        for (Category c : categories) {
            total = total.add(c.getBudgetAmount());
        }
        return total;
    }

    public BigDecimal getTotalSpent() {
        BigDecimal total = BigDecimal.ZERO;
        for (Category c : categories) {
            total = total.add(c.getSpent());
        }
        return total;
    }

    public BigDecimal getTotalRemaining() {
        return getTotalBudget().subtract(getTotalSpent());
    }

    /** Used by the period drop-down to show this period. */
    @Override
    public String toString() {
        return start + " to " + end;
    }
}
