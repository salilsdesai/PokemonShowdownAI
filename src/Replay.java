import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * A class that stores a list of gamestates and the winner
 * of a particular Pokemon Showdown Gen 1 Random Battle Replay
 */
public class Replay {
	
	public ArrayList<GameState> states;
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
	 * Parse a Replay from the provided file
	 * Precondition: [filename] must be the path of a valid
	 * gen 1 random battle replay file
	 */
	public Replay(String filename) {
		String[] lines = readFile(filename);
		
		int i = 0;
		while(i < lines.length && !lines[i].equals("|start")) {
			i++;
		}
		
		// TODO: Actually convert to gamestate
		
		
	}
	
	public static void main(String[] args) {
		Replay r = new Replay("Gen1RandomBattle-2019-11-30-sloworno-47olg10.html");
	}
	
}
