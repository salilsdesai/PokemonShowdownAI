import java.util.ArrayList;
import java.util.Arrays;

public class Simulator {
	public static class Team {
		public ArrayList<Pokemon> pokemonList;
		public Pokemon activePokemon;
		public Team(ArrayList<Pokemon> l) {
			pokemonList = l;
			activePokemon = l.get(0);
		}
		public ArrayList<Action> getActions() {
			ArrayList<Action> actions = new ArrayList<Action>();
			
			// Moves
			for(int i = 0; i < activePokemon.moves.length; i++) {
				if(activePokemon.pp[i] > 0) {
					actions.add(new AttackAction(activePokemon, activePokemon.moves[i]));
				}
			}
			
			// Switches
			for(Pokemon p : pokemonList) {
				if(p != activePokemon && p.isAlive()) {
					actions.add(new SwitchAction(p));
				}
			}
			
			return actions;
		}
		public String toString() {
			return "Active: " + activePokemon + "\n" + "Full Team: " + Arrays.toString(pokemonList.toArray());
		}
	}
	public enum ActionType {
		ATTACK, SWITCH;
	}
	public static interface Action {
		public ActionType getType();
	}
	public static class SwitchAction implements Action {
		public ActionType getType() {
			return ActionType.SWITCH;
		}
		public Pokemon switchTo;
		public SwitchAction(Pokemon switchTo) {
			this.switchTo = switchTo;
		}
		public String toString() {
			return "Switch to: " + switchTo.species;
		}
	}
	public static class AttackAction implements Action {
		public ActionType getType() {
			return ActionType.ATTACK;
		}
		public Pokemon user;
		public Move move;
		public AttackAction(Pokemon user, Move move) {
			this.user = user;
			this.move = move;
		}
		public String toString() {
			return "Attack: " + Arrays.toString(new String[] {user.species, move.name});
		}
	}
	
	
	public static void main(String[] args) {
		Team p1 = new Team(TeamGenerator.randomTeam());
		Team p2 = new Team(TeamGenerator.randomTeam());
		
		System.out.println(p1.getActions());
		
//		System.out.println(p1);
		
		// TODO
		
		
		
		
		
		
		
	}
}
