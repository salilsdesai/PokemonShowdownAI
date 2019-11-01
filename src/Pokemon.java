import java.util.Arrays;
import java.util.function.Function;

public class Pokemon {
	public String species;
	public Type[] types; // Always length 2, types[1] is NONE if single typed
	public Move[] moves;
	public int[] pp; /** pp[i] is the current pp of moves[i] **/
	public int level, maxHp, atk, def, spc, spe, currHp;
	public Status status;
	
	/* [lastAttacker] is the pokemon who attacked this pokemon last, and 
	 * [lastMoveUsed] is the last move used by this pokemon. Used for
	 * damage calculation of [mirrormove]. */
	public Pokemon lastAttacker;
	public Move lastMoveUsed;
	
	/* Class which reprsents all of a pokemon's possible status effects. */
	public static class Status {
		/* An array of length 7 containing the counter storing how many times
		 * a particular statistic has been modified. The indices 0-4 represent
		 * [hp, atk, def, spc, spe, acc, eva] respectively. A positive value [y] indicates a
		 * modified statistic of [x(1 + 0.5y)] where [x] is the original stat
		 * and a negative value [y] indicates a modified statistic [x/(1 + 0.5y)]. 
		 * All values are initialized to 0. */
		public int[] statMod;
		/* Statistics which vary a pokemon's ability to move. All values are
		 * initialized to false. */
		public boolean bide, freeze, paralyze, burn, recharge, charge, poison;
		/* Statistics which can vary in effect. [badly_poisoned] stores the
		 * number of turns since being inflicted as damage increases for each 
		 * successive turn. [sleep] will store the number of remaining turns
		 * for which the current pokemon will be asleep for, and [substitute]
		 * will hold the remaining [hp] of a dummy substitute used to tank 
		 * the opponents attacks. All values are initialized to 0. */
		public int badly_poisoned_counter, sleep_turns_left, confuse_turns_left, substitute_hp;
		/* bide_turns_left is number of turns after which pokemon should unleash energy
		 * or -1 if the pokemon is not biding
		 * bide_damage is amount of damage done to it so far */
		public int bide_turns_left, bide_damage;
		/* Amount of physical damage taken in the current turn for Counter */
		public int counter_damage;
		/* [mimicIndex] is which index in the pokemon's moveslot mimic was originally
		 * in before it got replaced, or -1 if it never got replaced
		 * [mimicPP] is how much pp mimic had before it got replaced. */
		public int mimicIndex, mimicPP;
		/* [transform] will store the active transformed pokemon. If [transform]
		 * is unknown or never used, value will be [null]. */
		public Pokemon transformed;
		
		public Status() {
			statMod = new int[7];
			mimicIndex = -1;
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(species);
		sb.append("\n\t" + Arrays.toString(types));
		sb.append("\n\t" + Arrays.toString(moves));
		sb.append("\n\t" + Arrays.toString(pp));
		sb.append("\n\t" + level);
		sb.append("\n\t" + currHp);
		sb.append("\n\t" + Arrays.toString(new int[] {maxHp, atk, def, spc, spe}));
		return new String(sb);
	}
	
	/**
	 * Construct a new pokemon with the specified species and moves.
	 * Uses Pokedex JSON to get base stats and types, and uses the Move objects
	 * to get move pp.
	 */
	public Pokemon(String species, String[] moves, int level) {
		this.species = species;
		this.level = level;
		
		this.moves = new Move[moves.length];
		for(int i = 0; i < moves.length; i++) {
			this.moves[i] = Move.getMove(moves[i]);
		}
		
		Pokedex.PokedexEntry entry = Pokedex.getDex().get(species);
		this.types = entry.types;
		
		this.pp = new int[this.moves.length];
		for(int i = 0; i < this.moves.length; i++) {
			pp[i] = this.moves[i] != null ? this.moves[i].maxPP : 0;
		}
		
		this.maxHp = (((2 * entry.baseStats[0] + 30 + (255/4))*level)/100) + level + 10;
		this.currHp = this.maxHp;
		
		Function<Integer, Integer> computeStat = (stat -> (((2 * stat + 30 + (255/4))*level)/100) + 5);
		this.atk = computeStat.apply(entry.baseStats[1]);
		this.def = computeStat.apply(entry.baseStats[2]);
		this.spc = computeStat.apply(entry.baseStats[3]);
		this.spe = computeStat.apply(entry.baseStats[4]);
		
		this.level = level;
		status = new Status();
	}
	
	/**
	 * Returns true if the current pokemon has positive [hp] and false otherwise.
	 */
	public boolean isAlive() {
		return (this.currHp > 0); 
	}
	
	public enum StatusCondition {
		FREEZE, PARALYZE, CONFUSE, BURN, POISON, BADLY_POISON, SLEEP
	}
	
	
	/**
	 * Returns whether or not this pokemon has a major status, meaning a new
	 * major status cannot be assigned
	 */
	private boolean hasMajorStatus() {
		return (this.status.freeze || this.status.paralyze || this.status.burn || this.status.poison || 
				(this.status.badly_poisoned_counter > 0) || (this.status.sleep_turns_left > 0));
	}
	
