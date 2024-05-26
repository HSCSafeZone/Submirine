import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class SafeZoneClient extends JFrame {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 9999;
    private static final int MAP_SIZE = 10;
    private JButton[][] buttons = new JButton[MAP_SIZE][MAP_SIZE];
    private boolean myTurn = false;
    private JLabel statusLabel = new JLabel("연결 대기중...");
    private BufferedReader in;
    private PrintWriter out;
    private String userName;

    public SafeZoneClient() {
        createAndShowGUI();
        connectToServer();
    }

    private void createAndShowGUI() {
        setTitle("Safe Zone: 지뢰 찾기 게임");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 550);
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(MAP_SIZE, MAP_SIZE));

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

        add(boardPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            userName = JOptionPane.showInputDialog(this, "Enter your name:");
            out.println(userName);  // Send user name to server

            // Read messages from the server and process them
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("TURN")) {
                    myTurn = line.substring(5).equals(userName);
                    statusLabel.setText(myTurn ? "당신의 차례입니다." : "상대 플레이어의 차례입니다.");
                } else if (line.startsWith("UPDATE")) {
                    updateBoard(line.substring(7));
                } else if (line.startsWith("GAME_STARTED")) {
                    statusLabel.setText("게임이 시작되었습니다!");
                } else if (line.startsWith("WINNER")) {
                    JOptionPane.showMessageDialog(this, line.substring(7));
                    break;
                } else if (line.startsWith("MESSAGE")) {
                    JOptionPane.showMessageDialog(this, line.substring(8));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "서버 연결 실패: " + e.getMessage());
        }
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
