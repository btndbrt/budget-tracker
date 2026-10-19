package btndbrt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * A small month calendar for picking a date. Swing has no date picker built in, so this is our own.
 * Use it with: {@code LocalDate date = CalendarDialog.pick(someComponent, initialDate);}
 */
public class CalendarDialog extends JDialog {

    private static final int CELLS = 7 * 6; // 6 weeks always fit any month, keeps the size stable

    private final LocalDate selected;
    private YearMonth shownMonth;
    private LocalDate result; // stays null if the user closes the window

    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel daysPanel = new JPanel(new GridLayout(0, 7, 2, 2));

    /** Shows the calendar and waits. Returns the chosen date, or null if the user closed it. */
    public static LocalDate pick(Component parent, LocalDate initial) {
        CalendarDialog dialog = new CalendarDialog(SwingUtilities.getWindowAncestor(parent), initial);
        dialog.setVisible(true); // blocks until the dialog is closed, because it is modal
        return dialog.result;
    }

    private CalendarDialog(Window owner, LocalDate initial) {
        super(owner, "Pick a date", ModalityType.APPLICATION_MODAL);
        selected = initial;
        shownMonth = YearMonth.from(initial);

        JButton previousButton = new JButton("<");
        JButton nextButton = new JButton(">");
        previousButton.addActionListener(e -> {
            shownMonth = shownMonth.minusMonths(1);
            fillDays();
        });
        nextButton.addActionListener(e -> {
            shownMonth = shownMonth.plusMonths(1);
            fillDays();
        });

        JPanel header = new JPanel(new BorderLayout());
        header.add(previousButton, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextButton, BorderLayout.EAST);

        JButton todayButton = new JButton("Today");
        todayButton.addActionListener(e -> choose(LocalDate.now()));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.add(todayButton);

        add(header, BorderLayout.NORTH);
        add(daysPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        fillDays();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Rebuilds the grid of day buttons for shownMonth. */
    private void fillDays() {
        monthLabel.setText(shownMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + shownMonth.getYear());
        daysPanel.removeAll();

        for (DayOfWeek day : DayOfWeek.values()) { // MONDAY ... SUNDAY
            daysPanel.add(new JLabel(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), SwingConstants.CENTER));
        }

        // Empty cells before day 1, e.g. 2 empty cells if the month starts on a Wednesday
        int emptyCells = shownMonth.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < emptyCells; i++) {
            daysPanel.add(new JLabel());
        }

        for (int day = 1; day <= shownMonth.lengthOfMonth(); day++) {
            LocalDate date = shownMonth.atDay(day);
            JButton button = new JButton(String.valueOf(day));
            button.setMargin(new Insets(2, 2, 2, 2));
            if (date.equals(selected)) {
                button.setFont(button.getFont().deriveFont(Font.BOLD));
            }
            button.addActionListener(e -> choose(date));
            daysPanel.add(button);
        }

        // Fill up the rest so every month has the same number of rows
        for (int i = emptyCells + shownMonth.lengthOfMonth(); i < CELLS; i++) {
            daysPanel.add(new JLabel());
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    private void choose(LocalDate date) {
        result = date;
        dispose();
    }
}
