
public enum Type {
	NORMAL, FIGHT, FLYING, POISON, GROUND, ROCK, BUG, GHOST, 
	FIRE, WATER, GRASS, ELECTRIC, PSYCHIC, ICE, DRAGON, NONE;
	
	
	/** 
	 * Returns damage modifier of current type attacking type [t]. 
	 */
	public double effectiveness(Type t) {
		switch (this) {
		case NORMAL:
			if (t == ROCK) {
				return 0.5;
			}
			else if (t == GHOST) {
				return 0.0;
			}
			
		case FIGHT:
			if (t == NORMAL || t == ROCK || t == ICE) {
				return 2.0;
			}
			else if (t == FLYING || t == POISON || t == BUG || t == PSYCHIC) {
				return 0.5;
			}
			else if (t == GHOST) {
				return 0.0;
			}
			
		case FLYING:
			if (t == FIGHT || t == BUG || t == GRASS) {
				return 2.0;
			}
			else if (t == ROCK || t == ELECTRIC) {
				return 0.5;
			}
		case POISON:
			if (t == BUG || t == GRASS) {
				return 2.0;
			}
			else if (t == POISON || t == GROUND || t == ROCK || t == GHOST) {
				return 0.5;
			}
			
		case GROUND:
			if (t == POISON || t == ROCK || t == FIRE || t == ELECTRIC) {
				return 2.0;
			}
			else if (t == BUG || t == GRASS) {
				return 0.5;
			}
			else if (t == FLYING) {
				return 0.0;
			}
		
		case ROCK:
			if (t == FLYING || t == FIRE || t == BUG || t == ICE) {
				return 2.0;
			}
			else if (t == FIGHT || t == GROUND) {
				return 0.5;
			}
			
		case BUG:
			if (t == POISON || t == GRASS || t == PSYCHIC) {
				return 2.0;
			}
			else if (t == FIGHT || t == FLYING || t == GHOST || t == FIRE) {
				return 0.5;
			}
			
		case GHOST:
			if (t == GHOST) {
				return 2.0;
			}
			else if (t == NORMAL || t == PSYCHIC) {
				return 0.0;
			}
			
		case FIRE:
			if (t == BUG || t == GRASS || t == ICE) {
				return 2.0;
			}
			else if (t == ROCK || t == FIRE || t == WATER || t == DRAGON) {
				return 0.5;
			}
			
		case WATER:
			if (t == GROUND || t == ROCK || t == FIRE) {
				return 2.0;
			}
			else if (t == WATER || t == GRASS || t == DRAGON) {
				return 0.5;
			}
		
		case GRASS:
			if (t == GROUND || t == ROCK || t == WATER) {
				return 2.0;
			}
			else if (t == FLYING || t == POISON || t == BUG || t == FIRE ||
					t == GRASS || t == DRAGON) {
				return 0.5;
			}
			
		case ELECTRIC:
			if (t == FLYING || t == WATER) {
				return 2.0;
			}
			else if (t == GRASS || t == ELECTRIC || t == DRAGON) {
				return 0.5;
			}
			else if (t == GROUND) {
				return 0.0;
			}
		
		case PSYCHIC:
			if (t == FIGHT || t == POISON) {
				return 2.0;
			}
			else if (t == PSYCHIC) {
				return 0.5;
			}
			
		case ICE:
			if (t == FLYING || t == GROUND || t == GRASS || t == DRAGON) {
				return 2.0;
			}
			else if (t == WATER || t == ICE) {
				return 0.5;
			}
			
		case DRAGON:
			if (t == DRAGON) {
				return 2.0;
			}
		
		case NONE:
			return 1.0;
		}
		return 1.0;
	}
	
	/**
	 * Returns true if [this] is a physical type and false otherwise.
	 */
	public boolean isPhysical() {
		return (this == NORMAL || this == FIGHT || this == FLYING || 
				this == GROUND || this == ROCK || this == BUG || 
				this == GHOST || this == POISON);
	}
}
