import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

/**
 * A class that stores a single randomly chosen gamestate from 
 * the perspective of p1 and and the winner from a particular 
 * Pokemon Showdown Gen 1 Random Battle Replay
 */
public class Replay {
	
	public static class ReplaySwitchAction {
		/** true if p1, false if p2 */
		public boolean player;
		/** the species of the pokemon being switched in */
		public String species;
		/** the level of the pokemon being switched in */
		public int level;
		/**
		 * [line] is text of a switch line in a replay file,
		 * in the form "|switch|p#a: ...|..., L##|##\/##"
		 * @param line
		 */
		public ReplaySwitchAction(String line) {
			int playerIndex = line.indexOf('p') + 1;
			int speciesIndex = line.indexOf('|', playerIndex) + 1;
			int commaIndex = line.indexOf(',', speciesIndex);
			int endOfLevelIndex = line.indexOf('|', commaIndex);
			player = (line.charAt(playerIndex) == '1');
			species = filterNonLetters(line.substring(speciesIndex, commaIndex).toLowerCase());
			level = Integer.parseInt(line.substring(commaIndex+3, endOfLevelIndex));
		}
		public String toString() {
			return "Player " + (player ? '1' : '2') + " switch in " + species + " (" + level + ")";
		}
	}
	
	public GameState state;
	/** true if p1 won, false if p2 won */
	public boolean winner; 
	/** the turn number of the saved state from the replay */
	public int turnNum;
	
	public String toString() {
		return (state.toString() + "\nTurn Num: " + turnNum + "\nWinner: " + (winner ? "p1" : "p2"));
	}
	
