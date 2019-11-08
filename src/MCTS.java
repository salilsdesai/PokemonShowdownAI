import java.util.List;

public class MCTS {
	public static class TreeNode {
		Simulator.Action[] playerActions;
		Simulator.Action[] opponentActions;
		/**
		 * MatrixEntry[i][j] is the entry associated with
		 * playerActions[i] and opponentActions[j]
		 */
		public MatrixEntry[][] entries;
		public TreeNode(List<Simulator.Action> p1Actions, List<Simulator.Action> oppoActions) {
			playerActions = p1Actions.toArray(new Simulator.Action[p1Actions.size()]);
			opponentActions = oppoActions.toArray(new Simulator.Action[oppoActions.size()]);
			entries = new MatrixEntry[playerActions.length][opponentActions.length];
		}
		/**
		 * Should be called after we are done exploring tree
		 * Choose whichever player actions has highest average 
		 */
		public Simulator.Action getBestAction() {
			// TODO
			return null;
		}
	}
	public static class MatrixEntry {
		public TreeNode successor;
		/**
		 * X and n as defined in this paper:
		 * http://mlanctot.info/files/papers/cig14-smmctsggp.pdf
		 */
		public int X;
		public int n;
		public MatrixEntry() {
			X = 0;
			n = 0;
		}
	}
	public static Simulator.Action chooseMove(GameState gs) {
		List<Simulator.Action> playerActions = gs.p1_team.getActions();
		List<Simulator.Action> opponentActions = gs.getOpponentTeam().getActions();
		
		TreeNode root = new TreeNode(playerActions, opponentActions);
		
		while(!stopSearch()) {
			SMMCTS(root);
		}
		
		return root.getBestAction();
	}
	
	public static int SMMCTS(TreeNode s) {
		// TODO
		return 0;
	}
	
	public static boolean stopSearch() {
		// TODO
		return false;
	}

}
