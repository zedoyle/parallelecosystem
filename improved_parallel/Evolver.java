import java.util.Random;

public class Evolver implements Runnable{
	public Ecosystem home;
	public double[] initguesses;
	public double[] doneguesses;
	public int error = -1;
	public boolean done = false;
	public Evolver(Ecosystem h){
		this.home=h;
		this.initguesses = new double[home.varCount];
		for(int i = 0; i < this.initguesses.length; i++){
			this.initguesses[i]=(new Random().nextInt(home.getMaxGuess()/20));
			this.initguesses[i]-=(new Random().nextInt(home.getMaxGuess()/5));
			if(new Random().nextBoolean())
				this.initguesses[i] = Math.floor(this.initguesses[i]);
			else
				this.initguesses[i] = Math.ceil(this.initguesses[i]);
	}}
	public Evolver(Evolver p1, Evolver p2){
		this.home=p1.home;
		this.initguesses = new double[home.varCount];
		for(int i = 0; i < this.initguesses.length; i++){
			this.initguesses[i] = (p1.initguesses[i]+p2.initguesses[i]);
			this.initguesses[i] /= 2;
			int randomOffset = (new Random().nextInt(p1.home.getGuessRange())-p1.home.getGuessRange()/2);
			this.initguesses[i] += randomOffset;
			if(new Random().nextBoolean())
				this.initguesses[i] = Math.floor(this.initguesses[i]);
			else
				this.initguesses[i] = Math.ceil(this.initguesses[i]);
			//System.out.println("VARIABLE #"+i+": averaged parents ("+p1.initguesses[i]+","+p2.initguesses[i]+") and added offset ("+randomOffset+") to get "+this.initguesses[i]+".");  
	}}
	public int computeError(){
		double errsum = 0;
		double eqsum = 0;
		for(int i = 0; i < home.eqCount; i++){
			eqsum = 0;
			for(int j = 0; j < home.varCount; j++)
				eqsum += home.constants[i][j]*doneguesses[j];
			errsum += Math.abs(eqsum-home.righthand[i]);
		}
		return (int)errsum;
	}
	private int computeErrorFromVector(double[] sols){
		double errsum = 0;
		double eqsum = 0;
		for(int i = 0; i < home.eqCount; i++){
			eqsum = 0;
			for(int j = 0; j < home.varCount; j++)
				eqsum += home.constants[i][j]*sols[j];
			errsum += Math.abs(eqsum-home.righthand[i]);
		}
		return (int)errsum;
	}
	public void run(){
		double[] finalguesses = initguesses.clone();
		int steps = 0;
		while(steps < home.getGuessRange()){
		for(int i = 0; i < this.initguesses.length; i++){
			double[] curguesses =finalguesses.clone();
			curguesses[i]++;
			if(computeErrorFromVector(curguesses)<computeErrorFromVector(finalguesses))
				finalguesses[i]++;
			else if(computeErrorFromVector(curguesses)>computeErrorFromVector(finalguesses))
				finalguesses[i]--;
			//else if(computeErrorFromVector(curguesses)==computeErrorFromVector(finalguesses))
				//System.out.println("changing variable "+i+" had no effect");
		}
		steps++;
		}
		this.doneguesses = finalguesses;
		this.error = computeError();
		this.done = true;
	}
}
