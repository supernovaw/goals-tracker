package main;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Consumer;

public class Timeline {
	private static final int GOAL_STRIP_THICKNESS = 20, GOAL_STRIPS_GAP = 5;

	private static final List<TimelineMarkingsLevel> LEVELS = List.of(TimelineMarkingsLevel.seconds,
			TimelineMarkingsLevel.minutes, TimelineMarkingsLevel.hourQuarters, TimelineMarkingsLevel.hours,
			TimelineMarkingsLevel.sixHours, TimelineMarkingsLevel.days, TimelineMarkingsLevel.weeks,
			TimelineMarkingsLevel.months, TimelineMarkingsLevel.years, TimelineMarkingsLevel.decades);

	private final double zoomSpeed = 0.5;
	private final long minRange = 60_000; // 1 minute
	private final long maxRange = 100L * 365 * 24 * 60 * 60_000; // 100 years
	private final int minPixelsBetweenNamedMarkings = 150;
	private final int zoomAnimationDuration = 200;
	private final SimpleDateFormat pointerFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
	private final SimpleDateFormat detailsFormat = new SimpleDateFormat("MMM d");

	private int x, y, width, height, timelineY;
	private double startTimestamp, endTimestamp;
	private double startTimestampPrev, endTimestampPrev, timestampChangeTimestamp;
	private boolean zoomAnimationFinished = true;
	private int mainLevel;
	private double mouseTimestamp;
	private Goal hoveredGoal;
	private String hoveredGoalInfo;
	private Consumer<Long> clickListener;
	private Consumer<Goal> goalClickListener;

	public Timeline() {
		long time = System.currentTimeMillis();
		long range = 24 * 60 * 60_1000;
		startTimestamp = time - range / 2d;
		endTimestamp = time + range / 2d;
		mouseTimestamp = time;
	}

	public void paint(Graphics2D g) {
		double start = start(), end = end();

		g.translate(x, y);
		g.setColor(new Color(21, 21, 21));
		g.fillRect(0, 0, width, height);
		g.setColor(Color.lightGray);

		paintTimeline(g, start, end);
		paintTimelinePointers(g);
		paintGoals(g);

		g.setColor(Color.darkGray);
		g.drawString(pointerFormat.format(mouseTimestamp), 10, timelineY + 40);

		if (hoveredGoalInfo != null) {
			g.setColor(Color.gray);
			g.drawString(hoveredGoalInfo, width - g.getFontMetrics().stringWidth(hoveredGoalInfo) - 20, timelineY + 40);
		}

		g.translate(-x, -y);
	}

	private void paintTimeline(Graphics2D g, double start, double end) {
		if (!zoomAnimationFinished) mainLevel = suitableMainMarkingsLevel();

		g.drawLine(0, timelineY, width, timelineY);

		g.setFont(g.getFont().deriveFont(20f));
		LEVELS.get(mainLevel).listMarkings((long) start, (long) end, (t, name) -> {
			int x = unixToX(t);
			g.drawLine(x, timelineY - 10, x, timelineY);
			g.drawString(name, x + 5, timelineY - 5);
		});
		if (mainLevel != 0) {
			LEVELS.get(mainLevel - 1).listMarkings((long) start, (long) end, (t, name) -> {
				int x = unixToX(t);
				g.drawLine(x, timelineY + 5, x, timelineY);
			});
		}
	}

	private void paintTimelinePointers(Graphics g) {
		int currentTimeX = unixToX(System.currentTimeMillis());
		g.fillPolygon(new int[]{currentTimeX, currentTimeX - 5, currentTimeX + 5},
				new int[]{timelineY + 5, timelineY + 15, timelineY + 15}, 3);
		int mouseX = unixToX(mouseTimestamp);
		g.fillPolygon(new int[]{mouseX, mouseX - 5, mouseX + 5},
				new int[]{timelineY + 15, timelineY + 25, timelineY + 25}, 3);
	}

	private void paintGoals(Graphics2D g) {
		AppData.getGoals().forEach(goal -> paintGoal(g, goal));
	}

	private void paintGoal(Graphics2D g, Goal goal) {
		if (isGoalOutsideBounds(goal)) return;
		Rectangle b = goalBounds(goal);

		g.setFont(g.getFont().deriveFont(18f));
		FontMetrics fm = g.getFontMetrics();
		Color bgColor = goal == hoveredGoal ? new Color(205, 205, 205) : Color.lightGray;
		if (goal.isCompleted()) bgColor = bgColor.darker();
		g.setColor(bgColor);

		g.fillRoundRect(b.x, b.y, b.width, b.height, 5, 5);
		String title = goal.getName() + (goal.isCompleted() ? "" : "   " + msToUnitName(System.currentTimeMillis() - goal.getInitiated()));
		int stringWidth = fm.stringWidth(title);
		if (stringWidth < (b.width - 10)) {
			g.setColor(Color.black);
			g.drawString(title, b.x + (b.width - stringWidth) / 2,
					b.y + GOAL_STRIP_THICKNESS / 2 + (fm.getAscent() - fm.getDescent()) / 2);
		}
	}

	public void mousePressed(MouseEvent e) {
		if (goalClickListener != null) {
			Goal g = getGoalAt(e.getX() - x, e.getY() - y);
			if (g != null) {
				goalClickListener.accept(g);
				return;
			}
		}
		if (clickListener != null) {
			if (new Rectangle(x, y, width, height).contains(e.getPoint()))
				clickListener.accept(xToUnix(e.getX() - x));
		}
	}

