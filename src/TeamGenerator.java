import java.util.ArrayList;
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
		String[] h = {"Magikarp", "Weedle", "Kakuna", "Caterpie", "Metapod"};
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
		String[] nuList = {"UU", "UUBL", "NFE", "LC", "NU"};
		String[] uuList = {"NFE", "UU", "UBL", "NU"};
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
		ArrayList<Pokemon> pool = getPokemonPool();
		int counter = 0;
		
		// initialize pool to be ready for random sampling
		shuffle(pool);
		
		
		while (counter < pool.size() && team.size() < 6) {
			/* Select a random pokemon in the pool and makes
			 * sure the team is not handicapped by two pokemon.
			 * Unfortunately, if the pool consists only of only
			 * handicaps, the team must select them. Such a
			 * situation is unlikely. */
			Pokemon p = pool.get(counter);
			if (pool.size() - counter == 6 - team.size()) {
				team.add(p);
				counter++;
				continue;
			}
			while (pool.size() - counter > 7 - team.size() && handicaps.contains(p.species) && handicapped) {
				counter++;
			}

			/* Check the tier of the selected pokemon to construct
			 * balanced team. If the tier of the selected pokemon
			 * is not compatible with current team, select the next
			 * random pokemon. */
			String tier = getTier(p.species);
			if (tier.equals("LC") || tier.contentEquals("NFE")) {
				if (nuCount > 3 || (handicapped && nuCount > 2) || Math.random() * 3 < 1.0) {
					counter++;
					continue;
				}
			}
			else if (tier.equals("Uber")) {
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
			
			/* Counter true if the pokemon is rejected. */
			boolean reject = false;
			
			/* Check the type of the pokemon to avoid stacking
			 * multiple of the same type on the team. */
			for (Type t : p.types) {
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
				if (t.effectiveness(p.types[0]) * t.effectiveness(p.types[1]) < 1.0) {
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
			team.add(p);
			/* Reflect the number of types in the new team. */
			for (Type t : p.types) {
				Integer tCount = typeCount.get(t);
				typeCount.put(t, tCount == null ? 1 : tCount + 1);
			}
			/* Reflect the number of weaknesses in the new team. */
			for (Type t : Type.weaknesses(p.types[0], p.types[1])) {
				Integer wCount = weaknessCount.get(t);
				weaknessCount.put(t, wCount == null ? 1 : wCount + 1);
			}
			/* Reflect the handicapped status of the new team. */
			handicapped = handicapped || handicaps.contains(p.species);
			/* Reflect the tier count of the new team. */
			if (tier.equals("Uber")) {
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
	private static void shuffle(ArrayList<Pokemon> pokedex) {
		Random rand = new Random(System.nanoTime());
		for (int i = 0; i < pokedex.size(); i++) {
			// pick a random index to swap with the current index
			Pokemon p = pokedex.get(i);
			int swap_with = rand.nextInt(pokedex.size() - i) + i;
			// execute the swap
			pokedex.set(i, pokedex.get(swap_with));
			pokedex.set(swap_with, p);
		}
	}
	
	/**
	 * Returns the list of all generation one pokemon as pokemon
	 * objects (regardless of tier, type, etc).
	 */
	private static ArrayList<Pokemon> getPokemonPool() {
		//TODO: function which gets all generation 1 pokemon
		return new ArrayList<> ();
	}
	
	/**
	 * Returns the tier of a pokemon species [species] with 
	 * respect to multiplayer battles. Example: Mewtwo is
	 * "uber" tier because it is very strong.
	 */
	private static String getTier(String species) {
		// TODO: function which returns the tier of [species] in string form
		return "";
	}
}
