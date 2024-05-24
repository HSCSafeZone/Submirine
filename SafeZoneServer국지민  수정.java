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

public class SafeZoneServer extends JFrame {
    public static final int IN_PORT = 9999;
    public static final int MAX_PLAYER = 2;
    public static final int MAP_WIDTH = 10;
    public static final int MAP_HEIGHT = 10;
    public static final int NUM_MINES = 10;
    private Vector<Client> clients = new Vector<>();
    private int numPlayer = 0;
    private boolean[][] mines = new boolean[MAP_WIDTH][MAP_HEIGHT];
    private JTextArea player1Info, player2Info, serverConsole;
    private JPanel mapPanel;
    private JButton startButton, stopButton;
    private Instant startTime;
    private ServerSocket serverSocket;

    public SafeZoneServer() {
        prepareGUI();
        initializeServer();
    }

    private void prepareGUI() {
        setTitle("Safe Zone Server");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));  // 패널 간에 간격 추가

        Font infoFont = new Font("SansSerif", Font.BOLD, 14);

        // 플레이어 정보 창 스타일링
        player1Info = new JTextArea(5, 10);
        player1Info.setFont(infoFont);
        player1Info.setEditable(false);
        JScrollPane scrollPane1 = new JScrollPane(player1Info);
        scrollPane1.setBorder(BorderFactory.createTitledBorder("Player 1"));
        scrollPane1.setBackground(new Color(220, 220, 250)); // 밝은 회색 배경

        player2Info = new JTextArea(5, 10);
        player2Info.setFont(infoFont);
        player2Info.setEditable(false);
        JScrollPane scrollPane2 = new JScrollPane(player2Info);
        scrollPane2.setBorder(BorderFactory.createTitledBorder("Player 2"));
        scrollPane2.setBackground(new Color(220, 220, 250)); // 밝은 회색 배경

        // 맵 패널 스타일링
        mapPanel = new JPanel(new GridLayout(MAP_WIDTH, MAP_HEIGHT));
        mapPanel.setPreferredSize(new Dimension(400, 400));
        mapPanel.setBackground(new Color(120, 120, 150)); // 변경된 배경색

        // 상단 패널
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(scrollPane1, BorderLayout.WEST);
        topPanel.add(mapPanel, BorderLayout.CENTER);
        topPanel.add(scrollPane2, BorderLayout.EAST);

        add(topPanel, BorderLayout.CENTER);

        // 서버 콘솔 크기 및 스타일 조정
        serverConsole = new JTextArea(8, 60);  // 높이 변경
        serverConsole.setFont(new Font("Monospaced", Font.PLAIN, 12));
        serverConsole.setEditable(false);
        JScrollPane consoleScrollPane = new JScrollPane(serverConsole);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder("Server Console"));
        consoleScrollPane.setBackground(new Color(250, 250, 220)); // 구분되는 색상

        // 컨트롤 버튼
        startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.BOLD, 12));
        startButton.addActionListener(e -> initGame());
        startButton.setBackground(new Color(125, 250, 125));

        stopButton = new JButton("Stop Server");
        stopButton.setFont(new Font("Arial", Font.BOLD, 12));
        stopButton.addActionListener(e -> {
            try {
                closeServer();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        stopButton.setBackground(new Color(250, 125, 125));

        JPanel controlPanel = new JPanel();
        controlPanel.setBackground(new Color(220, 225, 230));
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        // 하단 패널에 서버 콘솔과 컨트롤 추가
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(consoleScrollPane, BorderLayout.CENTER);
        bottomPanel.add(controlPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
        startTime = Instant.now();
    }


    private void initializeServer() {
        try {
            serverSocket = new ServerSocket(IN_PORT);
            appendServerConsole("Server started on port: " + IN_PORT);
            while (numPlayer < MAX_PLAYER) {
                Socket socket = serverSocket.accept();
                Client client = new Client(socket);
                clients.add(client);
                numPlayer++;
                appendServerConsole("Player connected: " + client.getUserName());
                if (numPlayer == MAX_PLAYER) {
                    startButton.setEnabled(true);
                }
            }
        } catch (IOException e) {
            appendServerConsole("Server Error: " + e.getMessage());
            System.exit(-1);
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
        appendServerConsole("Game started!");
    }

    private void clearMap() {
        mapPanel.removeAll();
        mapPanel.revalidate();
        mapPanel.repaint();
        for (int i = 0; i < MAP_WIDTH; i++) {
            for (int j = 0; j < MAP_HEIGHT; j++) {
                mines[i][j] = false;
                JLabel label = new JLabel("", SwingConstants.CENTER);
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

    private void appendServerConsole(String text) {
        SwingUtilities.invokeLater(() -> serverConsole.append(text + "\n"));
    }

    private void closeServer() throws IOException {
        appendServerConsole("Shutting down server...");
        long duration = Duration.between(startTime, Instant.now()).getSeconds();
        appendServerConsole("Server uptime: " + duration + " seconds");
        for (Client client : clients) {
            client.closeConnection();
        }
        serverSocket.close();
        System.exit(0);
    }

    public static void main(String[] args) {
        new SafeZoneServer();
    }

    class Client extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;

        Client(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            userName = in.readLine();
            start();
        }

        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    processMessage(msg);
                }
            } catch (IOException e) {
                appendServerConsole(userName + " error: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void processMessage(String msg) {
            appendServerConsole(userName + ": " + msg);
        }

        private void closeConnection() {
            try {
                socket.close();
            } catch (IOException e) {
                appendServerConsole("Error closing connection: " + e.getMessage());
            }
        }

        String getUserName() {
            return userName;
        }
    }
}
