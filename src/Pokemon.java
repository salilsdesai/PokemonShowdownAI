import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
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
	public Pokemon(String species, Move[] moves, int level) {
		this.species = species;
		this.level = level;
		this.moves = moves;
		
		Pokedex.PokedexEntry entry = Pokedex.getDex().get(species);
		this.types = entry.types;
		
		this.pp = new int[moves.length];
		for(int i = 0; i < moves.length; i++) {
			pp[i] = moves[i].maxPP;
		}
		
		this.maxHp = ((((entry.baseStats[0] + 30) * 2 + (int)(Math.ceil(Math.sqrt(255))/4)) * level)/100) + level + 10;
		this.currHp = this.maxHp;
		
		Function<Integer, Integer> computeStat = (stat -> ((((entry.baseStats[0] + 30) * 2 + (int)(Math.ceil(Math.sqrt(255))/4)) * level)/100) + 5);
		this.atk = computeStat.apply(entry.baseStats[1]);
		this.def = computeStat.apply(entry.baseStats[2]);
		this.spc = computeStat.apply(entry.baseStats[3]);
		this.spe = computeStat.apply(entry.baseStats[4]);
		
		this.level = level;
	}
}
