
// BricksManager.java
// Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th

/* Loads and manages a wraparound bricks map. 
   It also deals with various collision detection tests from
   JumperSprite.

   A 'bricks map' is read in from a configuration file, and used
   to make an ArrayList of Brick objects, bricksList. 

   The collection of bricks defines a bricks map, which is moved
   and drawn in the same way as an image ribbon in a Ribbon object.

   The bricks map movement step is fixed by moveSize, which
   is some fraction of the width of a brick image.

   ----
   JumperSprite uses BricksManager for collision detection.
   As it rises/falls it must curtail the movement if it will
   enter a brick; checkBrickBase() and checkBrickTop() are used
   for testing this.

   When JumperSprite moves left/right, it must first check that it will
   not move into a brick. It uses insideBrick() for this test.

   When JumperSprite does move, it may move off into space -- there
   is no brick below it. It tests this using insideBrick() also.
*/

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;



public class BricksManager
{
  private final static String IMAGE_DIR = "Images/";
  private final static int MAX_BRICKS_LINES = 15;
     // maximum number of lines (rows) of bricks in the scene

  private final static double MOVE_FACTOR = 0.25;  
     // modifies how fast the bricks map moves; smaller is slower

  private int pWidth, pHeight;    // dimensions of display panel
  private int width, height;      // max dimensions of bricks map
                                  // width > pWidth

  private int imWidth, imHeight;  // dimensions of a brick image
  private int numCols, numRows;   
      // number of bricks in the x- and y- dimensions

  private int moveSize;       
      // size of the map move (in pixels) in each tick
      // moveSize == the width of a brick image * MOVE_FACTOR
      /* The ribbons and 'jack' use the same moveSize value. */

  private boolean isMovingRight;  // movement flags
  private boolean isMovingLeft;

  private int xMapHead;    
     /* The x-coord in the panel where the start of the bricks map
        (its head) should be drawn. 
        It can range between -width to width (exclusive), so can
        have a value beyond the confines of the panel (0-pWidth).

        As xMapHead varies, the on-screen map will usually
        be a combination of its tail followed by its head.

        xMapHead plays the same role as xImHead in a map object.
     */


  private ArrayList bricksList;   
    // stores Brick objects which makes up the brick map

  private ArrayList[] columnBricks;
    // Brick objects saved in column order 
    // (faster to search than bricksList)

  private ImagesLoader imsLoader;
  private ArrayList brickImages = null;    
         // holds all the images loaded by imsLoader
 

  public BricksManager(int w, int h, String fnm, ImagesLoader il)
  {
    pWidth = w; pHeight = h;
    imsLoader = il;

    bricksList = new ArrayList();
    loadBricksFile(fnm);
    initBricksInfo();
    createColumns();

    moveSize = (int)(imWidth * MOVE_FACTOR);
    if (moveSize == 0) {
      System.out.println("moveSize cannot be 0, setting it to 1");
      moveSize = 1;
    }

    isMovingRight = false;   // no movement at start
    isMovingLeft = false;
    xMapHead = 0;
  }  // end of BricksManager()


  // ----------- load the bricks information -------------------

  private void loadBricksFile(String fnm)
  /* Load the bricks map from a configuration file, fnm.
     The map starts with an image strip which contains the 
     images referred to in the map. Format:
        s <fnm> <number> 

     This means that the map can use images numbered 0 to
     <number-1>. We assume number is less than 10.

     The bricks map follows. Each line is processed by
     storeBricks(). There can only be at most
     MAX_BRICKS_LINES lines.

     The configuration file can contain empty lines and
     comment lines (those starting with //), which are ignored.
  */
  { 
    String imsFNm = IMAGE_DIR + fnm;
    System.out.println("Reading bricks file: " + imsFNm);

    int numStripImages = -1;
    int numBricksLines = 0;
    try {
      BufferedReader br = new BufferedReader( new FileReader(imsFNm));
      String line;
      char ch;
      while((line = br.readLine()) != null) {
        if (line.length() == 0)  // ignore a blank line
          continue;
        if (line.startsWith("//"))   // ignore a comment line
          continue;
        ch = Character.toLowerCase( line.charAt(0) );
        if (ch == 's')  // an images strip
          numStripImages = getStripImages(line);
        else {  // a bricks map line
          if (numBricksLines > MAX_BRICKS_LINES) 
            System.out.println("Max reached, skipping bricks line: " + line);
          else if (numStripImages == -1) 
            System.out.println("No strip image, skipping bricks line: " + line);
          else {
            storeBricks(line, numBricksLines, numStripImages);
            numBricksLines++;
          }
        }
      }
      br.close();
    } 
    catch (IOException e) 
    { System.out.println("Error reading file: " + imsFNm);
      System.exit(1);
    }
  }  // end of loadBricksFile()