	/**
	 * Set a pokemon to have a particular status condition if possible, otherwise do nothing
	 * n is parameter used by some status conditions, and ignored by all others
	 * - Sleep: n = number of turns sleep should last
	 * - Confuse: n = number of turns confusion should last 
	 */
	public void setStatusCondition(StatusCondition s, int n) {
		switch (s) {
			case FREEZE:
				if(!this.hasMajorStatus() && this.status.substitute_hp == 0) {
					this.status.freeze = true;
				}	
			break;
			case PARALYZE:
				if(!this.hasMajorStatus() && this.status.substitute_hp == 0) {
					this.status.paralyze = true;
				}	
			break;
			case CONFUSE:
				if(this.status.confuse_turns_left == 0 && this.status.substitute_hp == 0) {
					this.status.confuse_turns_left = n;
				}	
			break;
			case BURN:
				if(!this.hasMajorStatus() && this.status.substitute_hp == 0) {
					this.status.burn = true;
				}	
			break;
			case POISON:
				if(!this.hasMajorStatus() && this.status.substitute_hp == 0 && this.types[0] != Type.POISON && this.types[1] != Type.POISON) {
					this.status.poison = true;
				}	
			break;
			case BADLY_POISON: 
				if(!this.hasMajorStatus() && this.status.substitute_hp == 0 && this.types[0] != Type.POISON && this.types[1] != Type.POISON) {
					this.status.badly_poisoned_counter = 1;
				}	
			break;
			case SLEEP:
				if(!this.hasMajorStatus() && this.status.substitute_hp == 0) {
					this.status.sleep_turns_left = n;
				}	
			break;
		}
	}
	
	public enum Stat {
		HP, ATK, DEF, SPC, SPE, ACC, EVA;
		
		public int getIndex() {
			switch (this) {
				case HP:
					return 0;
				case ATK:
					return 1;
				case DEF:
					return 2;
				case SPC:
					return 3;
				case SPE:
					return 4;
				case ACC:
					return 5;
				case EVA:
					return 6;
			}
			return 0;
		}
			
	}
	
	public void statMod(Stat s, int level) {
		this.status.statMod[s.getIndex()] = Math.max(Math.min(this.status.statMod[s.getIndex()] + level, 6), -6);
	}
	
	
	/**
	 * Returns the value of the specified stat for this pokemon
	 * after applying stat modifications and paralysis speed drop
	 * 
	 * Precondition: Don't pass in hp, accuracy or evasiveness
	 */
	public int modifiedStat(Stat s) {
		int stat = 0;
		int i = 0;
		switch (s) {
			case ATK:
				stat = this.atk / (this.status.burn ? 2 : 1);
				i = 1;
			break;
			case DEF:
				stat = this.def;
				i = 2;
			break;
			case SPC:
				stat = this.spc;
				i = 3;
			break;
			case SPE:
				stat = this.spe / (this.status.paralyze ? 4 : 1);
				i = 4;
			break;
			case HP:
			case ACC:
			case EVA:
				return 0;
		}
		
		return (int)(stat * Math.pow(1 + 0.5*this.status.statMod[i], this.status.statMod[i] > 0 ? 1 : -1));
		
	}
	
	public void transformTo(Pokemon p) {
		String[] m = new String[p.moves.length];
		for(int i = 0; i < p.moves.length; i++)
			m[i] = p.moves[i].name;
		this.status.transformed = new Pokemon(p.species, m, p.level);
		
		this.status.transformed.level = this.level;
		this.status.transformed.currHp = this.currHp;
		this.status.transformed.maxHp = this.maxHp;
		this.status.transformed.status = this.status;
		this.status.transformed.status.statMod = Arrays.copyOf(p.status.statMod, p.status.statMod.length);
		for(int i = 0; i < this.status.transformed.pp.length; i++)
			this.status.transformed.pp[i] = 5;
	}
	
	
	/**
	 * Resets the statistics and certain statuses of a pokemon. Intended to be used
	 * only after a pokemon is switched out (when this happens, it is intended for
	 * stat modifications and statuses including mimic/transform/confusion/substitute
	 * and badly-poisoned counter to be cleared).
	 */
	public void resetUponSwitch() {
		
		this.status.confuse_turns_left = 0;
		this.status.substitute_hp = 0;
		this.status.badly_poisoned_counter = Math.max(this.status.badly_poisoned_counter, 1);
		this.status.counter_damage = 0;
		
		// Clear Transform
		this.status.transformed = null;
		
		// Clear mimic
		if(this.status.mimicIndex != -1) {
			this.moves[this.status.mimicIndex] = Move.getMove("mimic");
			this.pp[this.status.mimicIndex] = this.status.mimicPP;
			this.status.mimicIndex = -1;
		}
		
		for(int i = 0; i < this.status.statMod.length; i++) {
			this.status.statMod[i] = 0;
		}
		
		lastMoveUsed = null;
		lastAttacker = null;
	}
	
}
