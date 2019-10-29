import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONObject; 
import org.json.simple.parser.*; 

public class Pokemon {
	public String species;
	public Type[] types; // Always length 2, types[1] is NONE if single typed
	public Move[] moves;
	public int[] pp; /** pp[i] is the current pp of moves[i] **/
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
		// TODO: EVERYTHING
	}
}
