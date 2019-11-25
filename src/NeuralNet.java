import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class NeuralNet {
	/** Size of input layer. */
	private int SIZE = 81;
	/** Number of layers in neural network (including input and output).*/
	private int LAYERS = 3;
	/** Size of the output layer. */
	private int OUTPUT = 9;
	
	// What each node in input corresponds to: https://i.imgur.com/ym1ra0S.jpg
	
	/** 
	 * Weights of the neural network. weights[i][j][k] will refer
	 * to the weight of the edge between j'th node at layer i to 
	 * the k'th node at layer i + 1. 
	 */
	public List<double[][]> weights;
	
	public NeuralNet() {
		weights = new ArrayList<>();
		
		// Initialize all weights to 0
		for (int i = 0; i < LAYERS - 2; i++) {
			weights.add(new double[SIZE][SIZE]);
		}
		weights.add(new double[SIZE][OUTPUT]);
	}
	
	/**
	 * Construct a neural network with the provided weights
	 */
	public NeuralNet(List<double[][]> weights) {
		SIZE = weights.get(0).length;
		LAYERS = weights.size() + 1;
		OUTPUT = weights.get(weights.size() - 1)[0].length;
		this.weights = weights;
	}
	
	/** Activation function for the neural net. */
	private double activate(double s) {
		//TODO: implement activation funciton
		return 0;
		
	}
	
	private double derivative(double s) {
		//TODO: implement derivate of activation
		return 0;
	}
	
	/**
	 * forward_pass[0][i][j] is the unactivated value of the j'th node in layer i (s[i][j])
	 * forward_pass[1][i][j] is the activated value of the j'th node in layer i (v[i][j])
	 */
	public ArrayList<ArrayList<double[]>> forward_pass(double[] input) {
		// Check to see if the input length is valid		
		if (input.length != SIZE) {
			throw new RuntimeException("invalid input size into the neural net. expected " + SIZE + " got " + input.length + ". ");
		}
		
		/** 
		 * s[i][j] is the unactivated value of the j'th node of layer i (for i > 0)
		 * (s[0][j] is just the j'th value of the input)
		 */
		ArrayList<double[]> s = new ArrayList<double[]>();
		
		/** v[i][j] is the activated value of the j'th node of layer i */
		ArrayList<double[]> v = new ArrayList<double[]>();
		
		s.add(new double[SIZE]);
		v.add(new double[SIZE]);
		for(int i = 0; i < SIZE; i++) {
			s.get(0)[i] = input[i];
			v.get(0)[i] = input[i];
		}
		
		for (int i = 1; i < LAYERS; i++) { // layer number
			double[][] w = weights.get(i-1);
			double[] s_i = new double[w[0].length];
			double[] v_i = new double[w[0].length];
			
			for(int j = 0; j < w.length; j++) {
				for(int k = 0; k < w[0].length; k++) {
					s_i[k] += w[j][k]*v.get(i-1)[j];
				}
			}
			
			for(int k = 0; k < w[0].length; k++) {
				v_i[k] = activate(s_i[k]);
			}
			s.add(s_i);
			v.add(v_i);
		}
		
		ArrayList<ArrayList<double[]>> sAndV = new ArrayList<ArrayList<double[]>>();
		sAndV.add(s);
		sAndV.add(v);
		
		return sAndV;
	}
	
	/**
	 * Test the neural net on sample input and print the result
	 * Expected Result: https://i.imgur.com/ym1ra0S.jpg
	 */
	public static void forwardPassTest() {
		
		ArrayList<double[][]> w = new ArrayList<double[][]>();
		w.add(new double[][] {
			{0.1, 0.3, 0.5},
			{0.2, 0.4, 0.6}
		});
		w.add(new double[][] {
			{0.7, 1.0},
			{0.8, 1.1},
			{0.9, 1.2}
		});
		
		NeuralNet n = new NeuralNet(w);
		double[] input = new double[] {13, 14};
		
		ArrayList<ArrayList<double[]>> sAndV = n.forward_pass(input);
		ArrayList<double[]> s = sAndV.get(0);
		ArrayList<double[]> v = sAndV.get(1);
		
		for(double[] d : s.toArray(new double[s.size()][])) {
			System.out.println(Arrays.toString(d));
		}
		System.out.println("--");
		for(double[] d : v.toArray(new double[s.size()][])) {
			System.out.println(Arrays.toString(d));
		}
	}
	
	public static void main(String[] args) {
		forwardPassTest();
	}
}
