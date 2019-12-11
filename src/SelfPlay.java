
public class SelfPlay {
	/** 
	 * Runs a simulation between p1 and player p2. Returns 1 if p1
	 * is the winner and 2 otherwise. 
	 */
	/* Note that it is possible that the game is 'tied' based on 
	 * the simulation in which case p2 is declared the winner 
	 * irrationally. However, such a case is rare so it is ignored.
	 */
	public static int run(NeuralNet p1, NeuralNet p2) {
		// t1 is player one's team, t2 is player two's team
		Team t1 = new Team(TeamGenerator.randomTeam());
		Team t2 = new Team(TeamGenerator.randomTeam());
		// gs1 is the gamestate from player one's perspective, gs2 from player twos' persepctive
		GameState gs1 = new GameState(t1, t2.activePokemon);
		GameState gs2 = new GameState(t2, t1.activePokemon);
		
		// counter to maintain turn number
		int turn = 1;
		
		while(t1.hasAlive() && t2.hasAlive()) {
			t1.print();
			t2.print();
			
			// Pick a move based for each player
			Simulator.Action p1Action = MCTS.chooseMove(gs1);
			Simulator.Action p2Action = MCTS.chooseMove(gs2);
			
			Simulator.message = null;
			Simulator.addMessage("Turn #" + turn);
			
			// Execute the action
			Simulator.executeTurn(p1Action, p2Action, t1, t2);
			
			// Update the game states for each player
			if(p1Action.getType().equals(Simulator.ActionType.SWITCH)) {
				gs2.update(t1.activePokemon, null);
			}
			else if(p1Action.getType().equals(Simulator.ActionType.ATTACK)) {
				Move m = ((Simulator.AttackAction)p1Action).move;
				if(!Move.isFiller(m)) {
					gs2.update(null, m);
				}
			}
			
			if (p2Action.getType().equals(Simulator.ActionType.SWITCH)) {
				gs1.update(t2.activePokemon, null);
			}
			else if (p2Action.getType().equals(Simulator.ActionType.ATTACK)) {
				Move m = ((Simulator.AttackAction)p2Action).move;
				if (!Move.isFiller(m)) {
					gs1.update(null, m);
				}
			}

			// Print and clear the current turn's message
			System.out.println(Simulator.message);
			Simulator.message = null;
			
			// apply end of turn mechanics (poison, burn etc.) and move on to the next turn
			Simulator.endOfTurn(t1, t2);
			turn++;
		}
		
		return t1.hasAlive() ? 1 : 2;
	}
	
	public static void main(String[] args) {
		System.out.println(run(null, null));
	}
}
