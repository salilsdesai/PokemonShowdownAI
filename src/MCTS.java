import java.util.ArrayList;
import java.util.HashMap;

public class MCTS {
	
	public static final int SIMULATION_TIME_LIMIT_SECONDS = 3;
	/** 
	 * The mixing parameter for combining result of valuation neural network
	 * and result from random rollout (see page 3 of AlphaGo paper) 
	 * 
	 * Alpha go paper said that lambda = 0.5 worked best for them
	 */
	public static final double LAMBDA = 0.5;
	
	/**
	 * The amount to scale P/(1+N) by to get u since
	 * u is proportional to P/(1+N) (see page 3 of AlphaGo paper)
	 */
	public static final double U_SCALE = 1.0;
	
	/**
	 * Max number of turns during playout after which just
	 * return the value predicted by the valuation neural network
	 * as the result of the playout
	 */
	public static final int PLAYOUT_MAX_DEPTH = 300;
	
	
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
		 * or
		 * 
		 * 
		 * C should be optimally an upperbound on the variance
		 * of the rewards. We thus choose C = 1/4 which is an upperbound
		 * on the variance of a bernoulli random variable (as explained in below paper)
		 * https://pdfs.semanticscholar.org/fb61/d223fd6c17be5837997a435e2ec22f8212b0.pdf
		 */
		public static final double C = 0.25;
		
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
		 * The value of the state of this node, as predicted by the valuation
		 * neural network
		 */
		private double v_theta;
		
		/** The i'th entry is the probability of playerActions[i] based on the policy network */
		private double[] actionProbabilityDistribution;
		
		/**
		 * This network should be compatible with the input of NeuralNet.input(Gamestate) and
		 * should have 9 output nodes with values corresponding to weights on choosing certain actions
		 * The weights on output nodes 0-3 should correspond to weight on using attacks 0-3, and 
		 * the weights on output nodes 4-8 should correspond to weight on switching to pokemon 0-4 respectively
		 * 
		 * The same network can be used by multiple tree nodes
		 * (which will each just override network node values when they
		 * call forward prop)
		 */
		private NeuralNet policyNetwork;
		
		/**
		 * This network should be compatible with the input of NeuralNet.input(Gamestate) and
		 * should have 1 output node with values corresponding to how likely it is that we win
		 * from the current gamestate
		 * 
		 * The same network can be used by multiple tree nodes
		 * (which will each just override network node values when they
		 * call forward prop)
		 */
		private NeuralNet valuationNetwork;
		
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
			SuccessorNodes[i][j] = new TreeNode(newGS, policyNetwork, valuationNetwork);
			
