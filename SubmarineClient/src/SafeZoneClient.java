import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class SafeZoneClient extends JFrame {
    private static final int SERVER_PORT = 9999;
    private static final int MAP_SIZE = 10;
    private JButton[][] buttons = new JButton[MAP_SIZE][MAP_SIZE];
    private boolean myTurn = false;
    private JLabel statusLabel = new JLabel("연결 대기중...");
    private BufferedReader in;
    private PrintWriter out;
    private String userName;
    private String serverAddress = "localhost"; // Default to localhost

    public SafeZoneClient() {
        connectGUI(); // Start with the connection GUI
    }

    private void connectGUI() {
        JFrame connectFrame = new JFrame("서버 연결 설정");
        connectFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connectFrame.setSize(300, 160);
        connectFrame.setLocationRelativeTo(null);
        connectFrame.setResizable(false);

        JPanel consolePanel = new JPanel();
        consolePanel.setLayout(new BoxLayout(consolePanel, BoxLayout.Y_AXIS));
        consolePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Address input
        JPanel addressPanel = new JPanel();
        addressPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel lAddress = new JLabel("서버 주소: ");
        JTextField tAddress = new JTextField(15);
        tAddress.setText(serverAddress);
        addressPanel.add(lAddress);
        addressPanel.add(tAddress);

        // User name input
        JPanel userNamePanel = new JPanel();
        userNamePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel lUserName = new JLabel("사용자 이름: ");
        JTextField tUserName = new JTextField(15);
        userNamePanel.add(lUserName);
        userNamePanel.add(tUserName);

        // Connect button
        JButton cButton = new JButton("연결");
        cButton.addActionListener(e -> {
            serverAddress = tAddress.getText().isEmpty() ? "localhost" : tAddress.getText();
            userName = tUserName.getText();
            if (userName.matches("[a-zA-Z0-9]+")) {
                connectFrame.dispose();
                createAndShowGUI(); // Initialize the game GUI after connection
                new Thread(this::connectToServer).start(); // Run server connection in a separate thread
            } else {
                JOptionPane.showMessageDialog(connectFrame, "잘못된 사용자 이름입니다. 영문자와 숫자만 사용하세요.");
            }
        });

        consolePanel.add(addressPanel);
        consolePanel.add(userNamePanel);
        consolePanel.add(cButton);

        connectFrame.getContentPane().add(consolePanel);
        connectFrame.setVisible(true);
    }

    private void createAndShowGUI() {
        JFrame gameFrame = new JFrame("Safe Zone: 지뢰 찾기 게임");
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setSize(500, 550);
        gameFrame.setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(MAP_SIZE, MAP_SIZE));
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(50, 50));
                int finalI = i;
                int finalJ = j;
                button.addActionListener(e -> {
                    if (myTurn && button.isEnabled()) {
                        sendMove(finalI, finalJ);
                    }
                });
                buttons[i][j] = button;
                boardPanel.add(button);
            }
        }

        gameFrame.add(boardPanel, BorderLayout.CENTER);
        gameFrame.add(statusLabel, BorderLayout.SOUTH);

        gameFrame.setVisible(true);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(serverAddress, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(userName); // Send user name to server

            String line;
            while ((line = in.readLine()) != null) {
                processServerMessage(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "서버 연결 실패: " + e.getMessage()));
        }
    }

    private void processServerMessage(String line) {
        SwingUtilities.invokeLater(() -> {
            if (line.startsWith("TURN")) {
                myTurn = line.substring(5).equals(userName);
                statusLabel.setText(myTurn ? "당신의 차례입니다." : "상대 플레이어의 차례입니다.");
            } else if (line.startsWith("UPDATE")) {
                updateBoard(line.substring(7));
            } else if (line.startsWith("GAME_STARTED")) {
                statusLabel.setText("게임이 시작되었습니다!");
            } else if (line.startsWith("WINNER")) {
                JOptionPane.showMessageDialog(null, line.substring(7));
            } else if (line.startsWith("MESSAGE")) {
                JOptionPane.showMessageDialog(null, line.substring(8));
            }
        });
    }

    private void sendMove(int x, int y) {
        out.println("MOVE " + x + " " + y);
    }

    private void updateBoard(String data) {
        SwingUtilities.invokeLater(() -> {
            String[] rows = data.split(";");
            for (int i = 0; i < MAP_SIZE; i++) {
                String[] cells = rows[i].split(",");
                for (int j = 0; j < MAP_SIZE; j++) {
                    buttons[i][j].setText(cells[j]);
                    buttons[i][j].setEnabled(cells[j].equals(""));
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SafeZoneClient::new);
    }
}

