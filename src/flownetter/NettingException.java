package flownetter;

public class NettingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NettingException(String entity, long before, long after) {
		super(
			"Code inconsistency. Entity " + entity + " was net " + before + 
			" but is net " + after + " after netting");
	}
}
