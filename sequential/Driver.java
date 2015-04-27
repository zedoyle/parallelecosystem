public class Driver{
	public static void main(String[] args){
		Ecosystem e = new Ecosystem(4,4,100, 10);
		for(int i = 0; i < 75; i++){
			e.addEvolver();
		}
		try{Thread.sleep(3000);}catch(Exception except){}
		e.generationLoop();
	}
}
