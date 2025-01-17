
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;

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
	/** true if the gamestate is from the perspective of p1 (meaning p1 won),
	 *  false if from the perspective of p2 (meaning p2 won) */
	public boolean player; 
	/** the turn number of the saved state from the replay */
	public int turnNum;
	/** the action taken by p1 from the given state (i.e. the node we want the neural network to activate
	 * 	given this replay's Game State)
	 *  - [action] = -1 means that p1 did not take any action at the given game state (this replay should be tossed)
	 *	- 0 ≤ [action] ≤ 3 means p1 used the [action]'th move in their active pokemon's move list  
	 * 	- 4 ≤ [action] ≤ 8 means p1 switched to the [action - 4]'th pokemon on their team, skipping
	 * 	  over their previous action pokemon when counting
	 */
	public int action;
	
	public String toString() {
		String s = (state.toString() + "\nTurn Num: " + turnNum + "\nPlayer/Winner: " + (player ? "p1" : "p2") + "\nNext Action: ");
		if(action == -1) {
			s += "<No Action Taken>";
		}
		else if(action >= 0 && action <= 3) {
			s += "use " + state.p1_team.activePokemon.moves[action].name;
		}
		else if (action >= 4 && action <= 8) {
			int switchNum = 0;
			for(Pokemon p : state.p1_team.pokemonList) {
				if(p.species.equals(state.p1_team.activePokemon.species)) {
					switchNum--;
				}
				else if(switchNum == action-4) {
					s += ("switch to " + p.species);
				}
				switchNum++;
			}
		}
		else {
			s += "<invalid action>";
		}
		return s;
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
	 * Parse a Replay from replays/[i].html
	 */
	public Replay(int i) {
		this("replays/" + i + ".html");
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
		
		String winnerName = null;
		// Determine who the winner is (who we the gamestate should be from the perspective from)
		for(int j = i; winnerName == null; j++) {
			if(lines[j].length() >= 5 && lines[j].substring(0,5).equals("|win|")) {
				winnerName = lines[j].substring(lines[j].lastIndexOf('|') + 1, lines[j].length());
			}
		}
		player = winnerName.equals(p1Name);
		
		
		// Get all of player's pokemon and their moves eventually used
		HashMap<String, Pokemon> playerPokemonMap = new HashMap<String,Pokemon>(); // maps from Species to Pokemon
		Pokemon p1Active = null;
		for(int j = i; j < lines.length; j++) {
			int secondBarIndex = lines[j].indexOf('|', 1);
			if(lines[j].length() >= 2 && secondBarIndex != -1) {
				String actionCategory = lines[j].substring(1, secondBarIndex);
				if(actionCategory.equals("switch")) {
					ReplaySwitchAction rsa = new ReplaySwitchAction(lines[j]);
					if(rsa.player == player) {
						Pokemon p = playerPokemonMap.get(rsa.species);
						if(p == null) {
							p = new Pokemon(rsa.species, new String[4], rsa.level);
							playerPokemonMap.put(rsa.species, p);
						}
						p1Active = p;
					}
				}
				else if (actionCategory.equals("move")) {
					boolean player = (lines[j].charAt(secondBarIndex + 2) == '1');
					int thirdBarIndex = lines[j].indexOf('|', secondBarIndex+1);
					String moveName = filterNonLetters(lines[j].substring(thirdBarIndex+1, lines[j].indexOf('|', thirdBarIndex+1)).toLowerCase());
					Move move = Move.getMove(moveName);
					if(player == this.player) {
						// p1 used the move
						for(int k = 0; k < p1Active.moves.length; k++) {
							if(p1Active.moves[k] == move) {
								// If we already know p1's active has this move, do nothing
								k = p1Active.moves.length;
							}
							else if(p1Active.moves[k] == null) {
								// Since we didn't know p1's active has this move, add it to its move list
								p1Active.moves[k] = move;
								k = p1Active.moves.length;
							}
						}
					}
				}
			}
		}
		
		// Shuffle each Pokemon's move list and assign PP
		for(Pokemon p : playerPokemonMap.values()) {
			for(int j = 3; j > 0; j--) {
				int index = (int)(Math.random()*(j+1));
				Move temp = p.moves[index];
				p.moves[index] = p.moves[j];
				p.moves[j] = temp;
			}
			for(int j = 0; j < p.moves.length; j++) {
				p.pp[j] = p.moves[j] != null ? p.moves[j].maxPP : 0;
			}
		}
		
		
		// Start parsing the game state turn by turn
		
		while(!lines[i].equals("|start")) {
			i++;
		}
				
		ReplaySwitchAction playerStartAction = new ReplaySwitchAction(lines[i+(player ? 1 : 2)]);
		ReplaySwitchAction opponentStartAction = new ReplaySwitchAction(lines[i+(player ? 2 : 1)]);
		
		ArrayList<Pokemon> playerStartTeamList = new ArrayList<Pokemon>();
		playerStartTeamList.add(playerPokemonMap.get(playerStartAction.species));
		for(Pokemon p : playerPokemonMap.values()) {
			if(!p.species.equals(playerStartAction.species)) {
				// Don't add their starting pokemon because we added it first
				playerStartTeamList.add(p);
			}
		}
		
		Team playerStartTeam = new Team(playerStartTeamList);
		Pokemon playerStartPokemon = new Pokemon(opponentStartAction.species, new String[4], opponentStartAction.level);
		
		state = new GameState(playerStartTeam, playerStartPokemon);
		
		i += 3;
		
		int numTurns = numTurns(lines);
		turnNum = (int)(numTurns * Math.random() + 1);
		int currTurn = 1;
		
		/*
		 * Now we have initial game state ready, go turn by turn, 
		 * adding a new game state each time, until we reach our target turn
		 */
		
		while(i < lines.length && currTurn < turnNum) {
			
			int secondBarIndex = lines[i].indexOf('|', 1);
			
			if(lines[i].length() >= 2 && secondBarIndex != -1) {
				String actionCategory = lines[i].substring(1, secondBarIndex);
				
				if(actionCategory.equals("switch")) {
					ReplaySwitchAction rsa = new ReplaySwitchAction(lines[i]);
					if(rsa.player == player) {
						// player
						state.p1_team.activePokemon.resetUponSwitch();
						for(Pokemon p : state.p1_team.pokemonList) {
							if(p.species.equals(rsa.species)) {
								state.p1_team.activePokemon = p;
							}
						}
					}
					else {
						// opponent switched
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
					
					if(player == this.player) {
						// player used the move
						for(int k = 0; k < state.p1_team.activePokemon.moves.length; k++) {
							if(state.p1_team.activePokemon.moves[k] == move) {
								state.p1_team.activePokemon.pp[k]--;
								k = state.p1_team.activePokemon.moves.length;
							}
						}
						
					}
					else {
						// opponent used the move
						state.p2_pokemon.get(state.p2_active).add(move);
					}
				}
				else if (actionCategory.equals("win")) {
					currTurn = turnNum; // end the loop
				}
				else if (actionCategory.equals("turn")) {
					currTurn = Integer.parseInt(lines[i].substring(secondBarIndex+1));
				}
				else if (actionCategory.equals("-damage")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					int thirdBarIndex = lines[i].indexOf('|', secondBarIndex+1);
					int backslashIndex = lines[i].indexOf('\\', thirdBarIndex);
					int remainingHp = (lines[i].charAt(thirdBarIndex+1) == '0' ? 0 : Integer.parseInt(lines[i].substring(thirdBarIndex+1, backslashIndex)));
					Pokemon targetPokemon = (player == this.player) ? state.p1_team.activePokemon : state.p2_active;
					targetPokemon.currHp = remainingHp;
				}
				else if (actionCategory.equals("-status")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					Pokemon targetPokemon = (player == this.player) ? state.p1_team.activePokemon : state.p2_active;
					
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
				}
				else if (actionCategory.equals("-curestatus")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					Pokemon targetPokemon = (player == this.player) ? state.p1_team.activePokemon : state.p2_active;
					
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
				}
				else if (actionCategory.equals("faint")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					Pokemon targetPokemon = (player == this.player) ? state.p1_team.activePokemon : state.p2_active;
					targetPokemon.currHp = 0;
				}
				else if (actionCategory.equals("-boost") || actionCategory.equals("-unboost")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					Pokemon targetPokemon = (player == this.player) ? state.p1_team.activePokemon : state.p2_active;
					
					int thirdBarIndex = lines[i].indexOf('|', secondBarIndex+1);
					int fourthBarIndex = lines[i].indexOf('|', thirdBarIndex+1);
					
					String statString = lines[i].substring(thirdBarIndex+1, fourthBarIndex).toUpperCase();
					if(statString.equals("SPD") || statString.equals("SPA"))
						statString = "SPC";
					if(statString.equals("ACCURACY"))
						statString = "ACC";
					Pokemon.Stat stat =  Pokemon.Stat.valueOf(statString);
					
					int boostAmount = Integer.parseInt(lines[i].substring(fourthBarIndex+1));
					boostAmount *= (actionCategory.equals("-boost") ? 1 : -1);
					
					targetPokemon.statMod(stat, boostAmount);
				}
				else if (actionCategory.equals("-start")) {
					String whatIsStarting = lines[i].substring(lines[i].lastIndexOf('|') + 1);			
					if(whatIsStarting.equals("confusion")) {
						boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
						Pokemon targetPokemon = (player == this.player) ? state.p1_team.activePokemon : state.p2_active;
						targetPokemon.status.confuse_turns_left = 2; // we don't know how long it will last, approximate as 2		
					}
				}
				else if (actionCategory.equals("-end")) {
					String whatIsEnding = lines[i].substring(lines[i].lastIndexOf('|') + 1);			
					if(whatIsEnding.equals("confusion")) {
						boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
						Pokemon targetPokemon = (player == this.player) ? state.p1_team.activePokemon : state.p2_active;
						targetPokemon.status.confuse_turns_left = 0;
					}
				}
			}
			i++;
		}
		
		// Determine what players's next action was
		boolean setAction = false;
		while(!setAction) {
			
			int secondBarIndex = lines[i].indexOf('|', 1);
			
			if(lines[i].length() >= 2 && secondBarIndex != -1) {
				String actionCategory = lines[i].substring(1, secondBarIndex);
				
				if(actionCategory.equals("switch")) {
					ReplaySwitchAction rsa = new ReplaySwitchAction(lines[i]);
					if(rsa.player == this.player) {
						int switchNum = 0;
						for(Pokemon p : state.p1_team.pokemonList) {
							if(p.species.equals(rsa.species)) {
								action = switchNum + 4;
								setAction = true;
							}
							else if(p.species.equals(state.p1_team.activePokemon.species)) {
								switchNum--;
							}
							switchNum++;
						}
					}
				}
				else if (actionCategory.equals("move")) {
					boolean player = (lines[i].charAt(secondBarIndex + 2) == '1');
					int thirdBarIndex = lines[i].indexOf('|', secondBarIndex+1);
					String moveName = filterNonLetters(lines[i].substring(thirdBarIndex+1, lines[i].indexOf('|', thirdBarIndex+1)).toLowerCase());
					Move move = Move.getMove(moveName);
					if(player == this.player) {
						// player used the move
						for(int k = 0; k < state.p1_team.activePokemon.moves.length; k++) {
							if(state.p1_team.activePokemon.moves[k] == move) {
								action = k;
								setAction = true;
							}
						}
						
					}
				}
				else if (actionCategory.equals("win") || actionCategory.equals("turn")) {
					// The turn or match ended without p1 making an action
					action = -1;
					setAction = true;
				}
			}
			i++;
		}
	}
	
/**
	 * Download the most recent [numReplays] 
	 * Gen1RandomBattle replays from Pokemon Showdown.
	 * Save the i'th replay as /replays/[i].html
	 */
	public static void downloadLatestReplays(int numReplays) {
		try {
			WebClient webClient = new WebClient(BrowserVersion.FIREFOX_60);
			webClient.getOptions().setThrowExceptionOnScriptError(false);
			webClient.getOptions().setUseInsecureSSL(true);
			webClient.getCookieManager().setCookiesEnabled(true);
			
			HtmlPage htmlPage = webClient.getPage("https://replay.pokemonshowdown.com/search/?format=gen1randombattle");

			// Click "more results" until at least as many replays as we want are loaded on the page
			int numReplaysOnPage = 50;
			HtmlButton b = htmlPage.getElementByName("moreResults");
			while(numReplaysOnPage < numReplays) {
				b.click();
				numReplaysOnPage += 50;
				while(b.isDisabled());
			}
			
			// Get the urls of the latest replays
			List<HtmlAnchor> anchors = htmlPage.getAnchors();
			String[] urls = new String[numReplays];
			int i = -8; // Skip the first 8 anchors
			for(HtmlAnchor anchor : anchors) {
				if(i >= 0 && i < numReplays) {
					urls[i] = "https://replay.pokemonshowdown.com" + anchor.getHrefAttribute();
				}
				i++;
			}
			
			// Download the replay from each URL
			for(int j = 0; j < urls.length; j++) {
				HtmlPage replayPage = webClient.getPage(urls[j]);
				HtmlAnchor replayDownloadButton = replayPage.getAnchors().get(6);
				replayDownloadButton.click();
				
				String encoded = replayDownloadButton.getHrefAttribute().substring(23);
				String decoded = new String(Base64.getDecoder().decode(URLDecoder.decode(encoded,"UTF-8")));
			    BufferedWriter writer = new BufferedWriter(new FileWriter("replays/" + j + ".html"));
			    writer.write(decoded);
			    writer.close();
			}
			webClient.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public static void trainPolicyNetwork(int numLayers, int epochs, double stepSize, int batchSize, int numTrainingSamples) {
		List<NeuralNet.Data> data = new ArrayList<>();
		Replay[] r = new Replay[numTrainingSamples];
		for (int i = 0; i < r.length; i++) {
			int retryCount = 0; // try not to have states have no action
			do {
				r[i] = new Replay(i);
				retryCount++;
			} while(r[i].action == -1 && retryCount < 10);
			data.add(new NeuralNet.Data(r[i]));
		}
		
		NeuralNet nn = new NeuralNet(77, numLayers, 9, epochs, stepSize);
		nn.back_prop_batch_with_checkpoints(data, batchSize, "PolicyNetwork/PolicyNetworkWeights", 1000);
		nn.save_to_file("PolicyNetwork/PolicyNetworkWeights.txt");
		
		for (int i = 0; i < 5; i++) { // print out the first 5 to check
			System.out.println("nn1 Ouput: ");
			nn.forward_prop(NeuralNet.input(r[i].state));
			for (int j = 0; j < nn.nn.get(nn.LAYERS).length; j++) {
				System.out.println(nn.nn.get(nn.LAYERS)[j].value + " ");
			}
			System.out.print("Expected: ");
			System.out.println(r[i].action + "\n\n");
		}
	}
	
	/**
	 * Print the results of testing the Policy Neural Network with weights saved
	 * in [weigthsFilePath] on replays 500-999
	 */
	public static void printPolicyNetworkTestResults(String weightsFilePath) {
		NeuralNet nn = new NeuralNet(weightsFilePath);
		Replay[] r = new Replay[500];
		for(int i = 0; i < r.length; i++) {
			int retryCount = 0; // try not to have states have no action
			do {
				r[i] = new Replay(i);
				retryCount++;
			} while(r[i].action == -1 && retryCount < 10);
		}
		
		System.out.println("Five Randomly Chosen Replays:\n");
		for(int i = 0; i < 5; i++) {
			int index = (int)(Math.random()*500 + 500);
			if(r[i].action != -1) {
				nn.forward_prop(NeuralNet.input(r[i].state));
				System.out.println(index + ": ");
				System.out.println("\tNeural Network Output Layer:");
				for (int j = 0; j < nn.nn.get(nn.LAYERS).length; j++) {
					System.out.println("\t\t[" + j + "]: "+ nn.nn.get(nn.LAYERS)[j].value + " ");
				}
				System.out.println("\tExpected: " + r[i].action);
			}
			else {
				i--;
			}
		}
		
		/** 
		 * correctCount[i] is the number of times the
		 * players action was [i] and the i'th output
		 * node had the highest value in the neural network
		 */
		int[] correctCount = new int[9];
		
		/** 
		 * top3Count[i] is the number of times the
		 * players action was [i] and the i'th output
		 * node had one of the three highest values 
		 * in the neural network
		 */
		int[] top3Count = new int[9];
		
		/** 
		 * actualCount[i] is the number of times the
		 * players action was [i]
		 */
		int[] actualCount = new int[9];
		
		for(int i = 0; i < 500; i++) {
			if(r[i].action != -1) {
				nn.forward_prop(NeuralNet.input(r[i].state));
				// actions are stored as {action number, action value}
				// lower value actions have higher priority
				PriorityQueue<double[]> pq = new PriorityQueue<double[]>(
					4, 
					new Comparator<double[]>() {
						@Override
						public int compare(double[] d1, double[] d2) {
							return Double.compare(d1[1], d2[1]);
						}
					}
				);
				for (int j = 0; j < nn.nn.get(nn.LAYERS).length; j++) {
					pq.add(new double[] {j, nn.nn.get(nn.LAYERS)[j].value});
					while(pq.size() > 3) {
						pq.poll();
					}
				}
				
				actualCount[r[i].action]++;
				
				for(int j = 0; j < 3; j++) {
					double[] d = pq.poll();
					if(d[0] == r[i].action) {
						top3Count[r[i].action]++;
						if(j == 2) { // this action had the highest value
							correctCount[r[i].action]++;
						}
					}
				}
			}
		}
		
		int totalCorrect = 0;
		int totalTop3 = 0;
		int totalActual = 0;
		for(int i = 0; i < 9; i++) {
			totalCorrect += correctCount[i];
			totalTop3 += top3Count[i];
			totalActual += actualCount[i];
		}
		
		
		System.out.println("Correctness of Each Action");
		for(int i = 0; i < 9; i++) {
			System.out.println("\tAction " + i + ":");
			System.out.println("\t\tCorrect: " + correctCount[i] + "/" + actualCount[i]);
			System.out.println("\t\tIn top 3: " + top3Count[i] + "/" + actualCount[i]);
		}
		
		System.out.println("Overall Correctness");
		System.out.println("\tCorrect: " + totalCorrect + "/" + totalActual);
		System.out.println("\tIn top 3: " + totalTop3 + "/" + totalActual);
		
	}
	
	public static void main(String[] args) {
		System.out.println("Starting neural net training.");
		trainPolicyNetwork(2, 30000, 0.2, 5, 500); // train only the first 500 samples
	}
}
