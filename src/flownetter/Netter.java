package flownetter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalLong;

public class Netter {

	FlowCollection flowCollection = new FlowCollection();
	
	public void add(Flow flow) {
		flowCollection.add(flow);
	}
	
	
	public List<Flow> net() {
		Map<String, Long> netBefore = getNets();
		
		aggregateSameSourceAndSink();
		
		while (netRound());
		
		verifyEquals(netBefore, getNets());
		 
		return flowCollection.getFlows();
	}
	
	/**
	 * Returns a map containing the net value for each node
	 */
	private Map<String, Long> getNets() {
		Map<String, Long> nets = new HashMap<>();
		
		for (Flow flow : flowCollection.getFlows()) {
			nets.merge(flow.getSink(), flow.getValue(), (oldv, newv) -> oldv + newv);
			nets.merge(flow.getSource(), -flow.getValue(), (oldv, newv) -> oldv + newv);
		}
		
		return nets;
	}


	/**
	 * Verifies that the two maps are equal with a missing value being equal to 0.
	 */
	private void verifyEquals(
			Map<String, Long> netBefore,
			Map<String, Long> netAfter) {

		Iterator<String> beforeIterator = netBefore.keySet().iterator();
		
		while (beforeIterator.hasNext()) {
			String entity = beforeIterator.next();
			
			long beforeValue = netBefore.get(entity);
			long afterValue = netAfter.containsKey(entity) ? netAfter.get(entity) : 0;
			
			if (afterValue != beforeValue) {
				throw new NettingException(entity, beforeValue, afterValue);
			}
			
			beforeIterator.remove();
			netAfter.remove(entity);
		}
		
		Iterator<String> afterIterator = netAfter.keySet().iterator();
		
		while (afterIterator.hasNext()) {
			String entity = beforeIterator.next();
			
			long afterValue = netAfter.get(entity);

			if (afterValue != 0) {
				throw new NettingException(entity, 0, afterValue);
			}
		}
	}


	private boolean netRound() {
		flowCollection.setChanged(false);

		netBetweenParties();
		
		removeCircularFlow();
		
		findNetZeroString();
		
		aggregateSameSourceAndSink();
		
		return flowCollection.isChanged();
	}
	
	/**
	 * For all entities with a net zero flow (outgoing == incoming),
	 * finds suitable re-mapping of incoming to outgoing flows,
	 * thus removing this String from the graph completely.
	 * 
	 * Max one String is removed each time this function is called.
	 * 
	 */
	private void findNetZeroString() {

		FlowReplacements bestReplacement = null;
		
		for (String String : flowCollection.getSources()) {

			FlowList incoming = new FlowList();
			FlowList outgoing = new FlowList();
			
			flowCollection.getSources(String).stream().forEach(
				sourceString -> incoming.addAll(
					flowCollection.getFlows(sourceString, String)));
			
			flowCollection.getSinks(String).stream().forEach(
				sinkString -> outgoing.addAll(
					flowCollection.getFlows(String, sinkString)));
			
			if (incoming.getTotalValue() !=	outgoing.getTotalValue()) {
				continue;
			}
			
			FlowReplacements replacements = getReplacements(incoming, outgoing);
			
			if (bestReplacement == null || 
				bestReplacement.getSavings() < replacements.getSavings()) {
				
				bestReplacement = replacements;
			}
		}
		
		if (bestReplacement != null) {
			flowCollection.remove(bestReplacement.getIncoming());
			flowCollection.remove(bestReplacement.getOutgoing());
			bestReplacement.getReplacements().forEach(flow -> flowCollection.add(flow));		
		}
	}
	
