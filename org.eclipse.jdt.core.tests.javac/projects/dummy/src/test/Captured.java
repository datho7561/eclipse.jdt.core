package test;

import java.util.List;

class Captured {
	void m() {
		Object o = List.of();
		if (o instanceof List<?> list) {
			list.stream().COMPLETE_HERE();
		}
	}
}