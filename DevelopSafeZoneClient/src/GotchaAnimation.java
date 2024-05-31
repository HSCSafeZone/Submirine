import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.io.IOException;
import java.awt.image.BufferedImage;

class AnimationPanel extends JPanel implements ActionListener, MouseListener { // MouseListener 추가
    private final int WIDTH = 300;
    private final int HEIGHT = 200;
    private final int START_X = 130;
    private final int START_Y = -10;
    private final int RESET_Y = 25; // 이미지를 다시 시작할, 도달한 y 위치
    private BufferedImage image;
    private Timer timer;
    private double x, y;
    private int imgWidth, imgHeight;
    
    public AnimationPanel() {
    	JLabel textLabel0 = new JLabel("지뢰를 찾았습니다!");
    	textLabel0.setFont(new Font("Plain", Font.BOLD, 20)); 
    	JLabel textLabel1 = new JLabel("(이 창을 닫으려면 아무 곳이나 클릭하세요..)");
    	textLabel0.setAlignmentX(Component.CENTER_ALIGNMENT);
    	textLabel1.setAlignmentX(Component.CENTER_ALIGNMENT);
    	add(textLabel0);
    	add(textLabel1);
    	
        setBackground(new Color(238, 238, 238));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setDoubleBuffered(true);
        File input = new File("Img//landMineImg.png");
        try {
            image = ImageIO.read(input);
        } catch(IOException e) {
            e.printStackTrace();
        } 
        x = START_X;
        y = START_Y;
        imgWidth = 150;
        imgHeight = 120;
        
        timer = new Timer(10, this);
        timer.start();
        
        addMouseListener(this); // MouseListener 추가
    }
    
    // 그래픽을 출력하는 메소드
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, (int)x, (int)y, imgWidth, imgHeight, this);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        x -= 1.0;
        y += 0.5; // y는 -일수록 높아짐
        imgWidth += 2;
        imgHeight += 2;
        if (y == RESET_Y) {
            x = START_X;
            y = START_Y;
            imgWidth = 150;
            imgHeight = 120;
        }
        repaint();
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
    	JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        topFrame.dispose(); // 클릭 시 창만 닫음
    }
    
    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
}

public class GotchaAnimation extends JFrame {
    public GotchaAnimation() {
        add(new AnimationPanel());
        setTitle("애니메이션 테스트");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
            }
    
    public static void main(String[] args) {
        new GotchaAnimation();
    }
}
