import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;
import java.net.URL;

public class SafeZoneClient extends JFrame {
    private static final int SERVER_PORT = 9999;
    private static final int MAP_SIZE = 10;
    private JButton[][] buttons = new JButton[MAP_SIZE][MAP_SIZE];
    private BufferedReader in;
    private PrintWriter out;
    private String userName;
    private String serverAddress = "localhost";
    private boolean isMyTurn = false;
    
    public int size=10,  num_mine=0,  num_try=0,  num_point=0;
    public JPanel mapPanel, topPanel, gamePanel, statusPanel;
    public JLabel mineLabel, timerLabel, tryLabel, scoreLabel;    
    public int successfulDetections = 0, totalDetections = 0, failedDetections = 0, totalPlayTime = 0;
    public JTextField mineField, tryField, scoreField, chatField;
    public JPopupMenu setupMenu;
    public JButton[] mapButtons;
    public TimerTask timerTask;
    public JTextArea textArea;
    public JFrame matchingFrame;
    public Container cont;
    public Timer timer;
    public long startTimer;
    
    public SafeZoneClient() {
        connectGUI();
    }

    private void connectGUI() {
        JFrame connectFrame = new JFrame("서버 연결 설정");
        connectFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connectFrame.setSize(300, 150);
        connectFrame.setLocationRelativeTo(null);
        connectFrame.setResizable(false);

        JPanel consolePanel = new JPanel();
        consolePanel.setLayout(new BoxLayout(consolePanel, BoxLayout.Y_AXIS));
        consolePanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 50, 20));

        Dimension textFieldSize = new Dimension(150, 20);
        // Address input
        JPanel addressPanel = new JPanel();
        BoxLayout boxlayout1 = new BoxLayout(addressPanel, BoxLayout.X_AXIS);
        addressPanel.setLayout(boxlayout1);
        JLabel lAddress = new JLabel("   서버 주소:  ");
        JTextField tAddress = new JTextField(15);
        tAddress.setPreferredSize(textFieldSize);
        tAddress.setMinimumSize(textFieldSize);
        tAddress.setMaximumSize(textFieldSize);
        tAddress.setText(serverAddress);
        addressPanel.add(lAddress);
        addressPanel.add(tAddress);

        // User name input
        JPanel userNamePanel = new JPanel();
        BoxLayout boxlayout2 = new BoxLayout(userNamePanel, BoxLayout.X_AXIS);
        userNamePanel.setLayout(boxlayout2);
        JLabel lUserName = new JLabel("사용자 이름:  ");
        JTextField tUserName = new JTextField(15);
        tUserName.setPreferredSize(textFieldSize);
        tUserName.setMinimumSize(textFieldSize);
        tUserName.setMaximumSize(textFieldSize);
        userNamePanel.add(lUserName);
        userNamePanel.add(tUserName);

        // Connect button
        JButton cButton = new JButton("연결");
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(cButton);

        
        cButton.addActionListener(e -> {
            serverAddress = tAddress.getText().isEmpty() ? "localhost" : tAddress.getText();
            userName = tUserName.getText();
            if (userName.matches("[a-zA-Z0-9]+")) {
                connectFrame.dispose();
                new Thread(() -> {
                    boolean connected = connectToServer();
                    if (connected) {
                        SwingUtilities.invokeLater(this::matchingGUI);
                    }
                }).start();
            } else {
                JOptionPane.showMessageDialog(connectFrame, "잘못된 사용자 이름입니다. 영문자와 숫자만 사용하세요.");
            }
        });

        consolePanel.add(addressPanel);
        consolePanel.add(Box.createVerticalStrut(5));
        consolePanel.add(userNamePanel);
        consolePanel.add(Box.createVerticalStrut(20));
        consolePanel.add(buttonPanel);

        connectFrame.getContentPane().add(consolePanel);
        connectFrame.setVisible(true);
    }
    // 서버 연결
    private boolean connectToServer() {
        try {
            Socket socket = new Socket(serverAddress, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(userName); // Send user name to server

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        handleServerMessage(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "서버 연결 실패: " + e.getMessage()));
                }
            }).start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "서버 연결 실패: " + e.getMessage()));
            return false;
        }
    }

    // 매칭 대기화면 GUI
    private void matchingGUI() {
        matchingFrame = new JFrame("매칭 대기");
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
            public void run() {
                try {
                    while (true) { 
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                matchingLabel.setText("매칭 중" + ".".repeat(dotCount));
                            }
                        });
                        dotCount++;
                        if (dotCount > 3) dotCount = 1;
                        Thread.sleep(500); 
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    private void gameGUI() {
        setTitle("지뢰찾기");
        setSize(600, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  	
        cont = getContentPane();
        cont.setLayout(new BorderLayout());

        topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));
        topPanel.setBackground(new Color(141, 153, 174));

        mineLabel = new JLabel("", SwingConstants.RIGHT);

     // 수정된 부분: ImageIcon 경로 확인 및 설정
        String imagePath = "/mine.png";
        URL imageURL = getClass().getResource(imagePath);
        if (imageURL != null) {
            System.out.println("Image path: " + imageURL.toExternalForm());  // 로그로 경로 출력
            Image img1 = new ImageIcon(imageURL).getImage();
            mineLabel.setIcon(new ImageIcon(img1));
        } else {
            System.err.println("Cannot find image: " + imagePath);
            mineLabel.setText("Mines");
        }
        
        mineField = new JTextField(5);
        mineField.setEditable(false);
        tryLabel = new JLabel("TRY", SwingConstants.RIGHT);
        tryLabel.setFont(tryLabel.getFont().deriveFont(Font.BOLD));
        tryField = new JTextField(5);
        tryField.setEditable(false);

        timerLabel = new JLabel("⏱️00:00", SwingConstants.CENTER);

        // 설정 버튼
        JButton setupButton = new JButton("...");
        setupButton.addActionListener(e -> {
            setupMenu.show(setupButton, 0, setupButton.getHeight());
        });
        setupButton.setBackground(new Color(141, 153, 174));
        setupButton.setForeground(Color.WHITE);
        setupMenu = new JPopupMenu();

        JMenuItem First_option = new JMenuItem("통계보기");
        setupMenu.add(First_option);
        First_option.addActionListener(e -> {
            resultGUI();
        });

        JMenuItem Second_option = new JMenuItem("항복하기");
        setupMenu.add(Second_option);
        Second_option.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(null, "항복하고 게임을 종료하시겠습니까? (패배 처리됨)", "항복하기", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                sendSurrenderToServer();
            }
        });

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        centerPanel.add(mineLabel);
        centerPanel.add(mineField);
        centerPanel.add(tryLabel);
        centerPanel.add(tryField);

        rightPanel.add(timerLabel);
        rightPanel.add(setupButton);

        JPanel infoPanel = new JPanel(new GridLayout(1, 3));
        infoPanel.add(leftPanel);
        infoPanel.add(centerPanel);
        infoPanel.add(rightPanel);

        topPanel.add(infoPanel);
        cont.add(topPanel, BorderLayout.NORTH);
        
        gamePanel = new JPanel();
        gamePanel.setBackground(new Color(141, 153, 174));
        cont.add(gamePanel, BorderLayout.CENTER);

        createMapPanel();

        createChatPanel();

        setVisible(true);
    }

    // 맵 패널 생성
    private void createMapPanel() {
    	mapPanel = new JPanel(new GridLayout(MAP_SIZE, MAP_SIZE));
    	
        JPanel wrappedPanel = new JPanel(new BorderLayout());
        wrappedPanel.add(mapPanel, BorderLayout.CENTER);
        Border border = BorderFactory.createLineBorder(new Color(237, 242, 244), 50);
        wrappedPanel.setBorder(border);
        
        JPanel infoPanel2 = new JPanel(new GridLayout(1, 3));
        
        JPanel scorePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        scoreLabel = new JLabel("My score(" + userName + "): ", SwingConstants.RIGHT);
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD));
        scoreField = new JTextField(2);
        scoreField.setEditable(false);
        
        scorePanel.add(scoreLabel);
        scorePanel.add(scoreField);
        
        infoPanel2.add(scorePanel);

        wrappedPanel.add(infoPanel2, BorderLayout.NORTH);

        gamePanel.removeAll();
        gamePanel.setLayout(new GridLayout(1, 1));  // 단일 맵을 배치하기 위해 GridLayout 사용
        gamePanel.add(wrappedPanel);

        gamePanel.revalidate();
        gamePanel.repaint();
    }
    
    // 맵 생성
    private void creatMapButtons() {
    	buttons = new JButton[MAP_SIZE][MAP_SIZE];  // 2차원 배열 초기화
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                JButton button = new JButton();
                button.setActionCommand(i + "," + j);
                button.addActionListener(new Detect());
                buttons[i][j] = button;
                button.setBackground(new Color(255, 250, 230));
                mapPanel.add(button);
            }
        }
    }

    // 게임GUI 하단(채팅창)
    private void createChatPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        JPanel chatPanel = new JPanel(new BorderLayout());

        // 채팅창
        textArea = new JTextArea(8, 60);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setBackground(new Color(200, 200, 200));
        textArea.setForeground(new Color(0, 0, 0));
        JScrollPane statusScrollPane = new JScrollPane(textArea);
        statusScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // 채팅 입력란
        chatField = new JTextField(60);
        chatField.setFont(new Font("Monospaced", Font.PLAIN, 12));
        chatField.setBackground(Color.WHITE);
        chatField.setForeground(Color.BLACK);
        chatField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String chat = chatField.getText();
                sendChatToServer(chat);
            }
        });

        chatPanel.add(statusScrollPane, BorderLayout.NORTH);
        chatPanel.add(chatField, BorderLayout.SOUTH);

        bottomPanel.add(chatPanel);
        bottomPanel.add(Box.createVerticalStrut(5));

        cont.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void resultGUI() {
        JFrame resultFrame = new JFrame("통계");
        resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        resultFrame.setSize(300, 160);
        resultFrame.setLocationRelativeTo(null);
        resultFrame.setResizable(false);
        resultFrame.setLayout(new GridLayout(0, 1)); // 라벨을 수직으로 배치
        
        totalDetections = num_try;
        successfulDetections = num_point;
        failedDetections = num_try - num_point;

        // 탐지 확률 계산
        double detectionRate = (double) successfulDetections / totalDetections * 100;
        DecimalFormat df = new DecimalFormat("0.00"); // 소수점 2자리 형식

        // 라벨 생성
        JLabel totalDetectionsLabel = new JLabel("탐지 시도: " + totalDetections);
        JLabel successfulDetectionsLabel = new JLabel("탐지 성공: " + successfulDetections);
        JLabel failedDetectionsLabel = new JLabel("탐지 실패: " + failedDetections);
        JLabel detectionRateLabel = new JLabel("탐지 확률: " + df.format(detectionRate) + "%");
        JLabel totalPlayTimeLabel = new JLabel("총 플레이 시간: " + formatPlayTime(totalPlayTime));

        // 라벨을 프레임에 추가
        resultFrame.add(totalDetectionsLabel);
        resultFrame.add(successfulDetectionsLabel);
        resultFrame.add(failedDetectionsLabel);
        resultFrame.add(detectionRateLabel);
        resultFrame.add(totalPlayTimeLabel);

        resultFrame.setVisible(true);
    }
    
    private String formatPlayTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
 // 턴 변경
    private void switchTurn(boolean isMyTurn) {
        this.isMyTurn = isMyTurn;
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < MAP_SIZE; i++) {
                for (int j = 0; j < MAP_SIZE; j++) {
                    if (buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setEnabled(isMyTurn);
                    }
                }
            }
            // 추후에 채팅 해결되면 서버와 연결
            String turnText = isMyTurn ? "Server: 당신의 차례입니다." : "Server: 상대 플레이어의 차례입니다.";
            sendMessage(turnText);
        });
    }

 // 기본 플레이어 맵 클릭
    class Detect implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!isMyTurn) return; // 내 턴이 아니면 클릭 무시
            
            JButton b = (JButton) e.getSource();
            if (!b.isEnabled()) return; // 이미 눌린 버튼이면 무시
            
            b.setEnabled(false); // 버튼을 비활성화하여 중복 클릭 방지

            String[] coordinates = b.getActionCommand().split(",");
            int x = Integer.parseInt(coordinates[0]);
            int y = Integer.parseInt(coordinates[1]);
            sendClick(x, y);
        }
    }

 // 플레이어 맵 클릭 추가 서버 관리
    private void handleMoveResponse(String line) {
        String[] parts = line.split(" ");
        int score = Integer.parseInt(parts[1]);
        int remainingMines = Integer.parseInt(parts[2]);
        int x = Integer.parseInt(parts[3]);
        int y = Integer.parseInt(parts[4]);
        JButton button = buttons[x][y];
        if (parts[0].equals("MOVE_OK")) {
            SwingUtilities.invokeLater(() -> new GotchaAnimation());
            String OText = ("지뢰를 찾았습니다! (점수 +1)\n");
            sendMessage(OText);
            num_point++;
            scoreField.setText("" + num_point);
            button.setText("🚩");
            button.setBackground(new Color(239, 35, 60));
        } else {
            String XText = ("지뢰가 아닙니다.\n");
            sendMessage(XText);
            button.setText("❌");
            button.setBackground(new Color(104, 163, 87));
        }
        button.setEnabled(false); // 응답 후 버튼을 비활성화

        num_mine = remainingMines;
        mineField.setText("" + num_mine);
        num_try++;
        tryField.setText("" + num_try);
        switchTurn(false); // 응답 후 턴 전환
    }

    // 서버 메시지 일괄 관리 (기능 아래에 계속 추가)
    private void handleServerMessage(String line) {
        SwingUtilities.invokeLater(() -> {
            if (line.startsWith("YOUR_TURN")) {
                switchTurn(true);
            } else if (line.startsWith("MOVE_OK") || line.startsWith("MOVE_FAIL")) {
                handleMoveResponse(line);
            } else if (line.startsWith("MATCH_FOUND")) {
                handleMatchFound();
            } else if (line.startsWith("GAME_STARTED")) {
                handleGameStarted();
            } else if (line.startsWith("GAME_OVER")) {
                handleGameOver(line);
            } else if (line.startsWith("CHAT:")) {
                String chatMessage = line.substring(5); // "CHAT:" 다음 부분만 추출
                textArea.append(chatMessage + "\n");
            } else if (line.startsWith("GAME_END")) {
                handleGameEnd();
            } else if (line.startsWith("THANK_YOU")) {
                handleThankYou();
            } else if (line.startsWith("OPPONENT_SURRENDERED")) {
                handleOpponentSurrendered();
            } else if (line.startsWith("YOU_SURRENDERED")) {
                handleYouSurrendered();
            }
        });
    }
    
    private void sendSurrenderToServer() {
        out.println("SURRENDER");
    }
    
    private void handleOpponentSurrendered() {
        JOptionPane.showMessageDialog(this, "상대방이 항복하였습니다. 당신이 승리하였습니다!");
        handleGameEnd();
    }

    // 본인 항복 처리 메소드 추가
    private void handleYouSurrendered() {
        JOptionPane.showMessageDialog(this, "항복하였습니다. 게임이 종료됩니다.");
        handleGameEnd();
    }

    private void handleMatchFound() {
    	if(matchingFrame != null) {
    		matchingFrame.dispose();
    	}
        JOptionPane.showMessageDialog(this, "매칭이 완료되었습니다. 게임이 곧 시작됩니다.");
        gameGUI();
    }

    private JDialog waitingDialog;

    private void handleGameStarted() {
        creatMapButtons();
        startTimer();
        mineField.setText("" + num_mine);
        num_try = 0;
        tryField.setText("" + num_try);
        num_point = 0;
        scoreField.setText("" + num_point);
        String startText = ("게임이 시작되었습니다!");
        sendMessage(startText);
        startText = ("맵의 지뢰는 총 10개 입니다.");
        sendMessage(startText);
        switchTurn(false);
    }

    private void handleGameOver(String line) {
        String message;
        if (line.contains("No Winner")) {
            message = "게임 종료! 무승부입니다.";
        } else {
            String[] parts = line.split(" ");
            if (parts.length >= 2) {
                String winnerName = parts[1];
                message = "게임 종료! 승자: " + winnerName;
            } else {
                message = "게임 종료!";
            }
        }

        // 기존 게임 창 닫기
        dispose();

        // 종료 안내 메시지 띄우기
        JOptionPane.showMessageDialog(
                null,
                message + "\n플레이해주셔서 감사합니다.",
                "게임 종료",
                JOptionPane.INFORMATION_MESSAGE
        );

        // 프로그램 종료
        System.exit(0);
    }

    private void showWaitingForServerStart() {
        JFrame waitingForStartFrame = new JFrame("게임 시작 대기");
        waitingForStartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        waitingForStartFrame.setSize(300, 160);
        waitingForStartFrame.setLocationRelativeTo(null);
        waitingForStartFrame.setResizable(false);

        JLabel waitingLabel = new JLabel("서버의 게임 시작을 기다리는 중...");
        waitingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        waitingLabel.setVerticalAlignment(SwingConstants.CENTER);
        waitingForStartFrame.add(waitingLabel, BorderLayout.CENTER);

        waitingForStartFrame.setVisible(true);
    }

    private void handleRestartGame() {
        SwingUtilities.invokeLater(() -> {
            // 기존 게임 창 닫기
            dispose();
            showWaitingForServerStart(); // 서버의 게임 시작 호출을 기다리는 상태로 전환
        });
    }

    private void handleGameEnd() {
        SwingUtilities.invokeLater(() -> {
            // 선택지 창이 열려 있는 경우 닫기
            Window[] windows = Window.getWindows();
            for (Window window : windows) {
                if (window instanceof JDialog) {
                    window.dispose();
                }
            }

            JOptionPane.showMessageDialog(null, "상대방이 게임을 종료하였습니다. 플레이 해주셔서 감사합니다.");
            System.exit(0);
        });
    }
    
    // 종료 인사
    private void handleThankYou() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, "플레이해주셔서 감사합니다. 안녕히가세요.");
            System.exit(0);
        });
    }
    
    // 타이머 가동
    public void startTimer() {
        startTimer = System.currentTimeMillis();
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                long elapsed = System.currentTimeMillis() - startTimer;
                int minutes = (int) (elapsed / 60000);
                int seconds = (int) ((elapsed / 1000) % 60);
                SwingUtilities.invokeLater(() -> 
                    timerLabel.setText(String.format("⏱️%02d:%02d", minutes, seconds))
                );
            }
        };
        timer.schedule(timerTask, 0, 1000);
    }

    // 타이머 정지
    public void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    // 타이머 리셋
    public void resetTimer() {
    	stopTimer();
    	timerLabel.setText("⏱️00:00");
    }
    
    // 모든 버튼 비활성화
	private void disableAllButtons() {
    	for (JButton button : mapButtons) {
    		button.setEnabled(false);
    	}
	}

	// 채팅창 업데이트
	private void sendMessage(String message) {
		textArea.append(message + "\n");
		chatField.setText("");
    }
	
	private void sendChatToServer(String chat) {
        if (!chat.trim().isEmpty()) {
            out.println("CHAT:" + userName + ": " + chat);
            chatField.setText(""); // 채팅 입력 필드 비우기
        }
    }
    
    // 클릭 전송
    private void sendClick(int x, int y) {
    	out.println("MOVE " + x + " " + y);
    }
    
    // swing 컴포넌트 생성
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SafeZoneClient::new);
    }
}
