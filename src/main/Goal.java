package main;

public class Goal {
	private String name;
	private long initiated;
	private long completed;
	private boolean isCompleted;
	public int displayLevel;

	public Goal(String name, long initiated, long completed) {
		this.name = name;
		this.initiated = initiated;
		this.completed = completed;
		isCompleted = true;
	}

	public Goal(String name, long initiated) {
		this.name = name;
		this.initiated = initiated;
	}

	public void complete() {
		if (isCompleted) throw new IllegalStateException("Already completed");
		completed = System.currentTimeMillis();
		isCompleted = true;
		AppData.recalculatePlacement();
	}

	public void complete(long at) {
		if (at < initiated) throw new IllegalArgumentException("Cannot complete before the initiation (init "
				+ initiated + ", complete " + at + ")");
		if (at > System.currentTimeMillis())
			throw new IllegalArgumentException("Cannot complete in the point in the future (" + at + ")");

		completed = at;
		isCompleted = true;
		AppData.recalculatePlacement();
	}

	public void cancelCompletion() {
		if (!isCompleted) throw new IllegalStateException("Not completed to be cancelled");
		isCompleted = false;
		completed = 0;
		AppData.recalculatePlacement();
	}

	public void setInitiated(long at) {
		if (isCompleted && at > completed) throw new IllegalArgumentException("Cannot start after the end");
		if (at > System.currentTimeMillis())
			throw new IllegalArgumentException("Cannot start in the point in the future");
		initiated = at;
		AppData.recalculatePlacement();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public long getInitiated() {
		return initiated;
	}

	public long getCompleted() {
		return completed;
	}

	public boolean isCompleted() {
		return isCompleted;
	}
}