	/**
	 * Given a list of incoming flows and a list of outgoing flows, where the
	 * total value of the incoming flows equals the total value of the outgoing flows,
	 * tries to find a mapping of incoming to outgoing flows using a minimum amount
	 * of mappings.
	 * 
	 * For example, if incoming is:
	 * 
	 *  - I1 10
	 *  - I2 20
	 * 
	 *  and outgoing is
	 *  
	 *  - O1 5
	 *  - O2 5
	 *  - O3 20
	 *  
	 *  Then an optimal mapping would be
	 *  
	 *  I1 -> O1 (5)
	 *  I1 -> O2 (5)
	 *  I2 -> O3 (20)
	 *  
	 *  A sub-optimal mapping would be
	 *  
	 *  I1 -> O3 (10)
	 *  I2 -> O3 (10)
	 *  I2 -> O1 (5)
	 *  I2 -> O2 (5)
	 * 
	 * This is essentially a variable bin packing problem.
	 */
	private FlowReplacements getReplacements(List<Flow> incoming, List<Flow> outgoing) {
		
		FlowList incomingToProcess = new FlowList();
		FlowList outgoingToProcess = new FlowList();
		List<Flow> replacements = new ArrayList<>();
		
		incoming.forEach(flow -> incomingToProcess.add(new Flow(flow)));
		outgoing.forEach(flow -> outgoingToProcess.add(new Flow(flow)));
		
		while (!incomingToProcess.isEmpty()) {
			long smallestIncoming = incomingToProcess.getSmallestValue().getAsLong();
			long smallestOutgoing = outgoingToProcess.getSmallestValue().getAsLong();
			
			Flow incomingFlow = null;
			Flow outgoingFlow = null;
			
			if (smallestIncoming < smallestOutgoing) {
				incomingFlow = incomingToProcess.getFlow(smallestIncoming).get();
				outgoingFlow = outgoingToProcess.getSmallestFlow(smallestIncoming).get();
			} else {
				outgoingFlow = outgoingToProcess.getFlow(smallestOutgoing).get();
				incomingFlow = incomingToProcess.getSmallestFlow(smallestOutgoing).get();
			}
			

			//
			// Create a new replacement flow
			//
			long flowValue = Math.min(outgoingFlow.getValue(), incomingFlow.getValue());
			
			replacements.add(
				new Flow(incomingFlow.getSource(), outgoingFlow.getSink(), flowValue));
			//
			// Reduce the value of the incoming and outgoing flow
			//
			incomingToProcess.remove(incomingFlow);
			outgoingToProcess.remove(outgoingFlow);
			
			Flow newIncoming = new Flow(
				incomingFlow.getSource(), incomingFlow.getSink(), incomingFlow.getValue() - flowValue);
			
			Flow newOutgoing = new Flow(
				outgoingFlow.getSource(), outgoingFlow.getSink(), outgoingFlow.getValue() - flowValue);
			
			if (newIncoming.getValue() > 0) {
				incomingToProcess.add(newIncoming);
			}
			
			if (newOutgoing.getValue() > 0) {
				outgoingToProcess.add(newOutgoing);
			}
			
		}
		
		FlowReplacements flowReplacements = new FlowReplacements();
		flowReplacements.setIncoming(incoming);
		flowReplacements.setOutgoing(outgoing);
		flowReplacements.setReplacements(replacements);
		
		return flowReplacements;
	}

	private static class FlowReplacements {
		private List<Flow> incoming = new ArrayList<>();
		private List<Flow> outgoing = new ArrayList<>();
		private List<Flow> replacements = new ArrayList<>();
	
		public int getSavings() {
			return incoming.size() + outgoing.size() - replacements.size();
		}

		public List<Flow> getIncoming() {
			return incoming;
		}

		public void setIncoming(List<Flow> incoming) {
			this.incoming = incoming;
		}

		public List<Flow> getOutgoing() {
			return outgoing;
		}

		public void setOutgoing(List<Flow> outgoing) {
			this.outgoing = outgoing;
		}

		public List<Flow> getReplacements() {
			return replacements;
		}

		public void setReplacements(List<Flow> replacements) {
			this.replacements = replacements;
		}
	}


	/**
	 * Tries to remove circular flow from the graph
	 */
	private void removeCircularFlow() {

		// Guarantees max one flow between the same source and sink
		aggregateSameSourceAndSink();
		
		List<List<Flow>> circularFlows = new ArrayList<>();
		
		for (String source : flowCollection.getSources()) {
			circularFlows.addAll(getCircularFlow(source, source, new ArrayList<>()));
		}
		
		if (circularFlows.isEmpty()) {
			return;
		}
		
		// If we have multiple circular flows, decide which one to remove
		// We chose the one for which we can remove the most flows.
		Collections.sort(circularFlows, new CircularComparator());
		
		List<Flow> circularFlow = circularFlows.get(0);
		
		OptionalLong minValue = circularFlow.stream().mapToLong(Flow::getValue).min();
		
		for (Flow flow : circularFlow) {
			flowCollection.remove(flow);
			
			if (flow.getValue() > minValue.getAsLong()) {
				Flow newFlow = new Flow(
					flow.getSource(), flow.getSink(), flow.getValue() - minValue.getAsLong());
				
				flowCollection.add(newFlow);
			}
		}
		
	}
	
