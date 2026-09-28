import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGameApp extends JFrame {

    public SnakeGameApp() {
        setTitle("Enhanced Animated Snake Game (Pure Java)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        pack();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SnakeGameApp::new);
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Board Dimensions (Expanded Ground Size: 600x600)
    public static final int PANEL_WIDTH = 600;
    public static final int PANEL_HEIGHT = 600;
    public static final int GRID_SIZE = 24;
    public static final int CELL_COUNT = PANEL_WIDTH / GRID_SIZE; // 25x25 grid

    // Game Entities
    private final List<Point> snake = new ArrayList<>();
    private Point food = new Point();
    private Point velocity = new Point(1, 0);
    private Point nextVelocity = new Point(1, 0);
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    // Game State
    private int score = 0;
    private int highScore = 0;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private boolean isGameOver = false;

    // Timers & Animations
    private Timer gameTimer;
    private Timer renderTimer;
    private int gameSpeed = 85; // Default medium speed (ms)
    private double animTick = 0;

    // High score storage file
    private final File highScoreFile = new File("snake_highscore.dat");

    // UI Buttons and Dropdown
    private JButton startBtn;
    private JButton pauseBtn;
    private JButton stopBtn;
    private JComboBox<String> difficultyCombo;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT + 70));
        setBackground(new Color(13, 19, 26));
        setLayout(new BorderLayout());
        setFocusable(true);
        addKeyListener(this);

        loadHighScore();
        setupControlBar();

        // Game simulation tick
        gameTimer = new Timer(gameSpeed, this);

        // Smooth 60 FPS rendering timer for fluid animations & particles
        renderTimer = new Timer(16, e -> {
            animTick += 0.15;
            updateParticles();
            repaint();
        });
        renderTimer.start();
    }

    private void setupControlBar() {
        JPanel controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(PANEL_WIDTH, 60));
        controlPanel.setBackground(new Color(16, 25, 36));
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 12));

        startBtn = createStyledButton("Start", new Color(0, 255, 170), Color.BLACK);
        pauseBtn = createStyledButton("Pause", new Color(51, 65, 85), Color.WHITE);
        stopBtn = createStyledButton("Stop", new Color(255, 71, 87), Color.WHITE);

        String[] levels = {"Easy", "Medium", "Fast"};
        difficultyCombo = new JComboBox<>(levels);
        difficultyCombo.setSelectedIndex(1);
        difficultyCombo.setBackground(new Color(30, 41, 59));
        difficultyCombo.setForeground(new Color(0, 255, 170));
        difficultyCombo.setFocusable(false);

        startBtn.addActionListener(e -> {
            initGame();
            requestFocusInWindow();
        });

        pauseBtn.addActionListener(e -> {
            togglePause();
            requestFocusInWindow();
        });

        stopBtn.addActionListener(e -> {
            stopGame();
            requestFocusInWindow();
        });

        difficultyCombo.addActionListener(e -> {
            String selected = (String) difficultyCombo.getSelectedItem();
            if ("Easy".equals(selected)) gameSpeed = 125;
            else if ("Medium".equals(selected)) gameSpeed = 85;
            else if ("Fast".equals(selected)) gameSpeed = 50;

            if (gameTimer.isRunning()) {
                gameTimer.setDelay(gameSpeed);
            }
            requestFocusInWindow();
        });

        controlPanel.add(startBtn);
        controlPanel.add(pauseBtn);
        controlPanel.add(stopBtn);
        controlPanel.add(new JLabel("Speed: ") {{ setForeground(Color.LIGHT_GRAY); }});
        controlPanel.add(difficultyCombo);

        add(controlPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void initGame() {
        snake.clear();
        snake.add(new Point(12, 12));
        snake.add(new Point(11, 12));
        snake.add(new Point(10, 12));

        velocity = new Point(1, 0);
        nextVelocity = new Point(1, 0);
        score = 0;
        isGameOver = false;
        isPaused = false;
        isRunning = true;
        particles.clear();
        pauseBtn.setText("Pause");

        spawnFood();
        gameTimer.setDelay(gameSpeed);
        gameTimer.start();
    }

    private void spawnFood() {
        boolean valid = false;
        while (!valid) {
            int fx = random.nextInt(CELL_COUNT);
            int fy = random.nextInt(CELL_COUNT);
            Point p = new Point(fx, fy);
            if (!snake.contains(p)) {
                food = p;
                valid = true;
            }
        }
    }

    private void togglePause() {
        if (!isRunning || isGameOver) return;
        isPaused = !isPaused;
        if (isPaused) {
            gameTimer.stop();
            pauseBtn.setText("Resume");
        } else {
            gameTimer.start();
            pauseBtn.setText("Pause");
        }
    }

    private void stopGame() {
        isRunning = false;
        isPaused = false;
        gameTimer.stop();
        pauseBtn.setText("Pause");
    }

    private void triggerGameOver() {
        isGameOver = true;
        isRunning = false;
        gameTimer.stop();
        createParticles(snake.get(0).x, snake.get(0).y, new Color(255, 71, 87));
    }

    private void createParticles(int gridX, int gridY, Color color) {
        int originX = gridX * GRID_SIZE + GRID_SIZE / 2;
        int originY = gridY * GRID_SIZE + GRID_SIZE / 2;
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 3.5;
            particles.add(new Particle(originX, originY, Math.cos(angle) * speed, Math.sin(angle) * speed, color));
        }
    }

    private void updateParticles() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.alpha <= 0) {
                particles.remove(i);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isRunning || isPaused || isGameOver) return;

        velocity = new Point(nextVelocity);
        Point head = snake.get(0);
        Point newHead = new Point(head.x + velocity.x, head.y + velocity.y);

        // Wall collision check
        if (newHead.x < 0 || newHead.x >= CELL_COUNT || newHead.y < 0 || newHead.y >= CELL_COUNT) {
            triggerGameOver();
            return;
        }

        // Tail collision check
        if (snake.contains(newHead)) {
            triggerGameOver();
            return;
        }

        snake.add(0, newHead);

        // Food eaten check
        if (newHead.equals(food)) {
            score += 10;
            if (score > highScore) {
                highScore = score;
                saveHighScore();
            }
            createParticles(food.x, food.y, new Color(0, 255, 170));
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2);
        drawFood(g2);
        drawSnake(g2);
        drawParticles(g2);
        drawHUD(g2);

        if (!isRunning && !isGameOver) {
            drawCenteredOverlay(g2, "PRESS START TO PLAY", new Color(0, 255, 170));
        } else if (isPaused) {
            drawCenteredOverlay(g2, "PAUSED", new Color(255, 211, 42));
        } else if (isGameOver) {
            drawCenteredOverlay(g2, "GAME OVER", new Color(255, 71, 87));
        }
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 8));
        for (int i = 0; i <= PANEL_WIDTH; i += GRID_SIZE) {
            g2.drawLine(i, 0, i, PANEL_HEIGHT);
            g2.drawLine(0, i, PANEL_WIDTH, i);
        }
    }

    private void drawFood(Graphics2D g2) {
        int cx = food.x * GRID_SIZE + GRID_SIZE / 2;
        int cy = food.y * GRID_SIZE + GRID_SIZE / 2;
        double pulse = Math.sin(animTick) * 2.0;
        int radius = (int) (GRID_SIZE / 2 - 2 + pulse);

        // Food Outer Glow
        g2.setColor(new Color(255, 71, 87, 80));
        g2.fill(new Ellipse2D.Double(cx - radius - 3, cy - radius - 3, (radius + 3) * 2, (radius + 3) * 2));

        // Core Food
        g2.setColor(new Color(255, 71, 87));
        g2.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    private void drawSnake(Graphics2D g2) {
        if (snake.isEmpty()) return;

        // Draw body segments (tail to neck)
        for (int i = snake.size() - 1; i > 0; i--) {
            Point seg = snake.get(i);
            int cx = seg.x * GRID_SIZE + GRID_SIZE / 2;
            int cy = seg.y * GRID_SIZE + GRID_SIZE / 2;
            int radius = GRID_SIZE / 2 - 2;

            boolean isEven = (i % 2 == 0);
            Color bodyColor = isEven ? new Color(0, 184, 148) : new Color(0, 148, 114);
            Color scaleColor = isEven ? new Color(85, 239, 196) : new Color(0, 206, 201);

            // Base scale circle
            g2.setColor(bodyColor);
            g2.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));

            // Interior dorsal diamond scale pattern
            g2.setColor(scaleColor);
            int innerRadius = radius / 2;
            g2.fill(new Ellipse2D.Double(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2));
        }

        // Draw dynamic viper head
        Point head = snake.get(0);
        int hx = head.x * GRID_SIZE + GRID_SIZE / 2;
        int hy = head.y * GRID_SIZE + GRID_SIZE / 2;

        Graphics2D gh = (Graphics2D) g2.create();
        gh.translate(hx, hy);

        double angle = 0;
        if (velocity.x == 1) angle = 0;
        else if (velocity.x == -1) angle = Math.PI;
        else if (velocity.y == 1) angle = Math.PI / 2;
        else if (velocity.y == -1) angle = -Math.PI / 2;
        gh.rotate(angle);

        // Animated flickering tongue
        double tongueWiggle = Math.sin(animTick * 2.5) * 3;
        gh.setColor(new Color(255, 56, 56));
        gh.setStroke(new BasicStroke(2));
        int headEdge = GRID_SIZE / 2;
        gh.drawLine(headEdge, 0, headEdge + 6, (int) tongueWiggle);
        gh.drawLine(headEdge + 6, (int) tongueWiggle, headEdge + 10, (int) tongueWiggle - 2);
        gh.drawLine(headEdge + 6, (int) tongueWiggle, headEdge + 10, (int) tongueWiggle + 2);

        // Viper head shape
        gh.setColor(new Color(0, 230, 118));
        gh.fillOval(-GRID_SIZE / 2, -GRID_SIZE / 2 + 1, GRID_SIZE, GRID_SIZE - 2);

        // Eyes (Black Sclera)
        gh.setColor(Color.BLACK);
        gh.fillOval(2, -6, 5, 5);
        gh.fillOval(2, 2, 5, 5);

        // Pupils (Yellow Slits)
        gh.setColor(Color.YELLOW);
        gh.fillRect(4, -5, 2, 3);
        gh.fillRect(4, 3, 2, 3);

        gh.dispose();
    }

    private void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (p.alpha * 255)));
            g2.fill(new Ellipse2D.Double(p.x, p.y, 4, 4));
        }
    }

    private void drawHUD(Graphics2D g2) {
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2.setColor(new Color(0, 255, 170));
        g2.drawString("Score: " + score, 20, 30);

        String highStr = "High Score: " + highScore;
        int strWidth = g2.getFontMetrics().stringWidth(highStr);
        g2.drawString(highStr, PANEL_WIDTH - strWidth - 20, 30);
    }

    private void drawCenteredOverlay(Graphics2D g2, String msg, Color color) {
        g2.setColor(new Color(8, 12, 16, 215));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 30));
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int x = (PANEL_WIDTH - fm.stringWidth(msg)) / 2;
        int y = (PANEL_HEIGHT / 2) - 10;
        g2.drawString(msg, x, y);
    }

    private void loadHighScore() {
        if (!highScoreFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(highScoreFile))) {
            highScore = Integer.parseInt(br.readLine());
        } catch (Exception ignored) {}
    }

    private void saveHighScore() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(highScoreFile))) {
            bw.write(String.valueOf(highScore));
        } catch (Exception ignored) {}
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if ((key == KeyEvent.VK_UP || key == KeyEvent.VK_W) && velocity.y == 0) {
            nextVelocity = new Point(0, -1);
        } else if ((key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) && velocity.y == 0) {
            nextVelocity = new Point(0, 1);
        } else if ((key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) && velocity.x == 0) {
            nextVelocity = new Point(-1, 0);
        } else if ((key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) && velocity.x == 0) {
            nextVelocity = new Point(1, 0);
        } else if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_P) {
            togglePause();
        } else if (key == KeyEvent.VK_R) {
            initGame();
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    // Inner class for visual burst animations
    private static class Particle {
        double x, y;
        double vx, vy;
        float alpha = 1.0f;
        Color color;

        public Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
        }

        public void update() {
            x += vx;
            y += vy;
            alpha -= 0.035f;
            if (alpha < 0) alpha = 0;
        }
    }
}