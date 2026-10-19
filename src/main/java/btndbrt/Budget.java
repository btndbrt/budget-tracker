package btndbrt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** The whole budget: a list of categories. The net total is the sum of all categories. */
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

    public BigDecimal getNetTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (Category c : categories) {
            total = total.add(c.getTotal());
        }
        return total;
    }
}
