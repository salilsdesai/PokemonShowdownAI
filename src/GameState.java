import java.util.ArrayList;
import java.util.HashMap;

public class GameState {
    /* Representation of player-one's team of pokemon and movesets. */
    public Team p1_team;
    /* List of pokemon player-one knows the opponent has and their statuses. The
     * moves of the pokemon stored in the key are purely random and meaningless.
     * The value of each key stores the moves which player-one knows player-two's
     * pokemon can perform. If player-one has not seen a specific pokemon use a move,
     * then the value in the map will be an empty list. */
    public HashMap<Pokemon, ArrayList<Move>> p2_pokemon;

    /* Player-two's active pokemon. */
    public static Pokemon p2_active;

    /* Default game state which describes the game as it begins. */
    public GameState(Team p1_team, Pokemon p2_active) {
	/* Initialize variables. */
	p2_pokemon = new HashMap<>();
	/* Set up the current field. */
	this.p1_team = p1_team;
	this.p2_active = p2_active;
	p2_pokemon.put(p2_active, new ArrayList<>());
    }
}
