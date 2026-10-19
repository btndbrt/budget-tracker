package btndbrt;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** A text field for a date (YYYY-MM-DD) with a "Pick" button that opens the calendar. */
public class DateField extends JPanel {

    private final JTextField textField = new JTextField(10);

    public DateField(LocalDate date) {
        super(new BorderLayout(3, 0));
        textField.setText(date.toString());

        JButton pickButton = new JButton("Pick");
        pickButton.addActionListener(e -> {
            LocalDate current = getDate();
            LocalDate picked = CalendarDialog.pick(this, current != null ? current : LocalDate.now());
            if (picked != null) {
                textField.setText(picked.toString());
            }
        });

        add(textField, BorderLayout.CENTER);
        add(pickButton, BorderLayout.EAST);
    }

    /** The typed or picked date, or null if the text is not a valid date. */
    public LocalDate getDate() {
        try {
            return LocalDate.parse(textField.getText().trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
