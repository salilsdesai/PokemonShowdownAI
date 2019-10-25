import java.util.ArrayList;
import java.util.Random;
import java.util.HashSet;
import java.util.Arrays;

public class TeamGenerator {
	public ArrayList<Pokemon> randomTeam() {
		/* list/set of pokemon with limited ability and status of whether any are on the potential team
		 * - game becomes unfair if certain player has more than one handicap */
		String[] h = {"Magikarp", "Weedle", "Kakuna", "Caterpie", "Metapod"};
		HashSet<String> handicaps = new HashSet<>(Arrays.asList(h));
		boolean handicapped = false;
		/* list of counters to store number of pokemon on potential team that are weak to
		 * electric, psychic, water, ice, ground */
		int[] weaknessCount = new int[5];
		/* counters to maintain number of high tier and low tier pokemon on potential team */
		int uberCount = 0, nuCount = 0;
		/* list of potential pokemon that are considered for the team */
		ArrayList<Pokemon> pool = getPokemonPool();
		/* random number generator to select random index in pokemon pool */
		Random rand = new Random(System.nanoTime());
		/* output of the function */
		ArrayList<Pokemon> team = new ArrayList<>();
		
		
		while (team.size() < 6) {
			// select a random pokemon that is in the pool
			int i = 0;
			Pokemon p = null;
			do {
				i = rand.nextInt(pool.size());
				p = pool.get(i);
			} while(p == null || );
			
			for (String s : handicaps) {
				if (p.species == s && handicapped) { //change to physical equality later
					
				}
			}

		}
		
		return team;
	}
	
	public ArrayList<Pokemon> getPokemonPool() {
		//TODO: function which gets all generation 1 pokemon
		return new ArrayList<>();
	}
}
