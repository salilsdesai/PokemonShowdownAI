import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Pokedex {
	public static class PokedexEntry {
		public String species;
		public Type[] types;
		public int[] baseStats; // [hp, atk, def, spc, spe]
		public String essentialMove;
		public String[] randomBattleMoves;
		public String[] exclusiveMoves;
		public String[] comboMoves;
		public String tier;
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(species);
			sb.append("\n\t" + Arrays.toString(types));
			sb.append("\n\t" + Arrays.toString(baseStats));
			sb.append("\n\t" + essentialMove);
			sb.append("\n\t" + Arrays.toString(randomBattleMoves));
			sb.append("\n\t" + Arrays.toString(exclusiveMoves));
			sb.append("\n\t" + Arrays.toString(comboMoves));
			sb.append("\n\t" + tier);
			return new String(sb);
		}
	}
	
	private static Map<String, PokedexEntry> dex;
	
	@SuppressWarnings("unchecked")
	private static void loadPokedex() {
		
		dex = new HashMap<String, PokedexEntry>();
		
		JSONArray jA;
		try {
			jA = (JSONArray)(new JSONParser().parse(new FileReader("pokedex.json")));
		}
		catch (IOException | ParseException e) {
			// Realistically this catch block will never be reached
			return;
		}
		
		List<JSONObject> jPokemon = jA;
		for(JSONObject p : jPokemon) {
			PokedexEntry entry = new PokedexEntry();
			
			entry.species = (String)(p.get("species"));
			entry.tier = ((String)(p.get("tier"))).toLowerCase();
			
			Object em = p.get("essentialMove");
			entry.essentialMove = em != null ? (String)(em) : null;
			
			List<String> rbm = (List<String>)(p.get("randomBattleMoves"));
			entry.randomBattleMoves = rbm.toArray(new String[rbm.size()]);
			
			List<String> xm = (List<String>)(p.get("exclusiveMoves"));
			entry.exclusiveMoves = xm != null ? xm.toArray(new String[xm.size()]) : new String[0];
			
			List<String> cm = (List<String>)(p.get("comboMoves"));
			entry.comboMoves = cm != null ? cm.toArray(new String[cm.size()]) : new String[0];
			
			List<String> stringTypes = (List<String>)(p.get("types"));
			entry.types = new Type[2];
			entry.types[0] = Type.valueOf(stringTypes.get(0).toUpperCase());
			entry.types[1] = stringTypes.size() > 1 ? Type.valueOf(stringTypes.get(1).toUpperCase()) : Type.NONE;
			
			JSONObject stats = (JSONObject)(p.get("baseStats"));
			entry.baseStats = new int[5];
			entry.baseStats[0] = (int)(long)(stats.get("hp"));
			entry.baseStats[1] = (int)(long)(stats.get("atk"));
			entry.baseStats[2] = (int)(long)(stats.get("def"));
			entry.baseStats[3] = (int)(long)(stats.get("spa"));
			entry.baseStats[4] = (int)(long)(stats.get("spe"));
			
			dex.put(entry.species, entry);
		}
	}
	
	public static Map<String, PokedexEntry> getDex() {
		if(dex == null) {
			loadPokedex();
		}
		return dex;
	}
}
