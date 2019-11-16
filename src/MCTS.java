import java.util.ArrayList;
import java.util.List;

public class MCTS {
	
	public static int SimulationTimeLimitSeconds = 10;
	
	/**
	 * This class contains data for a particular action of a 
	 * node of the MCTS tree
	 * 
	 * We need to store X and n for each action because Decoupled UCT
	 * stores X and n that way instead of in each node
	 */
	public static class ActionData {
		/**
		 * The parameter of the select function of SMMCTS that controls 
		 * how much weight to place on exploration 
		 * See page 3, second column, first paragraph in
		 * http://mlanctot.info/files/papers/cig14-smmctsggp.pdf
		 */
		public static final double C = 1;
		
		public Simulator.Action action;
		public double X;
		public int n;
		public ActionData(Simulator.Action a) {
			action = a;
			X = 0;
			n = 0;
		}
		public void update(double u1) {
			X += u1;
			n +=1 ;
		}
		/**
		 * Return the reward used to compare actions to each other
		 * to determine which one to select after simulations are over
		 */
		public double estimatedReward() {
			return (n != 0 ? X/n : Double.MIN_VALUE);
		}

		/**
		 * Calculated using the formula at the bottom left of page 3 of
		 * http://mlanctot.info/files/papers/cig14-smmctsggp.pdf
		 */
		public double UCB(int ns) {
			return X/n + C*Math.sqrt(Math.log(ns)/n);
		}
		
		
		/**
		 * Return the index of the action in [actions] with highest UCB value
		 * 
		 * This function is used as a helper function for select()
		 */
		public static int bestAction(ActionData[] actions) {
			int ns = 0;
			for(int i = 0; i < actions.length; i++)
				ns += actions[i].n;
			
			int bestAction = 0;
			double bestUCB = actions[0].UCB(ns);
			
			for(int i = 1; i < actions.length; i++) {
				double actionIUCB = actions[i].UCB(ns);
				if(actionIUCB > bestUCB) {
					bestUCB = actionIUCB;
					bestAction = i;
				}
			}
			
			return bestAction;
		}
	}
	
	public static class TreeNode {
		public ActionData[] playerActions;
		public ActionData[] opponentActions;
		
		public GameState currentState;
		/**
		 * MatrixEntry[i][j] is the entry associated with
		 * playerActions[i] and opponentActions[j]
		 */
		public TreeNode[][] SuccessorNodes;
		
		/**
		 * If there is a previously unselected action pair using playerActions[i] and opponentActions[j]
		 * for some i and j, [nextUnselectedActions] is {i, j}
		 * otherwise it is null
		 */
		private int[] nextUnselectedActions;
		
		/**
		 * if there is a previously unselected action for this node using playerActions[i] and opponentActions[j]
		 * - Initializes entries[i][j] to contain whatever the successor node for playerActions[i] and opponentActions[j] should be
		 * - Updates nextUnselectedActions class variable
		 * - returns {i, j}
		 * Otherwise, returns null
		 */
		public int[] getAndIncrementNextUnselectedActions() {
			if (nextUnselectedActions == null)
				return null;
			
			int i = nextUnselectedActions[0];
			int j = nextUnselectedActions[1];
			
			nextUnselectedActions[1]++;
			if(nextUnselectedActions[1] >= opponentActions.length) {
				nextUnselectedActions[1] = 0;
				nextUnselectedActions[0]++;
				if(nextUnselectedActions[0] >= playerActions.length) {
					nextUnselectedActions = null;
				}
			}
			
			GameState newGS = currentState.simulateTurn(playerActions[i].action, opponentActions[j].action);
			System.out.println("sim done");
			SuccessorNodes[i][j] = new TreeNode(newGS);
			
			return new int[] {i, j};
			
		}
		
		public TreeNode(GameState gs) {
			ArrayList<Simulator.Action> p1Actions = gs.p1_team.getActions(gs.getOpponentTeam().activePokemon.isAlive());
			ArrayList<Simulator.Action> oppoActions = gs.getOpponentTeam().getActions(gs.p1_team.activePokemon.isAlive());
			
			playerActions = new ActionData[p1Actions.size()];
			for(int i = 0; i < playerActions.length; i++)
				playerActions[i] = new ActionData(p1Actions.get(i));
			
			opponentActions = new ActionData[oppoActions.size()];
			for(int i = 0; i < opponentActions.length; i++)
				opponentActions[i] = new ActionData(oppoActions.get(i));
			
			SuccessorNodes = new TreeNode[playerActions.length][opponentActions.length];
			currentState = gs;
			nextUnselectedActions = (playerActions.length > 0 && opponentActions.length > 0 ? new int[] {0,0} : null);
		}
		/**
		 * Should be called after we are done exploring tree
		 * Choose whichever player actions has highest average 
		 */
		public Simulator.Action getBestAction() {
			int bestAction = 0;
			double bestActionReward = playerActions[0].estimatedReward();
			for(int i = 1; i < playerActions.length; i++) {
				double actionIReward = playerActions[i].estimatedReward();
				if(actionIReward > bestActionReward) {
					bestAction = i;
					bestActionReward = actionIReward;
				}
			}
			
			return playerActions[bestAction].action;
		}
		
