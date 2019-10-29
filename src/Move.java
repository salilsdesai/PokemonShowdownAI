import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Move {
	private static Map<String, Move> moves;

	public String name;
	public int maxPP, power;
	public int accuracy; /** Base accuracy of a move, or -1 if it doesn't check for accuracy */
	public Type type;
	public boolean highCritRatio;
	public int priority;
	
  public BiFunction<ArrayList<Pokemon>, Integer, Void> secondaryEffect;

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
		
		// special case where damage depends on the user's level
		if (name.equals("dragonrage")) {
			damage = 40;
		}
		else if (name.equals("nightshade") || name.equals("seismictoss")) {
			damage = user.level;
		}
		else if (name.equals("psywave")) {
			damage = (int)(Math.random() * 1.5 * user.level) + 1;
		}
		// special case where number of hits is random (so damage is not constant)
		else if (name.equals("pinmissle")) {
			double rand = Math.random();
			if (rand < 0.375) {
				damage *= 2;
			}
			else if (rand >= 0.375 && rand < 0.75) {
				damage *= 3;
			}
			else if (rand >= 0.75 && rand < 0.875) {
				damage *= 4;
			}
			else {
				damage *= 5;
			}
		}
		// special case where move requires charging
		else if (name.equals("skyattack") && !user.status.charge) {
			user.status.charge = true;
			return;
		}
		// special case where move depends on opponent's moveset
		else if (name.equals("mirrormove") && target.lastMoveUsed != null) {
			if (user.lastAttacker == target && !target.lastMoveUsed.name.equals("mirrormove")) {
				target.lastMoveUsed.use(user, target);
			}
		}
		
		// apply the damage onto the target
		target.currHp = Math.max(0, target.currHp - damage);
		
		// Set the [lastMoveUsed] and [lastAttacker]
		user.lastMoveUsed = this;
		target.lastAttacker = user;
		
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
	
	public static Move getMove(String moveName) {
		if(moves == null) {
			loadMoves();
		}
		return moves.get(moveName);
	}
	
	@SuppressWarnings("unchecked")
	private static void loadMoves() {
		
		moves = new HashMap<String, Move>();
		
		JSONArray jA;
		try {
			jA = (JSONArray)(new JSONParser().parse(new FileReader("moves.json")));
		}
		catch (IOException | ParseException e) {
			// Realistically this catch block will never be reached
			return;
		}
		
		List<JSONObject> jMoves = jA;
		for(JSONObject m : jMoves) {
			// TODO: Parse the move, add it to the map
		}
	}
	
}
