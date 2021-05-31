package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class UI {
	private static final int POPUP_DURATION = 3500;

	private JFrame frame;
	private JPanel content;
	private Timeline timeline;
	private List<Button> buttons;
	private int width, height;
	private String popupMessage;
	private long popupTimestamp;
	private boolean popupAnimationFinished = true;

	public UI() {
		frame = new JFrame();
		frame.setSize(1500, 900);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setTitle("Goals Tracker");
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				AppData.save();
			}
		});

		Font font = new Font("SF Pro Display", Font.PLAIN, 25);

		content = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(Color.black);
				g2d.fillRect(0, 0, getWidth(), getHeight());
				g2d.setFont(font);
				UI.this.paint(g2d);
				paintPopup(g2d);
			}
		};
		content.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				onResized(content.getWidth(), content.getHeight());
			}
		});
		MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				timeline.mouseWheelMoved(e);
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				timeline.mouseMoved(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				timeline.mouseMoved(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				buttons.forEach(b -> b.onClick(e));
				timeline.mousePressed(e);
			}
		};
		content.addMouseListener(mouseAdapter);
		content.addMouseMotionListener(mouseAdapter);
		content.addMouseWheelListener(mouseAdapter);
		frame.setContentPane(content);

		timeline = new Timeline();
		initButtons();
		new Timer(1000 / 60, e -> content.repaint()).start();
	}

	private void initButtons() {
		int baseY = 650;
		buttons = new ArrayList<>();
		buttons.add(new Button("Add goal", 50, baseY, 250, 30, this::onAddGoalClicked));
		buttons.add(new Button("Add goal in the past", 310, baseY, 280, 30, this::onAddGoalInPastClicked));
		buttons.add(new Button("Complete goal", 50, baseY + 40, 250, 30, this::onCompleteGoalClicked));
		buttons.add(new Button("Set completion point", 310, baseY + 40, 280, 30, this::onSetCompletionPointClicked));
		buttons.add(new Button("Delete", 50, baseY + 80, 250, 30, this::onDeleteClicked));
		buttons.add(new Button("Cancel completion", 310, baseY + 80, 280, 30, this::onCancelCompletionClicked));
		buttons.add(new Button("Rename", 50, baseY + 120, 250, 30, this::onRenameClicked));
		buttons.add(new Button("Change goal start", 310, baseY + 120, 280, 30, this::onChangeGoalStartClicked));
	}

	private void onAddGoalClicked() {
		String name = JOptionPane.showInputDialog("Enter goal name");
		if (name == null) return;

		AppData.addGoal(new Goal(name, System.currentTimeMillis()));
	}

	private void onAddGoalInPastClicked() {
		String name = JOptionPane.showInputDialog("Enter goal name");
		if (name == null) return;

		timeline.setClickListener(unix -> {
			if (unix > System.currentTimeMillis()) {
				showPopup("Cannot set the start to a moment in the future");
				return;
			}
			AppData.addGoal(new Goal(name, unix));
			timeline.setClickListener(null);
		});
		showPopup("Click on when the goal was set");
	}

	private void onCompleteGoalClicked() {
		timeline.setGoalClickListener(g -> {
			if (g.isCompleted()) {
				showPopup("This goal is already completed");
				return;
			}
			g.complete();
			timeline.setGoalClickListener(null);
		});
		showPopup("Click the goal that is completed");
	}

	private void onSetCompletionPointClicked() {
		timeline.setGoalClickListener(g -> {
			timeline.setClickListener(unix -> {
				if (unix <= g.getInitiated()) {
					showPopup("Cannot set the end before the start");
					return;
				}
				if (unix > System.currentTimeMillis()) {
					showPopup("Cannot set the end to a moment in the future");
					return;
				}
				g.complete(unix);
				timeline.setClickListener(null);
			});
			timeline.setGoalClickListener(null);
			showPopup("Click on when the goal was completed");
		});
		showPopup("Click the goal that was completed in the past");
	}

	private void onDeleteClicked() {
		timeline.setGoalClickListener(g -> {
			AppData.removeGoal(g);
			timeline.setGoalClickListener(null);
		});
		showPopup("Click the goal to delete");
	}

	private void onCancelCompletionClicked() {
		timeline.setGoalClickListener(g -> {
			if (!g.isCompleted()) {
				showPopup("This goal is not completed");
				return;
			}
			g.cancelCompletion();
			timeline.setGoalClickListener(null);
		});
		showPopup("Click the goal to cancel its completion");
	}

	private void onRenameClicked() {
		timeline.setGoalClickListener(g -> {
			timeline.setGoalClickListener(null);
			String name = JOptionPane.showInputDialog("Enter new goal name");
			if (name == null) return;
			g.setName(name);
		});
		showPopup("Click the goal to rename");
	}

	private void onChangeGoalStartClicked() {
		timeline.setGoalClickListener(g -> {
			timeline.setGoalClickListener(null);
			timeline.setClickListener(unix -> {
				if (unix > System.currentTimeMillis()) {
					showPopup("Cannot set the start in the future");
					return;
				}
				if (g.isCompleted() && unix > g.getCompleted()) {
					showPopup("Cannot set the start after the end");
					return;
				}
				g.setInitiated(unix);
				timeline.setClickListener(null);
			});
			showPopup("Click on when the goal started");
		});
		showPopup("Click the goal to change its starting point");
	}

	public void setVisible(boolean visible) {
		frame.setVisible(visible);
	}

	private void paint(Graphics2D g) {
		timeline.paint(g);
		buttons.forEach(b -> b.paint(g));
	}

	private void onResized(int newWidth, int newHeight) {
		width = newWidth;
		height = newHeight;

		Rectangle r = getAlignmentForTimeline();
		timeline.setBounds(r.x, r.y, r.width, r.height);
	}

	private Rectangle getAlignmentForTimeline() {
		return new Rectangle(0, 0, width, 600);
	}

	private void showPopup(String message) {
		popupMessage = message;
		popupTimestamp = System.currentTimeMillis();
		popupAnimationFinished = false;
	}

	private void paintPopup(Graphics2D g) {
		if (popupAnimationFinished) return;
		g.setColor(Color.white);
		g.setFont(g.getFont().deriveFont(40f));

		int t = (int) (System.currentTimeMillis() - popupTimestamp);
		double f = (double) t / POPUP_DURATION;
		if (f >= 1) {
			popupAnimationFinished = true;
			return;
		}
		double shiftDownF = Math.max(f - 0.75, 0) * 4;
		shiftDownF = Math.pow(shiftDownF, 1.5);
		int shiftDown = (int) (shiftDownF * 30);

		double alpha = Math.min(1d, 6d * f);
		alpha = Math.min(4 - 4 * f, alpha);
		alpha = 0.5 - Math.cos(alpha * Math.PI) / 2;

		Composite originalComposite = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
		g.drawString(popupMessage, (width - g.getFontMetrics().stringWidth(popupMessage)) / 2, height - 30 + shiftDown);
		g.setComposite(originalComposite);
	}
}
