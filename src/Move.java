import java.util.Arrays;

public class Move {
	public String name;
	public int maxPP, power;
	public int accuracy; /** Base accuracy of a move, or -1 if it doesn't check for accuracy */
	public Type type;
	public boolean highCritRatio;
	public int priority;
	
	public String toString() {
		return Arrays.toString(new Object[] {name, type, power, accuracy, maxPP});
	}
	
	// TODO: Add status/stat modification/secondary effect
	
	/**
	 * Have [user] attempt to use [this] on a [target]
	 * "attempt to use" means behave as if the user had selected the move, and it is 
	 * their turn in the speed bracket to try to attack, without considering or modifying pp
	 * i.e
	 * - Check for any status that would prevent the move's execution (not currently implemented)
	 * - 	e.x. sleep, paralysis, protect
	 * - Check if the move connected with target (by checking accuracy/evasion modifiers and base accuracy
	 * If the move connected
	 * - Update both pokemon's statuses and hp as necessary, considering base damage and secondary effects
	 */
	public void use(Pokemon user, Pokemon target) {
		
		// TODO: Check for status conditions that would prevent the move from being executed
		
		// Accuracy Check
		// TODO: Check user and target accuracy modifications
		if((accuracy != -1 && (Math.random() * 100) > accuracy) || (Math.random() < 1.0/256)) {
			// The attack missed
			// Any attack has a 1/256 chance of missing due to gen 1 glitch, independently of normal accuracy checks
			return;
		}
		
		int damage = damageDealt(user, target);
		
		System.out.println(damage);
		
		target.currHp = Math.max(0, target.currHp - damage);
		
		// TODO: Check for secondary effects
		
	}
	
	/**
	 * Returns amount of damage dealt to target assuming [this] lands successfully
	 * when used by [user] on [target]
	 * 
	 * Uses the following formula: https://bulbapedia.bulbagarden.net/wiki/Damage#Damage_calculation
	 */
	public int damageDealt(Pokemon user, Pokemon target) {
		boolean critical = (Math.random() < Pokedex.getDex().get(user.species).baseStats[4]/(!highCritRatio ? 512.0 : 64.0));
		
		int level = user.level * (critical ? 2 : 1);
		int power = this.power;
		
		int attackingStat, defendingStat;
		if(this.type.isPhysical()) {
			attackingStat = user.atk;
			defendingStat = target.def;
		}
		else {
			attackingStat = user.spc;
			defendingStat = target.spc;
		}
		
		int baseDamage = (((((((2 * level)/5) + 2) * power * attackingStat)/(defendingStat))/50) + 2);
		
		// Random damage range
		double modifier = ((Math.random() * 39) + 217)/(255);
		// STAB
		for(Type t : user.types)
			if(t == this.type)
				modifier *= 1.5;
		// Type effectiveness
		for(Type t : target.types) {
			modifier *= this.type.effectiveness(t);
		}
	
		//TODO: status + stat modifications
		
		return (int)(baseDamage * modifier);
	}
	
	/** 
	 * Not implemented yet, returns a move with the properties of Cut
	 */
	public static Move getMove(String moveName) {
		// TODO
		// returns Cut for now
		Move m = new Move();
		m.name = moveName;
		m.maxPP = 30;
		m.power = 50;
		m.accuracy = 95;
		m.type = Type.valueOf("NORMAL");
		m.highCritRatio = false;
		return m;
	}
	
}
