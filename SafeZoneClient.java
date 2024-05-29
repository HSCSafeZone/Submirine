import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.net.Socket;

public class SafeZoneClient extends JFrame {
    private static final int SERVER_PORT = 9999;
    private static final int MAP_SIZE = 10;
    private JButton[][] buttons = new JButton[MAP_SIZE][MAP_SIZE];
    private boolean isMyTurn = false; // 클라이언트의 턴 여부를 나타내는 변수
    private JLabel statusLabel = new JLabel("연결 대기중...");
    private JTextArea statusTextArea = new JTextArea(5, 20); // 상태 메시지를 위한 JTextArea 추가
    private boolean myTurn = false;
    private BufferedReader in;
    private PrintWriter out;
    private String userName;
    private String serverAddress = "localhost";

    public int size = 10;
    public int num_mine = 10;
    public int num_try = 0;
    public int num_round = 1;
    public Container cont;
    public JPanel topPanel, gamePanel, statusPanel;
    public JLabel roundLabel, mineLabel, timerLabel, tryLabel;
    public JTextArea playerStatus;
    public JTextField chatField;
    public JPopupMenu setupMenu;
    public JButton[] mapButtons;

    // 전체 실행 부
    public SafeZoneClient() {
    	connectGUI();
//    	gameMapGUI();
    }