	public void mouseMoved(MouseEvent e) {
		mouseTimestamp = xToUnix(e.getX() - x);
		hoveredGoal = getGoalAt(e.getX() - x, e.getY() - y);
		if (hoveredGoal != null) {
			StringBuilder sb = new StringBuilder();

			if (hoveredGoal.isCompleted()) {
				sb.append(detailsFormat.format(hoveredGoal.getInitiated()));
				sb.append(" to ");
				sb.append(detailsFormat.format(hoveredGoal.getCompleted()));

				sb.append(" (");
				sb.append(msToUnitName(hoveredGoal.getCompleted() - hoveredGoal.getInitiated()));
				sb.append(")");
			} else {
				sb.append("Initiated ");
				sb.append(msToUnitName(System.currentTimeMillis() - hoveredGoal.getInitiated()));
				sb.append(" ago (on ");
				sb.append(detailsFormat.format(hoveredGoal.getInitiated()));
				sb.append(")");
			}
			sb.append("     ");
			sb.append(hoveredGoal.getName());

			hoveredGoalInfo = sb.toString();
		} else hoveredGoalInfo = null;
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		mouseTimestamp = xToUnix(e.getX() - x);
		double xFraction = (double) (e.getX() - x) / width;
		long mouseUnix = xToUnix(e.getX() - x);
		double initialRange = end() - start();
		double newRange = initialRange * Math.exp(e.getPreciseWheelRotation() * zoomSpeed);
		newRange = Math.min(maxRange, Math.max(minRange, newRange));
		double newStart = mouseUnix - newRange * xFraction;
		double newEnd = mouseUnix + newRange * (1 - xFraction);
		mainLevel = suitableMainMarkingsLevel();
		setTimestamps(newStart, newEnd);
	}

	public void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		timelineY = height - 50;
		mainLevel = suitableMainMarkingsLevel();
	}

	public void setClickListener(Consumer<Long> listener) {
		clickListener = listener;
	}

	public void setGoalClickListener(Consumer<Goal> listener) {
		goalClickListener = listener;
	}

	private long xToUnix(int xWithin) {
		double start = start();
		double range = end() - start;
		double fraction = (double) xWithin / width;
		return (long) (start + (fraction * range));
	}

	private int unixToX(double unix) {
		double start = start();
		double fraction = (unix - start) / (end() - start);
		return (int) Math.round(width * fraction);
	}

	private boolean isGoalOutsideBounds(Goal g) {
		// the goal started after the end of range
		if (g.getInitiated() > end()) return true;

		// the goal ended before the start of range
		long goalEnd = g.isCompleted() ? g.getCompleted() : System.currentTimeMillis();
		return goalEnd < start();
	}

	private Rectangle goalBounds(Goal g) {
		int startX = unixToX(g.getInitiated());
		int endX = unixToX(g.isCompleted() ? g.getCompleted() : System.currentTimeMillis());
		startX = Math.max(startX, -10);
		endX = Math.min(endX, width + 10);
		int top = timelineY - GOAL_STRIP_THICKNESS - 30 - g.displayLevel * (GOAL_STRIP_THICKNESS + GOAL_STRIPS_GAP);

		return new Rectangle(startX, top, endX - startX, GOAL_STRIP_THICKNESS);
	}

	private Goal getGoalAt(int xWithin, int yWithin) {
		for (Goal g : AppData.getGoals()) {
			if (isGoalOutsideBounds(g)) continue;
			Rectangle bounds = goalBounds(g);
			if (bounds.contains(xWithin, yWithin)) {
				return g;
			}
		}
		return null;
	}

	private double zoomEase(double f) {
		return 1 - Math.pow(1 - f, 3);
	}

	private double start() {
		if (zoomAnimationFinished) return startTimestamp;
		double f = (System.currentTimeMillis() - timestampChangeTimestamp) / zoomAnimationDuration;
		f = zoomEase(f);
		if (f >= 1) {
			zoomAnimationFinished = true;
			return startTimestamp;
		}
		return (1 - f) * startTimestampPrev + f * startTimestamp;
	}

	private double end() {
		if (zoomAnimationFinished) return endTimestamp;
		double f = (System.currentTimeMillis() - timestampChangeTimestamp) / zoomAnimationDuration;
		f = zoomEase(f);
		if (f >= 1) {
			zoomAnimationFinished = true;
			return endTimestampPrev;
		}
		return (1 - f) * endTimestampPrev + f * endTimestamp;
	}

	private void setTimestamps(double start, double end) {
		startTimestampPrev = startTimestamp;
		startTimestamp = start;
		endTimestampPrev = endTimestamp;
		endTimestamp = end;
		timestampChangeTimestamp = System.currentTimeMillis();
		zoomAnimationFinished = false;
	}

	private int suitableMainMarkingsLevel() {
		long range = (long) (end() - start());
		for (int i = 0; i < LEVELS.size(); i++) {
			TimelineMarkingsLevel l = LEVELS.get(i);
			long expectedMarkings = range / l.getAveragePeriodicity();
			if (expectedMarkings == 0) return i;
			long pixelsPerMarking = width / expectedMarkings;
			if (pixelsPerMarking > minPixelsBetweenNamedMarkings) return i;
		}
		return LEVELS.size() - 1;
	}

	private String msToUnitName(long ms) {
		if (ms < 0) throw new IllegalArgumentException("Negative argument");
		long s = ms / 1000;
		String result;
		if (s < 60) result = s + " seconds";
		else if (s < 3600) result = s / 60 + " minutes";
		else if (s < 86400) result = s / 3600 + " hours";
		else if (s < 2629756) result = s / 86400 + " days";
		else result = s / 2629756 + " months";
		if (result.startsWith("1 ")) result = result.substring(0, result.length() - 1);
		return result;
	}
}
