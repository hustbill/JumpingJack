
// Brick.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* Information for a single brick.

   (mapX, mapY) are indicies in the brick map, which starts at (0,0)
   with the first row of bricks and increases to the left 
   and down.
 
   locY is the y-coordinate in the bricks ribbon, which starts
   at (0,0) at the top-left of the panel, and stretches down and 
   to the right.

   The brick is represented by a static image; animated or moveable
   bricks are not supported.
*/

import java.awt.*;
import java.awt.image.*;



public class Brick
{
  private int mapX, mapY;   // indicies in the bricks map
  private int imageID;
    /* the ID corresponds to the index position of the brick's
       image in the image strip */

  private BufferedImage image;
  private int height;   // of the brick image

  private int locY;   
     // the y- coordinate of the brick in the bricks ribbon


  public Brick(int id, int x, int y)
  { mapX = x;  mapY = y;
    imageID = id;
  }

  public int getMapX()
  {  return mapX;  }

  public int getMapY()
  {  return mapY;  }

  public int getImageID()
  {  return imageID;  }


  public void setImage(BufferedImage im)
  { image = im;
    height = im.getHeight();
  } // end of setImage()


  public void setLocY(int pHeight, int maxYBricks)
  {  locY = pHeight - ((maxYBricks-mapY) * height);  }

  public int getLocY()
  {  return locY;  }

  public void display(Graphics g, int xScr)
  // called by BricksManager's drawBricks()
  {  g.drawImage(image, xScr, locY, null);  }

}  // end of Brick class
