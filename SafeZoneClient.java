import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;

class SafeZoneClient extends JFrame {
    static int inPort = 9999;
    static String address = "172.30.1.78";
    static PrintWriter out;
    static BufferedReader in;
    static String userName;
    static int num_mine = 10;
    static int width = 10;    

    public SafeZoneClient() {
    	connectGUI();
    }
    
    private void connectGUI() {
    	JFrame c_frame = new JFrame("Connecting...");
        c_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        c_frame.setSize(300, 150);
        c_frame.setResizable(false);
        
        JPanel consolePanel = new JPanel();
        consolePanel.setLayout(new BoxLayout(consolePanel, BoxLayout.Y_AXIS));
        consolePanel.setBorder(BorderFactory.createEmptyBorder(30, 10, 10, 10));
        
        JLabel c_prompt = new JLabel("Enter your user name (letters & numbers only)");
        
        JPanel promptPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // 왼쪽 정렬을 위한 새 패널
        promptPanel.add(c_prompt);
        
        JTextField c_textField = new JTextField();
        c_textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, c_textField.getPreferredSize().height));
        
        JButton c_button = new JButton("Connect");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue()); // 버튼 앞에 수평 glue 추가
        buttonPanel.add(c_button);
        buttonPanel.add(Box.createHorizontalGlue()); // 버튼 뒤에 수평 glue 추가

        c_button.addActionListener(e -> {
            userName = c_textField.getText();
            if (userName.matches("[a-zA-Z0-9]+")) {
                c_frame.dispose();
                connectToServer();	
            } else {
                JOptionPane.showMessageDialog(c_frame, "Invalid username. Please use letters and numbers only.");
            }
        });
        
        consolePanel.add(promptPanel);
        consolePanel.add(c_textField);
        consolePanel.add(buttonPanel);

        c_frame.getContentPane().add(consolePanel);
        c_frame.setVisible(true);
    }
    
    private void prepareGUI() {	//도현
        setTitle("Safe Zone Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    } 
    
    private void connectToServer() {  
        int score = 0;
        String msg;
        boolean turn = true;

        try (Socket socket = new Socket(address, inPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
           
            out.println(userName); //사용자 이름 전송
            
            prepareGUI(); //도현
            
            msg = in.readLine(); // "Welcome userName!"
            System.out.println(msg);

            
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
    
    
        

    private static String guess() throws IOException {
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
    public static void main(String[] args) {
    	SwingUtilities.invokeLater(SafeZoneClient::new);
    }
    }
