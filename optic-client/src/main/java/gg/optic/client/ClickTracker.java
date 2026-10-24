package gg.optic.client;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ClickTracker {
	private final Deque<Long> left = new ArrayDeque<>();
	private final Deque<Long> right = new ArrayDeque<>();

	public void click(int button) {
		long now = System.currentTimeMillis();
		if (button == 0) left.addLast(now);
		if (button == 1) right.addLast(now);
		trim(left, now);
		trim(right, now);
	}

	public int leftCps() {
		trim(left, System.currentTimeMillis());
		return left.size();
	}

	public int rightCps() {
		trim(right, System.currentTimeMillis());
		return right.size();
	}

	private static void trim(Deque<Long> clicks, long now) {
		while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
			clicks.removeFirst();
		}
	}
}
