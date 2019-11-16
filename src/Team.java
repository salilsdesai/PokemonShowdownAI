import java.util.ArrayList;
import java.util.Arrays;


public class Team {
	public ArrayList<Pokemon> pokemonList;
	public Pokemon activePokemon;
	
	public Team(ArrayList<Pokemon> l) {
		pokemonList = l;
		activePokemon = l.get(0);
	}
	
	/** Default constructor with empty pokemonlist and null active pokemon. */
	public Team() {
		pokemonList = new ArrayList<>();
	}

	public ArrayList<Simulator.Action> getActions(boolean opponent_alive) {
		ArrayList<Simulator.Action> actions = new ArrayList<Simulator.Action>();
		
		// Must switch if knocked out
		if (!activePokemon.isAlive()) {
			return switchActions();
		}
		// Must wait for opponent if they are switching after being knocked out
		if (!opponent_alive) {
			actions.add(new Simulator.AttackAction(activePokemon, Move.getMove("NOTHING")));
			return actions;
		}
		
		// Recharge after hyperbeam
		if(activePokemon.status.recharge) {
			actions.add(new Simulator.AttackAction(activePokemon, Move.getMove("RECHARGE")));
			return actions;
		}
			
		// Sky attack -> forces player to use the attack once charged
		if(activePokemon.status.charge) {
			actions.add(new Simulator.AttackAction(activePokemon, Move.getMove("skyattack")));
			return actions;
		}
			
		// Bide -> forces player to use the attack once charging
		if(activePokemon.status.bide_turns_left > 0) {
			actions.add(new Simulator.AttackAction(activePokemon, Move.getMove("bide")));
			return actions;
		}
			
		// Attacks
		for(int i = 0; i < activePokemon.moves.length; i++) {
			if(activePokemon.moves[i] != null && activePokemon.pp[i] > 0) {
				actions.add(new Simulator.AttackAction(activePokemon, activePokemon.moves[i], i));
			}
		}
		if(actions.isEmpty()) {
			// No moves have any pp
			actions.add(new Simulator.AttackAction(activePokemon, Move.getMove("STRUGGLE")));
		}
		
		// Switches
		actions.addAll(switchActions());
		
		return actions;
	}
	public String toString() {
		return "Active: " + activePokemon + "\n\t" + "Full Team: " + Arrays.toString(pokemonList.toArray());
	}
	public boolean hasAlive() {
		for(Pokemon p : pokemonList)
			if(p.isAlive())
				return true;
		return false;
	}
	public ArrayList<Simulator.Action> switchActions() {
		ArrayList<Simulator.Action> actions = new ArrayList<Simulator.Action>();
		for(Pokemon p : pokemonList) {
			if(p != activePokemon && p != activePokemon.status.transformedFrom && p.isAlive()) {
				actions.add(new Simulator.SwitchAction(p));
			}
		}
		return actions;
	}
}
