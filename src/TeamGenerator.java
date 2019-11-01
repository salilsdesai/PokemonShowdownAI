import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;

public class TeamGenerator {
	/**
	 * Return a list of 6 pokemon to be used on a team for battle.
	 * Selection will maintain that teams are relatively balanced,
	 * and are not susceptible to a single weakness.
	 * 
	 * The selection algorithm is based on the selection algorithm
	 * used in PokemonShowdown to emulate the most similar results.
	 */
	public static ArrayList<Pokemon> randomTeam() {
		/* List/set of pokemon with limited ability and boolean status
		 * of whether any are on the potential team. Algorithm will
		 * prevent two of such Pokemon from being teamed together
		 * as such a team would result in an unfair match (in favor of
		 * the opponent). */
		String[] h = {"magikarp", "weedle", "kakuna", "caterpie", "metapod"};
		HashSet<String> handicaps = new HashSet<>(Arrays.asList(h));
		boolean handicapped = false;
		
		/* Counters to maintain the number of types in a team.
		 * Generally,the greater the distribution, the stronger
		 * the team as a similar team is susceptible to common
		 * weaknesses. */
		HashMap<Type, Integer> typeCount = new HashMap<>();
		/* Counters to maintain the number of weaknesses shared
		 * in the team. Example: if exactly two Pokemon
		 * on the team are both weak to [FIRE], then the counter
		 * for [FIRE] will contain the value of 2. */
		HashMap<Type,Integer> weaknessCount = new HashMap<>();

		/* Counters to maintain the strength of the team in terms
		 * of Pokemon tiers. [nuCount] refers to the number of
		 * pokemon on the team which are 'never used' and [uCount]
		 * refers to the number which are'uber'. */
		int nuCount = 0, uCount = 0;
		
		/* Different tier categories; [nu] are tiers within 'never
		 * used' and [uu] are tiers within 'under used'. */
		String[] nuList = {"uu", "uubl", "nfe", "lc", "nu"};
		String[] uuList = {"nfe", "uu", "uubl", "nu"};
		HashSet<String> nu = new HashSet<>(Arrays.asList(nuList));
		HashSet<String> uu = new HashSet<>(Arrays.asList(uuList));
		
		/* List of types with 'spammable' moves that have potential
		 * to sweep a team. */
		Type[] threats = {Type.ELECTRIC, Type.PSYCHIC, Type.WATER, Type.ICE, Type.GROUND};
		
		/* output of the function */
		ArrayList<Pokemon> team = new ArrayList<>();
		
		/* List of potential pokemon that are considered for the
		 * team and the counter used to select them. The list is
		 * shuffled randomly such that incrementing the counter
		 * results in a random pokemon. */
		ArrayList<String> pool = Pokedex.getAllSpecies();
		int counter = 0;
		
		// initialize pool to be ready for random sampling
		shuffle(pool);
		
		
		while (counter < pool.size() && team.size() < 6) {
			/* Select a random pokemon in the pool and makes
			 * sure the team is not handicapped by two pokemon.
			 * Unfortunately, if the pool consists only of only
			 * handicaps, the team must select them. Such a
			 * situation is unlikely. */
			String p_name = pool.get(counter);
			if (pool.size() - counter == 6 - team.size()) {
				team.add(new Pokemon(p_name, moveset(p_name), level(p_name, Pokedex.getDex().get(p_name).tier, handicaps)));
				counter++;
				continue;
			}
			while (pool.size() - counter > 7 - team.size() && handicaps.contains(p_name) && handicapped) {
				counter++;
				p_name = pool.get(counter);
			}
			
			/* Check the tier of the selected pokemon to construct
			 * balanced team. If the tier of the selected pokemon
			 * is not compatible with current team, select the next
			 * random pokemon. */
			String tier = Pokedex.getDex().get(p_name).tier;
			if (tier.equals("lc") || tier.contentEquals("nfe")) {
				if (nuCount > 3 || (handicapped && nuCount > 2) || Math.random() * 3 < 1.0) {
					counter++;
					continue;
				}
			}
			else if (tier.equals("uber")) {
				if (uCount >= 1 && !handicapped) {
					counter++;
					continue;
				}
			}
			else {
				if (uu.contains(tier) && (nuCount > 3 && Math.random() < 2)) {
					counter++;
					continue;
				}
			}
			
			/* Counter true if the pokemon is rejected because of typing. */
			boolean reject = false;
			
			/* Type of pokemon. */
			Type[] types = Pokedex.getDex().get(p_name).types;
			
			/* Check the type of the pokemon to avoid stacking
			 * multiple of the same type on the team. */
			for (Type t : types) {
				if (typeCount.get(t) != null) {
					if (typeCount.get(t) > 1 || (typeCount.get(t) == 1 && Math.random() < 0.5)) {
						reject = true;
						break;
					}
				}
			}
			
			/* Check the potential team against dangerous types
			 * which can sweep. If the team is too susceptible,
			 * reject the current pokemon. */
			for (Type t : threats) {
				if (t.effectiveness(types[0]) * t.effectiveness(types[1]) < 1.0) {
					continue;
				}
				if (weaknessCount.get(t) != null && weaknessCount.get(t) >= 2) {
					reject = true;
					break;
				}
			}
			
			/* Reject the selected pokemon if necessary. */
			if (reject) {
				counter++;
				continue;
			}
			
			/* Update the team. */
			team.add(new Pokemon(p_name, moveset(p_name), level(p_name, tier, handicaps)));
			/* Reflect the number of types in the new team. */
			for (Type t : types) {
				Integer tCount = typeCount.get(t);
				typeCount.put(t, tCount == null ? 1 : tCount + 1);
			}
			/* Reflect the number of weaknesses in the new team. */
			for (Type t : Type.weaknesses(types[0], types[1])) {
				Integer wCount = weaknessCount.get(t);
				weaknessCount.put(t, wCount == null ? 1 : wCount + 1);
			}
			/* Reflect the handicapped status of the new team. */
			handicapped = handicapped || handicaps.contains(p_name);
			/* Reflect the tier count of the new team. */
			if (tier.equals("uber")) {
				uCount++;
			}
			else if (nu.contains(tier)) {
				nuCount++;
			}
			
			counter++;
		}
		
		return team;
	}
	
