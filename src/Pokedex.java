import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Pokedex {
	public static class PokedexEntry {
		public String species;
		public List<Type> types;
		public int[] baseStats; // [hp, atk, def, spc, spe]
		public String[] randomBattleMoves;
		public String essentialMove;
		public String[] exclusiveMoves;
		public String[] comboMoves;
		public String tier;
	}
	
	private static Map<String, PokedexEntry> dex;
	
	private static void loadPokedex() {
		// Load Types, Base Stats, 
		JSONObject jo;
		try {
			jo = (JSONObject)(new JSONParser().parse(new FileReader("pokedex.json")));
		}
		catch (IOException | ParseException e) {
			// Realistically this catch block will never be reached
			jo = null;
		}
		
		JSONObject poke = (JSONObject)(jo.get(this.species.toLowerCase()));
		
		@SuppressWarnings("unchecked")
		List<String> stringTypes = (List<String>)(poke.get("types"));
		this.types = new ArrayList<Type>(stringTypes.stream().map(t -> Type.valueOf(t.toUpperCase())).collect(Collectors.toList()));
	
		// TODO: Compute stats based on base stats
	}
	
	public static PokedexEntry getEntry(String pokemonName) {
		if(dex == null) {
			loadPokedex();
		}
		return dex.get(pokemonName);
	}
	
}
