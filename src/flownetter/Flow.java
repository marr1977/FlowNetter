package flownetter;

public class Flow {
	private String sink;
	private String source;
	private long value;

	public Flow(String source, String sink, long value) {
		this.sink = sink;
		this.source = source;
		this.value = value;
	}

	public Flow(Flow flow) {
		this(flow.getSource(), flow.getSink(), flow.getValue());
	}

	public String getSink() {
		return sink;
	}

	public String getSource() {
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
