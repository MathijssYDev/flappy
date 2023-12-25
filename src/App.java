import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class App {

    public static void main(String[] args) {
        Game game = new Game(1600, 900, "Flappy");
        game.start();
    }
}

class Display {

    public JFrame frame;
    public Canvas canvas;
    public int width, height;

    public Display(int width, int height, String title, Game game) {
        this.width = width;
        this.height = height;

        frame = new JFrame();
        frame.setTitle(title);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        canvas = new Canvas();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    game.spaceKey = true;
                }
            } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    game.spaceKey = false;
                }
            }
            return false;
        });
        canvas.setFocusable(true);

        canvas.setPreferredSize(new Dimension(width, height));
        canvas.setMinimumSize(new Dimension(width, height));
        canvas.setMaximumSize(new Dimension(width, height));

        frame.add(canvas);
        frame.pack();

    }

}

class Game implements Runnable {
    float speed = 350;
    float defaultSpawnLocationX = 1600f;
    int distanceBeforeNewSpawn = 650;
    int flappyPosition = 550;
    short difficulty = 0;
    int MinimalJumpSize = 275;
    int ColumnWidth = 125;
    float VelocityIncreaseJump = 7f;
    float gravity = 2f;
    int flappyR = 35;

    ArrayList<ArrayList<Object>> Objects = new ArrayList<>();
    Random rand = new Random();

    double deltaTime = 0;
    public boolean gamerunning = false;
    public int width, height, fps = 60;
    private String title;
    public Display display;
    double Velocity = 0;
    double flappyY = -100;
    public boolean spaceKey = false;
    long starttime;
    boolean stop = false;
    long score = 0;
    long highscore = 0;

    public Thread thread;
    public boolean running = false;
    private BufferStrategy bs;
    private Graphics g;

    public Game(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;

        init();
    }

    private void init() {
        display = new Display(width, height, title, this);
    }

    short lastToFinish = -1;
    short previous = 0;
    boolean last = false;

    private void tick() {
        if (spaceKey && !gamerunning) {
            gamerunning = true;
            starttime = System.currentTimeMillis();

        }
        if (stop && gamerunning) {
            Velocity = 0;
            flappyY = display.frame.getHeight() / 2;
            Objects.clear();
            deltaTime = 0;
            stop = false;
        }
        if (spaceKey) {
            if (last == false) {
                Velocity = 0;
            }
            last = true;
            Velocity = 0.3f;
        }
        if (!gamerunning)
            return;
        if (!spaceKey) {
            last = false;
        }
        Velocity -= gravity * deltaTime;
        flappyY -= Velocity;

        if (Objects.size() == 0) {
            Objects.add(new ArrayList<Object>(Arrays.asList(defaultSpawnLocationX, 0, ColumnWidth)));
        }
        for (int x = 0; x < Objects.size(); x++) {
            ArrayList<Object> i = Objects.get(x);
            float locx = (float) i.get(0);

            if (locx <= distanceBeforeNewSpawn && lastToFinish != (short) x) {
                lastToFinish = (short) x;
                int r = rand.nextInt(500) - 250;
                int posX = ((lastToFinish * difficulty + (short) r) / (1 + difficulty));
                Objects.add(new ArrayList<Object>(Arrays.asList(defaultSpawnLocationX, posX, ColumnWidth)));
            }

            if (locx <= -ColumnWidth) {
                lastToFinish = (short) -1;
                Objects.remove(x);
            }
            int ypos = (int) i.get(1);
            i.set(0, (float) ((float) i.get(0) - speed * deltaTime));
            if (((locx < flappyPosition + flappyR))
                    && ((flappyY - flappyR * 2 < display.height / 2 - MinimalJumpSize / 2 + ypos)
                            || flappyY > display.height / 2 + MinimalJumpSize / 2 + ypos)
                    && !(locx + ColumnWidth < flappyPosition - flappyR) || flappyY >= height) {
                gamerunning = false;
                stop = true;
                score = System.currentTimeMillis() - starttime;
                if (highscore < score) {
                    highscore = score;
                }
            }

        }

    }

    private void render() {
        bs = display.canvas.getBufferStrategy();
        if (bs == null) {
            display.canvas.createBufferStrategy(3);
            return;
        }

        g = bs.getDrawGraphics();
        g.clearRect(0, 0, width, height);
        g.setColor(Color.darkGray);
        for (ArrayList<Object> i : Objects) {
            int squareHeight = display.height / 2 - MinimalJumpSize / 2;
            g.fillRect(Math.round((float) i.get(0)), 0, ColumnWidth, squareHeight + (int) i.get(1));
            g.fillRect(Math.round((float) i.get(0)), display.height - squareHeight + (int) i.get(1), ColumnWidth,
                    squareHeight - (int) i.get(1));
        }

        g.setColor(Color.ORANGE);

        if (flappyY == -100) {
            flappyY = display.frame.getHeight() / 2;
        }
        g.fillRect(flappyPosition - flappyR, (int) Math.round(flappyY - flappyR * 2), flappyR * 2, flappyR * 2);
        long starttimes = score;
        if (!gamerunning) {
            g.setColor(new Color(0, 0, 0, 128));
            g.fillRect(0, 0, width, height);
        } else {
            starttimes = System.currentTimeMillis() - starttime;
        }

        g.setColor(Color.BLACK);
        g.drawString("Score: " + String.valueOf(starttimes), 10, 10);
        g.drawString("Highscore: " + String.valueOf(highscore), 10, 30);
        bs.show();
        g.dispose();
    }

    public void run() {
        double lastDeltaTime = 0;
        while (running) {
            deltaTime = (System.nanoTime() - lastDeltaTime) / 1_000_000_000.0D;
            lastDeltaTime = System.nanoTime();
            tick();
            render();
        }

        stop();
    }

    public synchronized void start() {
        if (running)
            return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void stop() {
        if (!running)
            return;
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}