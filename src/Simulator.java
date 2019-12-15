import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Simulator {
    public enum ActionType {
        ATTACK, SWITCH;
    }

    public static interface Action {
        public ActionType getType();
    }

    public static class SwitchAction implements Action {
        @Override
        public ActionType getType() {
            return ActionType.SWITCH;
        }

        public Pokemon switchTo;

        public SwitchAction(Pokemon switchTo) {
            this.switchTo= switchTo;
        }

        @Override
        public String toString() {
            return "Switch to: " + switchTo.species;
        }
    }

    public static class AttackAction implements Action {
        @Override
        public ActionType getType() {
            return ActionType.ATTACK;
        }

        public Pokemon user;
        public Move move;
        /** The index in user's move list to deduct pp from, or -1 if no pp should be deducted */
        public int deductPPIndex;

        public AttackAction(Pokemon user, Move move) {
            this(user, move, -1);
        }

        public AttackAction(Pokemon user, Move move, int i) {
            this.user= user;
            this.move= move;
            this.deductPPIndex= i;
        }

        @Override
        public String toString() {
            return "Attack: " + Arrays.toString(new String[] { user.species, move.name });
        }
    }

    /** A message indicating what has happened over the past turn */
    public static String message;

    public static void addMessage(String s) {
//		System.out.println(s);
        if (message == null)
            message= s;
        else
            message+= "\n" + s;
    }

    public static Scanner input;

    /** Executes a single turn of battle given that player 1 selects action [a1] and player 2
     * selects action [a2]. As standard in pokemon battle, 'switch' moves are processed first. If
     * both players select to attack, then the battle pokemon with the higher speed stat will attack
     * first. If the speed stats are equal, then there is a one-half chance of either attacking
     * first.
     * 
     * Certain moves like 'quickattack' will have different speed priorities. Example: 'quickattack'
     * has priority +1, so it will go before any move with priority less than +1. */
    public static void executeTurn(Action a1, Action a2, Team t1, Team t2) {
        // If any actions are switch actions, execute them first.
        if (a1.getType() == ActionType.SWITCH) {
            SwitchAction s1= (SwitchAction) a1;
            addMessage(t1.activePokemon.species + " was switched with " + s1.switchTo.species);
            t1.activePokemon.resetUponSwitch();
            t1.activePokemon= s1.switchTo;
        }
        if (a2.getType() == ActionType.SWITCH) {
            SwitchAction s2= (SwitchAction) a2;
            addMessage(t2.activePokemon.species + " was switched with " + s2.switchTo.species);
            t2.activePokemon.resetUponSwitch();
            t2.activePokemon= s2.switchTo;
        }

        /* If both moves are attacking moves, compare speeds/priorities to
         * see who goes first. */
        if (a1.getType() == ActionType.ATTACK && a2.getType() == ActionType.ATTACK) {
            AttackAction aa1= (AttackAction) a1;
            AttackAction aa2= (AttackAction) a2;

            // Relevant statistics needed for analyzing turn-order.
            int p1= aa1.move.priority;
            int p2= aa2.move.priority;
            int spd1= aa1.user.modifiedStat(Pokemon.Stat.SPE);
            int spd2= aa2.user.modifiedStat(Pokemon.Stat.SPE);

            /* Player 1's pokemon can attack first if and only if:
             * 1) It's move is higher priority, or
             * 2) It's move is not lower priority and it wins out on speed. */
            if (p1 > p2 || (p1 == p2 && ((spd1 > spd2) || (spd1 == spd2 && Math.random() < 0.5)))) {
                aa1.move.use(aa1.user, t2.activePokemon);
                if (aa1.deductPPIndex != -1) {
                    aa1.user.pp[aa1.deductPPIndex]-- ;
                }
                if (t2.activePokemon.isAlive()) {
                    aa2.move.use(aa2.user, t1.activePokemon);
                    if (aa2.deductPPIndex != -1) {
                        aa2.user.pp[aa2.deductPPIndex]-- ;
                    }
                }
            }
            /* If none of the conditions above are satisifed, then player 2
             * must attack first. */
            else {
                aa2.move.use(aa2.user, t1.activePokemon);
                if (aa2.deductPPIndex != -1) {
                    aa2.user.pp[aa2.deductPPIndex]-- ;
                }
                if (t1.activePokemon.isAlive()) {
                    aa1.move.use(aa1.user, t2.activePokemon);
                    if (aa1.deductPPIndex != -1) {
                        aa1.user.pp[aa1.deductPPIndex]-- ;
                    }
                }
            }
        }
        /* If both actions aren't attack type, then one or less were. Check the
         * two different actions and if either is attack, the other must have been
         * switched, so attack immediately. */
        else {
            if (a1.getType() == ActionType.ATTACK) {
                AttackAction aa1= (AttackAction) a1;
                aa1.move.use(aa1.user, t2.activePokemon);
                if (aa1.deductPPIndex != -1) {
                    aa1.user.pp[aa1.deductPPIndex]-- ;
                }
            } else if (a2.getType() == ActionType.ATTACK) {
                AttackAction aa2= (AttackAction) a2;
                aa2.move.use(aa2.user, t1.activePokemon);
                if (aa2.deductPPIndex != -1) {
                    aa2.user.pp[aa2.deductPPIndex]-- ;
                }
            }
        }
    }

    /** Perform all necessary processes at the end of turns - Poison/Burn damage - reset mirror move
     * - reset counter damage */
    public static void endOfTurn(Team t1, Team t2) {

        // Apply poison/burn damage, reset counter damage
        for (Team t : new Team[] { t1, t2 }) {
            if (t.activePokemon.status.transformed != null)
                t.activePokemon= t.activePokemon.status.transformed;

            Pokemon p= t.activePokemon;

            if (p.isAlive()) {
                if (p.status.burn || p.status.poison) {
                    p.currHp-= p.maxHp / 16;
                    System.out
                        .println(p.species + " was hurt by " + (p.status.burn ? "burn" : "poison") +
                            "(" + (p.maxHp / 16) + ", " + p.currHp + "/" + p.maxHp + ")");
                }
                if (p.status.badly_poisoned_counter > 0) {
                    p.currHp-= p.maxHp * p.status.badly_poisoned_counter / 16;
                    System.out.println(p.species + " was hurt by badly poison (" +
                        (p.maxHp * p.status.badly_poisoned_counter / 16) + ", " + p.currHp + "/" +
                        p.maxHp + ")");
                    p.status.badly_poisoned_counter++ ;
                }
                p.status.counter_damage= 0;
            }

            if (!p.isAlive()) {
                System.out.println(p.species + " fainted");
            }
        }
    }

    /** "Get" an action choice out of a list of possible actions ex. Prompt for an action choice
     * using scanner Separating this into a separate method so we can replace it with random action
     * choice or action choice using neural network in the future without changing main simulator
     * loop */
    public static Action getActionChoice(ArrayList<Action> a) {
//		Choose random action
//		return a.get((int)(Math.random() * a.size()));

//		Prompt player using scanner
//		
        if (input == null)
            input= new Scanner(System.in);
        System.out.println("choose an action");
        for (int i= 0; i < a.size(); i++ ) {
            System.out.println("" + i + ": " + a.get(i));
        }
        int choice= input.nextInt();
        return a.get(choice);

    }

    public static void playFromFile() {
        // t1 is you, t2 is the AI
        Team t1= new Team(TeamGenerator.randomTeam());
        Team t2= new Team(TeamGenerator.randomTeam());

        GameState p2GameState= new GameState(t2, t1.activePokemon);

        int turn= 1;

        NeuralNet policyNet= new NeuralNet("PolicyNetwork/PolicyNetworkWeights.txt");

        while (t1.hasAlive() && t2.hasAlive()) {
            System.out.println(t1);
            System.out.println(t2);

            ArrayList<Action> p1Actions= t1.getActions(t2.activePokemon.isAlive());
            Action p1Action= getActionChoice(p1Actions);

            Action p2Action= MCTS.chooseMove(p2GameState, policyNet, null);

            Simulator.message= null;

            Simulator.addMessage("Turn #" + turn);
            executeTurn(p1Action, p2Action, t1, t2);

            // Update the game state
            if (p1Action.getType().equals(ActionType.SWITCH)) {
                p2GameState.update(t1.activePokemon, null);
            } else if (p1Action.getType().equals(ActionType.ATTACK)) {
                AttackAction aa= (AttackAction) p1Action;
                Move m= aa.move;
                if (m != null &&
                    !m.equals(Move.getMove("NOTHING")) &&
                    !m.equals(Move.getMove("RECHARGE")) &&
                    !m.equals(Move.getMove("STRUGGLE")))
                    p2GameState.update(null, m);
            }

            // Print and clear the current turn's message
            System.out.println(Simulator.message);
            Simulator.message= null;

            endOfTurn(t1, t2);
            turn++ ;
        }

    }

    /** TODO @IAN: Prompt user for information about the initialization of the game */
    public static GameState initializeGame() {
        return null;
    }

    /** TODO @IAN: Prompt user for what happened during the last turn Pass in a string with the
     * following structure:
     * 
     * P1 Health Loss [xx%]
     * 
     * P1 Status [Status]
     * 
     * P1 Alive [True/False]
     * 
     * P1 Swapped [New Pokemon if swapped, otherwise null]
     * 
     * P2 Health Loss [xx%]
     * 
     * P2 Status [Status]
     * 
     * P2 Alive [True/False]
     * 
     * P2 Swapped [New Pokemon if swapped, otherwise null]
     * 
     * P2 Previous Attack [Attack Name] */

    public static GameState updateTurn(GameState gs) {
        ArrayList<Object> updates= new ArrayList<>();
        Scanner reader= new Scanner(System.in); // Reading from System.in
        reader.useDelimiter("\n");
        System.out.println("Type in your turn updates: \n");
        //
        updates.add(reader.nextInt());
        updates.add(reader.next());
        updates.add(reader.nextBoolean());
        updates.add(reader.next());
        updates.add(reader.nextInt());
        updates.add(reader.next());
        updates.add(reader.nextBoolean());
        updates.add(reader.next());
        //
        reader.close();
        return null;
    }

    /** Assuming that you are P1, playLive will guide you through a game and tell you what actions
     * to do at every step! */
    public static void playLive() {

        GameState gs= initializeGame();

        NeuralNet policyNet= new NeuralNet("PolicyNetwork/PolicyNetworkWeights.txt");

        while (true) { // Just keep going until the user cancels the game & restarts

            // Figure out what action you should do in the given GameState
            Action p1Action= MCTS.chooseMove(gs, policyNet, null);

            // Tell the user what action to do by printing
            System.out.println(p1Action.toString());

            gs= updateTurn(gs);
        }
    }

    public static void main(String[] args) {
        // Either run a SIMULATION or play PokemonShowdown LIVE
        String gameType= "SIMULATION";
        //
        if (gameType == "SIMULATION") {
            playFromFile();
        } else if (gameType == "LIVE") {
            playLive();
        }

    }
}