		/**
		 * Return the payout of the terimal node obtained by simulating from this node
		 * to the end of the game (as used in SMMCTS alg pseudocode)
		 */
		public double playout() {
			if(currentState.isTerminal())
				return currentState.evalTerminalNode();
			
			Simulator.Action playerAction = playerActions[(int)(Math.random() * playerActions.length)].action;
			
			Simulator.Action opponentAction = opponentActions[(int)(Math.random() * opponentActions.length)].action;
			
			GameState nextGS = currentState.simulateTurn(playerAction, opponentAction);
			TreeNode nextTreeNode = new TreeNode(nextGS);
			
			return nextTreeNode.playout();
		}
		
		
		/**
		 * playerActions[i] and opponentActions[j] where
		 * the actions that simulation was performed on which we
		 * need to update the results from (as used in SMMCTS alg pseudocode)
		 */
		public void update(int i, int j, double u1) {
			playerActions[i].update(u1);
			opponentActions[j].update(-1*u1);
		}
		
		/**
		 * Returns {i, j} where (playerActions[i], opponentActions[j])
		 * is the pair of actions we want to select (as used in SMMCTS alg pseudocode)
		 * 
		 * Returns null if either player actions or opponent actions is empty
		 */
		public int[] select() {
			
			if(playerActions.length == 0 || opponentActions.length == 0)
				return null;
			
			// TODO: Replace this with input from neural networks
			
			return new int[] {MCTS.ActionData.bestAction(playerActions), MCTS.ActionData.bestAction(opponentActions)};
			
		}
		
		
		public double SMMCTS() {
			if(currentState.isTerminal())
				return currentState.evalTerminalNode();
			
			int i, j;
			double u1;
			
			// s ∈ T and ExpansionRequired(s) iff s has unexplored actions
			int[] unexploredActions = getAndIncrementNextUnselectedActions();
			if(unexploredActions != null) {
				i = unexploredActions[0];
				j = unexploredActions[1];
				TreeNode sPrime = SuccessorNodes[i][j];
				u1 = sPrime.playout();
				/*
				 * We are using Decoupled UCT, so X and n values are stored in matrix
				 * entries rather than in nodes like the pseudocode suggests
				 */
			}
			else {
				int[] selectedActions = select();
				i = selectedActions[0];
				j = selectedActions[1];
				TreeNode sPrime = SuccessorNodes[i][j];
				u1 = sPrime.SMMCTS();
			}
			
			update(i, j, u1);
			return u1;
		}
		
	}
	public static Simulator.Action chooseMove(GameState gs) {
		TreeNode root = new TreeNode(gs);
		
		long startTime = System.currentTimeMillis();
		
//		 Run simulations until [SimulationTimeLimitSeconds] seconds have elapsed
		while(System.currentTimeMillis() - startTime < (long)(SimulationTimeLimitSeconds*1000)) {
			root.SMMCTS();
		}
		
		return root.getBestAction();
	}

	public static void main(String[] args) {
		Pokemon bulb = new Pokemon("bulbasaur", new String[] {"surf", "thunderbolt", "quickattack", "twineedle"}, 100);
		Pokemon pid = new Pokemon("pidgey", new String[] {"thunderbolt", "psychic", "flamethrower", "earthquake"}, 90);

		ArrayList<Pokemon> p1b = new ArrayList<Pokemon>();
		p1b.add(bulb);
		
		ArrayList<Pokemon> p2b = new ArrayList<Pokemon>();
		p2b.add(pid);
		
		Team t1 = new Team(p1b);
		Team t2 = new Team(p2b);
		
		GameState p1GS = new GameState(t1, t2.activePokemon);
		
		Simulator.Action p1Action = MCTS.chooseMove(p1GS);
		ArrayList<Simulator.Action> p2ActionList = t2.getActions(true);
		Simulator.Action p2Action = p2ActionList.get(0);
		
		System.out.println(p1Action);
		System.out.println(p2Action);
		
		
	}
	
}
