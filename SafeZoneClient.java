import java.io.*;
import java.net.*;
import java.util.*;

class SafeZoneClient {
    static int inPort = 9999;
    static String address = "localhost";
    static PrintWriter out;
    static BufferedReader in;
    static String userName = "Alice";
    static int num_mine = 10;
    static int width = 9;

    public static void main(String[] args) {
        int score = 0;
        String msg;
        boolean turn = true;

        try (Socket socket = new Socket(address, inPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Welcome!");
            out.println(userName);
            msg = in.readLine(); // wait message
            System.out.println(msg);
            msg = in.readLine(); // start message
            System.out.println(msg);

            while (score <= num_mine) {
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

        System.out.print("\n Enter x coordinate:");
        int x = scan.nextInt();
        while ((x < 0) || (x >= width)) {
            System.out.println(" Invalid x, enter a new x coordinate");
            x = scan.nextInt();
        }
        System.out.print(" Enter y coordinate:");
        int y = scan.nextInt();
        while ((y < 0) || (y >= width)) {
            System.out.println(" Invalid y, enter a new y coordinate");
            y = scan.nextInt();
        }

        System.out.println("wait for turn");
        out.println(x + "," + y);
        String msg = in.readLine();

        return msg;
    }
}
