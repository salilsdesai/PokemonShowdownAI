import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
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
		
		if(this.name.equals("RECHARGE")) {
			Simulator.addMessage(user.species + " recharged");
			user.status.recharge = false;
			return;
		}
		
		// Check for status conditions that would prevent the move from being executed.
		if (user.status.paralyze && Math.random() < 0.25) {
			Simulator.addMessage(user.species + " is fully paralyzed");
			return;
		}
		if (user.status.freeze) {
			// TODO: Make freeze end when it should
			Simulator.addMessage(user.species + " is frozen solid");
			return;
		}
		if (user.status.sleep_turns_left > 0) {
			if(user.status.sleep_turns_left == 1)
				Simulator.addMessage(user.species + " woke up");
			else
				Simulator.addMessage(user.species + " is asleep");
			user.status.sleep_turns_left--;
			return;
		}
		// If the pokemon hurt itself in its confusion, apply damage and return.
		if (user.status.confuse_turns_left > 0) {
			Simulator.addMessage(user.species + " is confused");
			user.status.confuse_turns_left--;
			if(Math.random() < 0.5) {
				int damage = getMove("CONFUSED").damageDealt(user, user);
				user.currHp -= damage;
				Simulator.addMessage(user.species + " hurt itself in confusion (" + damage + ")");
				return;
			}
		}

		Simulator.addMessage(user.species + " used " + this.name);
		
		// special case where move requires charging
		if (name.equals("skyattack")) {
			if(!user.status.charge) {
				user.status.charge = true;
				Simulator.addMessage(user.species + " began charging ");
				return;
			}
			else {
				user.status.charge = false;
			}
		}
		
		int modifiedAccuracy = 
			(int)(
				accuracy * 
				Math.pow(1 + 0.5*user.status.statMod[5], user.status.statMod[5] > 0 ? 1 : -1) * 
				Math.pow(1 + 0.5*target.status.statMod[6], target.status.statMod[6] > 0 ? -1 : 1)
			);
		
		// Accuracy Check
		if((accuracy != -1 && (Math.random() * 100) > modifiedAccuracy)) {
			// The attack missed
			if(this.name.equals("highjumpkick")) {
				user.currHp -= 1;
			}
			Simulator.addMessage("The attack missed!");
			return;
		}
		
		int damage;
		// special damage calculation cases
		if (name.equals("dragonrage")) {
			damage = 40;
		}
		else if (name.equals("nightshade") || name.equals("seismictoss")) {
			damage = user.level;
		}
		else if (name.equals("psywave")) {
			damage = (int)(Math.random() * 1.5 * user.level) + 1;
		}
		else if (name.equals("superfang")) {
			damage = target.currHp/2;
		}
		// special case where move depends on opponent's moveset
		else if (name.equals("mirrormove")) {
			if (target.lastMoveUsed != null && user.lastAttacker == target && !target.lastMoveUsed.name.equals("mirrormove")) {
				damage = getMove(target.lastMoveUsed.name).damageDealt(user, target);
			}
			else {
				damage = 0;
			}
		}
		else if(name.equals("counter")) {
			damage = user.status.counter_damage*2;
		}
		else if(name.equals("bide")) {
			damage = 0;
			if(user.status.bide_turns_left == 0) {
				user.status.bide_turns_left = (int)((Math.random() * 2) + 2);
			}
			else {
				if(user.status.bide_turns_left == 1) {
					damage = user.status.bide_damage*2;
					user.status.bide_damage = 0;
				}
				user.status.bide_turns_left--;
			}
		}
		else {
			// Normal damage
			damage = damageDealt(user, target);
			
			// special case where number of hits is random (so damage is not constant)
			if (name.equals("pinmissle")) {
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
		}
		
		if(target.status.substitute_hp > 0) {
			damage = Math.min(damage, target.status.substitute_hp);
			target.status.substitute_hp -= damage;
			if(damage > 0) {
				Simulator.addMessage(target.species + "'s substitute took " + damage + " damage (" + target.status.substitute_hp + ")");
			}
		}
		else {
			target.currHp = Math.max(0, target.currHp - damage);
			if (damage > 0) {
				Simulator.addMessage(target.species + " lost " + damage + " hp (" + target.currHp + "/" + target.maxHp + ")");
			}
		}
		
		// If this was a physical move, store damage
		if(this.type.isPhysical())
			target.status.counter_damage = damage;
		
		// Save bide damage
		if(target.status.bide_turns_left > 0) {
			target.status.bide_damage += damage;
			Simulator.addMessage(target.species + " is storing damage " + (target.status.bide_damage));
		}
			
		
		// Set the [lastMoveUsed] and [lastAttacker] for mirror move
		user.lastMoveUsed = this;
		target.lastAttacker = user;
		
		// Apply any secondary effects of the current move.
		this.secondaryEffect.accept(new MoveDamage(user, target, damage));
		
	}
	
	/**
	 * Returns amount of damage dealt to target assuming [this] lands successfully
	 * when used by [user] on [target]
	 * 
	 * Uses the following formula: https://bulbapedia.bulbagarden.net/wiki/Damage#Damage_calculation
	 */
	public int damageDealt(Pokemon user, Pokemon target) {
		
		if(this.power == 0)
			return 0;
		
		boolean critical = (Math.random() < Pokedex.getDex().get(user.species).baseStats[4]/(!highCritRatio ? 512.0 : 64.0));
		
		if(critical) {
			Simulator.addMessage("Critical hit!");
		}
		
		int level = user.level * (critical ? 2 : 1);
		int power = this.power;
		
		int attackingStat, defendingStat;
		if(this.type.isPhysical()) {
			attackingStat = critical ? user.atk : user.modifiedStat(Pokemon.Stat.ATK);
			defendingStat = critical ? target.def : target.modifiedStat(Pokemon.Stat.DEF);
		}
		else {
			attackingStat = critical ? user.spc : user.modifiedStat(Pokemon.Stat.SPC);
			defendingStat = critical ? target.spc : target.modifiedStat(Pokemon.Stat.SPC);
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
			double typeModifier = this.type.effectiveness(t);
			if(typeModifier == 0)
				Simulator.addMessage("It didn't affect " + target.species);
			else if(typeModifier > 1)
				Simulator.addMessage("It's super effective!");
			else if(typeModifier < 1)
				Simulator.addMessage("It's not very effective");
			modifier *= typeModifier;
		}
		
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
		Move m = new Move();
		
		
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
		
		m.name = "twineedle";
		m.power = 50;
		m.maxPP = 32;
		m.accuracy = 100;
		m.type = Type.BUG;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (Math.random() < 0.2) {
					md.target.setStatusCondition(Pokemon.StatusCondition.POISON, 0);
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "triattack";
		m.power = 80;
		m.maxPP = 16;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "transform";
		m.power = 0;
		m.maxPP = 16;
		m.accuracy = -1;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.transformTo(md.target);
			}
		};
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
		m.type = Type.NONE;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {public void accept(MoveDamage md) {
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
		}};
		moves.put(m.name, m);

					
		m = new Move();
		m.name = "toxic";
		m.power = 0;
		m.maxPP = 16;
		m.accuracy = 90;
		m.type = Type.POISON;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.setStatusCondition(Pokemon.StatusCondition.BADLY_POISON, 0);
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "thunderbolt";
		m.power = 95;
		m.maxPP = 24;
		m.accuracy = 100;
		m.type = Type.ELECTRIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (Math.random() < 0.1) {
					md.target.setStatusCondition(Pokemon.StatusCondition.PARALYZE, 0);
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "thunderwave";
		m.power = 0;
		m.maxPP = 32;
		m.accuracy = 100;
		m.type = Type.ELECTRIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.setStatusCondition(Pokemon.StatusCondition.PARALYZE, 0);
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "thunder";
		m.power = 110;
		m.maxPP = 16;
		m.accuracy = 70;
		m.type = Type.ELECTRIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (Math.random() < 0.1) {
					md.target.setStatusCondition(Pokemon.StatusCondition.PARALYZE, 0);
				}
			}
		};
		moves.put(m.name, m);
		
		
		m = new Move();
		m.name = "tailwhip";
		m.power = 0;
		m.maxPP = 48;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (md.target.status.statMod[2] != -3) {
					md.target.status.statMod[2]--;
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "tackle";
		m.power = 0;
		m.maxPP = 56;
		m.accuracy = 95;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "swordsdance";
		m.power = 0;
		m.maxPP = 48;
		m.accuracy = -1;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.statMod(Pokemon.Stat.ATK, 2);
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "surf";
		m.power = 95;
		m.maxPP = 24;
		m.accuracy = 100;
		m.type = Type.WATER;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return; 
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "superfang";
		m.power = 0;
		m.maxPP = 16;
		m.accuracy = 90;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return; 
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "substitute";
		m.power = 0;
		m.maxPP = 16;
		m.accuracy = -1;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (md.user.currHp > md.user.maxHp/4 && md.user.status.substitute_hp <= 0) {
					md.user.status.substitute_hp = md.user.maxHp/4;
					md.user.currHp -= md.user.maxHp/4;
					Simulator.addMessage(md.user.species + "made a substitute (" + md.user.status.substitute_hp + ")");
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "submission";
		m.power = 80;
		m.maxPP = 40;
		m.accuracy = 80;
		m.type = Type.FIGHTING;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.currHp -= md.damage/4;
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "stunspore";
		m.power = 0;
		m.maxPP = 48;
		m.accuracy = 75;
		m.type = Type.GRASS;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.setStatusCondition(Pokemon.StatusCondition.PARALYZE, 0);
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "stringshot";
		m.power = 0;
		m.maxPP = 64;
		m.accuracy = 95;
		m.type = Type.BUG;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.statMod(Pokemon.Stat.SPE, -1);
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "spore";
		m.power = 0;
		m.maxPP = 21;
		m.accuracy = 100;
		m.type = Type.GRASS;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.setStatusCondition(Pokemon.StatusCondition.SLEEP, (int)(Math.random() * 7));
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "softboiled";
		m.power = 0;
		m.maxPP = 16;
		m.accuracy = -1;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.currHp = Integer.min(md.user.maxHp, md.user.currHp + md.user.maxHp/2);
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "smokescreen";
		m.power = 0;
		m.maxPP = 32;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.statMod(Pokemon.Stat.ACC, -1);
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "sludge";
		m.power = 65;
		m.maxPP = 32;
		m.accuracy = 100;
		m.type = Type.POISON;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (Math.random() < 0.4) {
					md.target.setStatusCondition(Pokemon.StatusCondition.POISON, 0);
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "sleeppowder";
		m.power = 0;
		m.maxPP = 21;
		m.accuracy = 75;
		m.type = Type.GRASS;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.setStatusCondition(Pokemon.StatusCondition.SLEEP, (int)(Math.random() * 7));
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "slash";
		m.power = 70;
		m.maxPP = 32;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = true;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "skyattack";
		m.power = 140;
		m.maxPP = 8;
		m.accuracy = 90;
		m.type = Type.FLYING;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "sing";
		m.power = 0;
		m.maxPP = 21;
		m.accuracy = 55;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.setStatusCondition(Pokemon.StatusCondition.SLEEP, (int)(Math.random() * 7));
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "selfdestruct";
		m.power = 260;
		m.maxPP = 8;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.currHp = 0;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "seismictoss";
		m.power = 0;
		m.maxPP = 32;
		m.accuracy = 100;
		m.type = Type.FIGHTING;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "screech";
		m.power = 0;
		m.maxPP = 64;
		m.accuracy = 85;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.statMod(Pokemon.Stat.DEF, -2);
			}
		};
		moves.put(m.name, m);
		
		
		m = new Move(); 
		m.name = "sandattack";
		m.power = 0;
		m.maxPP = 21;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.statMod(Pokemon.Stat.ACC, -1);
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "rockslide";
		m.power = 75;
		m.maxPP = 16;
		m.accuracy = 90;
		m.type = Type.ROCK;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "rest";
		m.power = 0;
		m.maxPP = 16;
		m.accuracy = -1;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if(md.user.currHp != md.user.maxHp) {
					// Clear existing statuses
					md.user.status.poison = false;
					md.user.status.freeze = false;
					md.user.status.paralyze = false;
					md.user.status.burn = false;
					md.user.status.badly_poisoned_counter = 0;
					
					md.user.currHp = md.user.maxHp;
					md.user.setStatusCondition(Pokemon.StatusCondition.SLEEP, 2);
				}
			}
		};
		moves.put(m.name, m);
		
		// TODO: check what reflect actually does (base defense or stat mod)
		m = new Move(); 
		m.name = "reflect";
		m.power = 0;
		m.maxPP = 20;
		m.accuracy = -1;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.statMod(Pokemon.Stat.DEF, 2);
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "recover";
		m.power = 0;
		m.maxPP = 32;
		m.accuracy = -1;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.currHp = Integer.min(md.user.maxHp, md.user.currHp + md.user.maxHp/2);
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "razorleaf";
		m.power = 55;
		m.maxPP = 40;
		m.accuracy = 95;
		m.type = Type.GRASS;
		m.highCritRatio = true;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "quickattack";
		m.power = 40;
		m.maxPP = 48;
		m.accuracy = 95;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 1;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "psywave";
		m.power = 0;
		m.maxPP = 21;
		m.accuracy = 80;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "psychic";
		m.power = 90;
		m.maxPP = 16;
		m.accuracy = 100;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (3.0 * Math.random() < 1) {
					md.target.statMod(Pokemon.Stat.SPC, -1);
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "poisonsting";
		m.power = 15;
		m.maxPP = 56;
		m.accuracy = 100;
		m.type = Type.POISON;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (Math.random() < 0.2) {
					md.target.setStatusCondition(Pokemon.StatusCondition.POISON, 0);
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "pinmissile";
		m.power = 14;
		m.maxPP = 32;
		m.accuracy = 85;
		m.type = Type.BUG;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "nightshade";
		m.power = 0;
		m.maxPP = 21;
		m.accuracy = 100;
		m.type = Type.GHOST;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "mirrormove";
		m.power = 0;
		m.maxPP = 32;
		m.accuracy = -1;
		m.type = Type.FLYING;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "mimic";
		m.power = 0;
		m.maxPP = 16;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				for(int i = 0; i < md.user.moves.length; i++) {
					if(md.user.moves[i].name.equals("mimic")) {
						md.user.status.mimicIndex = i;
						md.user.status.mimicPP = md.user.pp[i];
						md.user.moves[i] = md.target.moves[(int)(Math.random() * 4)];
						md.user.pp[i] = md.user.moves[i].maxPP;
					}
				}
			}
		};
		moves.put(m.name, m);
		
		
		m = new Move(); 
		m.name = "megakick";
		m.power = 120;
		m.maxPP = 8;
		m.accuracy = 75;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "megadrain";
		m.power = 40;
		m.maxPP = 16;
		m.accuracy = 100;
		m.type = Type.GRASS;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.currHp = Integer.min(md.user.maxHp, md.user.currHp + md.damage/2);
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "meditate";
		m.power = 0;
		m.maxPP = 64;
		m.accuracy = -1;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.user.statMod(Pokemon.Stat.ATK, 1);
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "lovelykiss";
		m.power = 0;
		m.maxPP = 16;
		m.accuracy = 75;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.setStatusCondition(Pokemon.StatusCondition.SLEEP, (int)(Math.random() * 7));
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "leer";
		m.power = 0;
		m.maxPP = 48;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.statMod(Pokemon.Stat.DEF, -1);
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "icebeam";
		m.power = 95;
		m.maxPP = 16;
		m.accuracy = 100;
		m.type = Type.ICE;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (Math.random() < 0.1 ) {
					md.target.setStatusCondition(Pokemon.StatusCondition.FREEZE, 0);
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "hypnosis";
		m.power = 0;
		m.maxPP = 32;
		m.accuracy = 60;
		m.type = Type.PSYCHIC;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				md.target.setStatusCondition(Pokemon.StatusCondition.SLEEP, (int)(Math.random() * 7));
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "hyperbeam";
		m.power = 150;
		m.maxPP = 8;
		m.accuracy = 90;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				if (md.target.currHp > 0) {
					md.user.status.recharge = true;
				}
			}
		};
		moves.put(m.name, m);
		
		m = new Move(); 
		m.name = "hydropump";
		m.power = 120;
		m.maxPP = 8;
		m.accuracy = 90;
		m.type = Type.WATER;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "CONFUSED";
		m.power = 40;
		m.maxPP = Integer.MAX_VALUE;
		m.accuracy = -1;
		m.type = Type.NONE;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() {
			public void accept(MoveDamage md) {
				return;
			}
		};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "RECHARGE";
		m.power = 0;
		m.maxPP = Integer.MAX_VALUE;
		m.accuracy = -1;
		m.type = Type.NONE;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() { public void accept(MoveDamage md) {
			md.user.status.recharge = false;
		}};
		moves.put(m.name, m);
		
		m = new Move();
		m.name = "STRUGGLE";
		m.power = 50;
		m.maxPP = Integer.MAX_VALUE;
		m.accuracy = 100;
		m.type = Type.NORMAL;
		m.highCritRatio = false;
		m.priority = 0;
		m.secondaryEffect = new Consumer<MoveDamage>() { public void accept(MoveDamage md) {
			md.user.currHp -= md.damage/2;
		}};
		moves.put(m.name, m);
	}
	
	public static void main(String[] args) {
		loadMoves();
		Object[] moveNameObject = moves.keySet().toArray();
		ArrayList<String> moveNames = new ArrayList<>();
		
		for (int i = 0; i < moveNameObject.length; i++) {
			moveNames.add((String)moveNameObject[i]);
		}
		
		Collections.sort(moveNames);
		
		for (int i = 0; i < moveNames.size(); i++) {
			System.out.println(moveNames.get(i));
		}
	}
}
