import java.util.ArrayList;

public class Pokemon {
	public String species;
	public ArrayList<Type> types;
	public ArrayList<Move> moves;
	public ArrayList<Integer> pp; /** pp[i] is the current pp of moves[i] **/
	public int level, maxHp, atk, def, spc, spe, currHp;
	
	
	//TODO: status effects ex. sleep, substitute
	//TODO: stat modifications ex. atk down, def up
}