	private static class CircularComparator implements Comparator<List<Flow>> {

		@Override
		public int compare(List<Flow> o1, List<Flow> o2) {
			long flowsWithMinimalValue1 = getFlowsWithMinimalValue(o1);
			long flowsWithMinimalValue2 = getFlowsWithMinimalValue(o2);
			
			if (flowsWithMinimalValue1 > flowsWithMinimalValue2) {
				return -1;
			}
			
			if (flowsWithMinimalValue1 < flowsWithMinimalValue2) {
				return 1;
			}
			
			return 0;
		}

		private long getFlowsWithMinimalValue(List<Flow> flows) {
			OptionalDouble minValue = flows.stream().mapToDouble(Flow::getValue).min();
			
			if (!minValue.isPresent()) {
				return 0;
			}
			
			return flows.stream().filter(goal -> goal.getValue() == minValue.getAsDouble()).count();
		}
	}

	private List<List<Flow>> getCircularFlow(String StringToFind, String source, List<String> visited) {
		
		// Must maintain a visited collection or risk running into infinite loops
		if (visited.contains(source)) {
			return Collections.emptyList();
		}
		visited.add(source);
		
		List<List<Flow>> circularFlows = new ArrayList<>();
		
		for (String sink : flowCollection.getSinks(source)) {
			Flow sinkFlow = flowCollection.getFlows(source, sink).get(0);
			
			if (sink.equals(StringToFind)) {
				circularFlows.add(Arrays.asList(sinkFlow));
			} else {
				List<List<Flow>> circularChildFlows = getCircularFlow(StringToFind, sink, visited);
				
				for (List<Flow> circularChildFlow : circularChildFlows) {
					List<Flow> flows = new ArrayList<>();
					flows.add(sinkFlow);
					flows.addAll(circularChildFlow);
					circularFlows.add(flows);
				}
			}
		}
		
		return circularFlows;
	}


	/**
	 * Nets payments between parties
	 */
	private void netBetweenParties() {
		for (String source : flowCollection.getSources()) {
			for (String sink : flowCollection.getSinks(source)) {
		
				Collection<Flow> flows = flowCollection.getFlows(source, sink);
				Collection<Flow> returnFlows = flowCollection.getFlows(sink, source);
				
				if (returnFlows.isEmpty() || flows.isEmpty()) {
					continue;
				}

				// Get the total value of all flows going from source to sink and vice versa.
				long value = 
					flows.stream().mapToLong(flow -> flow.getValue()).sum();
				
				long returnValue =
					returnFlows.stream().mapToLong(flow -> flow.getValue()).sum();
				
				// Remove all existing flows.
				flowCollection.remove(flows);
				flowCollection.remove(returnFlows);
		
				// Add a new net flow
				if (value > returnValue) {
					flowCollection.add(new Flow(source, sink, value - returnValue));
				} else if (value < returnValue){
					flowCollection.add(new Flow(sink, source, returnValue - value));
				}
			}
		}
		
	}

	/**
	 * Combines multiple flows from the same source and sink to one single flow. 
	 */
	private void aggregateSameSourceAndSink() {
		
		for (String source : flowCollection.getSources()) {
			for (String sink : flowCollection.getSinks(source)) {
				
				Collection<Flow> flows = flowCollection.getFlows(source, sink);
				
				if (flows.size() <= 1) {
					continue;
				}
				
				Flow aggregateFlow = 
					flows.stream()
						.reduce((a, b) -> 
						new Flow(source, sink, a.getValue() + b.getValue())).get();

				flowCollection.remove(flows);

				flowCollection.add(aggregateFlow);
			}
		}
	}
	
}
	