package main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AppData {
	private static final int VERSION = 1;

	private static final String PATH = "C:/Users/" + System.getProperty("user.name") + "/AppData/Roaming/Goals Tracker/";
	private static File goalsFile;
	private static List<Goal> goals;

	public static void init() {
		File directory = new File(PATH);
		if (directory.isFile()) throw new Error("There's a file occupying " + PATH);
		if (!directory.isDirectory()) {
			if (!directory.mkdir()) throw new Error("Failed to create a directory at " + PATH);
		}

		goalsFile = new File(directory, "goals.bin");
		if (goalsFile.isDirectory()) throw new Error(goalsFile.getAbsolutePath() + " is occupied by a directory");
		if (goalsFile.exists()) {
			loadGoals();
			recalculatePlacement();
		} else goals = new ArrayList<>();
	}

	public static void save() {
		saveGoals();
	}

	private static void loadGoals() {
		byte[] data;
		try {
			data = Files.readAllBytes(Paths.get(goalsFile.toURI()));
		} catch (Exception e) {
			throw new Error("Failed to read the goals file (" + goalsFile.getAbsolutePath() + ")", e);
		}
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int version = buffer.getInt();
		if (version != VERSION)
			throw new Error("Incompatible goals file (running version " + VERSION + ", found " + version);

		int goalsNumber = buffer.getInt();
		goals = new ArrayList<>(goalsNumber);
		for (int i = 0; i < goalsNumber; i++) {
			int nameLength = buffer.getInt();
			byte[] nameBytes = new byte[nameLength];
			buffer.get(nameBytes);
			String name = new String(nameBytes, StandardCharsets.UTF_8);
			long initiatedAt = buffer.getLong();
			byte isCompleted = buffer.get();
			if (isCompleted != 0 && isCompleted != 1) throw new Error("isCompleted sign byte is neither 0 nor 1 ("
					+ isCompleted + ") at byte " + (buffer.arrayOffset() - 1));
			boolean isCompletedBool = isCompleted == 1;
			long completedAt = 0;
			if (isCompletedBool) completedAt = buffer.getLong();

			Goal g = isCompletedBool ? new Goal(name, initiatedAt, completedAt) : new Goal(name, initiatedAt);
			goals.add(g);
		}
		if (buffer.hasRemaining()) throw new Error("Leftover bytes after reading the whole file. Read "
				+ buffer.position() + ", total " + data.length);
	}

	private static void saveGoals() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ByteBuffer converter = ByteBuffer.allocate(8);
		byte[] intByteArray = new byte[4];
		// Version
		converter.putInt(0, VERSION);
		converter.get(0, intByteArray);
		output.writeBytes(intByteArray);
		// Goals number
		converter.putInt(0, goals.size());
		converter.get(0, intByteArray);
		output.writeBytes(intByteArray);
		// Goals loop
		for (Goal g : goals) {
			// Name
			byte[] name = g.getName().getBytes(StandardCharsets.UTF_8);
			converter.putInt(0, name.length);
			converter.get(0, intByteArray);
			output.writeBytes(intByteArray);
			output.writeBytes(name);
			// Initiated
			converter.putLong(0, g.getInitiated());
			output.writeBytes(converter.array());
			// Is completed
			output.write(g.isCompleted() ? 1 : 0);
			// Completed
			if (g.isCompleted()) {
				converter.putLong(0, g.getCompleted());
				output.writeBytes(converter.array());
			}
		}

		try {
			Files.write(Paths.get(goalsFile.toURI()), output.toByteArray());
		} catch (IOException e) {
			throw new Error("Failed to save the goals file", e);
		}
	}

	public static void addGoal(Goal g) {
		goals.add(g);
		recalculatePlacement();
	}

	public static void removeGoal(Goal g) {
		goals.remove(g);
		AppData.recalculatePlacement();
	}

	public static List<Goal> getGoals() {
		return goals;
	}

	public static void recalculatePlacement() {
		goals.sort(Comparator.comparingLong(Goal::getInitiated));

		for (Goal g : goals) {

			List<Goal> overlappingBefore = getOverlappingGoalsBefore(g);
			if (overlappingBefore.isEmpty()) {
				g.displayLevel = 0;
				continue;
			}
			overlappingBefore.sort(Comparator.comparing(goal -> goal.displayLevel));

			int lastListIndex = overlappingBefore.size() - 1;
			if (overlappingBefore.get(lastListIndex).displayLevel == lastListIndex) {
				// All level are occupied; take the next available one.
				g.displayLevel = lastListIndex + 1;
				continue;
			}

			for (int i = 0; i < overlappingBefore.size(); i++) {
				if (i != overlappingBefore.get(i).displayLevel) {
					g.displayLevel = i;
					break;
				}
			}
		}
	}

	public static List<Goal> getOverlappingGoalsBefore(Goal g) {
		int index = goals.indexOf(g);
		if (index == -1) throw new IllegalArgumentException("Unlisted goal");
		List<Goal> result = new ArrayList<>();
		for (int i = 0; i < index; i++) {
			Goal gi = goals.get(i);
			if (gi.isCompleted()) {
				if (gi.getCompleted() > g.getInitiated()) result.add(gi);
			} else {
				result.add(gi);
			}
		}
		return result;
	}
}