  private int getStripImages(String line)
  /* format:
        s <fnm> <number>

     The strip images are used to represent the bricks when
     they are drawn. A number in the bricks map is mapped
     to the image at that position in the strip image.

     The strip images are loaded with an ImagesLoader object, and then
     retrieved to the bricksImages[] array.
  */
  { StringTokenizer tokens = new StringTokenizer(line);
    if (tokens.countTokens() != 3) {
      System.out.println("Wrong no. of arguments for " + line);
      return -1;
    }
    else {
      tokens.nextToken();    // skip command label
      System.out.print("Bricks strip: ");

      String fnm = tokens.nextToken();
      int number = -1;
      try {
        number = Integer.parseInt( tokens.nextToken() );
        imsLoader.loadStripImages(fnm, number);   // store strip image
        brickImages = imsLoader.getImages( getPrefix(fnm) ); 
            // store all the images in a global array
      }
      catch(Exception e)
      { System.out.println("Number is incorrect for " + line);  }

      return number;
    }
  }  // end of getStripImages()


  private String getPrefix(String fnm)
  // extract name before '.' of filename
  {
    int posn;
    if ((posn = fnm.lastIndexOf(".")) == -1) {
      System.out.println("No prefix found for filename: " + fnm);
      return fnm;
    }
    else
      return fnm.substring(0, posn);
  } // end of getPrefix()



  private void storeBricks(String line, int lineNo, int numImages)
  /* Read a single bricks line, and create Brick objects.
     A line contains digits and spaces (which are ignored). Each
     digit becomes a Brick object.
     The collection of Brick objects are stored in the bricksList
     ArrayList.
  */
  {
    int imageID;
    for(int x=0; x < line.length(); x++) {
      char ch = line.charAt(x);
      if (ch == ' ')   // ignore a space
        continue;
      if (Character.isDigit(ch)) {
        imageID = ch - '0';    // we assume a digit is 0-9
        if (imageID >= numImages)
          System.out.println("Image ID " + imageID + " out of range");
        else   // make a Brick object
          bricksList.add( new Brick(imageID, x, lineNo) );
      }
      else
        System.out.println("Brick char " + ch + " is not a digit");
    }
  }  // end of storeBricks()


  // --------------- initialise bricks data structures -----------------

  private void initBricksInfo()
  /* The collection of bricks in bricksList are examined to
     extract various global data, and to check if certain
     criteria are met (e.g. the maximum width of the bricks is
     greater than the width of the panel (width >= pWidth).
     Also each Brick object is assigned its image.
  */
  {
    if (brickImages == null) {
      System.out.println("No bricks images were loaded");
      System.exit(1);
    }
    if (bricksList.size() == 0) {
      System.out.println("No bricks map were loaded");
      System.exit(1);
    }

    // store brick image dimensions (assuming they're all the same)
    BufferedImage im = (BufferedImage) brickImages.get(0);
    imWidth = im.getWidth();
    imHeight = im.getHeight(); 

    findNumBricks();
    calcMapDimensions();
    checkForGaps();

    addBrickDetails();
  }  // end of initBricksInfo();

  
  private void findNumBricks()
  // find maximum number of bricks along the x-axis and y-axis
  {
    Brick b;
    numCols = 0;
    numRows = 0;
    for (int i=0; i < bricksList.size(); i++) {
      b = (Brick) bricksList.get(i);
      if (numCols < b.getMapX())
        numCols = b.getMapX();
      if (numRows < b.getMapY())
        numRows = b.getMapY();
    }
    numCols++;    // since we want the max no., not the max coord
    numRows++;
  }  // end of findNumBricks()


  private void calcMapDimensions()
  // convert max number of bricks into max pixel dimensions
  {
    width = imWidth * numCols;
    height = imHeight * numRows;

    // exit if the width isn't greater than the panel width
    if (width < pWidth) {
      System.out.println("Bricks map is less wide than the panel");
      System.exit(0);
    }
  }  // end of calcmapDimensions()


