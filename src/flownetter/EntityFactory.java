package flownetter;

import java.util.HashMap;
import java.util.Map;

public class EntityFactory {
	private Map<Entity, Entity> entities = new HashMap<>();
	
	public Entity get(String id) {
		
		Entity entity = new Entity(id);
		
		if (entities.containsKey(entity)) {
			return entities.get(entity);
		}
		
		entities.put(entity, entity);
		return entity;
	}

}
