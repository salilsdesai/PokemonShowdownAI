import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Move {
	public static class MoveDamage {
		public Pokemon user;
		public Pokemon target;
		public int damage;
		public MoveDamage(Pokemon u, Pokemon t, int d) {
			user = u;
			target = t;
			damage = d;
		}
	}
	
	private static Map<String, Move> moves;

	public String name;
	public int maxPP, power;
	public int accuracy; /** Base accuracy of a move, or -1 if it doesn't check for accuracy */
	public Type type;
	public boolean highCritRatio;
	public int priority;
	public Consumer<MoveDamage> secondaryEffect;

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
	
	private static void loadMoves() {
		
		moves = new HashMap<String, Move>();
		
		/*
		 
		TODO: Add all moves to map in this format:
		 
		Move m = new Move();
		m.power = ...
		m.... = ...
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			System.out.println(md.user);
		}};
		moves.put(name, m);
		 
		*/
		
		Move m;
		
		
		// Agility
		m = new Move();
		m.name = "agility";
		m.maxPP = 48;
		m.power = 0;
		m.accuracy = -1;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.user.statMod(Pokemon.Stat.SPE, 2);
		}};
		moves.put(m.name, m);
		
		// Amnesia
		m = new Move();
		m.name = "amnesia";
		m.maxPP = 32;
		m.power = 0;
		m.accuracy = -1;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.user.statMod(Pokemon.Stat.SPC, 2);
		}};
		moves.put(m.name, m);
		
		// Barrier
		m = new Move();
		m.name = "barrier";
		m.maxPP = 32;
		m.power = 0;
		m.accuracy = -1;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.user.statMod(Pokemon.Stat.DEF, 2);
		}};
		moves.put(m.name, m);
		
		// Bide
		m = new Move();
		m.name = "bide";
		m.maxPP = 16;
		m.power = 0;
		m.accuracy = -1;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			// TODO: Set secondary effect for BIDE
		}};
		moves.put(m.name, m);
		
		// Blizzard
		m = new Move();
		m.name = "blizzard";
		m.maxPP = 8;
		m.power = 120;
		m.accuracy = 90;
		m.type = Type.ICE;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			if(Math.random() < 0.1)
				md.target.setStatusCondition(Pokemon.StatusCondition.FREEZE, 0);
		}};
		moves.put(m.name, m);
		
		// Body Slam
		m = new Move();
		m.name = "bodyslam";
		m.maxPP = 24;
		m.power = 85;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			if(Math.random() < 0.3)
				md.target.setStatusCondition(Pokemon.StatusCondition.PARALYZE, 0);
		}};
		moves.put(m.name, m);
		
		// BubbleBeam
		m = new Move();
		m.name = "bubblebeam";
		m.maxPP = 32;
		m.power = 65;
		m.accuracy = 100;
		m.type = Type.WATER;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			if(Math.random() < 0.33)
				md.target.statMod(Pokemon.Stat.SPE, -1);
		}};
		moves.put(m.name, m);
		
		
//		Confuse Ray
		m = new Move();
		m.name = "confuseray";
		m.maxPP = 16;
		m.power = 0;
		m.accuracy = 100;
		m.type = Type.GHOST;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.target.setStatusCondition(Pokemon.StatusCondition.CONFUSE, (int)(Math.random() * 4 + 1));
		}};
		moves.put(m.name, m);
		
// 		Counter
		m = new Move();
		m.name = "counter";
		m.maxPP = 32;
		m.power = 0;
		m.accuracy = 100;
		m.type = Type.FIGHTING;
		m.highCritRatio = false;
		m.priority = -1;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			// TODO: Handle counter properly
		}};
		moves.put(m.name, m);
		
		
//		Crabhammer
		m = new Move();
		m.name = "crabhammer";
		m.maxPP = 16;
		m.power = 90;
		m.accuracy = 85;
		m.type = Type.WATER;
		m.highCritRatio = true;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
		}};
		moves.put(m.name, m);
		
//		Double Kick
		m = new Move();
		m.name = "doublekick";
		m.maxPP = 48;
		m.power = 60;
		m.accuracy = 100;
		m.type = Type.FIGHTING;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
		}};
		moves.put(m.name, m);
	
	
//		Double-Edge
		m = new Move();
		m.name = "doubleedge";
		m.maxPP = 24;
		m.power = 100;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.user.currHp = Math.max(md.user.currHp - md.damage/4, 0);
		}};
		moves.put(m.name, m);
		
//		Dragon Rage
		m = new Move();
		m.name = "dragonrage";
		m.maxPP = 16;
		m.power = 0;
		m.accuracy = 100;
		m.type = Type.DRAGON;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
		}};
		moves.put(m.name, m);
		
//		Drill Peck
		m = new Move();
		m.name = "drillpeck";
		m.maxPP = 32;
		m.power = 80;
		m.accuracy = 100;
		m.type = Type.FLYING;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
		}};
		moves.put(m.name, m);
		
//		Earthquake
		m = new Move();
		m.name = "earthquake";
		m.maxPP = 16;
		m.power = 100;
		m.accuracy = 100;
		m.type = Type.GROUND;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
		}};
		moves.put(m.name, m);
		
//		Egg Bomb
		m = new Move();
		m.name = "eggbomb";
		m.maxPP = 16;
		m.power = 100;
		m.accuracy = 75;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
		}};
		moves.put(m.name, m);
		
		
//		Explosion
		m = new Move();
		m.name = "explosion";
		m.maxPP = 8;
		m.power = 340;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.user.currHp = 0;
		}};
		moves.put(m.name, m);
		
//		Fire Blast
		m = new Move();
		m.name = "fireblast";
		m.maxPP = 8;
		m.power = 120;
		m.accuracy = 85;
		m.type = Type.FIRE;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			if(Math.random() < 0.3)
			md.target.setStatusCondition(Pokemon.StatusCondition.BURN, (int)(Math.random() * 4 + 1));
		}};
		moves.put(m.name, m);
		
//		Flamethrower
		m = new Move();
		m.name = "flamethrower";
		m.maxPP = 24;
		m.power = 95;
		m.accuracy = 100;
		m.type = Type.FIRE;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			if(Math.random() < 0.1)
				md.target.setStatusCondition(Pokemon.StatusCondition.BURN, (int)(Math.random() * 4 + 1));
		}};
		moves.put(m.name, m);
		
		
//		Glare
		m = new Move();
		m.name = "glare";
		m.maxPP = 48;
		m.power = 0;
		m.accuracy = 75;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.target.setStatusCondition(Pokemon.StatusCondition.PARALYZE, (int)(Math.random() * 4 + 1));
		}};
		moves.put(m.name, m);
		
//		Growth
		m = new Move();
		m.name = "growth";
		m.maxPP = 64;
		m.power = 0;
		m.accuracy = -1;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.user.statMod(Pokemon.Stat.SPC, 1);
		}};
		moves.put(m.name, m);
		
//		Harden
		m = new Move();
		m.name = "harden";
		m.maxPP = 48;
		m.power = 0;
		m.accuracy = -1;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			md.user.statMod(Pokemon.Stat.DEF, 1);
		}};
		moves.put(m.name, m);
		
//		High Jump Kick
		m = new Move();
		m.name = "highjumpkick";
		m.maxPP = 32;
		m.power = 85;
		m.accuracy = 90;
		m.type = Type.FIGHTING;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
			if(md.damage == 0)
				md.user.currHp -= 1;
			// TODO: set recoil if it misses
		}};
		moves.put(m.name, m);

	}
	
}
