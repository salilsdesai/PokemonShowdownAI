
public enum Type {
	NORMAL, FIGHT, FLYING, POISON, GROUND, ROCK, BUG, GHOST, 
	FIRE, WATER, GRASS, ELECTRIC, PSYCHIC, ICE, DRAGON, NONE;
	
	
	/** 
	 * Returns damage modifier of current type attacking type [t]. 
	 */
	public double effectiveness(Type t) {
		//TODO: refer to effectiveness table
		return 1.0;
	}
}
