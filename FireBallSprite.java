
// FireBallSprite.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* A fireball starts at the lower right hand side of the panel,
   and travels straight across to the left (at varying speeds).
   If it hits 'jack', it explodes (with a suitable explosion sound).

   A fireball that has left the left hand side, or exploded, is
   reused.
*/

import java.awt.*;


public class FireBallSprite extends Sprite
{
  // the ball's x- and y- step values are STEP +/- STEP_OFFSET
  private static final int STEP = -10;   // moving left
  private static final int STEP_OFFSET = 2;

  private JackPanel jp;    // tell JackPanel about colliding with jack
  private JumperSprite jack;


  public FireBallSprite(int w, int h, ImagesLoader imsLd,
                               JackPanel jp, JumperSprite j) 
  { super( w, h/2, w, h, imsLd, "fireball");  
        // the ball is positioned in the middle at the panel's rhs
    this.jp = jp;
    jack = j;
    initPosition();
  } // end of FireBallSprite()


  private void initPosition()
  // adjust the fireball's position and its movement left
  {
    int h = getPHeight()/2 + ((int)(getPHeight() * Math.random())/2);
                     // along the lower half of the rhs edge
    if (h + getHeight() > getPHeight())
      h -= getHeight();    // so all on screen

    setPosition(getPWidth(), h);   
    setStep(STEP + getRandRange(STEP_OFFSET), 0);   // move left
  } // end of initPosition()


  private int getRandRange(int x) 
  // random number generator between -x and x
  {   return ((int)(2 * x * Math.random())) - x;  }



  public void updateSprite() 
  { hasHitJack();
    goneOffScreen();
    super.updateSprite();
  }


  private void hasHitJack()
  /* If the ball has hit jack, tell JackPanel (which will
     display an explosion and play a clip), and begin again.
  */
  { 
    Rectangle jackBox = jack.getMyRectangle();
    jackBox.grow(-jackBox.width/3, 0);   // make jack's bounded box thinner

    if (jackBox.intersects( getMyRectangle() )) {    // jack collision?
      jp.showExplosion(locx, locy+getHeight()/2);  
             // tell JackPanel, supplying it with a hit coordinate
      initPosition();
    }
  } // end of hasHitJack()


  private void goneOffScreen()
  // when the ball has gone off the lhs, start it again.
  {
    if (((locx+getWidth()) <= 0) && (dx < 0)) // off left and moving left
      initPosition();   // start the ball in a new position
  }  // end of goneOffScreen()


}  // end of FireBallSprite class
