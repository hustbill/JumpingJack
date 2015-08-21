
// JumperSprite.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* A sprite can move left/right, jump and stand still.
   In fact, a sprite doesn't move horizontally at all, so
   the left and right movement requests only change various
   status flags, not its locx value.

   The sprite has looping images for when it is moving
   left or right, and single images for when it is
   standing still or jumping.

   The sprite stores its world coordinate in (xWorld, yWorld).

   Jumping has a rising and falling component. Rising and 
   falling can be stopped by the sprite hitting a brick.

   The sprite's movement left or right can be stopped by hitting 
   a brick.

   A sprite will start falling if it walks off a brick into space.

   Brick queries (mostly about collision detection) are sent
   to the BricksManager object.
*/

import java.awt.*;


public class JumperSprite extends Sprite
{
  private static double DURATION = 0.5;  // secs
    // total time to cycle through all the images

  private static final int NOT_JUMPING = 0;   
  private static final int RISING = 1;   
  private static final int FALLING = 2;   
  // used by vertMoveMode 
  //  (in J2SE 1.5 we could use a enumeration for these)

  private static final int MAX_UP_STEPS = 8;
    // max number of steps to take when rising upwards in a jump


  private int period;    // in ms; the game's animation period

  private boolean isFacingRight, isStill;

  private int vertMoveMode;
    /* can be NOT_JUMPING, RISING, or FALLING */
  private int vertStep;   // distance to move vertically in one step
  private int upCount;

  private BricksManager brickMan;
  private int moveSize;   // obtained from BricksManager

  private int xWorld, yWorld;
    /* the current position of the sprite in 'world' coordinates.
       The x-values may be negative. The y-values will be between
       0 and pHeight. */ 
  

  public JumperSprite(int w, int h, int brickMvSz, BricksManager bm, 
                                        ImagesLoader imsLd, int p)
  {
    super(w/2, h/2, w, h, imsLd, "runningRight");
       // standing center screen, facing right
    moveSize = brickMvSz;
          // the move size is the same as the bricks ribbon

    brickMan = bm;
    period = p;
    setStep(0,0);     // no movement

    isFacingRight = true;
    isStill = true;

    /* Adjust the sprite's y- position so it is
       standing on the brick at its mid x- psoition. */
    locy = brickMan.findFloor(locx+getWidth()/2)-getHeight();
    xWorld = locx; yWorld = locy;    // store current position

    vertMoveMode = NOT_JUMPING;
    vertStep = brickMan.getBrickHeight()/2;   
                // the jump step is half a brick's height
    upCount = 0;
  }  // end of JumperSprite()


  public void moveLeft()
  /* Request that the sprite move to the left. It doesn't
     actually move, but changes its image and status flags. */
  { setImage("runningLeft");
    loopImage(period, DURATION);   // cycle through the images
    isFacingRight = false;  isStill = false;
  }

  public void moveRight()
  /* Request that the sprite move to the right. It doesn't
     actually move, but changes its image and status flags. */
  { setImage("runningRight");
    loopImage(period, DURATION);   // cycle through the images
    isFacingRight = true;  isStill = false;
  }

  public void stayStill()
  /* Request that the sprite stops. It stops the
     image animation and sets the isStill status flag. */
  { stopLooping(); 
    isStill = true;
  }


  public void jump()
  /* The sprite is asked to jump. It sets its vertMoveMode
     to RISING, and changes its image. The y- position
     adjustment is done in updateSprite().
  */
  { if (vertMoveMode == NOT_JUMPING) { 
      vertMoveMode = RISING;
      upCount = 0;
      if (isStill) {    // only change image if the sprite is 'still'
        if (isFacingRight)
          setImage("jumpRight");
        else
          setImage("jumpLeft");
      }
    }
  } // end of jump()


  public boolean willHitBrick()
  /* Test if the next x position is inside a brick
     Ignore any y motion. This method should always
     be called before updateSprite() makes the actual
     move. */
  {
    if (isStill)
      return false;   // can't hit anything if not moving

    int xTest;   // for testing the new x- position
    if (isFacingRight)   // moving right
      xTest = xWorld + moveSize;
    else // moving left
      xTest = xWorld - moveSize;

    // test a point near the base of the sprite
    int xMid = xTest + getWidth()/2;
    int yMid = yWorld + (int)(getHeight()*0.8);   // use current y posn

    return brickMan.insideBrick(xMid,yMid);  
  }  // end of willHitBrick()


  public void updateSprite()
  /* Although the sprite is not moving in the x-direction, we 
     must still update its (xWorld, yWorld) coordinate. Also,
     if the sprite is jumping then its y position must be
     updated with moveVertically(). updateSprite() should
     only be called after collsion checking with willHitBrick()
  */
  {
    if (!isStill) {    // moving
      if (isFacingRight)  // moving right
        xWorld += moveSize;
      else // moving left
        xWorld -= moveSize;
      if (vertMoveMode == NOT_JUMPING)   // if not jumping
        checkIfFalling();   // may have moved out into empty space
    }

    // vertical movement has two components: RISING and FALLING
    if (vertMoveMode == RISING)
      updateRising();
    else if (vertMoveMode == FALLING)
      updateFalling();

    super.updateSprite();
  }  // end of updateSprite()



  private void checkIfFalling()
  /* If the left/right move has put the sprite out in thin air,
     then put it into falling mode. */
  {
    // could the sprite move downwards if it wanted to?
    // test its center x-coord, base y-coord
    int yTrans = brickMan.checkBrickTop( xWorld+(getWidth()/2), 
                     yWorld+getHeight()+vertStep, vertStep);
    // System.out.println("checkIfFalling: " + yTrans);
    if (yTrans != 0)   // yes it could
      vertMoveMode = FALLING;   // set it to be in falling mode
  }  // end of checkIfFalling()



  private void updateRising()
  /* Rising will continue until the maximum number of vertical steps
     is reached, or the sprite hits the base of a brick. The
     sprite then switches to falling mode.
  */
  { if (upCount == MAX_UP_STEPS) {
      vertMoveMode = FALLING;   // at top, now start falling
      upCount = 0;
    }
    else {
      int yTrans = brickMan.checkBrickBase(xWorld+(getWidth()/2), 
                                      yWorld-vertStep, vertStep);
      if (yTrans == 0) {   // hit the base of a brick
        vertMoveMode = FALLING;   // start falling
        upCount = 0;
      }
      else {   // can move upwards another step
        translate(0, -yTrans);
        yWorld -= yTrans;   // update position
        upCount++;
      }
    }
  }  // end of updateRising()


  private void updateFalling()
  /* Falling will continue until the sprite hits the top of 
     a brick. The game only allows a brick ribbon which has a 
     complete floor, so the sprite must eventually touch down.

     Falling mode can be entered without a corresponding rising 
     sequence, for instance, when the sprite walks off a cliff.
  */
  {
    int yTrans = brickMan.checkBrickTop(xWorld+(getWidth()/2), 
                        yWorld+getHeight()+vertStep, vertStep);
    if (yTrans == 0)   // hit the top of a brick
      finishJumping();
    else {    // can move downwards another step
      translate(0, yTrans);
      yWorld += yTrans;   // update position
    }
  }  // end of updateFalling()


  private void finishJumping()
  {
    vertMoveMode = NOT_JUMPING;
    upCount = 0;

    if (isStill) {    // change to running image, but not looping yet
      if (isFacingRight)
        setImage("runningRight");
      else    // facing left
        setImage("runningLeft");
    }
  }  // end of finishJumping()


}  // end of JumperSprite

