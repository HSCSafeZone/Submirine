import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class SafeZoneClient extends JFrame {
    private static final int SERVER_PORT = 9999;
    private static final int MAP_SIZE = 10;
    private JButton[][] buttons = new JButton[MAP_SIZE][MAP_SIZE];
    private boolean isMyTurn = false; // 클라이언트의 턴 여부를 나타내는 변수
    private JLabel statusLabel = new JLabel("연결 대기중...");
    private JTextArea statusTextArea = new JTextArea(5, 20); // 상태 메시지를 위한 JTextArea 추가
    private BufferedReader in;
    private PrintWriter out;
    private String userName;
    private String serverAddress = "localhost"; // Default to localhost

    public Container cont;
    public JPanel topPanel, gamePanel, statusPanel;
    public JLabel roundLabel, mineLabel, timerLabel;
    public JPopupMenu setupMenu;
    public int width = 10;  // 고정된 맵 크기
    public int num_mine = 10;  // 고정된 지뢰 수
    public int num_round = 1;
    public JButton[] buttonsFlat;
    public int UIwidth = 50;  // 가로 크기 조절 변수
    public int UIheight = 100;  // 세로 크기 조절 변수

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
        setTitle("지뢰찾기");
        setSize(400, 200); // 높이를 조정하여 맵 아래 공간 확보
        setLocation(400, 280);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cont = getContentPane();
        cont.setLayout(new BorderLayout());

        topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));
        topPanel.setBackground(Color.DARK_GRAY);

        roundLabel = new JLabel(num_round + "ROUND", SwingConstants.RIGHT);
        roundLabel.setFont(roundLabel.getFont().deriveFont(Font.BOLD));

        mineLabel = new JLabel("MINES: " + num_mine, SwingConstants.RIGHT);
        mineLabel.setFont(mineLabel.getFont().deriveFont(Font.BOLD));

        timerLabel = new JLabel("⏱️00:00", SwingConstants.CENTER);

        // SETUP
        JButton setupButton = new JButton("...");
        setupButton.addActionListener(e -> {
            setupMenu.show(setupButton, 0, setupButton.getHeight());
        });
        setupButton.setBackground(Color.LIGHT_GRAY);
        setupButton.setForeground(Color.WHITE);
        setupMenu = new JPopupMenu();

        JMenuItem First_option = new JMenuItem("설정1");
        setupMenu.add(First_option);
        First_option.addActionListener(e -> {
            setupButton.setText(First_option.getText());
            switchTurn(isMyTurn);
        });

        JMenuItem Second_option = new JMenuItem("설정2");
        setupMenu.add(Second_option);
        Second_option.addActionListener(e -> setupButton.setText(Second_option.getText()));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        leftPanel.add(roundLabel);
        centerPanel.add(mineLabel);
        rightPanel.add(timerLabel);
        rightPanel.add(setupButton);

        JPanel infoPanel = new JPanel(new GridLayout(1, 3));
        infoPanel.add(leftPanel);
        infoPanel.add(centerPanel);
        infoPanel.add(rightPanel);

        topPanel.add(infoPanel);
        cont.add(topPanel, BorderLayout.NORTH);

        gamePanel = new JPanel();
        gamePanel.setBackground(Color.DARK_GRAY);
        cont.add(gamePanel, BorderLayout.CENTER);

        createMapPanel();  // 프로그램 시작 시 바로 맵 생성

        startTimer();

        // 상태창 추가
        statusPanel = new JPanel(new BorderLayout()); // 상태 패널 추가 및 배치 관리자 설정
        statusPanel.setBackground(Color.LIGHT_GRAY);
        statusLabel = new JLabel("내 턴", SwingConstants.LEFT); // 초기 상태 "내 턴"으로 설정
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        // JTextArea 추가
        statusTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(statusTextArea);
        statusPanel.add(scrollPane, BorderLayout.SOUTH);

        cont.add(statusPanel, BorderLayout.SOUTH); // 상태 패널을 남쪽에 배치

        // statusLabel의 크기 조절
        Dimension statusLabelSize = new Dimension(UIwidth, UIheight); // 가로, 세로 크기 지정
        statusLabel.setPreferredSize(statusLabelSize); // 라벨의 크기를 설정

        pack();
        setVisible(true);
    }

    private void createMapPanel() {
        JPanel mapPanel = new JPanel(new GridLayout(MAP_SIZE, MAP_SIZE));
        buttons = new JButton[MAP_SIZE][MAP_SIZE];  // 2차원 배열 초기화
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                JButton button = new JButton();
                button.setActionCommand(i + "," + j); // 버튼에 좌표를 ActionCommand로 설정
                button.addActionListener(new MyActionListener1());
                buttons[i][j] = button;
                mapPanel.add(button);
            }
        }

        JPanel wrappedPanel = new JPanel(new BorderLayout());
        wrappedPanel.add(mapPanel, BorderLayout.CENTER);
        Border border = BorderFactory.createLineBorder(new Color(240, 240, 240), 50);
        wrappedPanel.setBorder(border);

        gamePanel.removeAll();
        gamePanel.setLayout(new GridLayout(1, 1));  // 단일 맵을 배치하기 위해 GridLayout 사용
        gamePanel.add(wrappedPanel);
        gamePanel.revalidate();
        gamePanel.repaint();
    }


    class MyActionListener1 implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!isMyTurn) return; // 내 턴이 아니면 클릭 무시

            JButton b = (JButton) e.getSource();
            String[] coordinates = b.getActionCommand().split(",");
            int x = Integer.parseInt(coordinates[0]);
            int y = Integer.parseInt(coordinates[1]);
            sendMove(x, y);  // 서버로 이동 전송
        }
    }

    private void switchTurn(boolean isMyTurn) {
        this.isMyTurn = isMyTurn; // 현재 턴 상태 업데이트
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < MAP_SIZE; i++) {
                for (int j = 0; j < MAP_SIZE; j++) {
                    buttons[i][j].setEnabled(isMyTurn);
                }
            }
            statusLabel.setText(isMyTurn ? "내 턴" : "상대 턴");
        });
    }

    private void startTimer() {
        long startTimer = System.currentTimeMillis();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTimer;
                int minutes = (int) (elapsed / 60000);
                int seconds = (int) ((elapsed / 1000) % 60);
                timerLabel.setText(String.format("⏱️%02d:%02d", minutes, seconds));
            }
        }, 0, 1000);
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
            if (line.startsWith("YOUR_TURN")) {
                switchTurn(true);
            } else if (line.startsWith("MOVE_OK") || line.startsWith("MOVE_FAIL")) {
                handleMoveResponse(line);
            } else if (line.startsWith("GAME_STARTED")) {
                statusLabel.setText("게임이 시작되었습니다!");
                switchTurn(false);
            } else if (line.startsWith("GAME_OVER")) {
                JOptionPane.showMessageDialog(null, "모든 지뢰가 찾아졌습니다. 게임 종료!");
                System.exit(0);
            }
        });
    }

    private void handleMoveResponse(String line) {
        String[] parts = line.split(" ");
        int score = Integer.parseInt(parts[1]);
        statusTextArea.append(parts[0].equals("MOVE_OK") ? "지뢰를 찾았습니다! 점수: " + score + "\n" : "지뢰가 아닙니다. 점수: " + score + "\n");
        switchTurn(false); // 턴을 상대방으로 넘김
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
