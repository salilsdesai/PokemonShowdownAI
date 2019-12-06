import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class NeuralNet {
	private int SIZE, LAYERS, OUTPUT, EPOCHS;
	
	private double ALPHA;
	
	// What each node in input corresponds to: https://i.imgur.com/ym1ra0S.jpg
	
	private List<Neuron[]> nn;
	
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
	
	private List<Double> input_values(Neuron n) {
		List<Double> ret = new ArrayList<>();
		for (Neuron input : n.inputs) {
			ret.add(input.value);
		}
		
		return ret;
	}
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
				for (Neuron unit : nn.get(LAYERS)) {
					List<Double> feed = input_values(unit);
					delta.put(unit, derivative(dot(unit.weights, feed)) * (d.y - unit.value));
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
	public static void main(String[] args) {
		NeuralNet nn = new NeuralNet(2, 2, 1, 1000000, 0.5);
		
		List<Double> x1 = new ArrayList<>();
		List<Double> x2 = new ArrayList<>();
		List<Double> x3 = new ArrayList<>();
		List<Double> x4 = new ArrayList<>();
		
		x1.add(0.0); x1.add(0.0);
		x2.add(0.0); x2.add(1.0);
		x3.add(1.0); x3.add(0.0);
		x4.add(1.0); x4.add(1.0);
		
		Data d1 = new Data(x1, 0.0), d2 = new Data(x2, 1.0), d3 = new Data(x3, 1.0), d4 = new Data(x4, 0.0);
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
	}
}

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

class Data {
	List<Double> x;
	double y;
	
	public Data(List<Double> x, double y) {
		this.x = x;
		this.y = y;
	}
}
