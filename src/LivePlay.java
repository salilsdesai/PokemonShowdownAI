import java.util.ArrayList;
import java.util.Scanner;

public class LivePlay {
	
	public static Scanner input;
	
	/** 
	 * Prompt user for information about the initialization of the game with the
	 * and convert to a game state
	 * 
	 * Pokemon1 [Name Move1 Move2 ...] 
	 * Pokemon2 [Name Move1 Move2 ...]
	 * Pokemon3 [Name Move1 Move2 ...]
	 * Pokemon4 [Name Move1 Move2 ...]
	 * Pokemon5 [Name Move1 Move2 ...]
	 * Pokemon6 [Name Move1 Move2 ...]
	 * OpponentPokemonActive [Name Level Move1 Move2 ...] 
	 */
	public static GameState initializeGame() {
		if (input == null)
			input = new Scanner(System.in);
		
		ArrayList<Pokemon> p1Team= new ArrayList<>();
		System.out.println("-- Game State Initialization --");
		System.out.println("Enter information about each of your pokemon in the form 'species move1 move2 ...'");
		for(int i = 0; i < 6; i++) {
			System.out.print("Pokemon " + (i+1) + ": ");
			String s = input.nextLine();
			String[] inputs = s.split(" ");
			String species = inputs[0];
			String[] moves = new String[4];
			for(int j = 1; j < inputs.length; j++) {
				moves[j-1] = inputs[j];
			}
			String tier = Pokedex.getDex().get(species).tier;
			int level = TeamGenerator.level(species, tier);
			Pokemon p = new Pokemon(species, moves, level);
			p1Team.add(p);
		}
		
		System.out.print("Opponent Pokemon Species: ");
		String oppoSpecies = input.nextLine();
		String oppoTier = Pokedex.getDex().get(oppoSpecies).tier;
		int oppoLevel = TeamGenerator.level(oppoSpecies, oppoTier);
		Pokemon oppoPokemon = new Pokemon(oppoSpecies, new String[4], oppoLevel);
		
		return new GameState(new Team(p1Team), oppoPokemon);

	}

	public static GameState updateTurn(GameState gs) {
		// TODO
		return null;
	}
	
	public static void playLive() {
		GameState gs = initializeGame();
		System.out.println(gs);
		// TODO: play the game
	}
	
	public static void main(String[] args) {
		playLive();
	}
}