  private void checkForGaps()
  /* Check that the bottom map line (numRows-1) has a brick in every 
     x position from 0 to numCols-1.
     This prevents 'jack' from falling down a hole at the bottom 
     of the panel. 
  */
  {
    boolean[] hasBrick = new boolean[numCols];
    for(int j=0; j < numCols; j++)
      hasBrick[j] = false;

    Brick b;
    for (int i=0; i < bricksList.size(); i++) {
      b = (Brick) bricksList.get(i);
      if (b.getMapY() == numRows-1)
        hasBrick[b.getMapX()] = true;   
    }

    for(int j=0; j < numCols; j++)
      if (!hasBrick[j]) {
        System.out.println("Gap found in bricks map bottom line at position " + j);
        System.exit(0);
      }
  }  // end of checkForGaps()


  private void addBrickDetails()
  /* Add image refs to the Bricks, and calculate their
     y-coordinates inside the brick map. */
  {
    Brick b;
    BufferedImage im;
    for (int i=0; i < bricksList.size(); i++) {
      b = (Brick) bricksList.get(i);
      im = (BufferedImage) brickImages.get( b.getImageID());
      b.setImage(im);
      b.setLocY(pHeight, numRows);
    }
  }  // end of addBrickDetails()



  private void createColumns()
  // store bricks info by column for faster search/accessing
  {
    columnBricks = new ArrayList[numCols];
    for (int i=0; i < numCols; i++)
      columnBricks[i] = new ArrayList();

    Brick b;
    for (int j=0; j < bricksList.size(); j++) {
      b = (Brick) bricksList.get(j);
      columnBricks[ b.getMapX() ].add(b);    // bricks not stored in any order
    }
  }  // end of createColumns()


  // ---------------------- move the bricks map ---------------

  public void moveRight()
  { isMovingRight = true;
    isMovingLeft = false;
  }  // end of moveLeft()


  public void moveLeft()
  { isMovingRight = false;
    isMovingLeft = true;
  }

  public void stayStill()
  { isMovingRight = false;
    isMovingLeft = false;
  }

  
  public void update()
  /* Increment the xMapHead value depending on the movement flags.
     It can range between -width to width (exclusive), which is
     the width of the image.
  */
  {
    if (isMovingRight)
      xMapHead = (xMapHead + moveSize) % width;
    else if (isMovingLeft)
      xMapHead = (xMapHead - moveSize) % width;

    // System.out.println("xMapHead is " + xMapHead);

  } // end of updateBricks()



  // -------------- draw the bricks ----------------------


  public void display(Graphics g)
  /* The bricks map (bm) is wider than the panel (width >= pWidth)
     Consider 4 cases: 
       when xMapHead >= 0, draw the bm tail and bm start, or only the bm tail.
       when xMapHead < 0, draw the bm tail, or the bm tail and bm start

     xMapHead can range between -width to width (exclusive)
  */
  {
    int bCoord = (int)(xMapHead/imWidth) * imWidth;
      // bCoord is the drawing x-coordinate of the brick containing xMapHead
    int offset;    // offset is the distance between bCoord and xMapHead
    if (bCoord >= 0)
      offset = xMapHead - bCoord;   // offset is positive
    else  // negative position
      offset = bCoord - xMapHead;   // offset is positive
    // System.out.println("bCoord: " + bCoord + ", offset: " + offset);


    if ((bCoord >= 0) && (bCoord < pWidth)) {  
      drawBricks(g, 0-(imWidth-offset), xMapHead, width-bCoord-imWidth);   // bm tail
      drawBricks(g, xMapHead, pWidth, 0);  // bm start
    }
    else if (bCoord >= pWidth)
      drawBricks(g, 0-(imWidth-offset), pWidth, width-bCoord-imWidth);  // bm tail
    else if ((bCoord < 0) && (bCoord >= pWidth-width+imWidth))
      drawBricks(g, 0-offset, pWidth, -bCoord);		// bm tail
    else if (bCoord < pWidth-width+imWidth) { 
      drawBricks(g, 0-offset, width+xMapHead, -bCoord);     // bm tail
      drawBricks(g, width+xMapHead, pWidth, 0);     // bm start
    } 
  } // end of display()


