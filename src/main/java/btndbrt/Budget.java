package btndbrt;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Everything the app stores: the currency and all budget periods (current and past). */
public class Budget {

    private final List<Period> periods = new ArrayList<>();
    private String currency = ""; // shown after every amount, e.g. "HUF"

    public List<Period> getPeriods() {
        return periods;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /** Keeps the periods in date order, oldest first. Call after adding a period or changing its dates. */
    public void sortPeriods() {
        periods.sort(Comparator.comparing(Period::getStart));
    }

    /** The period that ends last, or null if there are none. */
    public Period getLatestPeriod() {
        Period latest = null;
        for (Period p : periods) {
            if (latest == null || p.getEnd().isAfter(latest.getEnd())) {
                latest = p;
            }
        }
        return latest;
    }

    /** Adds a period and keeps the list in date order. */
    public void addPeriod(Period period) {
        periods.add(period);
        sortPeriods();
    }

    /**
     * Returns a period that overlaps the given dates, or null if there is none.
     * {@code ignore} is left out of the check (the period being edited), it can be null.
     */
    public Period findOverlap(LocalDate start, LocalDate end, Period ignore) {
        for (Period p : periods) {
            if (p != ignore && !start.isAfter(p.getEnd()) && !end.isBefore(p.getStart())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Makes (but does not add) the period after the latest one: it starts the day after the latest
     * period ends and lasts one month. The categories and their amounts are copied, but not the
     * transactions. With no periods yet, it is the current calendar month.
     */
    public Period nextPeriod() {
        Period latest = getLatestPeriod();
        if (latest == null) {
            return Period.ofMonth(YearMonth.now());
        }
        LocalDate start = latest.getEnd().plusDays(1);
        Period next = new Period(start, start.plusMonths(1).minusDays(1));
        for (Category c : latest.getCategories()) {
            next.getCategories().add(new Category(c.getName(), c.getBudgetAmount()));
        }
        return next;
    }
}
