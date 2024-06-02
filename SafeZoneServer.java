import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;

public class ChatProgram extends JPanel {
    private JTextArea chatArea;
    private JTextField chatField;
    private JButton sendButton;
    private PrintWriter out;

    public ChatProgram(PrintWriter out) {
        this.out = out;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(200, 200, 200));
        chatArea.setForeground(new Color(0, 0, 0));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        chatField = new JTextField();
        chatField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        sendButton = new JButton("보내기");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(chatField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String message = chatField.getText().trim();
        if (!message.isEmpty()) {
            out.println("CHAT:" + message);
            chatField.setText("");
        }
    }

    public void addMessage(String message) {
        chatArea.append(message + "\n");
    }
}
