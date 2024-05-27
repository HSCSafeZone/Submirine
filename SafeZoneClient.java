import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

public class SafeZoneClient extends JFrame {
    static int inPort = 9999;
    static String address = "localhost";
    static PrintWriter out;
    static BufferedReader in;
    static String userName;
    static int num_mine = 10;
    static int width = 9;

    public SafeZoneClient() {
        connectGUI();
    }

    private void connectGUI() {
        JFrame c_frame = new JFrame("서버에 연결 중...");
        c_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        c_frame.setSize(300, 160);
        c_frame.setLocationRelativeTo(null);
        c_frame.setResizable(false);

        JPanel consolePanel = new JPanel();
        consolePanel.setLayout(new BoxLayout(consolePanel, BoxLayout.Y_AXIS));
        consolePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel addressPrompt = new JPanel();
        addressPrompt.setLayout(new BoxLayout(addressPrompt, BoxLayout.Y_AXIS));
        JLabel l_address = new JLabel("서버 주소");
        JTextField t_address = new JTextField(15);
        addressPrompt.add(l_address);
        addressPrompt.add(t_address);
        
        JPanel userNamePrompt = new JPanel();
        userNamePrompt.setLayout(new BoxLayout(userNamePrompt, BoxLayout.Y_AXIS));
        JLabel l_userName = new JLabel("사용자 이름(영문자 및 숫자만 허용)");
        JTextField t_userName = new JTextField(10);
        userNamePrompt.add(l_userName);
        userNamePrompt.add(t_userName);
          
//        a JLabel iconLabel = new JLabel(mineIcon);
//        promptPanel.add(iconLabel);

//        Color backgroundColor = new Color(238,238,238);
        JButton c_button = new JButton("연결");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(c_button);
        buttonPanel.add(Box.createHorizontalGlue());

        c_button.addActionListener(e -> {
            userName = t_userName.getText();
            address = t_address.getText();
            if (userName.matches("[a-zA-Z0-9]+")) {
                if (address.matches("^[0-9.]+$")) {
                    c_frame.dispose();
                    connectToServer();
                } else {
                    JOptionPane.showMessageDialog(c_frame, "잘못된 주소입니다. 숫자와 점(.)만 사용하세요.");
                }
            } else {
                JOptionPane.showMessageDialog(c_frame, "잘못된 사용자 이름입니다. 영문자와 숫자만 사용하세요.");
            }
        });
//        consolePanel.add(iconLabel);
        consolePanel.add(addressPrompt);
        consolePanel.add(Box.createVerticalStrut(5));
        consolePanel.add(userNamePrompt);
        consolePanel.add(Box.createVerticalStrut(10));
        consolePanel.add(buttonPanel);

//        c_frame.add(iconLabel);
        c_frame.getContentPane().add(consolePanel);
        c_frame.setVisible(true);
    }

    private void connectToServer() {
        try (Socket socket = new Socket(address, inPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(userName);

            while (true) {
                String msg = in.readLine();
                if (msg == null) {
                    System.out.println("서버와의 연결이 끊어졌습니다. 안녕히 가세요!");
                    break;
                }
                System.out.println(msg);

                if (msg.startsWith("당신의 차례")) {
                    msg = guess();
                    if (msg.equals("성공")) {
                        System.out.println("성공!");
                    } else {
                        System.out.println("실패!");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("오류: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private static String guess() throws IOException {
        Scanner scan = new Scanner(System.in);
        int x = -1;
        int y = -1;
        
        // 입력 유효성 검사 루프 생략
        
        System.out.println("차례를 기다리는 중");
        out.println(x + "," + y);
        String msg = in.readLine();

        return msg;
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            System.err.println("스트림 닫기 오류: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SafeZoneClient::new);
    }
}
