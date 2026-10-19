package btndbrt;

import java.math.BigDecimal;

/** Turns amounts into text, so the window and the charts show money the same way. */
public class Money {

    /** Two decimals and the currency, e.g. "1,500.00 HUF". */
    public static String format(BigDecimal amount, String currency) {
        return withCurrency(String.format("%,.2f", amount), currency);
    }

    /** Whole numbers only, for chart axes where space is short, e.g. "1,500 HUF". */
    public static String formatShort(double amount, String currency) {
        return withCurrency(String.format("%,.0f", amount), currency);
    }

    private static String withCurrency(String text, String currency) {
        return currency.isEmpty() ? text : text + " " + currency;
    }
}
