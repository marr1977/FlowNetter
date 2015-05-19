package flownetter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowCollection {

	private List<Flow> flows = new ArrayList<>();
	
	private Map<Entity, Map<Entity, Collection<Flow>>> flowBySink = new HashMap<>();
	private Map<Entity, Map<Entity, Collection<Flow>>> flowBySource = new HashMap<>();

	private boolean changed;

	public List<Flow> getFlows() {
		return flows;
	}
	
	public Collection<Entity> getSources() {
		return new ArrayList<Entity>(flowBySource.keySet());
	}
	
	public Collection<Entity> getSinks(Entity source) {
		Map<Entity, Collection<Flow>> map = flowBySource.get(source);
		
		if (map == null) {
			return Collections.<Entity>emptyList();
		}
		
		return new ArrayList<Entity>(map.keySet());
	}
	
	public Collection<Entity> getSources(Entity sink) {
		Map<Entity, Collection<Flow>> map = flowBySink.get(sink);
		
		if (map == null) {
			return Collections.<Entity>emptyList();
		}
		
		return new ArrayList<Entity>(map.keySet());
	}

	public List<Flow> getFlows(Entity source, Entity sink) {
		Map<Entity, Collection<Flow>> map = flowBySource.get(source);
		
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
			Map<Entity, Map<Entity, Collection<Flow>>> collection,
			Entity primaryKey, 
			Entity secondaryKey,
			Flow flow) {
		
		Map<Entity, Collection<Flow>> map = collection.get(primaryKey);
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
			Map<Entity, Map<Entity, Collection<Flow>>> collection,
			Entity primaryKey, 
			Entity secondaryKey,
			Flow flow) {
		
		Map<Entity, Collection<Flow>> map = collection.get(primaryKey);
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
	