import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.time.Instant;

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
    private int currentPlayerIndex;
    private volatile boolean isClosing = false;

    public SafeZoneServer() {
        try {
            serverSocket = new ServerSocket(IN_PORT);
            prepareGUI();
            new Thread(this::initializeServer).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "서버가 이미 실행 중입니다.", "경고", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
    }

    private void prepareGUI() {
        setTitle("지뢰 찾기 서버");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 240, 240));

        Font infoFont = new Font("SansSerif", Font.BOLD, 14);

        player1Info = createTextArea("플레이어 1", infoFont, new Color(0, 0, 0));
        player2Info = createTextArea("플레이어 2", infoFont, new Color(0, 0, 0));
        statusLabel = createLabel("서버가 시작되었습니다...", new Font("Serif", Font.BOLD, 16), new Color(0, 0, 0));

        mapPanel = new JPanel(new GridLayout(MAP_WIDTH, MAP_HEIGHT));
        mapPanel.setPreferredSize(new Dimension(400, 400));
        mapPanel.setBackground(new Color(200, 200, 200));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JScrollPane(player1Info), BorderLayout.WEST);
        topPanel.add(new JScrollPane(player2Info), BorderLayout.EAST);
        topPanel.add(statusLabel, BorderLayout.NORTH);
        topPanel.add(mapPanel, BorderLayout.CENTER);
        topPanel.setBackground(new Color(240, 240, 240));

        add(topPanel, BorderLayout.CENTER);

        serverConsole = new JTextArea(8, 60);
        serverConsole.setFont(new Font("Monospaced", Font.PLAIN, 12));
        serverConsole.setEditable(false);
        serverConsole.setBackground(new Color(200, 200, 200));
        serverConsole.setForeground(new Color(0, 0, 0));
        JScrollPane consoleScrollPane = new JScrollPane(serverConsole);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder(null, "서버 콘솔", 0, 0, infoFont, new Color(0, 0, 0)));

        startButton = createButton("게임 시작", new Color(70, 130, 180));
        startButton.addActionListener(e -> checkPlayersAndStartGame());

        stopButton = createButton("서버 중지", new Color(70, 130, 180));
        stopButton.addActionListener(e -> closeServerAsync());

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(240, 240, 240));
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(240, 240, 240));
        bottomPanel.add(consoleScrollPane, BorderLayout.CENTER);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JTextArea createTextArea(String text, Font font, Color color) {
        JTextArea textArea = new JTextArea(text);
        textArea.setFont(font);
        textArea.setForeground(color);
        textArea.setPreferredSize(new Dimension(150, 100));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    private JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private synchronized void initializeServer() {
        statusLabel.setText("서버가 포트 " + IN_PORT + "에서 실행 중입니다.");
        startTime = Instant.now();
        try {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    Client client = new Client(socket);
                    synchronized (clients) {
                        clients.add(client);
                    }
                    pool.execute(client);
                    updateServerConsole(client.getPlayer().getName() + "님이 연결되었습니다.");
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        System.out.println("서버 소켓이 닫혔습니다.");
                    } else {
                        throw new RuntimeException("서버 오류: " + e.getMessage(), e);
                    }
                }
            }
        } finally {
            try {
                closeServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateServerConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            serverConsole.append(message + "\n");
        });
    }
    
    private void closeServerAsync() {
        new Thread(() -> {
            try {
                closeServer();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void closeServer() throws IOException {
        if (isClosing) return;
        isClosing = true;

        System.out.println("서버를 종료합니다...");
        if (startTime != null) {
            long duration = Duration.between(startTime, Instant.now()).getSeconds();
            System.out.println("서버 가동 시간: " + duration + "초");
        }

        // 모든 클라이언트에게 종료 메시지 전송 및 연결 종료
        for (Client client : clients) {
            client.sendShutdownMessage();
            client.interrupt();  // 스레드 인터럽트
            client.closeConnection();
        }

        // 서버 소켓 닫기
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        // ExecutorService 종료
        pool.shutdownNow();

        SwingUtilities.invokeLater(() -> {
            System.exit(0);
        });
    }

    private void checkPlayersAndStartGame() {
        synchronized (clients) {
            if (clients.size() < MAX_PLAYER) {
                JOptionPane.showMessageDialog(this, "Not enough players connected. At least 2 players are required.");
                return;
            }
        }
        initGame();
    }

    private void initGame() {
        clearMap();
        Random rand = new Random();
        for (int i = 0; i < NUM_MINES; i++) {
            int x, y;
            do {
                x = rand.nextInt(MAP_WIDTH);
                y = rand.nextInt(MAP_HEIGHT);
            } while (mines[x][y]);
            mines[x][y] = true;
        }
        updateMap();
        statusLabel.setText("The game has started!");
        notifyClientsGameStarted();

        // 첫 번째 턴을 랜덤 플레이어에게 할당
        assignFirstTurn();
    }
    
    private void notifyClientsGameStarted() {
        for (Client client : clients) {
            client.sendGameStartMessage();
        }
    }

    private void assignFirstTurn() {
        Random rand = new Random();
        currentPlayerIndex = rand.nextInt(MAX_PLAYER);
        sendTurnMessageToCurrentPlayer();
    }

    private void sendTurnMessageToCurrentPlayer() {
        if (clients.size() > currentPlayerIndex) {
            clients.get(currentPlayerIndex).sendTurnMessage();
        } else {
            System.err.println("유효한 플레이어가 없습니다.");
        }
    }

    private void switchTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % MAX_PLAYER;
        sendTurnMessageToCurrentPlayer();
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
                mapPanel.add(label);
            }
        }
    }

    private void updateMap() {
        Component[] components = mapPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            int x = i / MAP_WIDTH;
            int y = i % MAP_WIDTH;
            JLabel label = (JLabel) components[i];
            label.setText(mines[x][y] ? "X" : "");
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
        private int score = 0;

        Client(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String playerName = in.readLine();
            player = new Player(playerName);
            start();
        }

        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if ("SHUTDOWN".equals(msg)) {
                        break;
                    } else if (msg.startsWith("MOVE")) {
                        processMoveMessage(msg);
                    } else if ("GET_INFO".equals(msg)) {
                        sendPlayerInfo();
                    } else {
                        processMessage(msg);
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    System.err.println(player.getName() + " 오류: " + e.getMessage());
                }
            } finally {
                closeConnection();
            }
        }

        private void processMoveMessage(String msg) {
            String[] parts = msg.split(" ");
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            if (mines[x][y]) {
                score++;
                out.println("MOVE_OK " + score);
            } else {
                out.println("MOVE_FAIL " + score);
            }
            mines[x][y] = false; // 지뢰 제거
            updateMap();
            switchTurn();
        }

        public void sendPlayerInfo() {
            out.println(player.getInfo());
        }

        private void processMessage(String msg) {
            String consoleMsg = player.getName() + ": " + msg;
            System.out.println(consoleMsg);

            SwingUtilities.invokeLater(() -> {
                serverConsole.append(consoleMsg + "\n");
                if (player.getName().equals(player1Info.getText().split(" ")[0])) {
                    player1Info.setText(player.getInfo());
                } else if (player.getName().equals(player2Info.getText().split(" ")[0])) {
                    player2Info.setText(player.getInfo());
                }
            });
        }

        public void sendShutdownMessage() {
            try {
                if (out != null) {
                    out.println("SHUTDOWN");
                }
            } catch (Exception e) {
                System.err.println("Shutdown 메시지 전송 중 오류 발생: " + e.getMessage());
            }
        }

        public void sendGameStartMessage() {
            try {
                if (out != null) {
                    out.println("GAME_STARTED");
                }
            } catch (Exception e) {
                System.err.println("Game start 메시지 전송 중 오류 발생: " + e.getMessage());
            }
        }

        public void sendTurnMessage() {
            try {
                if (out != null) {
                    out.println("YOUR_TURN");
                }
            } catch (Exception e) {
                System.err.println("Turn 메시지 전송 중 오류 발생: " + e.getMessage());
            }
        }

        private void closeConnection() {
            try {
                socket.close();
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
            wins++;
        }

        public void addLoss() {
            losses++;
        }

        public String getInfo() {
            return String.format("%s - 승리: %d, 패배: %d", name, wins, losses);
        }
    }
}
