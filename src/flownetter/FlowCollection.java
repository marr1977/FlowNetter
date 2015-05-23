package flownetter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowCollection {

	private List<Flow> flows = new ArrayList<>();
	
	private Map<String, Map<String, Collection<Flow>>> flowBySink = new HashMap<>();
	private Map<String, Map<String, Collection<Flow>>> flowBySource = new HashMap<>();

	private boolean changed;

	public List<Flow> getFlows() {
		return flows;
	}
	
	public Collection<String> getSources() {
		return new ArrayList<String>(flowBySource.keySet());
	}
	
	public Collection<String> getSinks(String source) {
		Map<String, Collection<Flow>> map = flowBySource.get(source);
		
		if (map == null) {
			return Collections.<String>emptyList();
		}
		
		return new ArrayList<String>(map.keySet());
	}
	
	public Collection<String> getSources(String sink) {
		Map<String, Collection<Flow>> map = flowBySink.get(sink);
		
		if (map == null) {
			return Collections.<String>emptyList();
		}
		
		return new ArrayList<String>(map.keySet());
	}

	public List<Flow> getFlows(String source, String sink) {
		Map<String, Collection<Flow>> map = flowBySource.get(source);
		
		if (map != null) {
			Collection<Flow> flows = map.get(sink);
			if (flows != null) {
				return new ArrayList<>(flows);
			}
		}
		
		return Collections.<Flow>emptyList();
	}

	public void add(Flow flow) {
		setChanged(true);
		
		flows.add(flow);
	
		add(flowBySink, flow.getSink(), flow.getSource(), flow);
		add(flowBySource, flow.getSource(), flow.getSink(), flow);
	}
	

	public void remove(Flow flow) {
		setChanged(true);
		
		flows.remove(flow);
	
		remove(flowBySink, flow.getSink(), flow.getSource(), flow);
		remove(flowBySource, flow.getSource(), flow.getSink(), flow);
	}
	
	private void add(
			Map<String, Map<String, Collection<Flow>>> collection,
			String primaryKey, 
			String secondaryKey,
			Flow flow) {
		
		Map<String, Collection<Flow>> map = collection.get(primaryKey);
		if (map == null) {
			map = new HashMap<>();
			collection.put(primaryKey, map);
		}
		
		Collection<Flow> flows = map.get(secondaryKey);
		
		if (flows == null) {
			flows = new ArrayList<>();
			map.put(secondaryKey, flows);
		}

		flows.add(flow);
	}

	private void remove(
			Map<String, Map<String, Collection<Flow>>> collection,
			String primaryKey, 
			String secondaryKey,
			Flow flow) {
		
		Map<String, Collection<Flow>> map = collection.get(primaryKey);
		if (map == null) {
			return;
		}
		
		Collection<Flow> flows = map.get(secondaryKey);
		
		if (flows == null) {
			return;
		}

		flows.remove(flow);
		
		if (flows.isEmpty()) {
			map.remove(secondaryKey);
			
			if (map.isEmpty()) {
				collection.remove(primaryKey);
			}
		}
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	
	public boolean isChanged() {
		return changed;
	}

	public void remove(Collection<Flow> flows) {
		flows.stream()
			.forEach(flow -> remove(flow));
	}
}
	