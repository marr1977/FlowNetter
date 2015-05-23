import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import flownetter.Flow;
import flownetter.Netter;


public class NetterTest {

	private Netter netter;

	@Before
	public void before() {
		netter = new Netter();
	}
	
	@Test
	public void flowsAggregated() {
		netter.add(new Flow("a", "b", 3));
		netter.add(new Flow("a", "b", 2));
		netter.add(new Flow("a", "b", 1));
		
		List<Flow> flows = netter.net();
		
		assertEquals(1, flows.size());
		assertEquals(6, flows.get(0).getValue());
	}

	@Test
	public void netBetweenPartiesNonZero() {
		netter.add(new Flow("a", "b", 17));
		netter.add(new Flow("b", "a", 12));
		
		List<Flow> flows = netter.net();
		
		assertEquals(1, flows.size());
		assertEquals(5, flows.get(0).getValue());
		assertEquals("a", flows.get(0).getSource());
		assertEquals("b", flows.get(0).getSink());
	}

	
	@Test
	public void netBetweenPartiesZero() {
		netter.add(new Flow("a", "b", 17));
		netter.add(new Flow("b", "a", 15));
		netter.add(new Flow("b", "a", 2));
		
		List<Flow> flows = netter.net();
		
		assertEquals(0, flows.size());
	}

	@Test
	public void circularDependency() {
		netter.add(new Flow("a", "b", 10));
		netter.add(new Flow("b", "c", 10));
		netter.add(new Flow("c", "d", 10));
		netter.add(new Flow("d", "a", 10));
		
		List<Flow> flows = netter.net();
		
		assertEquals(0, flows.size());
	}

	@Test
	public void twoCircularDependenciesOnlyOneCanBeRemoved() {
		netter.add(new Flow("a", "b", 20));

		// Circle 1
		netter.add(new Flow("b", "c", 10));
		netter.add(new Flow("c", "d", 8));
		netter.add(new Flow("d", "e", 9));
		netter.add(new Flow("e", "a", 10));
		
		// Circle 2
		netter.add(new Flow("b", "f", 20));
		netter.add(new Flow("f", "g", 20)); 
		netter.add(new Flow("g", "a", 21));
		
		List<Flow> flows = netter.net();
		
		assertEquals(5, flows.size());
		
		verifyHas(flows, new Flow("b", "c", 10));
		verifyHas(flows, new Flow("c", "d", 8));
		verifyHas(flows, new Flow("d", "e", 9));
		verifyHas(flows, new Flow("e", "a", 10));
		verifyHas(flows, new Flow("g", "a", 1));
	}
	
	@Test
	public void twoCircularDependenciesBothCanBeRemoved() {
		netter.add(new Flow("a", "b", 100));

		// Circle 1
		netter.add(new Flow("b", "c", 10));
		netter.add(new Flow("c", "d", 8));
		netter.add(new Flow("d", "e", 8));
		netter.add(new Flow("e", "a", 10));
		
		// Circle 2
		netter.add(new Flow("b", "f", 20));
		netter.add(new Flow("f", "g", 20)); 
		netter.add(new Flow("g", "a", 20));
		
		List<Flow> flows = netter.net();
		
		assertEquals(3, flows.size());
		
		verifyHas(flows, new Flow("a", "b", 72));
		verifyHas(flows, new Flow("b", "c", 2));
		verifyHas(flows, new Flow("e", "a", 2));
	}
	
	@Test
	public void removeNetZeroEntities() {
		netter.add(new Flow("a", "b", 100));
		netter.add(new Flow("b", "c", 100));
		netter.add(new Flow("c", "d", 100));
		netter.add(new Flow("d", "e", 100));
		
		List<Flow> flows = netter.net();
		
		assertEquals(1, flows.size());
		verifyHas(flows, new Flow("a", "e", 100));
	}

	@Test
	public void bestNetZeroReplacements() {
		netter.add(new Flow("a1", "b", 1));
		netter.add(new Flow("a2", "b", 1));
		netter.add(new Flow("a3", "b", 2));
		netter.add(new Flow("a4", "b", 100));
		
		netter.add(new Flow("b", "c1", 1));
		netter.add(new Flow("b", "c2", 3));
		netter.add(new Flow("b", "c3", 100));

		List<Flow> flows = netter.net();
		
		assertEquals(4, flows.size());
		verifyHas(flows, new Flow("a1", "c1", 1));
		verifyHas(flows, new Flow("a2", "c2", 1));
		verifyHas(flows, new Flow("a3", "c2", 2));
		verifyHas(flows, new Flow("a4", "c3", 100));
	}
	
	private void verifyHas(List<Flow> flows, Flow flow) {
		for (Flow existingFlow : flows) {
			if (flow.getSink().equals(existingFlow.getSink()) &&
				flow.getSource().equals(existingFlow.getSource()) &&
				flow.getValue() == existingFlow.getValue()) {
				
				return;
			}
		}
		Assert.fail("No such flow");
	}

}
