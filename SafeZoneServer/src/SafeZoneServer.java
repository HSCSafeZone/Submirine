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
    public static final int NUM_MINES = 1;
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

        player1Info = createTextArea("플레이어 1: 대기 중", infoFont, new Color(0, 0, 0));
        player2Info = createTextArea("플레이어 2: 대기 중", infoFont, new Color(0, 0, 0));
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
        startButton.addActionListener(e -> initGame());

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
                        updateServerConsole(client.getPlayer().getName() + "님이 연결되었습니다.");
                        updatePlayerInfo(); // 여기서 호출
                        checkPlayersAndStartGame(); // 클라이언트 연결 시 버튼 상태 업데이트
                    }
                    pool.execute(client);
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
            if (clients.size() == MAX_PLAYER) {
                for (Client client : clients) {
                    client.sendMessage("MATCH_FOUND");
                }
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("모든 플레이어가 연결되었습니다. 게임을 시작하세요.");
                    startButton.setEnabled(true); // 두 명이 연결된 경우 버튼 활성화
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    startButton.setEnabled(false); // 두 명이 연결되지 않은 경우 버튼 비활성화
                });
            }
        }
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

        assignFirstTurn();
    }

    private void notifyClientsGameStarted() {
        for (Client client : clients) {
            client.sendMessage("GAME_STARTED");
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
    
    private void updatePlayerInfo() {
        SwingUtilities.invokeLater(() -> {
            if (clients.size() > 0) {
                player1Info.setText("플레이어 1: " + clients.get(0).getPlayer().getName());
            }
            if (clients.size() > 1) {
                player2Info.setText("플레이어 2: " + clients.get(1).getPlayer().getName());
            }
        });
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
        private Boolean wantsToRestart = null; // 재시작 여부를 나타내는 Boolean 객체

        // 클라이언트 생성자
        Client(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String playerName = in.readLine(); // 클라이언트로부터 사용자 이름을 받습니다.
            player = new Player(playerName);
            start();  // 스레드를 시작합니다.
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

        public void closeConnection() {
            try {
                if (socket != null) {
                    socket.close();  // 소켓 연결 종료
                }
                synchronized (clients) {
                    clients.remove(this);
                    updatePlayerInfo();
                    checkPlayersAndStartGame(); // 클라이언트 연결 종료 시 버튼 상태 업데이트
                }
            } catch (IOException e) {
                System.err.println("연결 종료 오류: " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        private void processMessage(String msg) {
            if (msg.startsWith("MOVE")) {
                processMoveMessage(msg);
            } else if ("GET_INFO".equals(msg)) {
                sendPlayerInfo();
            } else if ("SHUTDOWN".equals(msg)) {
                closeConnection();
            } else if ("RESTART".equals(msg)) {
                wantsToRestart = true;
                handleRestartRequest();
            } else if ("END".equals(msg)) {
                wantsToRestart = false;
                handleEndRequest(this);
            } else {
                handleOtherMessages(msg);
            }
        }

        private void handleRestartRequest() {
            synchronized (clients) {
                boolean allWantRestart = true;
                for (Client client : clients) {
                    if (client.wantsToRestart == null) {
                        allWantRestart = false;
                        break;
                    }
                }

                if (allWantRestart) {
                    for (Client client : clients) {
                        client.wantsToRestart = null; // 재설정
                        client.sendMessage("RESTART_MATCH");
                    }
                    initGame(); // 게임 초기화
                }
            }
        }

        private void handleEndRequest(Client requestingClient) {
            synchronized (clients) {
                // 요청한 클라이언트 처리
                requestingClient.sendMessage("THANK_YOU");
                requestingClient.closeConnection();
                
                // 나머지 클라이언트에게 GAME_END 메시지를 보냄
                for (Client client : clients) {
                    if (client != requestingClient) {
                        client.sendMessage("GAME_END");
                        client.closeConnection();
                    }
                }

                closeServerAsync();
            }
        }

        private void notifyRemainingPlayers() {
            for (Client client : clients) {
                if (client.wantsToRestart != null && client.wantsToRestart) {
                    client.sendMessage("WAITING_FOR_OPPONENT");
                } else if (!client.wantsToRestart) {
                    client.sendMessage("OPPONENT_LEFT");
                }
            }
        }

        private void resetGame() {
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
            notifyClientsGameStarted();
            assignFirstTurn();
        }

        private void notifyClientsGameStarted() {
            for (Client client : clients) {
                client.wantsToRestart = null; // 초기화
                client.sendMessage("RESTART_GAME");
            }
        }

        private void processMoveMessage(String msg) {
            String[] parts = msg.split(" ");
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            if (mines[x][y]) {
                score++;
                mines[x][y] = false; // 지뢰 찾았으니 false로 설정
                int remainingMines = getRemainingMines();
                out.println("MOVE_OK " + score + " " + remainingMines);
                broadcastRemainingMines(remainingMines);
                // 게임이 끝났는지 확인
                if (checkGameOver()) {
                    sendGameOver();
                }
            } else {
                int remainingMines = getRemainingMines();
                out.println("MOVE_FAIL " + score + " " + remainingMines);
                broadcastRemainingMines(remainingMines);
            }
            updateMap();
            switchTurn();  // 턴 전환
        }

        private void broadcastRemainingMines(int remainingMines) {
            for (Client client : clients) {
                client.out.println("UPDATE_MINES " + remainingMines);
            }
        }

        private int getRemainingMines() {
            int remainingMines = 0;
            for (int i = 0; i < MAP_WIDTH; i++) {
                for (int j = 0; j < MAP_HEIGHT; j++) {
                    if (mines[i][j]) {
                        remainingMines++;
                    }
                }
            }
            return remainingMines;
        }


        private void handleOtherMessages(String msg) {
            String consoleMsg = player.getName() + ": " + msg;
            System.out.println(consoleMsg);
            SwingUtilities.invokeLater(() -> {
                serverConsole.append(consoleMsg + "\n");
            });
        }
        
        private boolean checkGameOver() {
            int totalMinesFound = 0;
            for (Client client : clients) {
                totalMinesFound += client.score;
            }
            return totalMinesFound == NUM_MINES;
        }

        private void sendGameOver() {
            String winnerName = "";
            int maxScore = -1;
            for (Client client : clients) {
                if (client.score > maxScore) {
                    maxScore = client.score;
                    winnerName = client.player.getName();
                }
            }

            if (maxScore == 0) {
                winnerName = "No Winner";
            }

            for (Client client : clients) {
                client.sendMessage("GAME_OVER " + winnerName);
            }

            // 클라이언트들이 메시지를 받을 시간을 준 후 연결을 끊음
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // 3초 대기
                    closeAllClients();
                    closeServerAsync(); // 게임 종료 후 서버 종료
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        private void closeAllClients() {
            Vector<Client> clientsCopy;
            synchronized (clients) {
                clientsCopy = new Vector<>(clients);
            }
            for (Client client : clientsCopy) {
                client.closeConnection();
            }
        }

        public void sendPlayerInfo() {
            out.println(player.getInfo());  // 플레이어 정보 전송
        }
        
        public void sendShutdownMessage() {
            try {
                if (out != null) {
                    out.println("SHUTDOWN");  // SHUTDOWN 메시지 전송
                }
            } catch (Exception e) {
                System.err.println("Shutdown 메시지 전송 중 오류 발생: " + e.getMessage());
            }
        }

        public void sendGameStartMessage() {
            try {
                if (out != null) {
                    out.println("GAME_STARTED");  // 게임 시작 메시지 전송
                }
            } catch (Exception e) {
                System.err.println("Game start 메시지 전송 중 오류 발생: " + e.getMessage());
            }
        }

        public void sendTurnMessage() {
            try {
                if (out != null) {
                    out.println("YOUR_TURN");  // 턴 메시지 전송
                }
            } catch (Exception e) {
                System.err.println("Turn 메시지 전송 중 오류 발생: " + e.getMessage());
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