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
	
	/**
	 * Executes a single turn of battle given that player 1 selects action [a1]
	 * and player 2 selects action [a2]. As standard in pokemon battle, 'switch'
	 * moves are processed first. If both players select to attack, then the
	 * battle pokemon with the higher speed stat will attack first. If the speed
	 * stats are equal, then there is a one-half chance of either attacking first.
	 * 
	 * Certain moves like 'quickattack' will have different speed priorities.
	 * Example: 'quickattack' has priority +1, so it will go before any move with
	 * priority less than +1.
	 */
	public void executeTurn(Action a1, Action a2, Team t1, Team t2) {
		// If any actions are switch actions, execute them first.
		if (a1.getType() == ActionType.SWITCH) {
			SwitchAction s1 = (SwitchAction)a1;
			t1.activePokemon.resetUponSwitch();
			t1.activePokemon = s1.switchTo;
			
		}
		if (a2.getType() == ActionType.SWITCH) {
			SwitchAction s2 = (SwitchAction)a2;
			t2.activePokemon.resetUponSwitch();
			t2.activePokemon = s2.switchTo;
		}
		
		/* If both moves are attacking moves, compare speeds/priorities to
		 * see who goes first. */
		if(a1.getType() == ActionType.ATTACK && a2.getType() == ActionType.ATTACK) {
			AttackAction aa1 = (AttackAction)a1;
			AttackAction aa2 = (AttackAction)a2;
			
			// Relevant statistics needed for analyzing turn-order.
			int p1 = aa1.move.priority;
			int p2 = aa2.move.priority;
			int spd1 = aa1.user.modifiedStat(Pokemon.Stat.SPE);
			int spd2 = aa2.user.modifiedStat(Pokemon.Stat.SPE);
			
			/* Player 1's pokemon can attack first if and only if:
			 * 1) It's move is higher priority, or
			 * 2) It's move is not lower priority and it wins out on speed. */
			if (p1 > p2 || (p1 == p2 && ((spd1 > spd2) || (spd1 == spd2 && Math.random() < 0.5)))) {
				aa1.move.use(aa1.user, t2.activePokemon);
				if (t2.activePokemon.isAlive()) {
					aa2.move.use(aa2.user, t1.activePokemon);
				}
			}
			/* If none of the conditions above are satisifed, then player 2
			 * must attack first. */
			else {
				aa2.move.use(aa2.user, t1.activePokemon);
				if (t1.activePokemon.isAlive()) {
					aa1.move.use(aa1.user, t2.activePokemon);
				}
			}
		}
		/* If both actions aren't attack type, then one or less were. Check the
		 * two different actions and if either is attack, the other must have been
		 * switched, so attack immediately. */
		else {
			if (a1.getType() == ActionType.ATTACK) {
				AttackAction aa1 = (AttackAction)a1;
				aa1.move.use(aa1.user, t2.activePokemon);
			}
			else if (a2.getType() == ActionType.ATTACK){
				AttackAction aa2 = (AttackAction)a2;
				aa2.move.use(aa2.user, t1.activePokemon);
			}
		}
	}
	
	/**
	 * Perform all necessary processes at the end of turns
	 * - Poison/Burn damage
	 * - reset mirror move
	 * - reset counter damage
	 */
	public void endOfTurn(Team t1, Team t2) {
		// TODO
	}
	
	public static void main(String[] args) {
		// TODO: Finish main function.		
	}
}
