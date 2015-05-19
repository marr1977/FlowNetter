import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import flownetter.Entity;
import flownetter.EntityFactory;
import flownetter.Flow;
import flownetter.Netter;


public class NetterTest {

	private EntityFactory entityFactory;
	private Netter netter;

	@Before
	public void before() {
		entityFactory = new EntityFactory();
		netter = new Netter();
	}
	
	@Test
	public void flowsAggregated() {
		netter.add(new Flow(entity("a"), entity("b"), 3));
		netter.add(new Flow(entity("a"), entity("b"), 2));
		netter.add(new Flow(entity("a"), entity("b"), 1));
		
		List<Flow> flows = netter.net();
		
		assertEquals(1, flows.size());
		assertEquals(6, flows.get(0).getValue(), 0.0001);
	}

	@Test
	public void netBetweenPartiesNonZero() {
		netter.add(new Flow(entity("a"), entity("b"), 17));
		netter.add(new Flow(entity("b"), entity("a"), 12));
		
		List<Flow> flows = netter.net();
		
		assertEquals(1, flows.size());
		assertEquals(5, flows.get(0).getValue(), 0.0001);
		assertEquals(entity("a"), flows.get(0).getSource());
		assertEquals(entity("b"), flows.get(0).getSink());
	}

	
	@Test
	public void netBetweenPartiesZero() {
		netter.add(new Flow(entity("a"), entity("b"), 17));
		netter.add(new Flow(entity("b"), entity("a"), 15));
		netter.add(new Flow(entity("b"), entity("a"), 2));
		
		List<Flow> flows = netter.net();
		
		assertEquals(0, flows.size());
	}

	@Test
	public void circularDependency() {
		netter.add(new Flow(entity("a"), entity("b"), 10));
		netter.add(new Flow(entity("b"), entity("c"), 10));
		netter.add(new Flow(entity("c"), entity("d"), 10));
		netter.add(new Flow(entity("d"), entity("a"), 10));
		
		List<Flow> flows = netter.net();
		
		assertEquals(0, flows.size());
	}

	@Test
	public void twoCircularDependenciesOnlyOneCanBeRemoved() {
		netter.add(new Flow(entity("a"), entity("b"), 20));

		// Circle 1
		netter.add(new Flow(entity("b"), entity("c"), 10));
		netter.add(new Flow(entity("c"), entity("d"), 8));
		netter.add(new Flow(entity("d"), entity("e"), 9));
		netter.add(new Flow(entity("e"), entity("a"), 10));
		
		// Circle 2
		netter.add(new Flow(entity("b"), entity("f"), 20));
		netter.add(new Flow(entity("f"), entity("g"), 20)); 
		netter.add(new Flow(entity("g"), entity("a"), 21));
		
		List<Flow> flows = netter.net();
		
		assertEquals(5, flows.size());
		
		verifyHas(flows, new Flow(entity("b"), entity("c"), 10));
		verifyHas(flows, new Flow(entity("c"), entity("d"), 8));
		verifyHas(flows, new Flow(entity("d"), entity("e"), 9));
		verifyHas(flows, new Flow(entity("e"), entity("a"), 10));
		verifyHas(flows, new Flow(entity("g"), entity("a"), 1));
	}
	
	@Test
	public void twoCircularDependenciesBothCanBeRemoved() {
		netter.add(new Flow(entity("a"), entity("b"), 100));

		// Circle 1
		netter.add(new Flow(entity("b"), entity("c"), 10));
		netter.add(new Flow(entity("c"), entity("d"), 8));
		netter.add(new Flow(entity("d"), entity("e"), 8));
		netter.add(new Flow(entity("e"), entity("a"), 10));
		
		// Circle 2
		netter.add(new Flow(entity("b"), entity("f"), 20));
		netter.add(new Flow(entity("f"), entity("g"), 20)); 
		netter.add(new Flow(entity("g"), entity("a"), 20));
		
		List<Flow> flows = netter.net();
		
		assertEquals(3, flows.size());
		
		verifyHas(flows, new Flow(entity("a"), entity("b"), 72));
		verifyHas(flows, new Flow(entity("b"), entity("c"), 2));
		verifyHas(flows, new Flow(entity("e"), entity("a"), 2));
	}
	
	@Test
	public void removeNetZeroEntities() {
		netter.add(new Flow(entity("a"), entity("b"), 100));
		netter.add(new Flow(entity("b"), entity("c"), 100));
		netter.add(new Flow(entity("c"), entity("d"), 100));
		netter.add(new Flow(entity("d"), entity("e"), 100));
		
		List<Flow> flows = netter.net();
		
		assertEquals(1, flows.size());
		verifyHas(flows, new Flow(entity("a"), entity("e"), 100));
	}

	@Test
	public void bestNetZeroReplacements() {
		netter.add(new Flow(entity("a1"), entity("b"), 1));
		netter.add(new Flow(entity("a2"), entity("b"), 1));
		netter.add(new Flow(entity("a3"), entity("b"), 2));
		netter.add(new Flow(entity("a4"), entity("b"), 100));
		
		netter.add(new Flow(entity("b"), entity("c1"), 1));
		netter.add(new Flow(entity("b"), entity("c2"), 3));
		netter.add(new Flow(entity("b"), entity("c3"), 100));

		List<Flow> flows = netter.net();
		
		assertEquals(4, flows.size());
		verifyHas(flows, new Flow(entity("a1"), entity("c1"), 1));
		verifyHas(flows, new Flow(entity("a2"), entity("c2"), 1));
		verifyHas(flows, new Flow(entity("a3"), entity("c2"), 2));
		verifyHas(flows, new Flow(entity("a4"), entity("c3"), 100));
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

	private Entity entity(String id) {
		return entityFactory.get(id);
	}
}
