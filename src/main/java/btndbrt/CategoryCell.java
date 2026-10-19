package btndbrt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

/**
 * How one category looks in the list: name and money left on the first line,
 * a bar showing how much of the budget is used on the second.
 */
public class CategoryCell extends JPanel implements ListCellRenderer<Category> {

    private final JLabel nameLabel = new JLabel();
    private final JLabel leftLabel = new JLabel();
    private final MeterBar meter = new MeterBar();
    private String currency = "";

    public CategoryCell() {
        super(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        top.add(nameLabel, BorderLayout.CENTER);
        top.add(leftLabel, BorderLayout.EAST);

        add(top, BorderLayout.CENTER);
        add(meter, BorderLayout.SOUTH);
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Category> list, Category c, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        nameLabel.setText(c.getName());
        boolean over = c.getRemaining().signum() < 0;
        if (over) {
            leftLabel.setText("Over by " + Money.format(c.getRemaining().negate(), currency));
        } else {
            leftLabel.setText(Money.format(c.getRemaining(), currency) + " left");
        }

        double budget = c.getBudgetAmount().doubleValue();
        meter.setValue(budget <= 0 ? (c.getSpent().signum() > 0 ? 1 : 0) : c.getSpent().doubleValue() / budget, over);

        // The selection color is a light blue (set in Main), so the normal text colors stay readable
        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        nameLabel.setForeground(ChartColors.TEXT);
        leftLabel.setForeground(over ? ChartColors.CRITICAL : ChartColors.TEXT_SECONDARY);
        return this;
    }

    /** A thin rounded bar: gray track, colored part = share of the budget used. */
    static class MeterBar extends JComponent {
        private double fraction;
        private boolean over;

        MeterBar() {
            setPreferredSize(new Dimension(10, 6));
        }

        void setValue(double fraction, boolean over) {
            this.fraction = Math.max(0, Math.min(1, fraction));
            this.over = over;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            g.setColor(ChartColors.GRID);
            g.fillRoundRect(0, 0, getWidth(), h, h, h);
            int filled = (int) Math.round(fraction * getWidth());
            if (filled > 0) {
                g.setColor(over ? ChartColors.CRITICAL : ChartColors.CATEGORIES[0]);
                g.fillRoundRect(0, 0, Math.max(filled, h), h, h, h);
            }
            g.dispose();
        }
    }
}
