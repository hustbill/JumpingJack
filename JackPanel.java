
// JackPanel.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* The game's drawing surface. Uses active rendering to a JPanel
   with the help of Java 3D's timer.

   Set up the background and sprites, and update and draw
   them every period nanosecs.

   The background is a series of ribbons (wraparound images
   that move), and a bricks ribbon which the JumpingSprite
   (called 'jack') runs and jumps along.

   'Jack' doesn't actually move horizontally, but the movement
   of the background gives the illusion that it is.

   There is a fireball sprite which tries to hit jack. It shoots
   out horizontally from the right hand edge of the panel. After
   MAX_HITS hits, the game is over. Each hit is accompanied 
   by an animated explosion and sound effect.

   The game begins with a simple introductory screen, which
   doubles as a help window during the course of play. When
   the help is shown, the game pauses.

   The game is controlled only from the keyboard, no mouse
   events are caught.
*/

import javax.swing.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.*;
import com.sun.j3d.utils.timer.J3DTimer;


public class JackPanel extends JPanel 
                     implements Runnable, ImagesPlayerWatcher
{
  private static final int PWIDTH = 500;   // size of panel
  private static final int PHEIGHT = 360; 

  private static final int NO_DELAYS_PER_YIELD = 16;
  /* Number of frames with a delay of 0 ms before the animation thread yields
     to other running threads. */
  private static final int MAX_FRAME_SKIPS = 5;
    // no. of frames that can be skipped in any one animation loop
    // i.e the games state is updated but not rendered

  // image, bricks map, clips loader information files
  private static final String IMS_INFO = "imsInfo.txt";
  private static final String BRICKS_INFO = "bricksInfo.txt";
  private static final String SNDS_FILE = "clipsInfo.txt";

  // names of the explosion clips
  private static final String[] exploNames ={"explo1", "explo2", "explo3"};

  private static final int MAX_HITS = 20;
    // number of times jack can be hit by a fireball before the game is over


  private Thread animator;           // the thread that performs the animation
  private volatile boolean running = false;   // used to stop the animation thread
  private volatile boolean isPaused = false;

  private long period;                // period between drawing in _nanosecs_

  private JumpingJack jackTop;
  private ClipsLoader clipsLoader;

  private JumperSprite jack;          // the sprites
  private FireBallSprite fireball;
  private RibbonsManager ribsMan;     // the ribbons manager
  private BricksManager bricksMan;    // the bricks manager

  private long gameStartTime;   // when the game started
  private int timeSpentInGame;

  // used at game termination
  private volatile boolean gameOver = false;
  private int score = 0;

  // for displaying messages
  private Font msgsFont;
  private FontMetrics metrics;

  // off-screen rendering
  private Graphics dbg; 
  private Image dbImage = null;

  // to display the title/help screen
  private boolean showHelp;
  private BufferedImage helpIm;

  // explosion-related
  private ImagesPlayer explosionPlayer = null;
  private boolean showExplosion = false;
  private int explWidth, explHeight;   // image dimensions
  private int xExpl, yExpl;   // coords where image is drawn

  private int numHits = 0;   // the number of times 'jack' has been hit



  public JackPanel(JumpingJack jj, long period)
  {
    jackTop = jj;
    this.period = period;

    setDoubleBuffered(false);
    setBackground(Color.white);
    setPreferredSize( new Dimension(PWIDTH, PHEIGHT));

    setFocusable(true);
    requestFocus();    // the JPanel now has focus, so receives key events

	addKeyListener( new KeyAdapter() {
       public void keyPressed(KeyEvent e)
       { processKey(e);  }
     });

    // initialise the loaders
    ImagesLoader imsLoader = new ImagesLoader(IMS_INFO); 
    clipsLoader = new ClipsLoader(SNDS_FILE); 

    // initialise the game entities
    bricksMan = new BricksManager(PWIDTH, PHEIGHT, BRICKS_INFO, imsLoader);
    int brickMoveSize = bricksMan.getMoveSize();

    ribsMan = new RibbonsManager(PWIDTH, PHEIGHT, brickMoveSize, imsLoader);

    jack = new JumperSprite(PWIDTH, PHEIGHT, brickMoveSize, bricksMan, 
                               imsLoader, (int)(period/1000000L) ); // in ms

    fireball = new FireBallSprite(PWIDTH, PHEIGHT, imsLoader, this, jack);


    // prepare the explosion animation
    explosionPlayer =  new ImagesPlayer("explosion", (int)(period/1000000L), 
                                                0.5, false, imsLoader);
    BufferedImage explosionIm = imsLoader.getImage("explosion");
    explWidth = explosionIm.getWidth();
    explHeight = explosionIm.getHeight();
    explosionPlayer.setWatcher(this);     // report animation's end back here

    // prepare title/help screen
    helpIm = imsLoader.getImage("title");
    showHelp = true;    // show at start-up
    isPaused = true;

    // set up message font
    msgsFont = new Font("SansSerif", Font.BOLD, 24);
    metrics = this.getFontMetrics(msgsFont);
  }  // end of JackPanel()


  private void processKey(KeyEvent e)
  // handles termination, help, and game-play keys
  {
    int keyCode = e.getKeyCode();

    // termination keys
	// listen for esc, q, end, ctrl-c on the canvas to
	// allow a convenient exit from the full screen configuration
    if ((keyCode == KeyEvent.VK_ESCAPE) || (keyCode == KeyEvent.VK_Q) ||
        (keyCode == KeyEvent.VK_END) ||
        ((keyCode == KeyEvent.VK_C) && e.isControlDown()) )
      running = false;

    // help controls
    if (keyCode == KeyEvent.VK_H) {
      if (showHelp) {  // help being shown
        showHelp = false;  // switch off
        isPaused = false;
      }
      else {  // help not being shown
       showHelp = true;    // show it
       isPaused = true;    // isPaused may already be true
      }
    }

    // game-play keys
    if (!isPaused && !gameOver) {
      // move the sprite and ribbons based on the arrow key pressed
      if (keyCode == KeyEvent.VK_LEFT) {
        jack.moveLeft(); 
        bricksMan.moveRight();   // bricks and ribbons move the other way
        ribsMan.moveRight();
      }
      else if (keyCode == KeyEvent.VK_RIGHT) {
        jack.moveRight();
        bricksMan.moveLeft();
        ribsMan.moveLeft();
      } 
      else if (keyCode == KeyEvent.VK_UP)
        jack.jump();    // jumping has no effect on the bricks/ribbons
      else if (keyCode == KeyEvent.VK_DOWN) {
        jack.stayStill();
        bricksMan.stayStill();
        ribsMan.stayStill();
      }
    }
  }  // end of processKey()


  public void showExplosion(int x, int y)
  // called by fireball sprite when it hits jack at (x,y)
  { if (!showExplosion) {  // only allow a single explosion at a time
      showExplosion = true;
      xExpl = x - explWidth/2;   //\ (x,y) is the center of the explosion
      yExpl = y - explHeight/2;

      /* Play an explosion clip, but cycle through them.
         This adds variety, and gets round not being able to 
         play multiple instances of a clip at the same time. */
      clipsLoader.play( exploNames[numHits%exploNames.length], false);
      numHits++;
    } 
  } // end of showExplosion()


  public void sequenceEnded(String imageName)
  // called by ImagesPlayer when the explosion animation finishes
  {  showExplosion = false;
     explosionPlayer.restartAt(0);   // reset animation for next time

     if (numHits >= MAX_HITS) { 
       gameOver = true; 
       score = (int) ((J3DTimer.getValue() - gameStartTime)/1000000000L);
      clipsLoader.play("applause", false);

     }
  } // end of sequenceEnded()



  public void addNotify()
  // wait for the JPanel to be added to the JFrame before starting
  { super.addNotify();   // creates the peer
    startGame();         // start the thread
  }


  private void startGame()
  // initialise and start the thread 
  { 
    if (animator == null || !running) {
      animator = new Thread(this);
	  animator.start();
    }
  } // end of startGame()
    

  // ------------- game life cycle methods ------------
  // called by the JFrame's window listener methods


  public void resumeGame()
  // called when the JFrame is activated / deiconified
  { if (!showHelp)    // CHANGED
      isPaused = false;  
  } 


  public void pauseGame()
  // called when the JFrame is deactivated / iconified
  { isPaused = true;   } 


  public void stopGame() 
  // called when the JFrame is closing
  {  running = false;   }

  // ----------------------------------------------

  public void run()
  /* The frames of the animation are drawn inside the while loop. */
  {
    long beforeTime, afterTime, timeDiff, sleepTime;
    long overSleepTime = 0L;
    int noDelays = 0;
    long excess = 0L;

    gameStartTime = J3DTimer.getValue();
    beforeTime = gameStartTime;

	running = true;

	while(running) {
	  gameUpdate();
      gameRender();
      paintScreen();

      afterTime = J3DTimer.getValue();
      timeDiff = afterTime - beforeTime;
      sleepTime = (period - timeDiff) - overSleepTime;  

      if (sleepTime > 0) {   // some time left in this cycle
        try {
          Thread.sleep(sleepTime/1000000L);  // nano -> ms
        }
        catch(InterruptedException ex){}
        overSleepTime = (J3DTimer.getValue() - afterTime) - sleepTime;
      }
      else {    // sleepTime <= 0; the frame took longer than the period
        excess -= sleepTime;  // store excess time value
        overSleepTime = 0L;

        if (++noDelays >= NO_DELAYS_PER_YIELD) {
          Thread.yield();   // give another thread a chance to run
          noDelays = 0;
        }
      }

      beforeTime = J3DTimer.getValue();

      /* If frame animation is taking too long, update the game state
         without rendering it, to get the updates/sec nearer to
         the required FPS. */
      int skips = 0;
      while((excess > period) && (skips < MAX_FRAME_SKIPS)) {
        excess -= period;
	    gameUpdate();    // update state but don't render
        skips++;
      }
	}
    System.exit(0);   // so window disappears
  } // end of run()


  private void gameUpdate() 
  { 
    if (!isPaused && !gameOver) {
      if (jack.willHitBrick()) { // collision checking first
        jack.stayStill();    // stop jack and scenery
        bricksMan.stayStill();
        ribsMan.stayStill();
      }
      ribsMan.update();   // update background and sprites
      bricksMan.update();
      jack.updateSprite();
      fireball.updateSprite();

      if (showExplosion)
        explosionPlayer.updateTick();  // update the animation
    }
  }  // end of gameUpdate()


  private void gameRender()
  {
    if (dbImage == null){
      dbImage = createImage(PWIDTH, PHEIGHT);
      if (dbImage == null) {
        System.out.println("dbImage is null");
        return;
      }
      else
        dbg = dbImage.getGraphics();
    }

    // draw a white background
    dbg.setColor(Color.white);
    dbg.fillRect(0, 0, PWIDTH, PHEIGHT);

    // draw the game elements: order is important
    ribsMan.display(dbg);       // the background ribbons
    bricksMan.display(dbg);     // the bricks
    jack.drawSprite(dbg);       // the sprites
    fireball.drawSprite(dbg); 

    if (showExplosion)      // draw the explosion (in front of jack)
      dbg.drawImage(explosionPlayer.getCurrentImage(), xExpl, yExpl, null);

    reportStats(dbg);

    if (gameOver)
      gameOverMessage(dbg);

    if (showHelp)    // draw the help at the very front (if switched on)
      dbg.drawImage(helpIm, (PWIDTH-helpIm.getWidth())/2, 
                          (PHEIGHT-helpIm.getHeight())/2, null);
  }  // end of gameRender()


  private void reportStats(Graphics g)
  // Report the number of hits, and time spent playing
  {
    if (!gameOver)    // stop incrementing the timer once the game is over
      timeSpentInGame = 
          (int) ((J3DTimer.getValue() - gameStartTime)/1000000000L);  // ns --> secs
	g.setColor(Color.red);
    g.setFont(msgsFont);
	g.drawString("Hits: " + numHits + "/" + MAX_HITS, 15, 25);
	g.drawString("Time: " + timeSpentInGame + " secs", 15, 50);
	g.setColor(Color.black);
  }  // end of reportStats()


  private void gameOverMessage(Graphics g)
  // Center the game-over message in the panel.
  {
    String msg = "Game Over. Your score: " + score;

	int x = (PWIDTH - metrics.stringWidth(msg))/2; 
	int y = (PHEIGHT - metrics.getHeight())/2;
	g.setColor(Color.black);
    g.setFont(msgsFont);
	g.drawString(msg, x, y);
  }  // end of gameOverMessage()


  private void paintScreen()
  // use active rendering to put the buffered image on-screen
  { 
    Graphics g;
    try {
      g = this.getGraphics();
      if ((g != null) && (dbImage != null))
        g.drawImage(dbImage, 0, 0, null);
      // Sync the display on some systems.
      // (on Linux, this fixes event queue problems)
      Toolkit.getDefaultToolkit().sync();
      g.dispose();
    }
    catch (Exception e)
    { System.out.println("Graphics context error: " + e);  }
  } // end of paintScreen()


}  // end of JackPanel class
