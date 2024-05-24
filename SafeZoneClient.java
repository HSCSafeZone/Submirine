import java.io.*;
import java.net.*;
import java.util.*;

class SafeZoneClient {
    static int inPort = 9999;
    static String address = "172.30.1.78";
    static PrintWriter out;
    static BufferedReader in;
    static String userName;
    static int num_mine = 10;
    static int width = 9;

    public static void main(String[] args) {
        int score = 0;
        String msg;
        boolean turn = true;

        try (Socket socket = new Socket(address, inPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            System.out.println("Enter your user name(letters & numbers only)...");
            Scanner scan = new Scanner(System.in);
            userName = scan.nextLine();
            out.println(userName);
            msg = in.readLine(); // "Welcome userName!"
            System.out.println(msg);
//            msg = in.readLine(); // start message
//            System.out.println(msg);
            
            while (score <= num_mine) {
            	//서버에서 "ok"를 내보내면 "enter x coordinate" 뜨게 하기 
                msg = guess();
                if (msg.equalsIgnoreCase("ok")) {
                    msg = in.readLine();
                    int result = Integer.parseInt(msg);
                    if (result >= 0) {
                        score++;
                        System.out.println("hit , score = " + score);
                    } else
                        System.out.println("miss , score = " + score);
                }

            }

            in.close();
            out.close();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String guess() throws IOException {
        Scanner scan = new Scanner(System.in);
        int x = -1;
        int y = -1;
        
        while ((x < 0) || (x >= width)) {
        	try {
		        System.out.print("\n Enter x coordinate: ");
		        x = scan.nextInt();
		        if (x < 0 || x >= width) {
		        	System.out.println("Invalid x, enter a new x coordinate");
		        }
        	} catch (InputMismatchException e) {
	            System.out.println("Invalid input, please enter an integer.");
	            scan.next(); // Clear the invalid input
        	}
        }
        while ((y < 0) || (y >= width)) {
        	try {
		        System.out.print(" Enter y coordinate: ");
		        y = scan.nextInt();
		        if (y < 0 || y >= width) {
		        	System.out.println("Invalid y, enter a new y coordinate");
		        }
        	} catch (InputMismatchException e) {
	            System.out.println("Invalid input, please enter an integer.");
	            scan.next(); // Clear the invalid input
        	}
        }

        System.out.println("wait for your turn");
        out.println(x + "," + y);
        String msg = in.readLine();

        return msg;
    }
}
