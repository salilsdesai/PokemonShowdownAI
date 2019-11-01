import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

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

			// Recharge after hyperbeam
			if(activePokemon.status.recharge) {
				actions.add(new AttackAction(activePokemon, Move.getMove("RECHARGE")));
				return actions;
			}
			
			// Sky attack -> forces player to use the attack once charged
			if(activePokemon.status.charge) {
				actions.add(new AttackAction(activePokemon, Move.getMove("skyattack")));
				return actions;
			}
			
			// Bide -> forces player to use the attack once charging
			if(activePokemon.status.bide_turns_left > 0) {
				actions.add(new AttackAction(activePokemon, Move.getMove("bide")));
				return actions;
			}
			
			// Attacks
			for(int i = 0; i < activePokemon.moves.length; i++) {
				if(activePokemon.pp[i] > 0) {
					actions.add(new AttackAction(activePokemon, activePokemon.moves[i], i));
				}
			}
			if(actions.isEmpty()) {
				// No moves have any pp
				actions.add(new AttackAction(activePokemon, Move.getMove("STRUGGLE")));
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
		public ArrayList<Action> switchActions() {
			ArrayList<Action> actions = new ArrayList<Action>();
			for(Pokemon p : pokemonList) {
				if(p != activePokemon && p.isAlive()) {
					actions.add(new SwitchAction(p));
				}
			}
			return actions;
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
		/**
		 * The index in user's move list to deduct pp from, or -1
		 * if no pp should be deducted
		 */
		public int deductPPIndex;
		public AttackAction(Pokemon user, Move move) {
			this(user, move, -1);
		}
		public AttackAction(Pokemon user, Move move, int i) {
			this.user = user;
			this.move = move;
			this.deductPPIndex = i;
		}
		public String toString() {
			return "Attack: " + Arrays.toString(new String[] {user.species, move.name});
		}
	}
	
	/**
	 * A message indicating what has happened over the past turn
	 */
	public static String message;
	public static void addMessage(String s) {
		if(message == null)
			message = s;
		else
			message += "\n" + s;
	}
	
	public static Scanner input;
	
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
	public static void executeTurn(Action a1, Action a2, Team t1, Team t2) {
		// If any actions are switch actions, execute them first.
		if (a1.getType() == ActionType.SWITCH) {
			SwitchAction s1 = (SwitchAction)a1;
			addMessage(t1.activePokemon.species + " was switched with " + s1.switchTo.species);
			t1.activePokemon.resetUponSwitch();
			t1.activePokemon = s1.switchTo;
			
		}
		if (a2.getType() == ActionType.SWITCH) {
			SwitchAction s2 = (SwitchAction)a2;
			addMessage(t2.activePokemon.species + " was switched with " + s2.switchTo.species);
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
				if(aa1.deductPPIndex != -1) {
					aa1.user.pp[aa1.deductPPIndex]--;
				}
				if (t2.activePokemon.isAlive()) {
					aa2.move.use(aa2.user, t1.activePokemon);
					if(aa2.deductPPIndex != -1) {
						aa2.user.pp[aa2.deductPPIndex]--;
					}
				}
			}
			/* If none of the conditions above are satisifed, then player 2
			 * must attack first. */
			else {
				aa2.move.use(aa2.user, t1.activePokemon);
				if(aa2.deductPPIndex != -1) {
					aa2.user.pp[aa2.deductPPIndex]--;
				}
				if (t1.activePokemon.isAlive()) {
					aa1.move.use(aa1.user, t2.activePokemon);
					if(aa1.deductPPIndex != -1) {
						aa1.user.pp[aa1.deductPPIndex]--;
					}
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
				if(aa1.deductPPIndex != -1) {
					aa1.user.pp[aa1.deductPPIndex]--;
				}
			}
			else if (a2.getType() == ActionType.ATTACK){
				AttackAction aa2 = (AttackAction)a2;
				aa2.move.use(aa2.user, t1.activePokemon);
				if(aa2.deductPPIndex != -1) {
					aa2.user.pp[aa2.deductPPIndex]--;
				}
			}
		}
	}
	
	/**
	 * Perform all necessary processes at the end of turns
	 * - Poison/Burn damage
	 * - reset mirror move
	 * - reset counter damage
	 */
	public static void endOfTurn(Team t1, Team t2) {

		// TODO: Set mirror move
		
		// Apply poison/burn damage, reset counter damage
		for(Pokemon p : new Pokemon[] {t1.activePokemon, t2.activePokemon}) {
			if(p.isAlive()) {
				if(p.status.burn || p.status.poison) {
					p.currHp -= p.maxHp/16;
					Simulator.addMessage(p.species + " was hurt by " + (p.status.burn ? "burn" : "poison") + "(" + (p.maxHp/16) + ", " + p.currHp + "/" + p.maxHp+ ")");
				}
				if(p.status.badly_poisoned_counter > 0) {
					p.currHp -= p.maxHp*p.status.badly_poisoned_counter/16;
					p.status.badly_poisoned_counter++;
					Simulator.addMessage(p.species + " was hurt by badly poison (" + (p.maxHp*p.status.badly_poisoned_counter/16) + ", " + p.currHp + "/" + p.maxHp+ ")");
				}
				p.status.counter_damage = 0;
			}
		}
		
		// TODO: Make players switch in new pokemon if current ones are fainted
		for(Team t : new Team[] {t1, t2}) {
			if(!t.activePokemon.isAlive()) {
				System.out.println(t.activePokemon.species + " fainted ");
				if(t.hasAlive()) {
					ArrayList<Action> a = t.switchActions();
					System.out.println("Choose a switch in");
					for(int i = 0; i < a.size(); i++) {
						System.out.println("" + i + ": " + a.get(i));
					}
					int choice = input.nextInt();
					SwitchAction sa = (SwitchAction)(a.get(choice));
					t.activePokemon = sa.switchTo;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		input = new Scanner(System.in);
		
		Team t1 = new Team(TeamGenerator.randomTeam());
		Team t2 = new Team(TeamGenerator.randomTeam());
		
		int turn = 1;
		
		while(t1.hasAlive() && t2.hasAlive()) {
			System.out.println(t1);
			System.out.println(t2);
			
			ArrayList<ArrayList<Action>> bothPlayerActions = new ArrayList<ArrayList<Action>>();
			bothPlayerActions.add(t1.getActions());
			bothPlayerActions.add(t2.getActions());
			
			Action[] chosenActions = new Action[2];
			for(int i = 0; i < 2; i++) {
				System.out.println("t" + (i+1) + ", choose an action");
				for(int j = 0; j < bothPlayerActions.get(i).size(); j++) {
					System.out.println(j + ": " + bothPlayerActions.get(i).get(j));
				}
				int selection = input.nextInt();
				chosenActions[i] = bothPlayerActions.get(i).get(selection);
			}
			
			System.out.println("Turn #" + turn);
			executeTurn(chosenActions[0], chosenActions[1], t1, t2);
			
			// Print and clear the current turn's message
			System.out.println(Simulator.message);
			Simulator.message = null;
			
			endOfTurn(t1, t2);
			turn++;
		}
	}
}
