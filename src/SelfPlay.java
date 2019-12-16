import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
			// create the file
			File f = new File(s);
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
	private static Map<List<Double>, Integer> collectData(int size, NeuralNet policyNetwork) {
		
		// gather data from self-play
		Map<List<Double>, Integer> saved = new HashMap<>();
		for (int i = 0; i < size; i++) {
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
	
	/**
	 * Read the self play data samples from [start index, endIndex),
	 * (inclusive of the start index, exclusive of end index) from
	 * SelfPlayData/SelfPlayData.txt
	 * 
	 * If endIndex is more than the number of samples available,
	 * the end index possible will be used
	 */
	private static List<NeuralNet.Data> loadSelfPlayData(int startIndex, int endIndex) {
		List<NeuralNet.Data> data = new ArrayList<>();
		try {
			FileReader fr = new FileReader("SelfPlayData/SelfPlayData.txt");
			BufferedReader br = new BufferedReader(fr);
			
			List<String> inputStrings = new ArrayList<String>();
			List<String> outputStrings = new ArrayList<String>();
			
			String line = br.readLine();
			while(line != null && inputStrings.size() < endIndex) {
				inputStrings.add(line);
				outputStrings.add(br.readLine());// just assume there's an even number of lines
				line = br.readLine();
			}
			br.close();
			
			for(int i = startIndex; i < inputStrings.size(); i++) {
				String[] inputStringArray = inputStrings.get(i).split(" ");
				String outputString = outputStrings.get(i);
				
				List<Double> x = new ArrayList<Double>();
				List<Double> y = new ArrayList<Double>();
				
				for(int j = 0; j < inputStringArray.length; j++) {
					x.add(Double.parseDouble(inputStringArray[j]));
				}
				
				y.add(Double.parseDouble(outputString));
				
				NeuralNet.Data d = new NeuralNet.Data(x, y);
				data.add(d);
			}
			return data;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Train the valuation network, output result to "ValuationNetwork/ValuationNetworkWeights.txt", and
	 * checkpoint every 1000 steps during the training
	 * 
	 * Number of training samples used will be 
	 * min([maxNumTrainingSamples], number of training samples available) 
	 */
	public static void trainValuationNetwork(int numLayers, int epochs, double stepSize, int batchSize, int maxNumTrainingSamples) {
		List<NeuralNet.Data> data = loadSelfPlayData(0, 250);					
		NeuralNet nn = new NeuralNet(77, numLayers, 1, epochs, stepSize);
		nn.back_prop_batch_with_checkpoints(data, batchSize, "ValuationNetwork/ValuationNetworkWeights", 1000);
		nn.save_to_file("ValuationNetwork/ValuationNetworkWeights.txt");
		
		for (int i = 0; i < 20; i++) { // print out the first 20 to check
			System.out.println("nn1 Ouput: ");
			nn.forward_prop(data.get(i).x);
			for (int j = 0; j < nn.nn.get(nn.LAYERS).length; j++) {
				System.out.println(nn.nn.get(nn.LAYERS)[j].value + " ");
			}
			System.out.print("Expected: ");
			System.out.println(data.get(i).y.get(0) + "\n\n");
		}
	}
	
	public static void collectDataPoints() {
		NeuralNet nn = new NeuralNet("PolicyNetwork/PolicyNetworkWeights.txt");
		// collect ~1000 total data points
		for (int i = 0; i < 100; i++) {
			writeTo("SelfPlayData/SelfPlayData" + i + ".txt", collectData(10, nn));
		}
	}
	
	/**
	 * Merge the data of each SelfPlayData/SelfPlayData___ file
	 * into SelfPlayData/SelfPlayData.txt (by just appending all lines together)
	 */
	public static void mergeSelfPlayData() {
		try {
			File combinedData = new File("SelfPlayData/SelfPlayData.txt");
			BufferedWriter bw = new BufferedWriter(new FileWriter(combinedData));
			PrintWriter pw = new PrintWriter(bw);
			
			File folder = new File("SelfPlayData");
			for(String fileName : folder.list()) {
				if(fileName.length() >= 12 && fileName.substring(0,12).equals("SelfPlayData") && !fileName.equals("SelfPlayData.txt")) {
					FileReader fr = new FileReader("SelfPlayData/" + fileName);
					BufferedReader br = new BufferedReader(fr);
					String s = br.readLine();
					while(s != null) {
						pw.write(s + "\n");
						s = br.readLine();
					}
					br.close();
				}
			}
			
			pw.close();
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Print the results of testing the Policy Neural Network with weights saved
	 * in [weigthsFilePath] on SelfPlayData samples 250-499
	 * (because it was trained on 0-249)
	 */
	public static void printValuationNetworkTestResults(String weightsFilePath) {
		NeuralNet nn = new NeuralNet(weightsFilePath);
		List<NeuralNet.Data> data = loadSelfPlayData(250, 500);

		System.out.println("Five Randomly Chosen Data Points:\n");
		for(int i = 0; i < 5; i++) {
			int index = (int)(Math.random()*250);
			nn.forward_prop(data.get(index).x);
			System.out.println(index + ": ");
			System.out.println("\tOutput: " + nn.nn.get(nn.LAYERS)[0].value + " ");
			System.out.println("\tExpected: " + data.get(index).y.get(0));
		}
		
		// Consider a data point to be "correct" if it is < 0.5 for expected 0
		// and >= 0.5 for expeted 1
		int correctCount = 0;
		double totalError = 0;
		for(int i = 0; i < 250; i++) {
			
			nn.forward_prop(data.get(i).x);
			double out = nn.nn.get(nn.LAYERS)[0].value;
			int expected = (data.get(i).y.get(0)).intValue();
			
			System.out.print(i + ": " + (((int)(out*1000))/1000.0) + ", " + expected);
			if((out < 0.5 && expected == 0) || (out >= 0.5 && expected == 1)) {
				correctCount++;
				System.out.println(" (ok) " + correctCount);
			}
			else {
				System.out.println();
			}
			
			totalError += Math.abs(expected - out);	
		}
		
		System.out.println("Correct Count: " + correctCount + "/250");
		System.out.println("Average Error: " + (totalError/250));
	}
	
	public static void main(String[] args) {
		printValuationNetworkTestResults("ValuationNetwork/ValuationNetworkWeights.txt");
	}
}

