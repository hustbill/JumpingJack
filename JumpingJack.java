
// JumpingJack.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* A side-scroller showing how to implement background
   movement, bricks, and a jumping sprite (called 'jack)
   who can run, jump, and collide with bricks.

   Fireball shoot out from the rhs of the panel, and 
   will explode if they hit jack.

   The background is composed of multiple ribbons
   (wraparound images) which move at different rates 
   depending on how 'far back' they are in
   the scene. This effect is called parallax scrolling.

   -----
   Pausing/Resuming/Quiting are controlled via the frame's window
   listener methods.

   Active rendering is used to update the JPanel. See WormP for
   another example, with additional statistics generation.

   Using Java 3D's timer: J3DTimer.getValue()
     *  nanosecs rather than millisecs for the period

   The MidisLoader, ClipsLoader, ImagesLoader, and ImagesPlayer
   classes are used for music, images, and animation.

   The jumping and fireball sprites are subclasses of the 
   Sprite class discussed in chapter 6.
*/


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class JumpingJack extends JFrame implements WindowListener
{
  private static int DEFAULT_FPS = 30;      // 40 is too fast! 

  private JackPanel jp;        // where the game is drawn
  private MidisLoader midisLoader;


  public JumpingJack(long period)
  { super("JumpingJack");

    // load the background MIDI sequence
    midisLoader = new MidisLoader();
    midisLoader.load("jjf", "jumping_jack_flash.mid");
    midisLoader.play("jjf", true);   // repeatedly play it

    Container c = getContentPane();    // default BorderLayout used
    jp = new JackPanel(this, period);
    c.add(jp, "Center");

    addWindowListener( this );
    pack();
    setResizable(false);
    setVisible(true);
  }  // end of JumpingJack() constructor


  // ----------------- window listener methods -------------

  public void windowActivated(WindowEvent e) 
  { jp.resumeGame();  }

  public void windowDeactivated(WindowEvent e) 
  { jp.pauseGame();  }


  public void windowDeiconified(WindowEvent e) 
  {  jp.resumeGame();  }

  public void windowIconified(WindowEvent e) 
  {  jp.pauseGame(); }


  public void windowClosing(WindowEvent e)
  {  jp.stopGame();  
     midisLoader.close();  // not really required
  }


  public void windowClosed(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}

  // ----------------------------------------------------

  public static void main(String args[])
  { 
    long period = (long) 1000.0/DEFAULT_FPS;
    // System.out.println("fps: " + DEFAULT_FPS + "; period: " + period + " ms");
    new JumpingJack(period*1000000L);    // ms --> nanosecs 
  }

} // end of JumpingJack class


