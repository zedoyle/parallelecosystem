import java.util.Random;
import java.util.LinkedList;
import Jama.Matrix;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
/**
  Singleton host class for the Evolvers.
  */
public class Ecosystem{

	/**some magic numbers*/
	
	//how many individual evolver instantiations
	//(in effect, a range of adjacent guesses at the solution)
	//ought to be in a generation, per core
	//e.g., on an 8-core system, an ecosystem will
	//attempt to converge toward 2000 evolvers
	private static int evolversPerCore = 250;

	//how many evolvers can be on each core
	//before the ecosystem reaches "carrying capacity"
	//and begins to severely curtail evolver
	//reproduction
	private static int maxPerCore = 1000;

	//how many evolvers are produced per coupling of
	//two random survivors of a generation
	//coupling is sexual but single-gendered
	//4 means exact replacement occurs at 
	//the ideal number of evolvers (see above)
	private static int children = 4;

	//for computing the ideal number of evolvers
	//we need to know how many cores we have to
	//work with
	private static int cores = Runtime.getRuntime().availableProcessors();

	private Executor biomes; 
	private double[] solution;
	public double[][] constants;
	public double[] righthand;
	private int guessRange;
	private int maxGuess;
	private double errorDeltas = 0;
	private boolean done = false;
	private long lastGenAvg = -1;
	public double[] getSolution(){return this.solution;}
	public int getGuessRange(){/*if(lastGenAvg<0)*/ return guessRange; /*return lastGenAvg/30;*/}
	public int getMaxGuess(){return this.maxGuess;}
	private int generation = 0;
	public LinkedList<Evolver> beings;
	public Ecosystem(int eqcount, int varcount, int guessSpace, int walkDist){
		//allocate space for the solution array
		this.solution = new double[varcount];
		//allocate space for coefficient array	
		this.constants = new double[eqcount][varcount];
		//allocate space for (psedo-transposed) solution vector
		double[][] solution2d = new double[varcount][1];
		//random up a matrix
		for(int i = 0; i < eqcount; i++)
			for(int j = 0; j < varcount; j++){
				solution2d[j][0] = new Random().nextInt(guessSpace/10)-(guessSpace/20);
				this.constants[i][j] = new Random().nextInt(guessSpace)-(guessSpace/2);}
		
		//set up JAMA matricies, calculate the right-hand matrix to give to the evolvers
		//along with the coefficients
		Matrix jamaMatrix = new Matrix(constants);
		Matrix jamaSolution = new Matrix(solution2d);
		Matrix rightVector = jamaMatrix.times(jamaSolution);

		//take them out of JAMA format		
		this.solution = jamaSolution.getColumnPackedCopy();
		this.righthand = rightVector.getColumnPackedCopy();
		
		//show them for our benefit
		System.out.println("COEFFICIENT MATRIX:");jamaMatrix.print(2,0);
		System.out.println("RIGHT-HAND VECTOR:");rightVector.print(2,0);
		System.out.println("SOLUTION VECTOR:");jamaSolution.print(2,0);
		
		//set our evolver activity level and vector space size
		this.guessRange = walkDist;
		this.maxGuess = guessSpace;
		
		//create a linked list of evolvers and a pool to handle them
		this.beings = new LinkedList<Evolver>();
		this.biomes = Executors.newWorkStealingPool();
	}
	private static void prettyPrint(double[][] target){
		new Matrix(target).print(2,0);
	}
	private static void prettyPrint(double[] target){
		System.out.print("[");
		for(int i = 0; i < target.length; i++)
			System.out.print(((int)target[i])+" ");
		System.out.println("]");
	}
	public void addEvolver(){
		this.beings.add(new Evolver(this));
	}
	public void addEvolver(Evolver e){
		this.beings.add(e);
	}
	public void generationLoop(){
		System.out.println("The correct answer is: ");prettyPrint(solution);
		while(this.beings.size() > 1 && !done){
			System.out.println("Running Generation "+generation);
			doGeneration();
			generation++;
			System.out.println(this.beings.size()+" Evolvers alive for next generation.");
			//try{Thread.sleep(1500);}catch(Exception e){System.err.println("SOMETHING BROKE");}
		}
		if(done) System.out.println("Ran for "+generation+" Generations. All done, exiting.");
		else System.out.println("All evolvers dead after Generation #"+generation+", unable to continue.");
	}
	public void doGeneration(){
		LinkedList<Evolver> survivors = new LinkedList<Evolver>();
		int guessSum = 0;
		double[][] guesses = new double[this.beings.size()][this.constants[0].length];
		for(int i = 0; i < this.beings.size(); i++){
			biomes.execute(beings.get(i));
		}
		for(int i = 0; i < this.beings.size(); i++){
			while(!this.beings.get(i).done){Thread.yield();}	
			guesses[i] = this.beings.get(i).doneguesses;
			guessSum += this.beings.get(i).error;
		}
		System.out.println("Done computing, time to tally up...");
		long guessAvgError = /*this.beings.get(this.beings.size()/2).error;*/guessSum/this.beings.size();
		double fertility = Math.pow((Math.log(cores*evolversPerCore) / Math.log(this.beings.size())),0.5);
		System.out.println("Fertility factor for this round: "+fertility);
		double modifiedThreshold = guessAvgError*fertility;
		double capacityOverflow = this.beings.size()- (cores*maxPerCore);
		if(capacityOverflow>0){
			System.out.println("TOO MANY EVOLVERS ("+((int)capacityOverflow)+"), CULLING");
			modifiedThreshold *= 0.4;
		}
		for(int i = 0; i < this.beings.size(); i++){
			double[] guess = guesses[i];
			int error = this.beings.get(i).error;
			if (error == 0){done=true; System.out.println("FOUND CORRECT ANSWER:");prettyPrint(guess);return;}
			if(error<modifiedThreshold){
				survivors.add(this.beings.get(i));
				//System.out.println("valid guess (error="+computeError(guess)+"):");
				//prettyPrint(this.beings.get(i).initguesses);
				//System.out.println("      \\/");
				//prettyPrint(guesses[i]);
				//System.out.println("Guess at "+solution+" is "+this.beings.get(i).initguess+"->"+guess+" (+/-"+Math.abs(guess-answer)+" away)");
			}
		}
		LinkedList<Evolver> next = new LinkedList<Evolver>();
		for(int i = 0; i < survivors.size()/2; i++){
			for(int j = 0; j < this.children; j++){
				next.add(new Evolver(survivors.get(i),survivors.get(i+(survivors.size()/2))));
			}
		}
		this.beings = next;
		System.out.print("Average error for this generation was: "+guessAvgError+".");
		double errDelt = (Math.floor((((double)(guessAvgError-this.lastGenAvg))/((double)this.lastGenAvg))*10000)/100);
		if(this.lastGenAvg!=-1) System.out.print(" (last gen: "+this.lastGenAvg+", net change: "+
			errDelt	
		+"%)");
		this.errorDeltas += errDelt;
		System.out.print('\n');
		if(this.generation>0)System.out.println("Average change: "+(this.errorDeltas/this.generation)+"%");
		this.lastGenAvg = guessAvgError;
	}
}
