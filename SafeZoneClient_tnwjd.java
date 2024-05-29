import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.net.Socket;

public class SafeZoneClient_tnwjd extends JFrame {
    private static final int SERVER_PORT = 9999;
    private static final int MAP_SIZE = 10;
    private JButton[][] buttons = new JButton[MAP_SIZE][MAP_SIZE];
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
    public Timer timer;
    public TimerTask timerTask;
    public long startTimer;
    public boolean isMyTurn = true;

    // 전체 실행 부
    public SafeZoneClient_tnwjd() {
    	connectGUI();
//    	gameMapGUI();
//    	resultGUI();
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
    	
    	startTimer();
    }
    
    // 게임GUI 상단(상태창)
    private void createTopPanel() {
        topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(2, 1));
        topPanel.setBackground(new Color(70, 130, 180));

        roundLabel = new JLabel(num_round + "ROUND", SwingConstants.RIGHT);
        roundLabel.setFont(roundLabel.getFont().deriveFont(Font.BOLD));

        mineLabel = new JLabel("MINES: " + num_mine, SwingConstants.RIGHT);
        mineLabel.setFont(mineLabel.getFont().deriveFont(Font.BOLD));
        tryLabel = new JLabel("TRY: " + num_try, SwingConstants.RIGHT);
        tryLabel.setFont(tryLabel.getFont().deriveFont(Font.BOLD));

        timerLabel = new JLabel("⏱️00:00", SwingConstants.CENTER);

        // 설정 버튼
        JButton setupButton = new JButton("...");
        setupButton.addActionListener(e -> {
            setupMenu.show(setupButton, 0, setupButton.getHeight());
        });
        setupButton.setBackground(new Color(70, 130, 180));
        setupButton.setForeground(Color.WHITE);
        setupMenu = new JPopupMenu();

        JMenuItem First_option = new JMenuItem("턴 바꾸기");
        setupMenu.add(First_option);
        First_option.addActionListener(e -> {
            switchTurn();
        });

        JMenuItem Second_option = new JMenuItem("항복");
        setupMenu.add(Second_option);
        Second_option.addActionListener(e -> {
        	int response = JOptionPane.showConfirmDialog(null, "맵을 초기화하고 다시 시작하시겠습니까?", "다시 시작", JOptionPane.YES_NO_OPTION);
        	if (response == JOptionPane.YES_OPTION) {
        		num_try = 0;
        		tryLabel.setText("TRY: " + num_try);
        		num_round++;
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
    
    private void resultGUI() {
        JFrame resultFrame = new JFrame("통계");
        resultFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        resultFrame.setSize(300, 160);
        resultFrame.setLocationRelativeTo(null);
        resultFrame.setResizable(false);

        JLabel resultLabel = new JLabel(" ~ 결과 내용 ~");
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultLabel.setVerticalAlignment(SwingConstants.CENTER);
        resultFrame.add(resultLabel, BorderLayout.CENTER);
        // 플레이 시간
        // 승,패
        // 시도횟수
        // 탐지한 지뢰 수
        // 성공 확률
        // 등등 보여주고 싶은 데이터
        
        resultFrame.setVisible(true);
    }
    
    
    // >>>내부 기능

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
    
    // 턴 관리
    class MyActionListener0 implements ActionListener {
    	public void actionPerformed(ActionEvent e) {
    		String msg = null;
    		if (msg.equals("YOUR_TURN")) {
    			switchTurn();
    			startTimer();
    		}
    	}
    }

    // 턴 변경
    private void switchTurn() {
        isMyTurn = !isMyTurn;
        String turnText = isMyTurn ? "Server: 당신의 차례입니다." : "Server: 상대 플레이어의 차례입니다.";
        sendMessage(turnText);
        chatField.setText("");

        // 내 턴이면 버튼 활성화, 아니면 비활성화
        for (JButton button : mapButtons) {
            button.setEnabled(isMyTurn);
        }
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

    // 서버 연결
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

    // 서버 메세지(?)
    private void processServerMessage(String line) {
        SwingUtilities.invokeLater(() -> {
            if (line.startsWith("TURN")) {
                myTurn = line.substring(5).equals(userName);
                playerStatus.setText(myTurn ? "당신의 차례입니다." : "상대 플레이어의 차례입니다.");
            } else if (line.startsWith("UPDATE")) {
                updateBoard(line.substring(7));
            } else if (line.startsWith("GAME_STARTED")) {
            	playerStatus.setText("게임이 시작되었습니다!");
            } else if (line.startsWith("WINNER")) {
                JOptionPane.showMessageDialog(null, line.substring(7));
            } else if (line.startsWith("MESSAGE")) {
            	playerStatus.append(line.substring(8) + "\n");
            }
        });
    }

    
    // 보드 업데이트(?)
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
    
    // 맵 초기화
    public void resetMap() {
        for (JButton button : mapButtons) {
            button.setText("");
            button.setEnabled(true);
            button.setBackground(null); // 기본 배경색으로 변경
        }
        num_try = 0;
        tryLabel.setText("TRY: " + num_try);
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
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SafeZoneClient_tnwjd::new);
    }
}