	/**
	 * Shuffles the order of [pokedex] such that any possible permutation
	 * has the same probability of being the end result.
	 */
	private static void shuffle(List<String> pokedex) {
		Random rand = new Random(System.nanoTime());
		for (int i = 0; i < pokedex.size(); i++) {
			// pick a random index to swap with the current index
			String p = pokedex.get(i);
			int swap_with = rand.nextInt(pokedex.size() - i) + i;
			// execute the swap
			pokedex.set(i, pokedex.get(swap_with));
			pokedex.set(swap_with, p);
		}
	}

	private static String[] moveset(String p_name) {
		ArrayList<String> ret = new ArrayList<>();
		
		/* Entry containing data for [p_name]. */
		Pokedex.PokedexEntry p = Pokedex.getDex().get(p_name);
		
		/* Either add all the 'combo-moves' or none of them. */
		if (Math.random() < 0.5) {
			ret.addAll(Arrays.asList(p.comboMoves));
		}
		/* Add exactly one exclusive move into the set if not full. */
		if (ret.size() < 4 && p.exclusiveMoves.length > 0) {
			int index = (int)(Math.random() * p.exclusiveMoves.length);
			ret.add(p.exclusiveMoves[index]);
		}
		/* Add the essential move if set is not full. */
		if (ret.size() < 4 && p.essentialMove != null) {
			ret.add(p.essentialMove);
		}
		
		/* List of potential moves to be added into the set. The List
		 * is shuffled such that incrementing a counter will select a
		 * random element. */
		List<String> movePool = Arrays.asList(p.randomBattleMoves);
		int counter = 0;
		
		// initialize move pool to be ready for random sampling
		shuffle(movePool);
		
		while (counter < movePool.size() && ret.size() < 4) {
			// fill the moveset up to max
			while (counter < movePool.size() && ret.size() < 4) {
				ret.add(movePool.get(counter));
				counter++;
			}
			
			/* Search for any moves that do not synergize with the remainder of
			 * the set while there are more moves that can be added. If one is
			 * found, cut it from the set and return to the start of the loop
			 * such that the moveset will always contain four moves. */
			if (counter < movePool.size()) {
				// Counter to store the types of moves encountered in the moveset
				HashMap<String, Integer> type_counter = new HashMap<>();
				for (String m : ret) {
					String type = Move.getMove(m).type.toString();
					Integer reocc = type_counter.get(type);
					type_counter.put(type, reocc == null ? 1 :reocc + 1);
				}
				
				HashSet<String> currentSet = new HashSet<>(ret);
				for (int i = 0; i < ret.size(); i++) {
					String curr = ret.get(i);
					// Essential move always remains in the moveset if it is added
					if (curr == p.essentialMove) {
						continue;
					}
					
					boolean rejected = false;
					// Check for redundant moves.
					if (p.essentialMove == null) {
						// redundant water moves
						if (curr.equals("surf") && currentSet.contains("hydropump")) {
							rejected = true;
							break;
						}
						if (curr.equals("hydropump") && currentSet.contains("surf")) {
							rejected = true;
							break;
						}
						// contradictory moves which manipulate health bar
						if (curr.equals("selfdestruct") && currentSet.contains("rest")) {
							rejected = true;
							break;
						}
						if (curr.equals("rest") && currentSet.contains("selfdestruct")) {
							rejected = true;
							break;
						}
						// conflicting moves which modify different stats (optimal to focus one)
						if (curr.equals("swordsdance")) {
							Integer spec = type_counter.get("special");
							Integer phys = type_counter.get("physical");
							
							if (spec != null) {
								if (phys == null || spec > phys || currentSet.contains("growth")) {
									rejected = true;
								}
							}
						}
						if (curr.equals("amnesia") || curr.equals("growth")) {
							Integer spec = type_counter.get("special");
							Integer phys = type_counter.get("physical");
							
							if (phys != null) {
								if (spec == null || spec < phys || currentSet.contains("swordsdance")) {
									rejected = true;
								}
							}
						}
						// overlapping moves which place status effect on the opponent
						if (type_counter.get("status") != null && type_counter.get("status") > 1) {
							if (curr.equals("poisonpowder") || curr.equals("poisonpowder") ||
									curr.equals("sleeppowder") || curr.equals("toxic")) {
								rejected = true;
								break;
							}
						}
					}
					/* If a redundant move is found, remove it. Return to the top of the loop
					 * so that the moveset may be 'filled' again. This ensures that all movesets
					 * will be of maximum size. */
					if (rejected) {
						ret.remove(i);
						break;
					}
					
				}
			}
			
			counter++;
		}
		
		return ret.toArray(new String[4]);
	}
	
	/**
	 * Returns the level of the pokemon [name] which is designated by PokemonShowdown. 
	 * Higher tier pokemon will receive lower levels while lower tier ones will receive
	 * higher ones in order to 'balance' the match.
	 */
	private static int level(String p_name, String tier, HashSet<String> handicap) {
		if (p_name.equals("mewtwo")) {
			return 62;
		}
		else if (p_name.equals("ditto")) {
			return 88;
		}
		else if (handicap.contains(p_name)) {
			return 99;
		}
		else if (tier.equals("lc")) {
			return 88;
		}
		else if (tier.equals("nfe")) {
			return 80;
		}
		else if (tier.equals("uu")) {
			return 74;
		}
		else if (tier.equals("ou")) {
			return 68;
		}
		else if (tier.equals("uber")) {
			return 65;
		}
		else {
			throw new RuntimeException("[level] called with unrecognized tier!");
		}
	}
}
