
// RibbonsManager.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* RibbonsManager manages many ribbons (wraparound images 
   used for the game's background). 

   Ribbons 'further back' move slower than ones nearer the
   foreground of the game, creating a parallax distance effect.

   When a sprite is instructed to move left or right, the 
   sprite doesn't actually move, instead the ribbons move in
   the _opposite_direction (right or left).

*/

import java.awt.*;


public class RibbonsManager
{
  /* Background ribbons images, and their movement factors.
     Ribbons 'further back' are specified first in ribImages[], 
     and have smaller move factors so they will move slower.
  */
  private String ribImages[] = {"mountains", "houses", "trees"};
  private double moveFactors[] = {0.1, 0.5, 1.0};  // applied to moveSize
     // a move factor of 0 would make a ribbon stationary

  private Ribbon[] ribbons;
  private int numRibbons;
  private int moveSize;
     // standard distance for a ribbon to 'move' each tick


  public RibbonsManager(int w, int h, int brickMvSz, ImagesLoader imsLd)
  {
    moveSize = brickMvSz;
          // the basic move size is the same as the bricks ribbon

    numRibbons = ribImages.length;
    ribbons = new Ribbon[numRibbons];

    for (int i = 0; i < numRibbons; i++)
       ribbons[i] = new Ribbon(w, h, imsLd.getImage( ribImages[i] ),
						(int) (moveFactors[i]*moveSize) );
  }  // end of RibbonsManager()


  public void moveRight()
  { for (int i=0; i < numRibbons; i++)
      ribbons[i].moveRight();
  }

  public void moveLeft()
  { for (int i=0; i < numRibbons; i++)
      ribbons[i].moveLeft();
  }

  public void stayStill()
  { for (int i=0; i < numRibbons; i++)
      ribbons[i].stayStill();
  }


  public void update()
  { for (int i=0; i < numRibbons; i++)
      ribbons[i].update();
  }

  public void display(Graphics g)
  /* The display order is important.
     Display ribbons from the back to the front of the scene. */
  { for (int i=0; i < numRibbons; i++)
      ribbons[i].display(g);
  }

} // end of RibbonsManager

