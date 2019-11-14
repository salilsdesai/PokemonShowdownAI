import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GameState {
    /* Representation of player-one's team of pokemon and movesets. */
    public Team p1_team;
    /* List of pokemon player-one knows the opponent has and their statuses. The
     * moves of the pokemon stored in the key are purely random and meaningless.
     * The value of each key stores the moves which player-one knows player-two's
     * pokemon can perform. If player-one has not seen a specific pokemon use a move,
     * then the value in the map will be an empty set. */
    public HashMap<Pokemon, HashSet<Move>> p2_pokemon;

    /* Player-two's active pokemon. */
    public Pokemon p2_active;

    /** Default game state which describes the game as it begins. */
    public GameState(Team p1_team, Pokemon p2_active) {
    	/* Initialize variables. */
    	p2_pokemon = new HashMap<>();
    	/* Set up the current field. */
    	this.p1_team = p1_team;
    	this.p2_active = p2_active;
    	p2_pokemon.put(p2_active, new HashSet<>());
    }

    /* Pass a deep copy of [p2_pokemon]. Intended to be used by successor game state. */
    public HashMap<Pokemon, HashSet<Move>> pass_on() {
    	HashMap<Pokemon, HashSet<Move>> ret = new HashMap<>();
		// For every key, re-insert the key,value into the new hash map.
		for (Pokemon p : p2_pokemon.keySet()) {
			// TODO: It might be helpful to clone the pokemon.
			ret.put(p, p2_pokemon.get(p));
		}

		return ret;
    }
    
    /** Generates the team represented in the hashmap. */
    public Team getOpponentTeam() {
    	// List representation of the opponent's team. 
    	ArrayList<Pokemon> rep = new ArrayList<>();

    	/* Add every seen pokemon that was seen into the tree. */
    	for (Pokemon p : p2_pokemon.keySet()) {
    		/* If the pokemon seen has never been observed to perform moves, assign
    		 * it a random moveset which is consistent with how moves are generated. */
    		if (p2_pokemon.get(p).size() == 0) {
    			String[] moves = TeamGenerator.moveset(p.species);
    			p.moves = new Move[moves.length];
   				for(int i = 0; i < moves.length; i++) {
   					p.moves[i] = Move.getMove(moves[i]);
   				}
   			}
    		else {
    			// Debugging statement
    			if (p2_pokemon.get(p).size() > p.moves.length) {
    				throw new RuntimeException("Error in gamestate: pokemon knows too many moves.");
    			}
    			/* Assign all the known moves to the pokemon. Fill the remainder
    			 * of the moveset with [null]. */
    			int counter = 0;
    			for (Move m : p2_pokemon.get(p)) {
    				p.moves[counter++] = m;
    			}
    			for (; counter < p.moves.length; counter++) {
    				p.moves[counter] = null;
    			}
    		}
    		rep.add(p);
    	}
    	
    	/* Swap the ordering of the list such that the first pokemon is the active pokemon. */
    	int active_index = rep.indexOf(p2_active);
    	Pokemon tmp = rep.get(0);
    	rep.set(0, p2_active);
    	rep.set(active_index, tmp);
    	
    	return new Team(rep);
    }
    
    /** Returns if the current game-state is terminal (if either player has lost). */
    public boolean isTerminal() {
    	boolean lost = true;
    	for (Pokemon p : p1_team.pokemonList) {
    		if (p.isAlive()) {
    			lost = false;
    		}
    	}
    	
    	boolean won = true;
    	for (Pokemon p : p2_pokemon.keySet()) {
    		if (p.isAlive()) {
    			won = false;
    		}
    	}
    	
    	return lost || won;
    }
    
    /** Returns the estimate of how favorable a terminal state is for player one. */
    public double evalTerminalNode() {
    	//TODO: can replace with another function later
    	double pokemonRemaining = 0.;
    	double hpRemaining = 0.;
    	
    	for (Pokemon p : p1_team.pokemonList) {
    		if (p.isAlive()) {
    			hpRemaining += ((double)p.currHp/p.maxHp);
    			pokemonRemaining++;
    		}
    	}
    	
    	return pokemonRemaining/3 + hpRemaining;
    }
}
