import java.util.ArrayList;
import java.util.HashSet;

public class GameState {
    /* Representation of player-one's team of pokemon and movesets. */
    public Team p1_team;
    /* List of pokemon player one knows the opponent has and their statuses. The
     * moves of the pokemon in this list are purely random and are meaningless.
     * Moves which player-one knows player-two's pokemon have are stored in [playerTwoMoves].
     */
    public ArrayList<Pokemon> p2_pokemon;
    /* List of movesets which player-one knows for player-two's pokemon. Since the
     * game begins with unknown teams and movesets, this list is initially filled with
     * empty sets. If the i'th pokemon on player-two's uses a move never seen before,
     * this move is added to [playerTwoMoves[i]]. */
    public HashSet<Move>[] p2_moves;

    /* Player-two's active pokemon. */
    public static Pokemon p2_active;

    /* Default game state which describes the game as it begins. */
    public GameState(Team p1_team, Pokemon p2_active) {
	/* Set up the current field. */
	this.p1_team = p1_team;
	this.p2_active = p2_active;
	p2_pokemon.add(p2_active);
	/* Initialize the knowledge known about the opponents pokemon. */
	p2_moves = new HashSet[6];
	for (int i = 0; i < p2_moves.length; i++) {
	    p2_moves[i] = new HashSet<>();
	}
    }
    
}