			return new int[] {i, j};
			
		}
		
		/**
		 * Use weights on outputs of policy neural network
		 * to set actionProbabilityDistribution for the player.
		 * This function handles the scaling so that all probabilities sum to 1
		 * If none of the actions have probability assigned by the network,
		 * assign each action probability of 1/# of actions
		 */
		public void assignActionProbabilityDistribution() {
			
			policyNetwork.forward_prop(NeuralNet.input(currentState));
			double[] policyNetworkProbabilityDistribution = new double[policyNetwork.nn.get(policyNetwork.LAYERS).length];
			
			// Initially set the weight on each action to be the max of the output from
			// the neural network and 0.
			for(int i = 0; i < policyNetworkProbabilityDistribution.length; i++) {
				policyNetworkProbabilityDistribution[i] = Math.max(policyNetwork.nn.get(policyNetwork.LAYERS)[i].value, 0);
			}
			
			actionProbabilityDistribution = new double[playerActions.length];
			
			/** Maps from a Move for index in playerActions of the action using that move */
			HashMap<Move, Integer> attackMap = new HashMap<Move, Integer>();
			
			/** Maps from a Pokemon for index in playerActions of the action switching to that Pokemon */
			HashMap<Pokemon, Integer> switchMap = new HashMap<Pokemon, Integer>();
			
			for(int i = 0; i < playerActions.length; i++) {
				if(playerActions[i].action.getType().equals(Simulator.ActionType.ATTACK)) {
					Simulator.AttackAction aa = (Simulator.AttackAction)(playerActions[i].action);
					attackMap.put(aa.move, i);
				}
				else if (playerActions[i].action.getType().equals(Simulator.ActionType.SWITCH)) {
					Simulator.SwitchAction sa = (Simulator.SwitchAction)(playerActions[i].action);
					switchMap.put(sa.switchTo, i);
				}
			}
			
			if(currentState.p1_team.activePokemon.isAlive()) {
				// Attack options
				for(int i = 0; i <= 3; i++) {
					if(currentState.p1_team.activePokemon.pp[i] > 0) {
						Move m = currentState.p1_team.activePokemon.moves[i];
						if(m != null) {
							Integer actionIndex = attackMap.get(m);
							if(actionIndex != null) {
								actionProbabilityDistribution[actionIndex] = policyNetworkProbabilityDistribution[i];
							}
						}
					}	
				}
			}
			Pokemon[] switchOptions = new Pokemon[5];
			int index = 0;
			for(Pokemon p : currentState.p1_team.pokemonList) {
				if(p != currentState.p1_team.activePokemon) {
					switchOptions[index] = p;
					index++;
				}
			}
			for(int i = 0; i <= 4; i++) {
				// Switch options
				Pokemon p = switchOptions[i];
				if(p != null && p.isAlive()) {
					Integer actionIndex = switchMap.get(p);
					if(actionIndex != null) {
						actionProbabilityDistribution[actionIndex] = policyNetworkProbabilityDistribution[i+4];
					}
				}
			}
			
			// Scale all action probabilities so that they add to 1, or if all are
			// currently 0, set them to be uniform
			double totalWeight = 0;
			for(int i = 0; i < playerActions.length; i++) {
				totalWeight += actionProbabilityDistribution[i];
			}
			
			if(totalWeight != 0) {
				for(int i = 0; i < playerActions.length; i++) {
					actionProbabilityDistribution[i] /= totalWeight;
				}
			}
			else {
				for(int i = 0; i < playerActions.length; i++) {
					actionProbabilityDistribution[i] = 1.0/playerActions.length;
				}
			}
		}
		
		public TreeNode(GameState gs, NeuralNet policyNet, NeuralNet valuationNet) {
			
			ArrayList<ArrayList<Simulator.Action>> bothPlayerActions = gs.getPlayerAndOpponentActions();
			
			ArrayList<Simulator.Action> p1Actions = bothPlayerActions.get(0);
			ArrayList<Simulator.Action> oppoActions = bothPlayerActions.get(1);
			
			playerActions = new ActionData[p1Actions.size()];
			for(int i = 0; i < playerActions.length; i++)
				playerActions[i] = new ActionData(p1Actions.get(i));
			
			opponentActions = new ActionData[oppoActions.size()];
			for(int i = 0; i < opponentActions.length; i++)
				opponentActions[i] = new ActionData(oppoActions.get(i));
			
			SuccessorNodes = new TreeNode[playerActions.length][opponentActions.length];
			currentState = gs;
			nextUnselectedActions = (playerActions.length > 0 && opponentActions.length > 0 ? new int[] {0,0} : null);
			
			policyNetwork = policyNet;
			assignActionProbabilityDistribution();
			
			valuationNetwork = valuationNet;
			if(valuationNetwork != null) {
				valuationNetwork.forward_prop(NeuralNet.input(currentState));
				v_theta = valuationNetwork.nn.get(valuationNetwork.LAYERS)[0].value;				
			}
			
			
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
		public double playout(int playoutDepth) {
			if(currentState.isTerminal())
				return currentState.evalTerminalNode();
			
			if(playoutDepth > PLAYOUT_MAX_DEPTH) {
				return v_theta;
			}
			
			
			// If either player has no actions consider it a terminal node
			if(playerActions.length == 0 || opponentActions.length == 0)
				return currentState.evalTerminalNode();
			
			// For the player, choose an action randomly with probability assigned to it in actionProbabilityDistribution
			double cumulativeProbSum = 0;
			double rand = Math.random();
			Simulator.Action playerAction = null;
			for(int i = 0; i < playerActions.length && (playerAction == null); i++) {
				cumulativeProbSum += actionProbabilityDistribution[i];
				if(rand <= cumulativeProbSum) {
					playerAction = playerActions[i].action;
				}
			}
			// For the opponent, just choose a random action
			Simulator.Action opponentAction = opponentActions[(int)(Math.random() * opponentActions.length)].action;
			
			GameState nextGS = currentState.simulateTurn(playerAction, opponentAction);
			TreeNode nextTreeNode = new TreeNode(nextGS, policyNetwork, valuationNetwork);
			
			return nextTreeNode.playout(playoutDepth+1);
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
			
			// Use a = (Q(s,a) + u(s,a)) (from AlphaGo paper page 3)
			double[] N = new double[playerActions.length];
			double[] Q = new double[playerActions.length];
			double[] u = new double[playerActions.length];
			double[] actionValue = new double[playerActions.length];
			
			int maxActionValueIndex = 0;
			
			for(int i = 0; i < playerActions.length; i++) {
				N[i] = playerActions[i].n;
				Q[i] = v_theta*(1-LAMBDA) + playerActions[i].X*LAMBDA/playerActions[i].n;
				u[i] = actionProbabilityDistribution[i]/(1 + N[i]);
				actionValue[i] = Q[i] + u[i];
				
				if(actionValue[i] > actionValue[maxActionValueIndex]) {
					maxActionValueIndex = i;
				}
			}
			
			int chosenPlayerAction = maxActionValueIndex;
			
			// Use Decoupled UCB to choose action for opponent
			int chosenOpponentAction =  MCTS.ActionData.bestAction(opponentActions);
			
			return new int[] {chosenPlayerAction, chosenOpponentAction};
			
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
				u1 = sPrime.playout(0);
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
	
	public static Simulator.Action chooseMove(GameState gs, NeuralNet policyNetwork, NeuralNet valuationNetwork) {
		TreeNode root = new TreeNode(gs, policyNetwork, valuationNetwork);
		
		long startTime = System.currentTimeMillis();
		
		// Run simulations until [SimulationTimeLimitSeconds] seconds have elapsed
		while(System.currentTimeMillis() - startTime < (long)(SIMULATION_TIME_LIMIT_SECONDS*1000)) {
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
		
		Simulator.Action p1Action = MCTS.chooseMove(p1GS, new NeuralNet("PolicyNetwork/PolicyNetworkWeights.txt"), null);
		ArrayList<Simulator.Action> p2ActionList = t2.getActions(true);
		Simulator.Action p2Action = p2ActionList.get(0);
		
		System.out.println(p1Action);
		System.out.println(p2Action);
		
		
	}
	
}
