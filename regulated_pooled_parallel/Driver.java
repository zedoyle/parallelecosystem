import java.util.Scanner;
public class Driver{
	public static void main(String[] args){
		Ecosystem e = new Ecosystem(6,6,500, 50);
		for(int i = 0; i < 8000; i++){
			e.addEvolver();
		}
		//try{Thread.sleep(3000);}catch(Exception except){}
		new Scanner(System.in).nextLine();
		e.generationLoop();
		new Scanner(System.in).nextLine();
	}
}
