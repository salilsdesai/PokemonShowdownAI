import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
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
		
		ArrayList<Pokemon> p1Team= new ArrayList<>();
		System.out.println("-- Game State Initialization --");
		System.out.println("Enter information about each of your pokemon in the form 'species move1 move2 ...'");
		for(int i = 0; i < 6; i++) {
			try  {
				System.out.print("Pokemon " + (i+1) + ": ");
				String s = input.nextLine();
				String[] inputs = s.split(" ");
				String species = inputs[0];
				String[] moves = new String[4];
				for(int j = 1; j < inputs.length; j++) {
					moves[j-1] = inputs[j];
				}
				int level = getLevel(species);
				Pokemon p = new Pokemon(species, moves, level);
				p1Team.add(p);	
			}
			catch (Exception e) {
				e.printStackTrace();
				i--;
			}

		}
		
		System.out.print("Opponent Pokemon Species: ");
		Pokemon oppoPokemon = null;
		while(oppoPokemon == null) {
			try {
				String oppoSpecies = input.nextLine();
				int oppoLevel = getLevel(oppoSpecies);
				oppoPokemon = new Pokemon(oppoSpecies, new String[4], oppoLevel);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		}
		
		return new GameState(new Team(p1Team), oppoPokemon);

	}

	/**
	 * [update] must be in the form of one of the following (where # is the player number (1 or 2))
	 * - # s[witch] [species] 
	 * - # m[ove] [name]
	 * - # h[ealth] [percentage]
	 * - # b[egin status] ['sleep', 'paralyze', 'freeze', 'poison', 'toxic', 'burn', 'confuse', 'charge', 'recharge', 'substitute']
	 * - # e[nd status] ['sleep', 'paralyze', 'freeze', 'poison', 'toxic', 'burn', 'confuse', 'charge', 'recharge', 'substitute']
	 * - # c[hange stat] [atk, def, spe, spc, acc, eva] [change level]
	 * - [e]x[it]
	 * 
	 * 	ex. 
	 * 	1 s pikachu
	 *  2 m thunderbolt
	 *  1 h 95.2
	 *  2 b freeze
	 *  1 e sleep
	 * 	2 c spc -2
	 * 
	 *  return false if game should be ended (the update was "x")
	 */
	public static boolean updateGameState(GameState state, String update) {
		try{
			String[] split = update.split(" ");
			
			if(split[0].charAt(0) == 'e') {
				return false;
			}
			
			boolean p1;
			if(split[0].equals("1")) {
				p1 = true;
			}
			else if (split[0].equals("2")) {
				p1 = false;
			}
			else {
				throw new RuntimeException("Invalid player number");
			}
			String action = split[1];
			
			if(action.equals("s")) {
				String species = split[2];
				if(p1) {
					boolean found = false;
					for(Pokemon p : state.p1_team.pokemonList) {
						if(p.species.equals(species)) {
							found = true;
							state.p1_team.activePokemon = p;
						}
					}
					if(!found) {
						throw new RuntimeException("No pokemon found with that species on p1's team");
					}
					state.p1_team.activePokemon.resetUponSwitch();
				}
				else {
					boolean foundPokemon = false;
					for(Pokemon p : state.p2_pokemon.keySet()) {
						if(p.species.equals(species)) {
							state.p2_active = p;
							foundPokemon = true;
						}
					}
					if(!foundPokemon) {
						Pokemon p = new Pokemon(species, new String[4], getLevel(species));
						state.p2_pokemon.put(p, new HashSet<>());
						state.p2_active = p;
					}
					state.p2_active.resetUponSwitch();
				}
			}
			else if (action.equals("m")) {
				String moveName = Replay.filterNonLetters(split[2].toLowerCase());
				Move move = Move.getMove(moveName);
				if(p1) {
					boolean found = false;
					for(int k = 0; k < state.p1_team.activePokemon.moves.length; k++) {
						if(state.p1_team.activePokemon.moves[k] == move) {
							found = true;
							state.p1_team.activePokemon.pp[k]--;
							k = state.p1_team.activePokemon.moves.length;
						}
					}
					if(!found) {
						throw new RuntimeException("Move name not found for p1's active pokemon");
					}
				}
				else {
					state.p2_pokemon.get(state.p2_active).add(move);
				}
			}
			else if (action.equals("h")) {
				Pokemon targetPokemon = (p1) ? state.p1_team.activePokemon : state.p2_active;
				targetPokemon.currHp = (int)(targetPokemon.maxHp * Double.parseDouble(split[2]) / 100);
			}
			else if (action.equals("b")) {
				Pokemon targetPokemon = (p1) ? state.p1_team.activePokemon : state.p2_active;
				String status = split[2];
				if(status.equals("sleep")) {
					targetPokemon.status.sleep_turns_left = 3; // guess how long sleep will be because we don't know upfront
				}
				else if (status.equals("paralyze")) {
					targetPokemon.status.paralyze = true;
				}
				else if (status.equals("freeze")) {
					targetPokemon.status.freeze = true;
				}
				else if (status.equals("poison")) {
					targetPokemon.status.poison = true;
				}
				else if (status.equals("toxic")) {
					targetPokemon.status.badly_poisoned_counter = 1;
				}
				else if (status.equals("burn")) {
					targetPokemon.status.burn = true;
				}
				else if (status.equals("confuse")) {
					targetPokemon.status.confuse_turns_left = 2; // we don't know how long it will last, approximate as 2
				}
				else if (status.equals("charge")) {
					targetPokemon.status.charge = true;
				}
				else if (status.equals("recharge")) {
					targetPokemon.status.recharge = true;
				}
				else if (status.equals("substitute")) {
					targetPokemon.status.substitute_hp = targetPokemon.maxHp/4;
				}
				else {
					throw new RuntimeException("status name not found");
				}
			}
			else if (action.equals("e")) {
				Pokemon targetPokemon = (p1) ? state.p1_team.activePokemon : state.p2_active;
				String status = split[2];
				if(status.equals("sleep")) {
					targetPokemon.status.sleep_turns_left = 0;
				}
				else if (status.equals("paralyze")) {
					targetPokemon.status.paralyze = false;
				}
				else if (status.equals("freeze")) {
					targetPokemon.status.freeze = false;
				}
				else if (status.equals("poison")) {
					targetPokemon.status.poison = false;
				}
				else if (status.equals("toxic")) {
					targetPokemon.status.badly_poisoned_counter = 0;
				}
				else if (status.equals("burn")) {
					targetPokemon.status.burn = false;
				}
				else if (status.equals("confuse")) {
					targetPokemon.status.confuse_turns_left = 0;
				}
				else if (status.equals("charge")) {
					targetPokemon.status.charge = false;
				}
				else if (status.equals("recharge")) {
					targetPokemon.status.recharge = false;
				}
				else if (status.equals("substitute")) {
					targetPokemon.status.substitute_hp = 0;
				}
				else {
					throw new RuntimeException("status name not found");
				}
			}
			else if (action.equals("c")) {
				Pokemon targetPokemon = (p1) ? state.p1_team.activePokemon : state.p2_active;
				String statString = split[2].toUpperCase();
				Pokemon.Stat stat = Pokemon.Stat.valueOf(statString);
				int boostAmount = Integer.parseInt(split[3]);
				targetPokemon.statMod(stat, boostAmount);				
			}
			else {
				throw new RuntimeException("Invalid action [" + action + "]");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	public static void playLive() {
		if(input == null) {
			input = new Scanner(System.in);
		}
		
		GameState gs = initializeGame();
		
		NeuralNet pn = new NeuralNet("PolicyNetwork/PolicyNetworkWeights.txt");
		NeuralNet vn = new NeuralNet("PolicyNetwork/PolicyNetworkWeights.txt");
		
		boolean continueGame = true;
		
		while(continueGame && gs.p1_team.hasAlive()) {
			// Print out know information
			System.out.println(gs);
			
			Simulator.Action a = MCTS.chooseMove(gs, pn, vn);
			System.out.println("\nMove Chosen by AI:");
			System.out.println("\t" + a.toString());
			
			System.out.println("Enter updates to game state:");
			String s = input.nextLine();
			while(s != null && s.length() > 0) {
				continueGame = continueGame && updateGameState(gs, s);
				s = input.nextLine();
			}
		}
	}
	
	public static void main(String[] args) {
		playLive();
	}
	
	/**
	 * Use calls to functions in Dex and TeamGenerator to determine the level
	 * of a pokemon with the given species
	 */
	private static int getLevel(String species) {
		String tier = Pokedex.getDex().get(species).tier;
		return TeamGenerator.level(species, tier);
	}
}
