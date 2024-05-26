import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.Random;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SafeZoneServer extends JFrame {
    public static final int IN_PORT = 9999;
    public static final int MAX_PLAYER = 2;
    public static final int MAP_WIDTH = 10;
    public static final int MAP_HEIGHT = 10;
    public static final int NUM_MINES = 10;
    private Vector<Client> clients = new Vector<>();
    private ExecutorService pool = Executors.newFixedThreadPool(MAX_PLAYER);
    private boolean[][] mines = new boolean[MAP_WIDTH][MAP_HEIGHT];
    private JTextArea player1Info, player2Info, serverConsole;
    private JPanel mapPanel;
    private JButton startButton, stopButton;
    private JLabel statusLabel;
    private Instant startTime;
    private ServerSocket serverSocket;

    public SafeZoneServer() {
        prepareGUI();
        initializeServer();
    }

    private void prepareGUI() {
        setTitle("지뢰 찾기 서버");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 컬러 테마
        Color backgroundColor = new Color(240, 240, 240);  // 밝은 배경색
        Color panelColor = new Color(200, 200, 200);       // 중간 밝기의 패널 색상
        Color textColor = new Color(0, 0, 0);              // 어두운 텍스트 색상
        Color buttonColor = new Color(70, 130, 180);       // 파란 버튼 색상

        // 전체 배경색 설정
        getContentPane().setBackground(backgroundColor);

        Font infoFont = new Font("SansSerif", Font.BOLD, 14);

        player1Info = new JTextArea("플레이어 1 정보");
        player1Info.setFont(infoFont);
        player1Info.setEditable(false);
        player1Info.setBackground(panelColor);
        player1Info.setForeground(textColor);
        JScrollPane scrollPane1 = new JScrollPane(player1Info);
        scrollPane1.setBorder(BorderFactory.createTitledBorder(null, "플레이어 1", 0, 0, infoFont, textColor));

        player2Info = new JTextArea("플레이어 2 정보");
        player2Info.setFont(infoFont);
        player2Info.setEditable(false);
        player2Info.setBackground(panelColor);
        player2Info.setForeground(textColor);
        JScrollPane scrollPane2 = new JScrollPane(player2Info);
        scrollPane2.setBorder(BorderFactory.createTitledBorder(null, "플레이어 2", 0, 0, infoFont, textColor));

        statusLabel = new JLabel("서버가 시작되었습니다...", JLabel.CENTER);
        statusLabel.setFont(new Font("Serif", Font.BOLD, 16));
        statusLabel.setForeground(textColor);

        mapPanel = new JPanel(new GridLayout(MAP_WIDTH, MAP_HEIGHT));
        mapPanel.setPreferredSize(new Dimension(400, 400));
        mapPanel.setBackground(panelColor);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(scrollPane1, BorderLayout.WEST);
        topPanel.add(scrollPane2, BorderLayout.EAST);
        topPanel.add(statusLabel, BorderLayout.NORTH);
        topPanel.add(mapPanel, BorderLayout.CENTER);
        topPanel.setBackground(backgroundColor);

        add(topPanel, BorderLayout.CENTER);

        serverConsole = new JTextArea(8, 60);
        serverConsole.setFont(new Font("Monospaced", Font.PLAIN, 12));
        serverConsole.setEditable(false);
        serverConsole.setBackground(panelColor);
        serverConsole.setForeground(textColor);
        JScrollPane consoleScrollPane = new JScrollPane(serverConsole);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder(null, "서버 콘솔", 0, 0, infoFont, textColor));

        JButton startButton = new JButton("게임 시작");
        startButton.setBackground(buttonColor);
        startButton.setForeground(Color.WHITE);  // 버튼 텍스트 색상
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> initGame());

        JButton stopButton = new JButton("서버 중지");
        stopButton.setBackground(buttonColor);
        stopButton.setForeground(Color.WHITE);  // 버튼 텍스트 색상
        stopButton.setFocusPainted(false);
        stopButton.addActionListener(e -> {
            try {
                closeServer();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(backgroundColor);
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(backgroundColor);
        bottomPanel.add(consoleScrollPane, BorderLayout.CENTER);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }


    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(IN_PORT);
            statusLabel.setText("서버가 포트 " + IN_PORT + "에서 실행 중입니다.");
            startTime = Instant.now();  // 서버 시작 시간 초기화
            while (!serverSocket.isClosed()) {  // 서버 소켓이 닫히지 않은 동안 실행
                try {
                    Socket socket = serverSocket.accept();
                    Client client = new Client(socket);
                    clients.add(client);
                    pool.execute(client);
                    updateServerConsole(client.getPlayer().getName() + "님이 연결되었습니다.");
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        System.out.println("서버 소켓이 닫혔습니다.");
                    } else {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("서버 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void updateServerConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            serverConsole.append(message + "\n");
        });
    }

    private void closeServer() throws IOException {
        System.out.println("서버를 종료합니다...");
        if (startTime != null) {  // startTime이 null이 아닌지 확인
            long duration = Duration.between(startTime, Instant.now()).getSeconds();
            System.out.println("서버 가동 시간: " + duration + "초");
        }
        for (Client client : clients) {
            client.sendShutdownMessage();  // 클라이언트에게 종료 메시지 보내기
            client.closeConnection();
        }
        serverSocket.close();
        System.exit(0);
    }

    private void initGame() {
        clearMap();
        Random rand = new Random();
        for (int i = 0; i < NUM_MINES; i++) {
            int x, y;
            do {
                x = rand.nextInt(MAP_WIDTH);
                y = rand.nextInt(MAP_HEIGHT);
            } while (mines[x][y]);  // 이미 지뢰가 있는 위치를 피함
            mines[x][y] = true;  // 지뢰 배치
        }
        updateMap();  // 맵 업데이트
        statusLabel.setText("게임이 시작되었습니다!");  // 상태 레이블 업데이트
    }

    private void clearMap() {
        mapPanel.removeAll();
        mapPanel.revalidate();
        mapPanel.repaint();
        for (int i = 0; i < MAP_WIDTH; i++) {
            for (int j = 0; j < MAP_HEIGHT; j++) {
                mines[i][j] = false;
                JLabel label = new JLabel("", SwingConstants.CENTER);
                label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                mapPanel.add(label);  // 맵을 새로 그림
            }
        }
    }

    private void updateMap() {
        Component[] components = mapPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            int x = i / MAP_WIDTH;
            int y = i % MAP_WIDTH;
            JLabel label = (JLabel) components[i];
            label.setText(mines[x][y] ? "X" : "");  // 지뢰 위치에 "X" 표시
        }
    }

    public static void main(String[] args) {
        new SafeZoneServer();
    }

    class Client extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private Player player;

        Client(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String playerName = in.readLine();  // 플레이어 이름 읽기
            player = new Player(playerName);
            start();  // 스레드 시작
        }

        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if ("SHUTDOWN".equals(msg)) {
                        break;  // 종료 메시지를 받으면 루프 종료
                    } else if ("GET_INFO".equals(msg)) {
                        sendPlayerInfo();  // 플레이어 정보 전송
                    } else {
                        processMessage(msg);  // 메시지 처리
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println(player.getName() + " 오류: " + e.getMessage());
                }
            } finally {
                closeConnection();  // 연결 종료
            }
        }

        private void sendPlayerInfo() {
            out.println(player.getInfo());  // 플레이어 정보 전송
        }

        private void processMessage(String msg) {
            String consoleMsg = player.getName() + ": " + msg;
            System.out.println(consoleMsg);

            SwingUtilities.invokeLater(() -> {
                serverConsole.append(consoleMsg + "\n");
                if (player.getName().equals(player1Info.getText().split(" ")[0])) {
                    player1Info.setText(player.getInfo());  // 플레이어 1 정보 업데이트
                } else if (player.getName().equals(player2Info.getText().split(" ")[0])) {
                    player2Info.setText(player.getInfo());  // 플레이어 2 정보 업데이트
                }
            });
        }

        void sendShutdownMessage() {
            out.println("SHUTDOWN");
        }

        void closeConnection() {
            try {
                socket.close();  // 소켓 닫기
            } catch (IOException e) {
                System.err.println("연결 종료 오류: " + e.getMessage());
            }
        }

        Player getPlayer() {
            return player;
        }
    }


    class Player {
        private String name;
        private int wins = 0;
        private int losses = 0;

        public Player(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void addWin() {
            wins++;  // 승리 횟수 증가
        }

        public void addLoss() {
            losses++;  // 패배 횟수 증가
        }

        public String getInfo() {
            return String.format("%s - 승리: %d, 패배: %d", name, wins, losses);
        }
    }

}

        }
    }
}
