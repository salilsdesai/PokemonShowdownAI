
public class Ranking {
	
	public static class MatchResult {
		public int opponentElo;
		public int opponentGlickoRating;
		public int opponentGlickoDeviation;
		public boolean result;
		public MatchResult(int oie, int ogr, int ogd, boolean r) {
			opponentElo = oie;
			opponentGlickoRating = ogr;
			opponentGlickoDeviation = ogd;
			result = r;
		}
	}
	
	public static final MatchResult[] AIMatchResults = {
		new MatchResult(1070, 1537, 117, true),
		new MatchResult(1000, 1408, 112, false),
		new MatchResult(1450, 1847, 27, false),
		new MatchResult(1087, 1493, 56, true),
		new MatchResult(1000, 1500, 130, false),
		new MatchResult(1375, 1653, 25, false),
		new MatchResult(1000, 1405, 112, true),
		new MatchResult(1194, 1679, 39, false),
		new MatchResult(1371, 1665, 45, false),
		new MatchResult(1000, 1500, 130, true),
		new MatchResult(1000, 1500, 130, true),
		new MatchResult(1000, 1500, 130, true),
		new MatchResult(1091, 1412, 72, false),
		new MatchResult(1419, 1764, 25, true),
		new MatchResult(1356, 1716, 35, false),
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
	
	/**
	 * Compute the updated Glicko Rating and Glicko Deviation of a player based
	 * on opponent glicko rankings and deviations and match results 
	 * 
	 * First index of return is Rating, second index is deviation
	 * 
	 * Based on starting R = 1500, RD = 130 (said at below link)
	 * https://pokemonshowdown.com/pages/ladderhelp
	 * 
	 * Explanation Here:
	 * https://en.wikipedia.org/wiki/Glicko_rating_system
	 */
	public static int[] getUpdatedGlicko(int playerInitialGlickoRating, int playerInitialGlickoDeviation, MatchResult[] mr) {
		int RD = playerInitialGlickoDeviation; // all matches done in single 24 hour period so no decay
		int r0 = playerInitialGlickoRating;
		
		double q = Math.log(10)/400;
		double cumulativeSumInDSquared = 0;
		double cumulativeSumInR = 0;
		for(MatchResult m : mr) {
			double gRDi = 1/Math.sqrt(1 + (3*Math.pow(q*m.opponentGlickoDeviation/Math.PI, 2)));
			double EsrriRDi = 1/(1 + Math.pow(10, gRDi*(r0-m.opponentGlickoRating)/-400));
			int si = m.result ? 1 : 0;
			cumulativeSumInDSquared += (Math.pow(gRDi, 2)*EsrriRDi*(1-EsrriRDi));
			cumulativeSumInR += (gRDi*(si - EsrriRDi));
		}
		
		double dSquared = cumulativeSumInDSquared/(Math.pow(q, 2));
		
		int r = (int)(r0 + (q/(1/(Math.pow(RD, 2)) + 1/dSquared))*(cumulativeSumInR));
		int RDPrime = (int)(Math.sqrt(1/(1/(Math.pow(RD, 2)) + 1/dSquared)));
		
		return new int[] {r, RDPrime};
		
	}
	
	public static double gxe(int glickoRating, int glickoDeviation) {
		int R = glickoRating;
		int RD = glickoDeviation;
		return Math.round(10000 / (1 + Math.pow(10, (((1500 - R) * Math.PI / Math.sqrt(3 * Math.pow(Math.log(10),2) * Math.pow(RD, 2) + 2500 * (64 * Math.pow(Math.PI,2) + 147 * Math.pow(Math.log(10),2) )))))))/100.0;
	}
	
	public static void main(String[] args) {
		int elo = 1000;
		for(MatchResult mr : AIMatchResults) {
			elo = getUpdatedElo(elo, mr.opponentElo, mr.result);
		}
		System.out.println("Final Elo: " + elo);
		
		
		int[] glicko = getUpdatedGlicko(1500, 250, AIMatchResults);
		int glickoRating = glicko[0];
		int glickoDeviation = glicko[1];
		System.out.println("Glicko: " + glickoRating + " (" + glickoDeviation + ")");
		
		double gxe = gxe(glickoRating, glickoDeviation);
		System.out.println("GXE: " + gxe);
		
		
	}
	
}
