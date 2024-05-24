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

            System.out.println("Connected to server.");
            out.println(userName);  // Send user name to server

            // Server welcome message
            msg = in.readLine(); // Read welcome message from server
            System.out.println(msg);  // Display welcome message

            while (score < num_mine && (msg = in.readLine()) != null) {
                System.out.println(msg);  // Display messages from the server
                if (turn) {
                    msg = guess();
                    if ("OK".equalsIgnoreCase(msg)) {
                        int result = Integer.parseInt(in.readLine());
                        if (result == 1) {
                            score++;
                            System.out.println("Hit! Score: " + score);
                        } else {
                            System.out.println("Miss! Score remains: " + score);
                        }
                        turn = false;  // Toggle turn after a successful guess
                    }
                } else {
                    System.out.println("Waiting for your turn...");
                    turn = true;  // Toggle turn
                }
            }
            System.out.println("Game over. Your final score: " + score);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String guess() throws IOException {
        Scanner scan = new Scanner(System.in);
        int x, y;
        do {
            System.out.print("\nEnter x coordinate (0 to " + (width-1) + "): ");
            x = scan.nextInt();
        } while (x < 0 || x >= width);

        do {
            System.out.print("Enter y coordinate (0 to " + (width-1) + "): ");
            y = scan.nextInt();
        } while (y < 0 || y >= width);

        out.println(x + "," + y);
        return in.readLine();  // Read server's response (OK or not)
    }
}
