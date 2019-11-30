import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
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
			species = line.substring(speciesIndex, commaIndex).toLowerCase();
			level = Integer.parseInt(line.substring(commaIndex+3, endOfLevelIndex));
		}
		public String toString() {
			return "Player " + (player ? '1' : '2') + " switch in " + species + " (" + level + ")";
		}
	}
	
	public GameState state;
	/** true if p1 won, false if p2 won */
	public boolean winner; 
	
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
		int targetTurnNum = (int)(Math.random() * numTurns + 1);
		int currTurn = 1;
		
		/*
		 * Now we have initial game state ready, go turn by turn, 
		 * adding a new game state each time, until we reach our target turn
		 */
		
		while(i < lines[i].length() && currTurn < targetTurnNum) {
			int secondBarIndex = lines[i].indexOf('|', 1);
			if(lines[i].length() >= 2 && secondBarIndex != -1) {
				String actionCategory = lines[i].substring(1, secondBarIndex);
				if(actionCategory.equals("switch")) {
					// TODO
				}
				else if (actionCategory.equals("move")) {
					// TODO
				}
				else if (actionCategory.equals("faint")) {
					// TODO
				}
				else if (actionCategory.equals("win")) {
					// TODO
				}
				else if (actionCategory.equals("turn")) {
					// TODO
				}
				else if (actionCategory.equals("-damage")) {
					// TODO
				}
				else if (actionCategory.equals("-status")) {
					// TODO
				}
				else if (actionCategory.equals("-faint")) {
					// TODO
				}
				else if (actionCategory.equals("-unboost")) {
					// TODO
				}
			}
			i++;
		}
		
		// Determine winner
		while(lines[i].length() < 2 || lines[i].charAt(1) != 'w') {
			i++;
		}
		String winnerName = lines[i].substring(lines[i].lastIndexOf('|') + 1, lines[i].length());
		
		System.out.println(winnerName);
		System.out.println(p1Name);
		
		winner = (winnerName.equals(p1Name));
	}
	
	public static void main(String[] args) {
		Replay r = new Replay("Gen1RandomBattle-2019-11-30-sloworno-47olg10.html");	
	}
	
}
