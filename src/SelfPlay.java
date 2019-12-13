import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class SelfPlay {
	
	/** 
	 * List of every gamestate from player one's perspective associated
	 * with a particular replay. Each gamestate is stored as its 
	 * neural net input.
	 */
	private static List<List<Double>> replay;
	
	/** 
	 * Runs a simulation between p1 and player p2. Returns 1 if p1
	 * is the winner and 0 otherwise. 
	 */
	/* Note that it is possible that the game is 'tied' based on 
	 * the simulation in which case p2 is declared the winner 
	 * irrationally. However, such a case is rare so it is ignored.
	 */
	public static int run(NeuralNet p1, NeuralNet p2) {
		// reset replay to begin a new game
		replay = new ArrayList<>();
		
		// t1 is player one's team, t2 is player two's team
		Team t1 = new Team(TeamGenerator.randomTeam());
		Team t2 = new Team(TeamGenerator.randomTeam());
		// gs1 is the gamestate from player one's perspective, gs2 from player twos' persepctive
		GameState gs1 = new GameState(t1, t2.activePokemon);
		GameState gs2 = new GameState(t2, t1.activePokemon);
		
		
		while(t1.hasAlive() && t2.hasAlive()) {
			replay.add(NeuralNet.input(gs1));
			
			// Pick a move based for each player
			Simulator.Action p1Action = MCTS.chooseMove(gs1, p1, null);
			Simulator.Action p2Action = MCTS.chooseMove(gs2, p2, null);
			
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
			
			// apply end of turn mechanics (poison, burn etc.) and move on to the next turn
			Simulator.endOfTurn(t1, t2);
		}
		
		return t1.hasAlive() ? 1 : 0;
	}
	
	/**
	 * Writes the data stored in saved to file s.
	 */
	private static void writeTo(String s, Map<List<Double>, Integer> saved) {
		// write data to a file
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(s));
			PrintWriter pw = new PrintWriter(br);
			
			for (List<Double> x : saved.keySet()) {
				for (Double i : x) {
					pw.print(i + " ");
				}
				pw.print("\n" + saved.get(x) + "\n");
			}
			
			pw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Randomly selects a gamestate from replay Requires that replay
	 * is a valid replay populated by calling run.
	 */
	private static List<Double> pick() {
		return replay.get((int)(replay.size() * Math.random()));
	}
	
	/**
	 * Simulates [size] number of games and from each randomly selects
	 * one representative gamestate to be used for reinforcement learning.
	 */
	private static Map<List<Double>, Integer> collectData(int size) {
		NeuralNet policyNetwork = new NeuralNet("PolicyNetwork/PolicyNetworkWeights.txt");
		
		// gather data from self-play
		Map<List<Double>, Integer> saved = new HashMap<>();
		for (int i = 0; i < 1000; i++) {
			try {
				int tmp = run(policyNetwork, policyNetwork);
				saved.put(pick(), tmp);
			} catch (Exception e) {
				System.out.println("Error caught.");
				continue;
			}
		}
		
		return saved;
	}
	
	public static void main(String[] args) {
		writeTo("TrainingData/SelfPlayData.txt", collectData(5));
	}
}
