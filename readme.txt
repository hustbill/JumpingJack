
Chapter 12. A Side-Scroller

From:
  Killer Game Programming in Java
  Andrew Davison
  O'Reilly, May 2005
  ISBN: 0-596-00730-2
  http://www.oreilly.com/catalog/killergame/
  Web Site for the book: http://fivedots.coe.psu.ac.th/~ad/jg

Contact Address:
  Dr. Andrew Davison
  Dept. of Computer Engineering
  Prince of Songkla University
  Hat yai, Songkhla 90112, Thailand
  E-mail: ad@fivedots.coe.psu.ac.th


If you use this code, please mention my name, and include a link
to the book's Web site.

Thanks,
  Andrew


============================
Compilation:

> javac *.java 
    // if you get "Warning" messages, please see the note below

Sounds should be in the subdirectory Sounds/
Images should be in the subdirectory Images/

There are two alternative brick maps in Images/, in
bricksInfo1.txt and bricksInfo2.txt.

============================
Execution:

> java JumpingJack

-----------
Note on "unchecked or unsafe operation" Warnings

As explained in chapter 3, I have not used J2SE 5.0's type-safe 
collections, so that this code will compile in early versions of
J2SE (e.g. version 1.4).

The warning messages are always related to my use of collections
(e.g. ArrayList) without specifying a type for the objects they will
contain at run time.

No. of Warnings generated in J2SE 5.0 for the examples:
/JumpingJack: 18

-----
Last updated: 14th April 2005

