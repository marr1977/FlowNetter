package flownetter;

public class Entity {
	String id;
	
	public Entity(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Entity && ((Entity) obj).getId().equals(id);
	}
	
	@Override
	public String toString() {
		return id;
	}
}
