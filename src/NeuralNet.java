import java.util.ArrayList;
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
	/** 
	 * Structure of the neural network. SIZE specifies the number of inputs;
	 * LAYERS the number of layers excluding the output layer, and OUTPUT the
	 * number of outputs.
	 */
	private int SIZE, LAYERS, OUTPUT;
	/** Value denoting how many iterations to repeat during back progation. */
	private int EPOCHS;
	
	/** Learning rate during training. */
	private double ALPHA;
	
	// What each node in input corresponds to: https://i.imgur.com/ym1ra0S.jpg
	
	/** 
	 * Multi-layered neural network. Each index in the list corresponds to a
	 * single layer of neurons.
	 */
	private List<Neuron[]> nn;
	
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
		for (int i = 1; i < nn.size(); i++) {
			for (Neuron unit : nn.get(i)) {
				List<Double> feed = input_values(unit);
				unit.value = activate(dot(unit.weights, feed));
			}
		}
	}
	
	/** Activation function for the neural net (sigmoid). */
	private double activate(double s) {
		double exp = Math.exp(s);
		return exp / (exp + 1);		
	}
	
	/** Derivative of the activation function defined above (sigmoid). */
	private double derivative(double s) {
		double sig = activate(s);
		return sig * (1 - sig);
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
	
	public static List<Double> input(GameState gs) {
		List<Double> x = new ArrayList<>();
		
		// Consider the active pokemon's move-set
		for (Move m : gs.p1_team.activePokemon.moves) {
			x.add((double)m.power);
			x.add((double)m.accuracy);
			// TODO: add in stats and status chance
			x.add((double)m.priority);
			// TODO: add in counter and bide
			
			if (m.name.equals("hyperbeam") || m.name.equals("skyattack")) {
				x.add(1.0);
			}
			else {
				x.add(0.0);
			}
			
			// TODO: add recoil damage
			// TODO: add health recovered
			
			if (m.name.equals("substitute")) {
				x.add(1.0);
			}
			else {
				x.add(0.0);
			}
		}
		
		// Consider the active pokemon's effectiveness against the enemy team
		cal_effect(x, gs.p1_team.activePokemon.moves, gs.p2_pokemon.keySet());
		
		// Consider the opponent's active against player one's team
		x.add(cal_def(gs.p1_team.activePokemon, gs.p2_pokemon.get(gs.p2_active)));
		for (Pokemon p : gs.p1_team.pokemonList) {
			if (p.isAlive()) {
				if (!p.species.equals(gs.p1_team.activePokemon.species)) {
					x.add(cal_def(p, gs.p2_pokemon.get(gs.p2_active)));
				}
			}
			// Pokemon is fainted and cannot be used
			else {
				x.add(4.0);
			}
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
			if (m.type.effectiveness(p.types[0]) * m.type.effectiveness(p.types[1]) > 1.0) {
				return 1;
			}
		}
		
		return 0;
	}
	
	// Test the neural network using XOR function.
	public static void main(String[] args) {
		NeuralNet nn = new NeuralNet(2, 2, 1, 1000000, 0.15);
		
		List<Double> x1 = new ArrayList<>();
		List<Double> x2 = new ArrayList<>();
		List<Double> x3 = new ArrayList<>();
		List<Double> x4 = new ArrayList<>();
		
		x1.add(0.0); x1.add(0.0);
		x2.add(0.0); x2.add(1.0);
		x3.add(1.0); x3.add(0.0);
		x4.add(1.0); x4.add(1.0);
		
		List<Double> y1 = new ArrayList<>();
		List<Double> y2 = new ArrayList<>();
		List<Double> y3 = new ArrayList<>();
		List<Double> y4 = new ArrayList<>();
		
		y1.add(0.0);
		y2.add(1.0);
		y3.add(1.0);
		y4.add(0.0);
		
		Data d1 = new Data(x1, y1), d2 = new Data(x2, y2), d3 = new Data(x3, y3), d4 = new Data(x4, y4);
		List<Data> D = new ArrayList<>();
		D.add(d1);
		D.add(d2);
		D.add(d3);
		D.add(d4);
		
		nn.back_prop(D);
		
		nn.forward_prop(x1);
		System.out.println(nn.nn.get(nn.LAYERS)[0].value);
		
		nn.forward_prop(x2);
		System.out.println(nn.nn.get(nn.LAYERS)[0].value);
		
		nn.forward_prop(x3);
		System.out.println(nn.nn.get(nn.LAYERS)[0].value);
		
		nn.forward_prop(x4);
		System.out.println(nn.nn.get(nn.LAYERS)[0].value);
		
		Team t1 = new Team(TeamGenerator.randomTeam());
		Team t2 = new Team(TeamGenerator.randomTeam());
		
		GameState gs = new GameState(t2, t1.activePokemon);
		List<Double> first_input = input(gs);
		
		System.out.print(first_input.size());
	}
}

/** Single unit within the multi-layered neural network. */
class Neuron {
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
class Data {
	List<Double> x;
	List<Double> y;
	
	public Data(List<Double> x, List<Double> y) {
		this.x = x;
		this.y = y;
	}
}
