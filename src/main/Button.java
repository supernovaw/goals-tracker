package main;

import java.awt.*;
import java.awt.event.MouseEvent;

public class Button {
	private int x, y, width, height;
	private String text;
	private Runnable onClick;

	public Button(String text, int x, int y, int width, int height, Runnable onClick) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.text = text;
		this.onClick = onClick;
	}

	public void onClick(MouseEvent e) {
		if (new Rectangle(x, y, width, height).contains(e.getPoint())) onClick.run();
	}

	public void paint(Graphics2D g) {
		g.setColor(Color.gray);
		g.drawRoundRect(x, y, width, height, 5, 5);
		g.setFont(g.getFont().deriveFont(25f));
		FontMetrics fm = g.getFontMetrics();
		g.drawString(text, x + width / 2 - fm.stringWidth(text) / 2, y + height / 2 + (fm.getAscent() - fm.getDescent()) / 2);
	}
}
