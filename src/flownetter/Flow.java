package flownetter;

public class Flow {
	private Entity sink;
	private Entity source;
	private long value;

	public Flow(Entity source, Entity sink, long value) {
		this.sink = sink;
		this.source = source;
		this.value = value;
	}

	public Flow(Flow flow) {
		this(flow.getSource(), flow.getSink(), flow.getValue());
	}

	public Entity getSink() {
		return sink;
	}

	public Entity getSource() {
		return source;
	}

	public long getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return source + " -> " + sink + " " + value;
	}
}
