import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

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

    /** Empty game-state used for construction */
    private GameState() {
    	
    }
    
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("Our Team:\n\t" + p1_team.toString());
    	sb.append("\nOpponent active:\n\t" + p2_active);
    	sb.append("\nOpponent Pokemon:");
    	for(Map.Entry<Pokemon, HashSet<Move>> e : p2_pokemon.entrySet()) {
    		sb.append("\n\t" + e.getKey().toString());
    		sb.append("\n\t\t");
    		for(Move m : e.getValue()) {
    			sb.append(m == null ? "null, " : m.name + ", ");
    		}
    	}
    	return new String(sb);
    }
    
    /** 
     * Updates the game-state to reflect which move was played. 
     * If the opponent switched pokemon, m should be null
     * If the opponent used a move, new_p2_active should be null
     */
    public void update(Pokemon new_p2_active, Move m) {
    	// Opponent used a move
    	if (new_p2_active == null) {
    		// Update the moveset of the opponent's active pokemon
    		HashSet<Move> p2_active_moves = p2_pokemon.get(p2_active);
    		if(p2_active_moves != null)
    			p2_active_moves.add(m);
    		return;
    	}
    	
    	// Opponent switched to a previously seen pokemon
    	if (p2_pokemon.get(new_p2_active) != null) {
    		p2_active = new_p2_active;
    		return;
    	}

    	// Opponent switched to an unknown pokemon
    	p2_pokemon.put(new_p2_active, new HashSet<>());
    	p2_active = new_p2_active;
    }
    
    /** Default game state which describes the game as it begins. */
    public GameState(Team p1_team, Pokemon p2_active) {
    	/* Initialize variables. */
    	p2_pokemon = new HashMap<>();
    	/* Set up the current field. */
    	this.p1_team = p1_team;
    	this.p2_active = p2_active;
    	p2_pokemon.put(p2_active, new HashSet<>());
    }
   
    public GameState simulateTurn(Simulator.Action a1, Simulator.Action a2) {
    	GameState next = new GameState();
    	Pokemon t1 = null;

    	// Create a deep copy of player-one's team
    	Team next_team = new Team();
    	
    	for (Pokemon p : p1_team.pokemonList) {
    		Pokemon clone = p.clone();
    		next_team.pokemonList.add(clone);
    		if (p.species.equals(p1_team.activePokemon.species)) {
    			next_team.activePokemon = clone;
    		}
    		
    		// Change the action so that the user/targets are the clones
    		if (a1.getType() == Simulator.ActionType.SWITCH) {
    			Simulator.SwitchAction sw = (Simulator.SwitchAction)(a1);
    			if (p.species.equals(sw.switchTo.species)) {
    				sw.switchTo = clone;
    				t1 = p;
    			}
    		}
    		else if (a1.getType() == Simulator.ActionType.ATTACK) {
    			Simulator.AttackAction aa = (Simulator.AttackAction)(a1);
    			if (p.species.equals(aa.user.species)) {
    				aa.user = clone;
    				t1 = p;
    			}
    		}
    	}
    	next.p1_team = next_team;
    	
    	// Create a deep copy of player-two's known team
    	next.p2_pokemon = pass_on();
    	
    	for (Pokemon p : next.p2_pokemon.keySet()) {
    		if (p.species.equals(p2_active.species) || (p.status.transformed != null ? p.status.transformed.species.equals(p2_active.species) : false)) {
    			next.p2_active = p;
    		}
    		// Change the action so that the user/targets are the clones
    		if (a2.getType() == Simulator.ActionType.SWITCH) {
    			Simulator.SwitchAction sw = (Simulator.SwitchAction)(a2);
    			if (p.species.equals(sw.switchTo.species)) {
    				sw.switchTo = p;
    			}
    		}
    		else if (a2.getType() == Simulator.ActionType.ATTACK) {
    			Simulator.AttackAction aa = (Simulator.AttackAction)(a2);
    			if (p.species.equals(aa.user.species)) {
    				aa.user = p;
    			}
    		}
    	}
    	
    	// Execute the turn (analogous to the one in simulator)
		if (a1.getType() == Simulator.ActionType.SWITCH) {
			Simulator.SwitchAction s1 = (Simulator.SwitchAction)a1;
			Simulator.addMessage(p1_team.activePokemon.species + " was switched with " + s1.switchTo.species);
			
			// Switch
			next.p1_team.activePokemon.resetUponSwitch();
			next.p1_team.activePokemon = s1.switchTo;
		}
		if (a2.getType() == Simulator.ActionType.SWITCH) {
			Simulator.SwitchAction s2 = (Simulator.SwitchAction)a2;
			Simulator.addMessage(p2_active.species + " was switched with " + s2.switchTo.species);
			
			// Switch
			next.p2_active.resetUponSwitch();
			next.p2_active = s2.switchTo;
		}
		
		// Both moves are attack actions, so must compare speeds
		if(a1.getType() == Simulator.ActionType.ATTACK && a2.getType() == Simulator.ActionType.ATTACK) {
			Simulator.AttackAction aa1 = (Simulator.AttackAction)a1;
			Simulator.AttackAction aa2 = (Simulator.AttackAction)a2;
			
			// Relevant statistics needed for analyzing turn-order.
			int p1 = aa1.move.priority;
			int p2 = aa2.move.priority;
			int spd1 = aa1.user.modifiedStat(Pokemon.Stat.SPE);
			int spd2 = aa2.user.modifiedStat(Pokemon.Stat.SPE);
			
			/* Player 1's pokemon can attack first if and only if:
			 * 1) It's move is higher priority, or
			 * 2) It's move is not lower priority and it wins out on speed. */
			if (p1 > p2 || (p1 == p2 && ((spd1 > spd2) || (spd1 == spd2 && Math.random() < 0.5)))) {
				aa1.move.use(aa1.user, next.p2_active);
				if(aa1.deductPPIndex != -1) {
					aa1.user.pp[aa1.deductPPIndex]--;
				}
				if (next.p2_active.isAlive()) {
					// Attack
					aa2.move.use(aa2.user, next.p1_team.activePokemon);
					if(aa2.deductPPIndex != -1) {
						aa2.user.pp[aa2.deductPPIndex]--;
					}
				}
			}
			/* If none of the conditions above are satisifed, then player 2
			 * must attack first. */
			else {
				aa2.move.use(aa2.user, next.p1_team.activePokemon);
				if(aa2.deductPPIndex != -1) {
					aa2.user.pp[aa2.deductPPIndex]--;
				}
				
				
				if (next.p1_team.activePokemon.isAlive()) {
					aa1.move.use(aa1.user, next.p2_active);
					if(aa1.deductPPIndex != -1) {
						aa1.user.pp[aa1.deductPPIndex]--;
					}
				}
			}
		}
		/* If both actions aren't attack type, then one or less were. Check the
		 * two different actions and if either is attack, the other must have been
		 * switched, so attack immediately. */
		else {
			if (a1.getType() == Simulator.ActionType.ATTACK) {
				// Attack
				Simulator.AttackAction aa1 = (Simulator.AttackAction)a1;
				aa1.move.use(aa1.user, next.p2_active);
				
				if(aa1.deductPPIndex != -1) {
					aa1.user.pp[aa1.deductPPIndex]--;
				}
			}
			else if (a2.getType() == Simulator.ActionType.ATTACK){
				// Attack
				Simulator.AttackAction aa2 = (Simulator.AttackAction)a2;
				aa2.move.use(aa2.user, next.p1_team.activePokemon);
				
				if(aa2.deductPPIndex != -1) {
					aa2.user.pp[aa2.deductPPIndex]--;
				}
			}
		}
    	
		// Mechanics that take effect at the end of the turn
		
		// Apply poison/burn damage, reset counter damage
		
		for (Pokemon p : new Pokemon[] {next.p1_team.activePokemon, next.p2_active}) {
			
			// Apply transformation effects
			if (p.status.transformed != null) {
				if (p == next.p1_team.activePokemon) {
					next.p1_team.activePokemon = p.status.transformed;
				}
				else {
					next.p2_active = p.status.transformed;
				}
			}
			
			// Apply poison/burn damage
			if (p.isAlive()) {
				if (p.status.burn || p.status.poison) {
					p.currHp -= p.maxHp/16;
				}
				if (p.status.badly_poisoned_counter > 0) {
					p.currHp -= p.maxHp*p.status.badly_poisoned_counter/16;
					p.status.badly_poisoned_counter++;
				}
				
				// Reset counter damage
				p.status.counter_damage = 0;
			}
		}	

		if (a1.getType() == Simulator.ActionType.ATTACK) {
			Simulator.AttackAction aa = (Simulator.AttackAction)(a1);
			aa.user = t1;
		}
		else {
			Simulator.SwitchAction sa = (Simulator.SwitchAction)(a1);
			sa.switchTo = t1;
		}
    	return next;
    }

    /* Pass a deep copy of [p2_pokemon]. Intended to be used by successor game state. */
    private HashMap<Pokemon, HashSet<Move>> pass_on() {
    	HashMap<Pokemon, HashSet<Move>> ret = new HashMap<>();
		// For every key, re-insert the key,value into the new hash map.
		for (Pokemon p : p2_pokemon.keySet()) {
			Pokemon clone = p.clone();
			ret.put(clone, p2_pokemon.get(p));
		}

		return ret;
    }
    
    /**
     * Returns a list where the first element is a list of actions available to the player
     * and the second element is a list of actions available to the opponent (based on
     * which moves/pokemon we've seen the opponent use)
     */
    public ArrayList<ArrayList<Simulator.Action>> getPlayerAndOpponentActions() {
    	
    	// List representation of the opponent's team. 
    	ArrayList<Pokemon> rep = new ArrayList<>();

    	// Store original movesets in an array so we can set them back when we're done
    	Move[][] originalMoves = new Move[p2_pokemon.size()][];
    	Iterator<Pokemon> it = p2_pokemon.keySet().iterator();
    	int index = 0;
    	
    	/* Add every seen pokemon that was seen into the tree. */
    	while(it.hasNext()) {
    		Pokemon p = it.next();
    		originalMoves[index] = p.moves;
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
    			p.moves = new Move[4];
    			for (Move m : p2_pokemon.get(p)) {
    				p.moves[counter++] = m;
    			}
    			for (; counter < p.moves.length; counter++) {
    				p.moves[counter] = null;
    			}
    		}
    		rep.add(p);
    		index++;
    	}
    	
    	/* Swap the ordering of the list such that the first pokemon is the active pokemon. */
    	int active_index = rep.indexOf(p2_active);
    	if(active_index == -1) {
    		// They must have transformed
    		for(int i = 0; i < rep.size() && active_index == -1; i++)
    			if(rep.get(i).species.equals("ditto"))
    				active_index = i;
    	}
    	Pokemon tmp = rep.get(0);
    	rep.set(0, p2_active);
    	rep.set(active_index, tmp);
    	
    	
    	Team oppoTeam = new Team(rep);
		ArrayList<Simulator.Action> p1Actions = p1_team.getActions(oppoTeam.activePokemon.isAlive());
		ArrayList<Simulator.Action> oppoActions = oppoTeam.getActions(p1_team.activePokemon.isAlive());
    	
    	ArrayList<ArrayList<Simulator.Action>> actions = new ArrayList<ArrayList<Simulator.Action>>();
    	actions.add(p1Actions);
    	actions.add(oppoActions);
    	
    	// Restore original movesets of opposing pokemon
    	it = p2_pokemon.keySet().iterator();
    	index = 0;
    	while(it.hasNext()) {
    		Pokemon p = it.next();
    		p.moves = originalMoves[index];
    		index++;
    	}
    	
    	return actions;
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
    	double hpRemaining = 0;
    	double totalHp = 0;
    	
    	for (Pokemon p : p1_team.pokemonList) {
    		hpRemaining += p.currHp;
    		totalHp += p.maxHp;
    	}
    	
    	double p = hpRemaining/totalHp;
    	return proportionHpRemainingValue(p);
    }
    
    /**
     * Return the value of having p proportion of your
     * total hp remaining at the end of the game
     * 
     * Constraints:
     * this function f should 
     * - increase logarithmically
     * - f(0) = 0
     * - f(1) = 1
     * - f(1/6) = 1/2 -> this means ~1 of your pokemon is fully alive
     * 
     */
    private double proportionHpRemainingValue(double p) {
    	return Math.log(24*p+1)/Math.log(25);
    	
    }
}