	public static String filterNonLetters(String s) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < s.length(); i++) {
			if(s.charAt(i) >= 'a' && s.charAt(i) <= 'z')
				sb.append(s.charAt(i));
		}
		return new String(sb);
	}
	
	/**
	 * Read all lines from [filename] and return result
	 * as a String array
	 */
	public static String[] readFile(String filename) {
		try {
			File file = new File(filename);
			Scanner scan = new Scanner(file);
			ArrayList<String> lines = new ArrayList<String>();
			while(scan.hasNext()) {
				lines.add(scan.nextLine());
			}
			scan.close();
			return lines.toArray(new String[lines.size()]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Return the number of turns a particular replay lasted for
	 * [replayLines] are the lines parsed from the html replay file
	 */
	public static int numTurns(String[] replayLines) {
		int i = 0;
		while(replayLines[i].length() < 2 || replayLines[i].charAt(1) != 'w') {
			i++;
		}
		while(replayLines[i].length() < 2 || replayLines[i].charAt(1) != 't') {
			i--;
		}
		return Integer.parseInt(replayLines[i].substring(replayLines[i].lastIndexOf('|') + 1, replayLines[i].length()));
	}
	
	
	/**
	 * Parse a Replay from the provided file
	 * Precondition: [filename] must be the path of a valid
	 * gen 1 random battle replay file
	 */
	public Replay(String filename) {
		String[] lines = readFile(filename);
		
		int i = 0;
		while(lines[i].length() < 12 || !lines[i].substring(0,11).equals("|player|p1|")) {
			i++;
		}
		String p1Name = lines[i].substring(11, lines[i].indexOf('|', 11));
		
		while(!lines[i].equals("|start")) {
			i++;
		}
				
		ReplaySwitchAction p1StartAction = new ReplaySwitchAction(lines[i+1]);
		ReplaySwitchAction p2StartAction = new ReplaySwitchAction(lines[i+2]);
		
		Pokemon p1StartPokemon = new Pokemon(p1StartAction.species, new String[4], p1StartAction.level);
		Pokemon p2StartPokemon = new Pokemon(p2StartAction.species, new String[4], p2StartAction.level);
		
		ArrayList<Pokemon> p1StartTeamList = new ArrayList<Pokemon>();
		p1StartTeamList.add(p1StartPokemon);
		Team p1StartTeam = new Team(p1StartTeamList);
		
		state = new GameState(p1StartTeam, p2StartPokemon);
		
		i += 3;
		
		int numTurns = numTurns(lines);
//		turnNum = (int)(Math.random() * numTurns + 1);
		turnNum = 10;
		int currTurn = 1;
		
		/*
		 * Now we have initial game state ready, go turn by turn, 
		 * adding a new game state each time, until we reach our target turn
		 */
		
		while(i < lines.length && currTurn < turnNum) {
			
			String l = lines[i];
			
			int secondBarIndex = lines[i].indexOf('|', 1);
			
			if(lines[i].length() >= 2 && secondBarIndex != -1) {
				String actionCategory = lines[i].substring(1, secondBarIndex);
				
				if(actionCategory.equals("switch")) {
					ReplaySwitchAction rsa = new ReplaySwitchAction(lines[i]);
					if(rsa.player) {
						// p1 switched
						state.p1_team.activePokemon.resetUponSwitch();
						boolean foundPokemon = false;
						for(Pokemon p : state.p1_team.pokemonList) {
							if(p.species.equals(rsa.species)) {
								state.p1_team.activePokemon = p;
								foundPokemon = true;
							}
						}
						if(!foundPokemon) {
							Pokemon p = new Pokemon(rsa.species, new String[4], rsa.level);
							state.p1_team.pokemonList.add(p);
							state.p1_team.activePokemon = p;
						}
					}
					else {
						// p2 switched
						state.p2_active.resetUponSwitch();
						boolean foundPokemon = false;
						for(Pokemon p : state.p2_pokemon.keySet()) {
							if(p.species.equals(rsa.species)) {
								state.p2_active = p;
								foundPokemon = true;
							}
						}
						if(!foundPokemon) {
							Pokemon p = new Pokemon(rsa.species, new String[4], rsa.level);
							state.p2_pokemon.put(p, new HashSet<>());
							state.p2_active = p;
						}
					}
				}
				else if (actionCategory.equals("move")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					int thirdBarIndex = lines[i].indexOf('|', secondBarIndex+1);
					String moveName = filterNonLetters(lines[i].substring(thirdBarIndex+1, lines[i].indexOf('|', thirdBarIndex+1)).toLowerCase());
					Move move = Move.getMove(moveName);
					
					if(player) {
						// p1 used the move
						for(int j = 0; j < state.p1_team.activePokemon.moves.length; j++) {
							if(state.p1_team.activePokemon.moves[j] == move) {
								// If we already know p1's active has this move, update pp and do nothing else
								state.p1_team.activePokemon.pp[j]--;
								j = state.p1_team.activePokemon.moves.length;
								
							}
							else if(state.p1_team.activePokemon.moves[j] == null) {
								// Since we didn't know p1's active has this move, add it to its move list
								state.p1_team.activePokemon.moves[j] = move;
								state.p1_team.activePokemon.pp[j] = move.maxPP-1;
								j = state.p1_team.activePokemon.moves.length;
							}
						}
						
					}
					else {
						// p2 used the move
						state.p2_pokemon.get(state.p2_active).add(move);
					}
				}
				else if (actionCategory.equals("win")) {
					currTurn = turnNum; // end the loop
					i--; // decrement i so when it is incremented at the end of the loop we can detect the winner
				}
				else if (actionCategory.equals("turn")) {
					currTurn = Integer.parseInt(lines[i].substring(secondBarIndex+1));
				}
				else if (actionCategory.equals("-damage")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					int thirdBarIndex = lines[i].indexOf('|', secondBarIndex+1);
					int backslashIndex = lines[i].indexOf('\\', thirdBarIndex);
					int remainingHp = (lines[i].charAt(thirdBarIndex+1) == '0' ? 0 : Integer.parseInt(lines[i].substring(thirdBarIndex+1, backslashIndex)));
					Pokemon targetPokemon = player ? state.p1_team.activePokemon : state.p2_active;
					targetPokemon.currHp = remainingHp;
				}
				else if (actionCategory.equals("-status")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					Pokemon targetPokemon = player ? state.p1_team.activePokemon : state.p2_active;
					
					int thirdBarIndex = lines[i].indexOf('|', secondBarIndex+1);
					int fourthBarIndex = lines[i].indexOf('|', thirdBarIndex+1);
					if(fourthBarIndex == -1)
						fourthBarIndex = lines[i].length();
					
					String status = lines[i].substring(thirdBarIndex+1, fourthBarIndex);
					
					if(status.equals("frz")) {
						targetPokemon.status.freeze = true;
					}
					else if(status.equals("par")) {
						targetPokemon.status.paralyze = true;
					}
					else if(status.equals("psn")) {
						targetPokemon.status.poison = true;
					}
					else if(status.equals("brn")) {
						targetPokemon.status.burn = true;
					}
					else if(status.equals("tox")) {
						targetPokemon.status.badly_poisoned_counter = 1;
					}
					else if(status.equals("slp")) {
						targetPokemon.status.sleep_turns_left = 3; // guess how long sleep will be because we don't know upfront
					}
					// TODO: Add missing statuses (confusion, recharge?)
				}
				else if (actionCategory.equals("-curestatus")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					Pokemon targetPokemon = player ? state.p1_team.activePokemon : state.p2_active;
					
					int thirdBarIndex = lines[i].indexOf('|', secondBarIndex+1);
					int fourthBarIndex = lines[i].indexOf('|', thirdBarIndex+1);
					if(fourthBarIndex == -1)
						fourthBarIndex = lines[i].length();
					
					String status = lines[i].substring(thirdBarIndex+1, fourthBarIndex);
					
					if(status.equals("frz")) {
						targetPokemon.status.freeze = false;
					}
					else if(status.equals("par")) {
						targetPokemon.status.paralyze = false;
					}
					else if(status.equals("psn")) {
						targetPokemon.status.poison = false;
					}
					else if(status.equals("brn")) {
						targetPokemon.status.burn = false;
					}
					else if(status.equals("tox")) {
						targetPokemon.status.badly_poisoned_counter = 0;
					}
					else if(status.equals("slp")) {
						targetPokemon.status.sleep_turns_left = 0;
					}
					// TODO: Add missing statuses (confusion, recharge?)
				}
				else if (actionCategory.equals("faint")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					Pokemon targetPokemon = player ? state.p1_team.activePokemon : state.p2_active;
					targetPokemon.currHp = 0;
				}
				else if (actionCategory.equals("-boost") || actionCategory.equals("-unboost")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					Pokemon targetPokemon = player ? state.p1_team.activePokemon : state.p2_active;
					
					int thirdBarIndex = lines[i].indexOf('|', secondBarIndex+1);
					int fourthBarIndex = lines[i].indexOf('|', thirdBarIndex+1);
					
					Pokemon.Stat stat =  Pokemon.Stat.valueOf(lines[i].substring(thirdBarIndex+1, fourthBarIndex).toUpperCase());
					
					int boostAmount = Integer.parseInt(lines[i].substring(fourthBarIndex+1));
					boostAmount *= (actionCategory.equals("-boost") ? 1 : -1);
					
					targetPokemon.statMod(stat, boostAmount);
				}
			}
			i++;
		}
		
		// Determine winner
		while(lines[i].length() < 2 || lines[i].charAt(1) != 'w') {
			i++;
		}
		String winnerName = lines[i].substring(lines[i].lastIndexOf('|') + 1, lines[i].length());
		
		winner = (winnerName.equals(p1Name));
	}
	
	public static void main(String[] args) {
		Replay r = new Replay("Gen1RandomBattle-2019-11-30-scaldmanaphy-pi31.html");	
		System.out.println(r);
	}
	
}
