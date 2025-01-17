import java.util.ArrayList;

public enum Type {
	NORMAL, FIGHTING, FLYING, POISON, GROUND, ROCK, BUG, GHOST, 
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
			break;
		case FIGHTING:
			if (t == NORMAL || t == ROCK || t == ICE) {
				return 2.0;
			}
			else if (t == FLYING || t == POISON || t == BUG || t == PSYCHIC) {
				return 0.5;
			}
			else if (t == GHOST) {
				return 0.0;
			}
			break;
		case FLYING:
			if (t == FIGHTING || t == BUG || t == GRASS) {
				return 2.0;
			}
			else if (t == ROCK || t == ELECTRIC) {
				return 0.5;
			}
			break;
		case POISON:
			if (t == BUG || t == GRASS) {
				return 2.0;
			}
			else if (t == POISON || t == GROUND || t == ROCK || t == GHOST) {
				return 0.5;
			}
			break;
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
			break;
		case ROCK:
			if (t == FLYING || t == FIRE || t == BUG || t == ICE) {
				return 2.0;
			}
			else if (t == FIGHTING || t == GROUND) {
				return 0.5;
			}
			break;
		case BUG:
			if (t == POISON || t == GRASS || t == PSYCHIC) {
				return 2.0;
			}
			else if (t == FIGHTING || t == FLYING || t == GHOST || t == FIRE) {
				return 0.5;
			}
			break;
		case GHOST:
			if (t == GHOST) {
				return 2.0;
			}
			else if (t == NORMAL || t == PSYCHIC) {
				return 0.0;
			}
			break;
		case FIRE:
			if (t == BUG || t == GRASS || t == ICE) {
				return 2.0;
			}
			else if (t == ROCK || t == FIRE || t == WATER || t == DRAGON) {
				return 0.5;
			}
			break;
		case WATER:
			if (t == GROUND || t == ROCK || t == FIRE) {
				return 2.0;
			}
			else if (t == WATER || t == GRASS || t == DRAGON) {
				return 0.5;
			}
			break;
		case GRASS:
			if (t == GROUND || t == ROCK || t == WATER) {
				return 2.0;
			}
			else if (t == FLYING || t == POISON || t == BUG || t == FIRE ||
					t == GRASS || t == DRAGON) {
				return 0.5;
			}
			break;
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
			break;
		case PSYCHIC:
			if (t == FIGHTING || t == POISON) {
				return 2.0;
			}
			else if (t == PSYCHIC) {
				return 0.5;
			}
			break;
		case ICE:
			if (t == FLYING || t == GROUND || t == GRASS || t == DRAGON) {
				return 2.0;
			}
			else if (t == WATER || t == ICE) {
				return 0.5;
			}
			break;
		case DRAGON:
			if (t == DRAGON) {
				return 2.0;
			}
			break;
		case NONE:
			return 1.0;
		}
		return 1.0;
	}
	
	/**
	 * Returns true if [this] is a physical type and false otherwise.
	 */
	public boolean isPhysical() {
		return (this == NORMAL || this == FIGHTING || this == FLYING || 
				this == GROUND || this == ROCK || this == BUG || 
				this == GHOST || this == POISON);
	}
	
	/**
	 * Returns the list of types which are effective against a pokemon with types [t1] 
	 * and [t2]. If the pokemon is single-typed, then [t2] must be [None].
	 */
	public static ArrayList<Type> weaknesses(Type t1, Type t2) {
		ArrayList<Type> ret = new ArrayList<>();
		for (Type t: Type.values()) {
			if (t.effectiveness(t1) * t.effectiveness(t2) > 1.0) {
				ret.add(t);
			}
		}
		
		return ret;
	}
}
