import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class NeuralNet {
	/** Size of input layer. */
	private int SIZE = 50;
	/** Number of layers in neural network (including the output).*/
	private int LAYERS = 3;
	/** Size of the output layer. */
	private int OUTPUT = 9;
	
	/** 
	 * Weights of the neural network. weights[i][j][k] will refer
	 * to the weight of the edge between j'th node at layer i to 
	 * the k'th node at layer i + 1. 
	 */
	public List<double[][]> weights;
	
	public NeuralNet() {
		weights = new ArrayList<>();
		
		// Initialize all weights to 0
		for (int i = 0; i < LAYERS - 1; i++) {
			weights.add(new double[SIZE][SIZE]);
		}
		weights.add(new double[SIZE][OUTPUT]);
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
}
