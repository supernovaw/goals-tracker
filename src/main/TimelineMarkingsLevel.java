package main;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.function.BiConsumer;

public interface TimelineMarkingsLevel {
	TimelineMarkingsLevel seconds = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
			for (long t = start - start % 1000; t <= end; t += 1000)
				consumer.accept(t, f.format(t));
		}

		@Override
		public long getAveragePeriodicity() {
			return 1000;
		}
	};
	TimelineMarkingsLevel minutes = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("HH:mm");
			for (long t = start - start % 60_000; t <= end; t += 60_000)
				consumer.accept(t, f.format(t));
		}

		@Override
		public long getAveragePeriodicity() {
			return 60_000;
		}
	};
	TimelineMarkingsLevel hourQuarters = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("HH:mm");
			for (long t = start - start % (900_000); t <= end; t += 900_000)
				consumer.accept(t, f.format(t));
		}

		@Override
		public long getAveragePeriodicity() {
			return 900_000;
		}
	};
	TimelineMarkingsLevel hours = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("HH:mm");
			for (long t = start - start % 3600_000; t <= end; t += 3600_000)
				consumer.accept(t, f.format(t));
		}

		@Override
		public long getAveragePeriodicity() {
			return 3600_000;
		}
	};
	TimelineMarkingsLevel sixHours = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("E HH:mm");
			for (long t = start - (start + f.getTimeZone().getRawOffset()) % 21600_000; t <= end; t += 21600_000)
				consumer.accept(t, f.format(t));
		}

		@Override
		public long getAveragePeriodicity() {
			return 21600_000;
		}
	};
	TimelineMarkingsLevel days = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("MMM dd");
			for (long t = start - (start + f.getTimeZone().getRawOffset()) % 86400_000; t <= end; t += 86400_000)
				consumer.accept(t, f.format(t));
		}

		@Override
		public long getAveragePeriodicity() {
			return 86400_000;
		}
	};
	TimelineMarkingsLevel weeks = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("MMM dd");
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(start - start % 86400_000); // trim units up to hours
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.DAY_OF_WEEK, 1);
			for (long t = c.getTimeInMillis(); t <= end; t += 604800_000)
				consumer.accept(t, f.format(t));
		}

		@Override
		public long getAveragePeriodicity() {
			return 604800_000;
		}
	};
	TimelineMarkingsLevel months = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("MMM");
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(start - start % 86400_000); // trim units up to hours
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.DAY_OF_MONTH, 1);
			while (c.getTimeInMillis() <= end) {
				long t = c.getTimeInMillis();
				consumer.accept(t, f.format(t));
				c.add(Calendar.MONTH, 1);
				c.set(Calendar.DAY_OF_MONTH, 1);
			}
		}

		@Override
		public long getAveragePeriodicity() {
			return 2629756_800L;
		}
	};
	TimelineMarkingsLevel years = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("yyyy");
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(start - start % 86400_000); // trim units up to hours
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.DAY_OF_YEAR, 1);
			while (c.getTimeInMillis() <= end) {
				long t = c.getTimeInMillis();
				consumer.accept(t, f.format(t));
				c.add(Calendar.YEAR, 1);
				c.set(Calendar.DAY_OF_YEAR, 1);
			}
		}

		@Override
		public long getAveragePeriodicity() {
			return 31556956_320L;
		}
	};
	TimelineMarkingsLevel decades = new TimelineMarkingsLevel() {
		@Override
		public void listMarkings(long start, long end, BiConsumer<Long, String> consumer) {
			SimpleDateFormat f = new SimpleDateFormat("yyyy");
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(start - start % 86400_000); // trim units up to hours
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.DAY_OF_YEAR, 1);
			c.set(Calendar.YEAR, c.get(Calendar.YEAR) / 10 * 10);
			while (c.getTimeInMillis() <= end) {
				long t = c.getTimeInMillis();
				consumer.accept(t, f.format(t));
				c.add(Calendar.YEAR, 10);
				c.set(Calendar.DAY_OF_YEAR, 1);
			}
		}

		@Override
		public long getAveragePeriodicity() {
			return 315569563_200L;
		}
	};

	void listMarkings(long startTimestamp, long endTimestamp, BiConsumer<Long, String> timestampNameConsumer);

	long getAveragePeriodicity();
}
