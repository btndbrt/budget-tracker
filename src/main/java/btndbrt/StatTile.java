package btndbrt;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A small box with a caption, one big number and a line of detail, e.g. "Spent / 160,290 HUF / 65% of budget". */
public class StatTile extends JPanel {

    private final JLabel valueLabel = new JLabel(" ");
    private final JLabel detailLabel = new JLabel(" ");

    public StatTile(String caption) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false); // we paint the rounded background ourselves
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setForeground(ChartColors.TEXT_SECONDARY);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, valueLabel.getFont().getSize() + 7f));
        valueLabel.setForeground(ChartColors.TEXT);
        detailLabel.setForeground(ChartColors.MUTED);

        add(captionLabel);
        add(valueLabel);
        add(detailLabel);
    }

    /** {@code warning} shows the value in red, e.g. when the budget is overspent. */
    public void setValue(String value, String detail, boolean warning) {
        valueLabel.setText(value);
        valueLabel.setForeground(warning ? ChartColors.CRITICAL : ChartColors.TEXT);
        detailLabel.setText(detail.isEmpty() ? " " : detail); // " " keeps the height the same
        detailLabel.setForeground(warning ? ChartColors.CRITICAL : ChartColors.MUTED);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ChartColors.SURFACE);
        g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Card.ARC, Card.ARC);
        g.setColor(Card.BORDER);
        g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Card.ARC, Card.ARC);
        g.dispose();
        super.paintComponent(graphics);
    }
}
