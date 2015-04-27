import java.util.Random;
import java.util.LinkedList;
import Jama.Matrix;
public class Ecosystem{
	private double[] solution;
	private double[][] constants;
	private double[] righthand;
	private int guessRange;
	private int maxGuess;
	public int eqCount;
	public int varCount;
	private boolean done = false;
	private int children = 4;
	private int lastGenAvg = -1;
	public double[] getSolution(){return this.solution;}
	public int getGuessRange(){/*if(lastGenAvg<0)*/ return guessRange; /*return lastGenAvg/30;*/}
	public int getMaxGuess(){return this.maxGuess;}
	private int generation = 0;
	public LinkedList<Evolver> beings;
	public Ecosystem(int eqcount, int varcount, int guessSpace, int walkDist){
		this.solution = new double[varcount];
		this.varCount = varcount;
		this.eqCount = eqcount;
		this.constants = new double[eqcount][varcount];
		double[][] solution2d = new double[varcount][1];
		for(int i = 0; i < eqCount; i++)
			for(int j = 0; j < varCount; j++){
				solution2d[j][0] = new Random().nextInt(guessSpace/10)-(guessSpace/20);
				this.constants[i][j] = new Random().nextInt(guessSpace)-(guessSpace/2);}
		Matrix jamaMatrix = new Matrix(constants);
		System.out.println("COEFFICIENT MATRIX:");jamaMatrix.print(2,0);
		Matrix jamaSolution = new Matrix(solution2d);
		Matrix rightVector = jamaMatrix.times(jamaSolution);
		System.out.println("RIGHT-HAND VECTOR:");rightVector.print(2,0);
		//Matrix jamaSolution = jamaMatrix.solve(rightVector);
		System.out.println("SOLUTION VECTOR:");jamaSolution.print(2,0);
		this.solution = jamaSolution.getColumnPackedCopy();
		this.righthand = rightVector.getColumnPackedCopy();
		prettyPrint(this.solution);
		//new Random().nextInt(guessSpace);
		this.guessRange = walkDist;
		this.maxGuess = guessSpace;
		this.beings = new LinkedList<Evolver>();
		//this.thresh=threshold;
		//this.shrink=threshold_shrinkage;
	}
	private static void prettyPrint(double[][] target){
		new Matrix(target).print(2,0);
	}
	private static void prettyPrint(double[] target){
		System.out.print("[");
		for(int i = 0; i < target.length; i++)
			System.out.print(target[i]+" ");
		System.out.println("]");
	}
	public int computeError(double[] sols){
		double errsum = 0;
		double eqsum = 0;
		for(int i = 0; i < eqCount; i++){
			eqsum = 0;
			for(int j = 0; j < varCount; j++)
				eqsum += this.constants[i][j]*sols[j];
			errsum += Math.abs(eqsum-this.righthand[i]);
		}
		return (int)errsum;	
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
		double[][] guesses = new double[this.beings.size()][this.varCount];
		Thread[] beingPool = new Thread[this.beings.size()];
		for(int i = 0; i < this.beings.size(); i++)
			beingPool[i] = new Thread(this.beings.get(i));
		for(int i = 0; i < this.beings.size(); i++){
			beingPool[i].run();
		}
		for(int i = 0; i < this.beings.size(); i++){
			try{beingPool[i].join();}
			catch(InterruptedException e){}
			guesses[i] = this.beings.get(i).doneguesses;
			guessSum += computeError(guesses[i]);
		}
		int guessAvgError = guessSum/this.beings.size();
		for(int i = 0; i < this.beings.size(); i++){
			double[] guess = guesses[i];
			if (computeError(guess) == 0){done=true; System.out.println("FOUND CORRECT ANSWER:");prettyPrint(guess);return;}
			if(computeError(guess)<guessAvgError){
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
		this.lastGenAvg = guessAvgError;
	}
}