  private void drawBricks(Graphics g, int xStart, int xEnd, int xBrick)
  /* Draw bricks into the JPanel starting at xStart, ending at xEnd.
     The bricks are drawn a column at a time, separated by imWidth pixels.

     The first column of bricks drawn is the one at the xBrick location 
     in the bricks map.
  */
  { int xMap = xBrick/imWidth;   // get the column position of the brick
                                 // in the bricks map
    // System.out.println("xStart: " + xStart + "; xEnd: " + xEnd);
    // System.out.println("xBrick: " + xBrick + "; xMap: " + xMap);
    ArrayList column;
    Brick b;
    for (int x = xStart; x < xEnd; x += imWidth) {
      column = columnBricks[ xMap ];   // get the current column
      for (int i=0; i < column.size(); i++) {   // draw all bricks
         b = (Brick) column.get(i);
         b.display(g, x);   // draw brick b at JPanel posn x
      }
      xMap++;  // examine the next column of bricks
    }
  }  // end of drawBricks()



  // ----------------- JumperSprite related methods -------------
  // various forms of collision detection with the bricks


  public int getBrickHeight()
  {  return imHeight; }


  public int findFloor(int xSprite)
  /* Called at sprite initialisation to find a brick 
     containing the xSprite location which is higher up
     than other bricks containing that same location.
     Return the brick's y position. 

     xSprite is the same coordinate in the panel and the 
     bricks map since the map has not moved yet.

     xSprite is converted to an x-index in the brick map,
     and this is used to search the relevant bricks column
     for a max y location.

     The returned y-location is the 'floor' of the bricks
     where the sprite will be standing initially.
  */
  {
    int xMap = (int)(xSprite/imWidth);   // x map index

    int locY = pHeight;    // starting y position (the largest possible)
    ArrayList column = columnBricks[ xMap ];

    Brick b;
    for (int i=0; i < column.size(); i++) {
      b = (Brick) column.get(i);
      if (b.getLocY() < locY)
        locY = b.getLocY();   // reduce locY (i.e. move up)
    }
    return locY;
  }  // end of findFloor()



  public int getMoveSize()
  {  return moveSize;  }


  public boolean insideBrick(int xWorld, int yWorld)
  /* Check if the world coord is inside a brick. */
  {
    Point mapCoord = worldToMap(xWorld, yWorld);
    ArrayList column = columnBricks[ mapCoord.x ];

    Brick b;
    for (int i=0; i < column.size(); i++) {
      b = (Brick) column.get(i);
      if (mapCoord.y == b.getMapY())
        return true;
    }
    return false;
  }  // end of insideBrick()


  private Point worldToMap(int xWorld, int yWorld)
  // convert world coord (x,y) to a map index tuple
  {
    // System.out.println("World: " + xWorld + ", " + yWorld);

    xWorld = xWorld % width;   // limit to range (width to -width)
    if (xWorld < 0)            // make positive
      xWorld += width;
    int mapX = (int) (xWorld/imWidth);   // map x-index

    yWorld = yWorld - (pHeight-height);  // relative to map
    int mapY = (int) (yWorld/imHeight);  // map y-index

    if (yWorld < 0)   // above the top of the bricks
      mapY = mapY-1;  // match to next 'row' up

    // System.out.println("Map: " + mapX + ", " + mapY);
    return new Point(mapX, mapY);
  }  // end of worldToMap()

  
  public int checkBrickBase(int xWorld, int yWorld, int step)
  /* The sprite is moving upwards. It checks its next position
     (xWorld, yWorld) to see if it will enter a brick from below.

     If it does, then its step value is reduced to smallStep
     so it will only rise to touch the base of the brick.
  */
  {
    if (insideBrick(xWorld, yWorld)) {
      int yMapWorld = yWorld - (pHeight-height);
      int mapY = (int) (yMapWorld/imHeight);  // map y- index
      int topOffset = yMapWorld - (mapY * imHeight);
      int smallStep = step - (imHeight-topOffset);
      // System.out.println("base smallStep: " + smallStep);
      return smallStep;
    }
    return step;   // no change
  }  // end of checkBrickBase()


  public int checkBrickTop(int xWorld, int yWorld, int step)
  /* The sprite is moving downwards. It checks its next position
     (xWorld, yWorld) to see if it will enter a brick from above.

     If it does, then its step value is reduced to smallStep
     so it will only drop enough to touch the top of the brick.
  */
  {
    if (insideBrick(xWorld, yWorld)) {
      int yMapWorld = yWorld - (pHeight-height);
      int mapY = (int) (yMapWorld/imHeight);  // map y- index
      int topOffset = yMapWorld - (mapY * imHeight);
      int smallStep = step - topOffset;
      // System.out.println("top smallStep: " + smallStep);
      return smallStep;
    }
    return step;   // no change
  }  // end of checkBrickTop()

}  // end of BricksManager
