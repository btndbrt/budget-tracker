package btndbrt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** The whole budget: a list of categories. The net budget is the sum of all category amounts. */
public class Budget {

    private final List<Category> categories = new ArrayList<>();

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

    /** The net budget: all category amounts added together. */
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
}
