import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONObject; 
import org.json.simple.parser.*; 

public class Pokemon {
	public String species;
	public ArrayList<Type> types;
	public ArrayList<Move> moves;
	public ArrayList<Integer> pp; /** pp[i] is the current pp of moves[i] **/
	public int level, maxHp, atk, def, spc, spe, currHp;
	
	//TODO: status effects ex. sleep, substitute
	//TODO: stat modifications ex. atk down, def up
	
	/**
	 * Construct a new pokemon with the specified species and moves
	 * Uses Pokedex JSON to get base stats and types, and uses the Move objects
	 * to get move pp
	 * 
	 */
	public Pokemon(String species, ArrayList<Move> moves, int level) {
		this.species = species;
		this.moves = moves;
		this.pp = new ArrayList<Integer>();
		for(Move m : this.moves) {
			pp.add(m.maxPP);
		}
		this.level = level;
		
		// TODO: Load stats, moves, types from Pokedex.
		

		
	}
}
