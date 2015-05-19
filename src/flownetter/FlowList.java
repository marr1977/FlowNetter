package flownetter;

import java.util.ArrayList;
import java.util.Optional;
import java.util.OptionalLong;

public class FlowList extends ArrayList<Flow> {
	private static final long serialVersionUID = 5315867946953010969L;

	public OptionalLong getSmallestValue() {
		return stream().mapToLong(Flow::getValue).min();
	}
	
	public Optional<Flow> getFlow(long value) {
		return stream().filter(
			flow -> value == flow.getValue()).findFirst();
	}
	
	public Optional<Flow> getSmallestFlow(long minBound) {
		OptionalLong smallestFlow = stream().filter(
			flow -> (flow.getValue() >= minBound))
			.mapToLong(Flow::getValue)
			.min();
			
		if (!smallestFlow.isPresent()) {
			return Optional.empty();
		}
		
		return stream().filter(
			flow -> smallestFlow.getAsLong() == flow.getValue()).findFirst();
	}

	public long getTotalValue() {
		return stream().mapToLong(Flow::getValue).sum();
	}

}
