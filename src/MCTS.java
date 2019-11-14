import java.util.List;

public class MCTS {

	public static class TreeNode {
		public Simulator.Action[] playerActions;
		public Simulator.Action[] opponentActions;
		
		public GameState currentState;
		/**
		 * MatrixEntry[i][j] is the entry associated with
		 * playerActions[i] and opponentActions[j]
		 */
		public MatrixEntry[][] entries;
		
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
			
			GameState newGS = currentState.simulateTurn(playerActions[i], opponentActions[j]);
			TreeNode successorNode = new TreeNode(newGS);
			entries[i][j] = new MatrixEntry(successorNode);
			
			return new int[] {i, j};
			
		}
		
		public TreeNode(GameState gs) {
			List<Simulator.Action> p1Actions = gs.p1_team.getActions();
			List<Simulator.Action> oppoActions = gs.getOpponentTeam().getActions();
			
			playerActions = p1Actions.toArray(new Simulator.Action[p1Actions.size()]);
			opponentActions = oppoActions.toArray(new Simulator.Action[oppoActions.size()]);
			entries = new MatrixEntry[playerActions.length][opponentActions.length];
			currentState = gs;
			nextUnselectedActions = (playerActions.length > 0 && opponentActions.length > 0 ? new int[] {0,0} : null);
		}
		/**
		 * Should be called after we are done exploring tree
		 * Choose whichever player actions has highest average 
		 */
		public Simulator.Action getBestAction() {
			// TODO
			// Pick the action with largest min expected payout over opponent actions.s
			return null;
		}
		
		/**
		 * Return the payout of the terimal node obtained by simulating from this node
		 * to the end of the game
		 */
		public double playout() {
			// TODO
			return 0;
		}
		
		/**
		 * playerActions[i] and opponentActions[j] where
		 * the actions that simulation was performed on which we
		 * need to update the results from
		 */
		public void update(int i, int j, double u1) {
			// TODO
		}
		
		/**
		 * Returns {i, j} where (playerActions[i], opponentActions[j])
		 * is the pair of actions we want to select
		 */
		public int[] select() {
			// TODO
			return new int [] {0, 0};
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
				MatrixEntry entry = entries[i][j];
				TreeNode sPrime = entry.successor;
				u1 = sPrime.playout();
				/*
				 * We are using Decoupled UCT, so X and n values are stored in matrix
				 * entries rather than in nodes like the pseudocode suggests
				 */
				entry.X += u1;
				entry.n += 1;
			}
			else {
				int[] selectedActions = select();
				i = selectedActions[0];
				j = selectedActions[1];
				MatrixEntry entry = entries[i][j];
				TreeNode sPrime = entry.successor;
				u1 = sPrime.SMMCTS();
			}
			
			update(i, j, u1);
			return u1;
		}
		
	}
	public static class MatrixEntry {
		public TreeNode successor;
		/**
		 * X and n as defined in this paper:
		 * http://mlanctot.info/files/papers/cig14-smmctsggp.pdf
		 */
		public double X;
		public int n;
		public MatrixEntry(TreeNode s) {
			X = 0;
			n = 0;
			successor = s;
		}
	}
	public static Simulator.Action chooseMove(GameState gs) {
		TreeNode root = new TreeNode(gs);
		
		while(!stopSearch()) {
			root.SMMCTS();
		}
		
		return root.getBestAction();
	}
	
	public static boolean stopSearch() {
		// TODO
		return false;
	}

}