    // 연결 GUI
    private void connectGUI() {
        JFrame connectFrame = new JFrame("서버 연결 설정");
        connectFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connectFrame.setSize(300, 160);
        connectFrame.setLocationRelativeTo(null);
        connectFrame.setResizable(false);

        JPanel consolePanel = new JPanel();
        consolePanel.setLayout(new BoxLayout(consolePanel, BoxLayout.Y_AXIS));
        consolePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // 주소 입력
        JPanel addressPanel = new JPanel();
        addressPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel lAddress = new JLabel("서버 주소: ");
        JTextField tAddress = new JTextField(15);
        tAddress.setText(serverAddress);
        addressPanel.add(lAddress);
        addressPanel.add(tAddress);

        // 사용자 이름 입력
        JPanel userNamePanel = new JPanel();
        userNamePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel lUserName = new JLabel("사용자 이름: ");
        JTextField tUserName = new JTextField(15);
        userNamePanel.add(lUserName);
        userNamePanel.add(tUserName);

        // 연결 버튼
        JButton cButton = new JButton("연결");
        cButton.addActionListener(e -> {
            serverAddress = tAddress.getText().isEmpty() ? "localhost" : tAddress.getText();
            userName = tUserName.getText();
            if (userName.matches("[a-zA-Z0-9]+")) {
                connectFrame.dispose();
                new Thread(this::connectToServer).start(); // Run server connection in a separate thread
                matchingGUI();
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

    // 매칭 대기화면 GUI(현재는 자동화. 추후에 서버랑 연결보고 수정 필요)
    private void matchingGUI() {
        JFrame matchingFrame = new JFrame("매칭 대기");
        matchingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        matchingFrame.setSize(300, 160);
        matchingFrame.setLocationRelativeTo(null);
        matchingFrame.setResizable(false);

        JLabel matchingLabel = new JLabel("매칭 중...");
        matchingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        matchingLabel.setVerticalAlignment(SwingConstants.CENTER);
        matchingFrame.add(matchingLabel, BorderLayout.CENTER);

        matchingFrame.setVisible(true);

        new Thread(new Runnable() {
            private int dotCount = 1;
            private int elapsedTime = 0;
            public void run() {
                try {
                    while (elapsedTime < 3000) { 
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                matchingLabel.setText("매칭 중" + ".".repeat(dotCount));
                            }
                        });
                        dotCount++;
                        if (dotCount > 3) dotCount = 1;
                        Thread.sleep(500); 
                        elapsedTime += 500;
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            matchingLabel.setText("매칭 성공");
                        }
                    });
                    Thread.sleep(3000);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                        	gameMapGUI();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    // 게임 맵 GUI
    private void gameMapGUI() {
    	setTitle("지뢰찾기");
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    	cont = getContentPane();
    	cont.setLayout(new BorderLayout());

    	createTopPanel();

    	gamePanel = new JPanel();
    	gamePanel.setBackground(Color.DARK_GRAY);
    	cont.add(gamePanel, BorderLayout.CENTER);
    	createMapPanel();
    	
    	createBottomPanel();

    	setExtendedState(JFrame.MAXIMIZED_BOTH);
    	setVisible(true);
    }
    
    // 게임GUI 상단(상태창)
    private void createTopPanel() {
        topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));
        topPanel.setBackground(Color.DARK_GRAY);

        roundLabel = new JLabel(num_round + "ROUND", SwingConstants.RIGHT);
        roundLabel.setFont(roundLabel.getFont().deriveFont(Font.BOLD));

        mineLabel = new JLabel("MINES: " + num_mine, SwingConstants.RIGHT);
        mineLabel.setFont(mineLabel.getFont().deriveFont(Font.BOLD));
        tryLabel = new JLabel("TRY: " + num_try, SwingConstants.RIGHT);
        tryLabel.setFont(tryLabel.getFont().deriveFont(Font.BOLD));

        timerLabel = new JLabel("⏱️00:00", SwingConstants.CENTER);

        JButton setupButton = new JButton("...");
        setupButton.addActionListener(e -> {
            setupMenu.show(setupButton, 0, setupButton.getHeight());
        });
        setupButton.setBackground(Color.LIGHT_GRAY);
        setupButton.setForeground(Color.WHITE);
        setupMenu = new JPopupMenu();

        JMenuItem First_option = new JMenuItem("턴 바꾸기");
        setupMenu.add(First_option);
        First_option.addActionListener(e -> {
            setupButton.setText("...");
            switchTurn();
        });

        JMenuItem Second_option = new JMenuItem("게임종료");
        setupMenu.add(Second_option);
        Second_option.addActionListener(e -> {
            setupButton.setText("...");
            int response = JOptionPane.showConfirmDialog(null, "게임을 종료하시겠습니까?", "게임 종료", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                disableAllButtons();
                System.exit(0);
            }
        });

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        leftPanel.add(roundLabel);

        centerPanel.add(mineLabel);
        centerPanel.add(tryLabel);

        rightPanel.add(timerLabel);
        rightPanel.add(setupButton);

        JPanel infoPanel = new JPanel(new GridLayout(1, 3));
        infoPanel.add(leftPanel);
        infoPanel.add(centerPanel);
        infoPanel.add(rightPanel);

        topPanel.add(infoPanel);
        cont.add(topPanel, BorderLayout.NORTH);
    }

    // 게임GUI 중단(맵)
    private void createMapPanel() {
    	JPanel mapPanel = new JPanel(new GridLayout(size, size));
        mapButtons = new JButton[size * size];
        for (int i = 0; i < size * size; i++) {
            mapButtons[i] = new JButton("" + i);
            mapButtons[i].setEnabled(true);
            mapButtons[i].addActionListener(new PlayerDetect());
            mapPanel.add(mapButtons[i]);
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
    
    // 게임GUI 하단(채팅창)
    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // 채팅창
        playerStatus = new JTextArea(8, 60);
        playerStatus.setFont(new Font("Monospaced", Font.PLAIN, 12));
        playerStatus.setEditable(false);
        playerStatus.setBackground(new Color(200, 200, 200));
        playerStatus.setForeground(new Color(0, 0, 0));
        JScrollPane statusScrollPane = new JScrollPane(playerStatus);
        statusScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 채팅 입력란
        chatField = new JTextField(60);
        chatField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        chatField.setBackground(Color.WHITE);
        chatField.setForeground(Color.BLACK);
        chatField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = chatField.getText();
                sendMessage(message);
                chatField.setText("");
            }
        });

        bottomPanel.add(statusScrollPane, BorderLayout.NORTH);
        bottomPanel.add(chatField, BorderLayout.SOUTH);

        cont.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    
    // >>>내부 기능

    // 턴 관리
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
    
    // 플레이어 맵 클릭
    class PlayerDetect implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JButton b = (JButton) e.getSource();
            b.setText("❌");
            b.setEnabled(false);
            b.setBackground(Color.DARK_GRAY);
            
            num_try++;
            tryLabel.setText("TRY: " + num_try);

            switchTurn();
        }
    }

    // 턴 변경
 // 턴 변경 메소드
    private void switchTurn() {
        isMyTurn = !isMyTurn; // 턴 상태를 반전시킵니다.

        // GUI 업데이트를 위한 SwingUtilities.invokeLater 사용
        SwingUtilities.invokeLater(() -> {
            updateButtonStates(); // 버튼 활성화/비활성화 업데이트
            updateStatusLabel();  // 상태 라벨 업데이트
        });
    }

    // 모든 버튼의 활성화 상태를 현재 턴에 따라 설정
    private void updateButtonStates() {
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                buttons[i][j].setEnabled(isMyTurn && isButtonEnabled(i, j)); // 추가 조건을 체크하여 활성화
            }
        }
    }

    // 상태 라벨 업데이트 메소드
    private void updateStatusLabel() {
        if (isMyTurn) {
            statusLabel.setText("당신의 차례입니다.");
        } else {
            statusLabel.setText("상대방의 차례입니다.");
        }
    }

    // 버튼이 활성화 될 수 있는지 여부를 결정하는 메소드 (예: 버튼이 빈 상태인 경우 등)
    private boolean isButtonEnabled(int i, int j) {
        // 예를 들어 버튼이 빈 상태(아직 선택되지 않은 상태)일 때만 true를 반환하도록 설정
        // 이 부분은 게임의 로직에 따라 다를 수 있으므로 적절히 조정 필요
        return buttons[i][j].getText().equals("");
    }

    
   // 모든 버튼을 비활성화하는 메소드
    private void disableAllButtons() {
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                buttons[i][j].setEnabled(false);
            }
        }
    }


    // 타이머 가동
    private void startTimer() {
        long startTimer = System.currentTimeMillis();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
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
                isMyTurn = true;  // 내 턴으로 설정
                switchTurn();
            } else if (line.startsWith("MOVE_OK") || line.startsWith("MOVE_FAIL")) {
                handleMoveResponse(line);
            } else if (line.startsWith("GAME_STARTED")) {
                statusLabel.setText("게임이 시작되었습니다!");
                isMyTurn = false;  // 게임 시작 시에 상대방 턴으로 설정
                switchTurn();
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
        switchTurn(); // 턴을 상대방으로 넘김
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
    // 채팅창 업데이트
    private void sendMessage(String message) {
        playerStatus.append(message + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SafeZoneClient::new);
    }
}