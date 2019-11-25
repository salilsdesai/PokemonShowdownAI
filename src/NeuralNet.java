import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Arrays;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;


public class NeuralNet {
	/** Size of input layer. */
	private int SIZE;
	/** Number of layers in neural network (excluding the output).*/
	private int LAYERS;
	/** Size of the output layer. */
	private int OUTPUT;
	
	// What each node in input corresponds to: https://i.imgur.com/ym1ra0S.jpg
	
	/** 
	 * Weights of the neural network. weights[i][j][k] will refer
	 * to the weight of the edge between j'th node at layer i to 
	 * the k'th node at layer i + 1. 
	 */
	public List<double[][]> weights;
	
	public NeuralNet(List<double[][]> weights) {
		SIZE = weights.get(0).length;
		LAYERS = weights.size() + 1;
		OUTPUT = weights.get(weights.size() - 1)[0].length;
		
		this.weights = weights;
	}
	
	/** Read the weights of the neural net from file s. */
	public NeuralNet(String s) {
		try {
			FileReader fr = new FileReader(s);
			BufferedReader br = new BufferedReader(fr);
			StringTokenizer st = new StringTokenizer(br.readLine(), ",");
			
			SIZE = Integer.parseInt(st.nextToken());
			LAYERS = Integer.parseInt(st.nextToken());
			OUTPUT = Integer.parseInt(st.nextToken());

			weights = new ArrayList<>(LAYERS - 1);
			for (int i = 0; i < LAYERS - 1; i++) {
				// Set the initial values of the weights to 0 
				if (i != LAYERS - 2) {
					weights.add(i, new double[SIZE][SIZE]);
				}
				else {
					weights.add(i, new double[SIZE][OUTPUT]);
				}
			}
			
			// Read in layers line by line
			for (int i = 0; i < weights.size(); i++) {
				// Read the actual weights from the file
				st = new StringTokenizer(br.readLine(), ",");
				for (int j = 0; j < weights.get(i).length; j++) {
					for (int k = 0; k < weights.get(i)[j].length; k++) {
						weights.get(i)[j][k] = Double.parseDouble(st.nextToken());
						//System.out.println(weights.get(i)[j][k]);
					}
				}
			}
			
			br.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/** Writes the weights of the neural net to file s. */
	public void save(String s) {
		try {
			FileWriter fw = new FileWriter(s);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			
			pw.print(SIZE + "," + LAYERS + "," + OUTPUT + ",\n");
			
			// Print layers line by line
			for (int i = 0; i < weights.size(); i++) {
				for (int j = 0; j < weights.get(i).length; j++) {
					for (int k = 0; k < weights.get(i)[0].length; k++) {
						pw.print(weights.get(i)[j][k] + ",");
					}
				}
				pw.print("\n");
			}

			pw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
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
