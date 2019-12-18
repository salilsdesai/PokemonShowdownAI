
public class Ranking {
	
	public static class MatchResult {
		public int opponentInitialElo;
		public boolean result;
		public MatchResult(int oie, boolean r) {
			opponentInitialElo = oie;
			result = r;
		}
	}
	
	public static final MatchResult[] AIMatchResults = {
		new MatchResult(1070, true),
		new MatchResult(1000, false),
		new MatchResult(1450, false),
		new MatchResult(1087, true),
		new MatchResult(1000, false),
		new MatchResult(1375, false),
		new MatchResult(1000, true),
		new MatchResult(1194, false),
		new MatchResult(1371, false),
		new MatchResult(1000, true),
		new MatchResult(1000, true),
		new MatchResult(1000, true),
		new MatchResult(1091, false),
		new MatchResult(1419, true),
		new MatchResult(1356, false),
	};
	
	/**
	 * Compute the updated ELO ranking of a player based on a match against an opponent
	 * with [opponentInitialElo] ELO ranking
	 * 
	 * [result] being true means that they won, and false means that they lost
	 * 
	 * Formula From:
	 * http://bzstats.strayer.de/bzinfo/elo
	 * Which is linked to by this thread on the showdown forums
	 * https://www.smogon.com/forums/threads/everything-you-ever-wanted-to-know-about-ratings.3487422/
	 * 
	 * K factor determined by explanation on Showdown site
	 * https://pokemonshowdown.com/pages/ladderhelp
	 * 
	 */
	public static int getUpdatedElo(int playerInitialElo, int opponentInitialElo, boolean result) {
		
		double K;
		if(playerInitialElo == 1000)
			if (result)
				K = 80;
			else
				K = 20;
		else if (playerInitialElo < 1100)
			if (result)
				K = (80+50)/2; // Use the average where the showdown page says "between"
			else
				K = (50+20)/2;
		else if (playerInitialElo < 1300)
			K = 50;
		else if (playerInitialElo < 1600)
			K = 40;
		else
			K = 32;
		
		double S_A = (result ? 1 : 0);
		
		double E_A = 1/(1+Math.pow(10,((opponentInitialElo - playerInitialElo)/ 400)));
		return (int)(playerInitialElo + Math.round(K * (S_A-E_A)));
	}
	
	public static void main(String[] args) {
		int elo = 1000;
		for(MatchResult mr : AIMatchResults) {
			elo = getUpdatedElo(elo, mr.opponentInitialElo, mr.result);
		}
		System.out.println("Final Elo: " + elo);
	}
	
}
