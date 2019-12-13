import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class NeuralNet {

	/** Single unit within the multi-layered neural network. */
	public static class Neuron {
		public List<Neuron> inputs;
		public List<Double> weights;
		public double value;

		public Neuron() {
			inputs = new ArrayList<>();
			weights = new ArrayList<>();
			value = 0;
		}
	}

	/** Data point. */
	public static class Data {
		public List<Double> x;
		public List<Double> y;

		public Data(List<Double> x, List<Double> y) {
			this.x = x;
			this.y = y;
		}

		public Data(Replay r) {
			x = NeuralNet.input(r.state);
			y = new ArrayList<>();
			for(int i = 0; i < 9; i++) {
				if(i == r.action) {
					y.add(1.0);
				}
				else {
					y.add(0.0);
				}
			}

		}
	}

	/** 
	 * Structure of the neural network. SIZE specifies the number of inputs;
	 * LAYERS the number of layers excluding the output layer, and OUTPUT the
	 * number of outputs.
	 */
	public int SIZE, LAYERS, OUTPUT;
	/** Value denoting how many iterations to repeat during back progation. */
	private int EPOCHS;

	/** Learning rate during training. */
	private double ALPHA;

	// What each node in input corresponds to: https://i.imgur.com/ym1ra0S.jpg

	/** 
	 * Multi-layered neural network. Each index in the list corresponds to a
	 * single layer of neurons.
	 */
	public List<Neuron[]> nn;

	/** Constructs a neural network based on info from file "s". */
	public NeuralNet(String s) {
		try {
			FileReader fr = new FileReader(s);
			BufferedReader br = new BufferedReader(fr);
			StringTokenizer st = new StringTokenizer(br.readLine());

			// Read in the neural network structure from first line
			SIZE = Integer.parseInt(st.nextToken());
			LAYERS = Integer.parseInt(st.nextToken());
			OUTPUT = Integer.parseInt(st.nextToken());
			EPOCHS = Integer.parseInt(st.nextToken());
			ALPHA = Double.parseDouble(st.nextToken());

			// Read in the weights of the neural network
			nn = new ArrayList<>();
			// Initialize the structure of the network
			for (int i = 0; i < LAYERS; i++) {
				nn.add(new Neuron[SIZE]);	
			}
			nn.add(new Neuron[OUTPUT]);

			// Initialize first layer of neurons
			for (int i = 0; i < nn.get(0).length; i++) {
				nn.get(0)[i] = new Neuron();
			}
			// Initialize connectedness of network and all weights to random values
			for (int i = 1; i < nn.size(); i++) {
				for (int j = 0; j < nn.get(i).length; j++) {
					st = new StringTokenizer(br.readLine());
					nn.get(i)[j] = new Neuron();

					for (Neuron input : nn.get(i - 1)) {
						nn.get(i)[j].inputs.add(input);
						nn.get(i)[j].weights.add(Double.parseDouble(st.nextToken()));
					}
				}
			}	

			// Close the file
			br.close();

		} catch (IOException e) {
			throw new RuntimeException("Error when initializing neural network from " + s + ". ");
		}
	}

	/** Saves the weights of the neural network to this file. */
	public void save_to_file(String s) {
		try {
			FileWriter fw = new FileWriter(s);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);

			// Print the parameters of the neural network
			pw.print(SIZE + " ");
			pw.print(LAYERS + " ");
			pw.print(OUTPUT + " ");
			pw.print(EPOCHS + " ");
			pw.print(ALPHA + " ");
			pw.print("\n");

			// Print the weights of the neural network
			for (int i = 1; i < nn.size(); i++) {
				for (int j = 0; j < nn.get(i).length; j++) {
					for (int k = 0; k < nn.get(i)[j].weights.size(); k++) {
						pw.print(nn.get(i)[j].weights.get(k) + " ");
					}
					pw.print("\n");
				}
			}	
			// Close the file
			pw.close();


		} catch (IOException e) {
			throw new RuntimeException("Error when saving neural network to " + s + ".");
		}
	}

	/** 
	 * Constructs a neural network object with SIZE = s, LAYERS = l, OUTPUT = o,
	 * EPOCHS = e, and ALPHA = a. All neurons in layer i receive inputs from every
	 * neuron in layer i - 1 (fully connected). All weights are initialized to a 
	 * random value between 0 and 1.
	 */
	public NeuralNet(int s, int l, int o, int e, double a) {
		// Initialize parameters of neural net
		SIZE = s; LAYERS = l; OUTPUT = o; EPOCHS = e; ALPHA = a;

		nn = new ArrayList<>();
		// Initialize the structure of the network
		for (int i = 0; i < LAYERS; i++) {
			nn.add(new Neuron[SIZE]);	
		}
		nn.add(new Neuron[OUTPUT]);

		// Initialize first layer of neurons
		for (int i = 0; i < nn.get(0).length; i++) {
			nn.get(0)[i] = new Neuron();
		}
		// Initialize connectedness of network and all weights to random values
		for (int i = 1; i < nn.size(); i++) {
			for (int j = 0; j < nn.get(i).length; j++) {
				nn.get(i)[j] = new Neuron();

				for (Neuron input : nn.get(i - 1)) {
					nn.get(i)[j].inputs.add(input);
					nn.get(i)[j].weights.add(Math.random());
				}
			}
		}	
	}

	/** 
	 * Calculates the dot product of x and y. Throws runtime exception if
	 * the dimensions of x and y do not match.
	 */
	private double dot(List<Double> x, List<Double> y) {
		double ret = 0.0;
		if (x.size() != y.size()) {
			throw new RuntimeException("Dot product of different length arrays.");
		}

		for (int i = 0; i < x.size(); i++) {
			ret += (x.get(i) * y.get(i));
		}

		return ret;
	}

	/**
	 * Returns the corresponding values of every neuron which connects to n.
	 */
	private List<Double> input_values(Neuron n) {
		List<Double> ret = new ArrayList<>();
		for (Neuron input : n.inputs) {
			ret.add(input.value);
		}

		return ret;
	}

	/**
	 * Forward propagation through the neural network. Used for training and
	 * to calculate the neural network output given input x. Will modify the
	 * values of each neuron in the network. The values of the neurons in the
	 * output after execution are the outputs of input x. Throws runtime
	 * exception if the dimensions of x are improper.
	 */
	public void forward_prop(List<Double> x) {
		if (x.size() != nn.get(0).length) {
			throw new RuntimeException("Input invalid into neural network.");
		}

		// input x into the neural network
		for (int i = 0; i < x.size(); i++) {
			nn.get(0)[i].value = x.get(i);
		}

		// propogate the input values upwards
		for (int i = 1; i < nn.size() - 1; i++) {
			for (Neuron unit : nn.get(i)) {
				List<Double> feed = input_values(unit);
				unit.value = activate(dot(unit.weights, feed));
			}
		}

		for (Neuron unit : nn.get(LAYERS)) {
			List<Double> feed = input_values(unit);
			unit.value = dot(unit.weights, feed);
		}
	}

	/** Activation function for the neural net (ReLU). */
	private double activate(double s) {
		return 1/(1+Math.exp(-s/100));
	}

	/** Derivative of the activation function defined above (ReLU). */
	private double derivative(double s) {
		double a = activate(s);
		return 1.0/100*a*(1-a);
	}

	/** Back propagation based on data. */
	public void back_prop(List<Data> data) {
		Map<Neuron, Double> delta = new HashMap<>();
		// Initialize with all neurons in neural net and keys of 0
		for (int i = 0; i < nn.size(); i++) {
			for (Neuron n : nn.get(i)) {
				delta.put(n, 0.0);
			}
		}

		for (int t = 0; t < EPOCHS; t++) {
			for (Data d : data) {
				forward_prop(d.x);
				// update last layer gradients
				for (int i = 0; i < nn.get(LAYERS).length; i++) {
					List<Double> feed = input_values(nn.get(LAYERS)[i]);
					delta.put(nn.get(LAYERS)[i], derivative(dot(nn.get(LAYERS)[i].weights, feed)) * (d.y.get(i) - nn.get(LAYERS)[i].value));
				}
				// update hidden layer gradients
				for (int i = LAYERS - 1; i >= 0; i--) {
					for (Neuron n : nn.get(i)) {
						List<Double> feed = input_values(n);

						List<Double> u = new ArrayList<>();
						List<Double> q = new ArrayList<>();
						for (Neuron next : nn.get(i + 1)) {
							u.add(next.weights.get(next.inputs.indexOf(n)));
							q.add(delta.get(next));
						}

						delta.put(n, derivative(dot(n.weights, feed)) * dot(u, q));

					}
				}
				// update all weights
				for (int layer = 0; layer < nn.size(); layer++) {
					for (Neuron unit : nn.get(layer)) {
						for (int i = 0; i < unit.inputs.size(); i++) {
							unit.weights.set(i, unit.weights.get(i) + ALPHA * unit.inputs.get(i).value * delta.get(unit));
						}
					}
				}
			}
		}
	}

	/** Separates the data into batches of size batch_size. */
	private static List<List<Data>> to_batches(List<Data> data, int batch_size) {
		// Shuffles data randomly
		Random rand = new Random(System.nanoTime());
		for (int i = 0; i < data.size(); i++) {
			int swap = rand.nextInt(data.size() - i) + i;
			Data tmp = data.get(i);
			data.set(i, data.get(swap));
			data.set(swap, tmp);
		}

		List<List<Data>> ret = new ArrayList<>();
		int idx = 0;

		// Separate the data into batches
		while (idx < data.size()) {
			List<Data> next_layer = new ArrayList<>();
			// Populate the batch and add it to the return
			while (next_layer.size() < batch_size) {
				next_layer.add(data.get(idx++));	
			}
			ret.add(next_layer);
		}
		return ret;
	}
	
	/** 
	 * Back propagation using stochastic gradient descent based on data. 
	 */
	public void back_prop_batch(List<Data> data, int batch_size) {
		back_prop_batch_with_checkpoints(data, batch_size, null, 0);
	}
	
	/** 
	 * Back propagation using stochastic gradient descent based on data. 
	 * 
	 * if [checkpointFilePath] != null, after every [checkpointNumIterations] iterations, 
	 * the current weights will be saved at [checkpointFilePath][t].txt
	 */
	public void back_prop_batch_with_checkpoints(List<Data> data, int batch_size, String checkpointFilePath, int checkpointNumIterations) {
		for (int t = 0; t < EPOCHS; t++) {
			// Separate the data into batches to be used for training on this epoch
			List<List<Data>> batches = to_batches(data, batch_size);

			for (List<Data> batch : batches) {

				Map<Neuron, Double> delta = new HashMap<>();
				// Initialize with all neurons in neural net and keys of 0
				for (int i = 0; i < nn.size(); i++) {
					for (Neuron n : nn.get(i)) {
						delta.put(n, 0.0);
					}
				}

				for (Data d : batch) {
					forward_prop(d.x);
					// update last layer gradients
					for (int i = 0; i < nn.get(LAYERS).length; i++) {
						List<Double> feed = input_values(nn.get(LAYERS)[i]);
						delta.put(nn.get(LAYERS)[i], derivative(dot(nn.get(LAYERS)[i].weights, feed)) * (d.y.get(i) - nn.get(LAYERS)[i].value));
					}
					// update hidden layer gradients
					for (int i = LAYERS - 1; i >= 0; i--) {
						for (Neuron n : nn.get(i)) {
							List<Double> feed = input_values(n);

							List<Double> u = new ArrayList<>();
							List<Double> q = new ArrayList<>();
							for (Neuron next : nn.get(i + 1)) {
								u.add(next.weights.get(next.inputs.indexOf(n)));
								q.add(delta.get(next));
							}

							delta.put(n, derivative(dot(n.weights, feed)) * dot(u, q));

						}
					}

					for (Neuron n : delta.keySet()) {
						delta.put(n, delta.get(n)/batch_size);
					}
				}

				// update all weights
				for (int layer = 0; layer < nn.size(); layer++) {
					for (Neuron unit : nn.get(layer)) {
						for (int i = 0; i < unit.inputs.size(); i++) {
							unit.weights.set(i, unit.weights.get(i) + ALPHA * unit.inputs.get(i).value * delta.get(unit));
						}
					}
				}
			}

			if(checkpointFilePath != null && t % checkpointNumIterations == checkpointNumIterations-1) {
				this.save_to_file(checkpointFilePath + t + ".txt");
				System.out.println("t = " + t);
			}
		}
	}

	public static List<Double> input(GameState gs) {
		List<Double> x = new ArrayList<>();

		// Consider the active pokemon's move-set
		for (Move m : gs.p1_team.activePokemon.moves) {
			if (m == null) {
				for (int i = 0; i < 10; i++) {
					x.add(0.0);
				}
				continue;
			}
			// Move statistics
			x.add((double)m.power);
			x.add((double)m.accuracy);
			x.add(m.stat_boost);
			x.add(m.status_chance);
			x.add(m.health_decrease);
			x.add((double)m.priority);

			/* Check if counter is a viable choice (1 if yes 0 otherwise). Must also
			 * Check if the opponent pokemon has a physical move in order to counter. */
			boolean hasPhysical = false;
			for (Move opp_move : gs.p2_pokemon.get(gs.p2_active)) {
				if (opp_move != null && TeamGenerator.getType(opp_move.name).equals("physical")) {
					hasPhysical = true;
				}
			}
			if (m.name.equals("counter") && hasPhysical) {
				x.add(1.0);
			}
			else {
				x.add(0.0);
			}

			// Check if the move is bide (1 yes 0 otherwise)
			if (m.name.equals("bide")) {
				x.add(1.0);
			}
			else {
				x.add(0.0);
			}

			// Check if move requires charging (1 if yes, 0 otherwise)
			if (m.name.equals("hyperbeam") || m.name.equals("skyattack")) {
				x.add(1.0);
			}
			else {
				x.add(0.0);
			}

			// Check if the move is substitute
			if (m.name.equals("substitute")) {
				x.add(1.0);
			}
			else {
				x.add(0.0);
			}
		}
		// Ensure that the x is properly populated
		while (x.size() < 40) {
			x.add(0.0);
		}

		// Consider the active pokemon's effectiveness against the enemy team
		cal_effect(x, gs.p1_team.activePokemon.moves, gs.p2_pokemon.keySet());
		// Ensure that the x is properly populated
		while (x.size() < 64) {
			x.add(0.0);
		}

		// Consider the opponent's active against player one's team
		x.add(cal_def(gs.p1_team.activePokemon, gs.p2_pokemon.get(gs.p2_active)));
		if (gs.p1_team.activePokemon.hasMajorStatus()) {
			x.add(1.0);
		}
		else {
			x.add(0.0);
		}
		for (Pokemon p : gs.p1_team.pokemonList) {
			if (!p.species.equals(gs.p1_team.activePokemon.species)) {
				if (p.isAlive()) {
					x.add(cal_def(p, gs.p2_pokemon.get(gs.p2_active)));
					if (p.hasMajorStatus()) {
						x.add(1.0);
					}
					else {
						x.add(0.0);
					}
				}
				// Pokemon is fainted and cannot be used
				else {
					x.add(4.0);
					x.add(1.0);
				}
			}
		}

		// Ensure that the x is properly populated
		while (x.size() < 76) {
			x.add(0.0);
		}

		// Compare speeds: 1 if faster, 0 otherwise.
		if (Pokedex.getDex().get(gs.p1_team.activePokemon.species).baseStats[4] > Pokedex.getDex().get(gs.p2_active.species).baseStats[4]) {
			x.add(1.0);
		}
		else {
			x.add(0.0);
		}

		return x;
	}

	/**
	 * Modify x to reflect the effectiveness of moveset on the pokemon in opponent.
	 */
	private static void cal_effect(List<Double> x, Move[] moveset, Set<Pokemon> opponent) {
		for (Move m : moveset) {
			// Counter used to account for unseen pokemon
			int fill_counter = 0;
			for (Pokemon p : opponent) {
				if (m == null) {
					x.add(0.0);
				}
				else if (p.isAlive()) {
					x.add(m.type.effectiveness(p.types[0]) * m.type.effectiveness(p.types[1]));
				}
				// Use neutral value if opponent pokemon is fainted
				else {
					x.add(0.0);
				}
				fill_counter++;
			}
			// Assume neutral damage for unseen pokemon
			while (fill_counter++ < 6) {
				x.add(1.0);
			}
		}
	}

	/**
	 * Returns 1 if a move in moveset if super-effective versus p, 0 otherwise.
	 */
	private static double cal_def(Pokemon p, Set<Move> moveset) {
		for (Move m : moveset) {
			if (m != null && m.type.effectiveness(p.types[0]) * m.type.effectiveness(p.types[1]) > 1.0) {
				return 1;
			}
		}

		return 0;
	}

	// Test the neural network using XOR function.
	public static void main(String[] args) {
		for (int i = 0; i < 1000000000; i++) {
			System.out.println(i);
		}
	}
}

