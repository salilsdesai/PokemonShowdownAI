import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
	
	public double[] compute_output(double[] input) {
		// Check to see if the input length is valid
		if (input.length != SIZE) {
			throw new RuntimeException("invalid input size into the neural net. expected " + SIZE + " got " + input.length + ". ");
		}
		
		double[] ret = new double[OUTPUT];
		
		for (int i = 0; i < LAYERS; i++) {
			double[] tmp = new double[OUTPUT];
			
		}
		
		return ret;
	}
	
	public static void main(String[] args) {
		NeuralNet nn = new NeuralNet("input");
		nn.save("output");
	}
}
