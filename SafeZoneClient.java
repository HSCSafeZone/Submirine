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
    private BufferedReader in;
    private PrintWriter out;
    private String userName;
    private String serverAddress = "localhost";

    public int size=10,  num_mine=0,  num_try=0,  num_round=0,  num_point=0;
    public Container cont;
    public JPanel mapPanel, topPanel, gamePanel, statusPanel;
    public JLabel roundLabel, mineLabel, timerLabel, tryLabel, pointLabel;
    public JPopupMenu setupMenu;
    public JButton[] mapButtons;
    public Timer timer;
    public TimerTask timerTask;
    public long startTimer;
    public int UIwidth = 50;  // 가로 크기 조절 변수
    public int UIheight = 100;  // 세로 크기 조절 변수
    public JTextArea playerStatus;
    public JTextField chatField;

    //
    // 실행부
    public SafeZoneClient() {
        connectGUI();
//        gameGUI();
    }
    
    
    //
    // UI 출력
    // 서버 연결 GUI
    private void connectGUI() {
    	JFrame connectFrame = new JFrame("서버 연결 설정");
        connectFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connectFrame.setSize(300, 160);
        connectFrame.setLocationRelativeTo(null);
        connectFrame.setResizable(false);

        JPanel consolePanel = new JPanel();
        consolePanel.setLayout(new BoxLayout(consolePanel, BoxLayout.Y_AXIS));
        consolePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        Dimension dim1 = new Dimension(150, 20);
        // 주소 입력
        JPanel addressPanel = new JPanel();
        BoxLayout boxlayout1 = new BoxLayout(addressPanel, BoxLayout.X_AXIS);
        addressPanel.setLayout(boxlayout1);
        JLabel lAddress = new JLabel("   서버 주소:  ");
        JTextField tAddress = new JTextField(15);
        tAddress.setPreferredSize(dim1);
        tAddress.setMinimumSize(dim1);
        tAddress.setMaximumSize(dim1);
        tAddress.setText(serverAddress);
        addressPanel.add(lAddress);
        addressPanel.add(tAddress);

        // 사용자 이름 입력
        JPanel userNamePanel = new JPanel();
        BoxLayout boxlayout2 = new BoxLayout(userNamePanel, BoxLayout.X_AXIS);
        userNamePanel.setLayout(boxlayout2);
        JLabel lUserName = new JLabel("사용자 이름:  ");
        JTextField tUserName = new JTextField(15);
        tUserName.setPreferredSize(dim1);
        tUserName.setMinimumSize(dim1);
        tUserName.setMaximumSize(dim1);
        userNamePanel.add(lUserName);
        userNamePanel.add(tUserName);

        // 연결 버튼
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
    
    // 서버 접속
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

        new Thread(() -> {
            try {
                while (true) {
                    // 매칭 로직
                    Thread.sleep(1000); // 매칭 대기 시간

                    // 매칭 성공 시
                    SwingUtilities.invokeLater(() -> {
                        matchingLabel.setText("매칭 성공");
                    });
                    Thread.sleep(1000);
                    SwingUtilities.invokeLater(() -> {
                        matchingFrame.dispose();
                        gameGUI();
                    });
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void gameGUI() {
        setTitle("지뢰찾기");
        setSize(600, 800);
        setLocation(400, 20);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cont = getContentPane();
        cont.setLayout(new BorderLayout());

        topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));
        topPanel.setBackground(new Color(70, 130, 180));

        roundLabel = new JLabel(num_round + "ROUND", SwingConstants.RIGHT);
        roundLabel.setFont(roundLabel.getFont().deriveFont(Font.BOLD));

        mineLabel = new JLabel("MINES: " + num_mine, SwingConstants.RIGHT);
        mineLabel.setFont(mineLabel.getFont().deriveFont(Font.BOLD));
        tryLabel = new JLabel("TRY: " + num_try, SwingConstants.RIGHT);
        tryLabel.setFont(tryLabel.getFont().deriveFont(Font.BOLD));
        pointLabel = new JLabel( userName + ": " + num_point + "점", SwingConstants.RIGHT);
        pointLabel.setFont(pointLabel.getFont().deriveFont(Font.BOLD));

        timerLabel = new JLabel("⏱️00:00", SwingConstants.CENTER);

        // 설정 버튼
        JButton setupButton = new JButton("...");
        setupButton.addActionListener(e -> {
            setupMenu.show(setupButton, 0, setupButton.getHeight());
        });
        setupButton.setBackground(new Color(70, 130, 180));
        setupButton.setForeground(Color.WHITE);
        setupMenu = new JPopupMenu();

        JMenuItem First_option = new JMenuItem("1");
        setupMenu.add(First_option);
        First_option.addActionListener(e -> {
//            switchTurn();
        });

        JMenuItem Second_option = new JMenuItem("2");
        setupMenu.add(Second_option);
        Second_option.addActionListener(e -> {
        	int response = JOptionPane.showConfirmDialog(null, "맵을 초기화하고 다시 시작하시겠습니까?", "다시 시작", JOptionPane.YES_NO_OPTION);
        	if (response == JOptionPane.YES_OPTION) {
//        		num_try = 0;
//        		tryLabel.setText("TRY: " + num_try);
        		createMapPanel();
        		resetTimer();
        	}
        });

        JMenuItem Third_option = new JMenuItem("게임종료");
        setupMenu.add(Third_option);
        Third_option.addActionListener(e -> {
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
        centerPanel.add(pointLabel);

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

        createMapPanel();

        createBottomPanel();
        
        setVisible(true);
    }

    // 맵 패널 생성
    private void createMapPanel() {
        mapPanel = new JPanel(new GridLayout(MAP_SIZE, MAP_SIZE));
        

        JPanel wrappedPanel = new JPanel(new BorderLayout());
        wrappedPanel.add(mapPanel, BorderLayout.CENTER);
        Border border = BorderFactory.createLineBorder(new Color(240, 240, 240), 50);
        wrappedPanel.setBorder(border);

        gamePanel.removeAll();
        gamePanel.setLayout(new GridLayout(1, 1));
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
                mapPanel.add(button);
            }
        }
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
            }
        });

        bottomPanel.add(statusScrollPane, BorderLayout.NORTH);
        bottomPanel.add(chatField, BorderLayout.SOUTH);

        cont.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    
    //
    // >>>내부 기능
    // 턴 변경
    private void switchTurn(boolean isMyTurn) {
        this.isMyTurn = isMyTurn; // 현재 턴 상태 업데이트
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < MAP_SIZE; i++) {
                for (int j = 0; j < MAP_SIZE; j++) {
                    buttons[i][j].setEnabled(isMyTurn);
                }
            }
            String turnText = isMyTurn ? "Server: 당신의 차례입니다." : "Server: 상대 플레이어의 차례입니다.";
            sendMessage(turnText);
        });
    }
    
    // 플레이어 맵 클릭
    class Detect implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!isMyTurn) return; // 내 턴이 아니면 클릭 무시

            JButton b = (JButton) e.getSource();
            String[] coordinates = b.getActionCommand().split(",");
            int x = Integer.parseInt(coordinates[0]);
            int y = Integer.parseInt(coordinates[1]);
            sendClick(x, y);  // 서버로 이동 전송
            
            b.setText("❌");
    		b.setEnabled(false);
    		b.setBackground(Color.DARK_GRAY);
        }
    }
    
    // 플레이어 맵 클릭 서버 관리
    private void handleMoveResponse(String line) {
        String[] parts = line.split(" ");
        int score = Integer.parseInt(parts[1]);
        int remainingMines = Integer.parseInt(parts[2]);
        if (parts[0].equals("MOVE_OK")) {
            String OText = ("지뢰를 찾았습니다! 점수: " + score + "\n");
            sendMessage(OText);
            num_mine = remainingMines;
            mineLabel.setText("MINES: " + num_mine);
            // 변수 업데이트
            num_try++;
            tryLabel.setText("TRY: " + num_try);
            num_point++;
            pointLabel.setText(num_point + "점");
        } else {
            String XText = ("지뢰가 아닙니다. 점수: " + score + "\n");
            sendMessage(XText);
            num_mine = remainingMines;
            mineLabel.setText("MINES: " + num_mine);
            // 변수 업데이트
            num_try++;
            tryLabel.setText("TRY: " + num_try);
        }
        switchTurn(false);
    }
    
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
            } else if (line.startsWith("RESTART_GAME")) {
                handleRestartGame();
            } else if (line.startsWith("START_TIMER")) {
                startTimer();
            } else if (line.startsWith("상대의 선택을 기다리고 있습니다.")) {
                showWaitingDialog();
            } else if (line.startsWith("CHAT:")) {
                // 채팅 메시지 처리
            } else if (line.startsWith("ROUND+")) {
                num_round++;
                roundLabel.setText(num_round + "ROUND");
            } else if (line.startsWith("MINE-")) {
                num_mine--;
                mineLabel.setText("MINE: " + num_mine);
            } else if (line.startsWith("UPDATE_MINES")) {
                int remainingMines = Integer.parseInt(line.split(" ")[1]);
                num_mine = remainingMines;
                mineLabel.setText("MINES: " + num_mine);
            } else if (line.startsWith("GAME_OVER_FINAL")) {
                hideWaitingDialog();
                JOptionPane.showMessageDialog(this, "상대가 접속을 종료하였습니다.");
                JOptionPane.showMessageDialog(this, "플레이 해주셔서 감사합니다.");
                System.exit(0);
            }
        });
    }


    private void handleMatchFound() {
        JOptionPane.showMessageDialog(this, "매칭이 완료되었습니다. 게임이 곧 시작됩니다.");
    }

    private void handleGameStarted() {
        creatMapButtons();
        startTimer();
        num_round++;
        roundLabel.setText(num_round + "ROUND");
        num_mine = 10;
        mineLabel.setText("MINE: " + num_mine);
        String startText = ("게임이 시작되었습니다!");
        sendMessage(startText);
        startText = ("맵의 지뢰는 총 10개 입니다.");
        sendMessage(startText);
        switchTurn(false);
    }
    
    private void handleGameOver(String line) {
        String[] parts = line.split(" ");
        String message;
        if (parts.length >= 2) {
            String winnerName = parts[1];
            message = "게임 종료! 승자: " + winnerName + "\n다시 하시겠습니까?";
        } else {
            message = "게임 종료!\n다시 하시겠습니까?";
        }
        int response = JOptionPane.showConfirmDialog(this, message, "게임 종료", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            out.println("RESTART");
            showWaitingDialog();
        } else {
            out.println("NO_RESTART");
            showWaitingDialog();
        }
    }

    private JDialog waitingDialog;

    private void showWaitingDialog() {
        if (waitingDialog == null) {
            waitingDialog = new JDialog(this, "상대의 선택을 기다리고 있습니다.", true);
            waitingDialog.setSize(300, 150);
            waitingDialog.setLocationRelativeTo(this);
        }
        SwingUtilities.invokeLater(() -> waitingDialog.setVisible(true));
    }

    private void hideWaitingDialog() {
        if (waitingDialog != null) {
            waitingDialog.setVisible(false);
        }
    }

    private void handleRestartGame() {
        hideWaitingDialog();
        creatMapButtons();
        startTimer();
        num_round++;
        roundLabel.setText(num_round + "ROUND");
        num_mine = 10;
        mineLabel.setText("MINE: " + num_mine);
        String startText = "게임이 다시 시작되었습니다!";
        sendMessage(startText);
        startText = "맵의 지뢰는 총 10개 입니다.";
        sendMessage(startText);
        switchTurn(false);
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
        playerStatus.append(message + "\n");
        chatField.setText("");
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
