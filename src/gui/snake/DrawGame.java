package gui.snake;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*; // needed for event handling
import java.awt.Toolkit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.awt.BorderLayout;
import java.util.Scanner;
import gui.snake.Snake;
import gui.snake.SnakeSection;

public class DrawGame extends JPanel implements KeyListener {
  boolean gameOver=false;         
  
  Toolkit toolkit;
  Timer timer;
  
  static final int SCREEN_SIZE_X=40;         // In units of snake sections.
  static final int SCREEN_SIZE_Y=30;
  
  final double MIN_TIME_INTERVAL=50;
  int totalTimeSteps=0;
  int millisecs=200;
  
  Snake redSnake = new Snake(new SnakeSection(10,9,0),1,0,Color.red);
  Snake blueSnake = new Snake(new SnakeSection(10,15,0),1,0,Color.blue);
  
  int redScore=0;
  int blueScore=0;
  
  int redCrashes=0;
  int blueCrashes=0;
  
  int foodValue;
  SnakeSection foodPosition;
    
  public DrawGame() {    
    setFocusable(true);
    
    foodValue = 3;   // Initial food value.
    foodPosition = getNewFoodPosition();
    
    // First thing to do: Start up the periodic task:
    System.out.println("About to start the snake.");
    startSnake(millisecs);   // Argument is number of milliseconds per snake move.
    System.out.println("Snake started.");
 
    setBackground(Color.black);        
    addKeyListener(this);
  }

  public void startSnake(int milliseconds) {
    toolkit = Toolkit.getDefaultToolkit();
    timer = new Timer();
    Date firstTime = new Date();  // Start task now.
    timer.schedule(new AdvanceTheSnakeTask(), firstTime, milliseconds);
  }
    
  public void resetSnakes() {
    millisecs=200;    
    totalTimeSteps=0;    
    redSnake = new Snake(new SnakeSection(10,9,0),1,0,Color.red);  
    blueSnake = new Snake(new SnakeSection(10,15,0),1,0,Color.blue);
    timer.cancel();
    startSnake(200);
  }
  
  public SnakeSection getNewFoodPosition() {    
    // Now we need to find a position for the food that is not already on one of the snakes.
    // We try different positions until we find one that is not part of a snake.
    boolean acceptable=false;
    int newFoodX=0,newFoodY=0;
    while (!acceptable) {
      newFoodX=(int) (Math.random()*SCREEN_SIZE_X);          
      newFoodY=(int) (Math.random()*SCREEN_SIZE_Y);
      if (!redSnake.contains(newFoodX,newFoodY) && !blueSnake.contains(newFoodX,newFoodY))
        acceptable=true;
    }
    // Now that we have an acceptable position for the food, put the food in that position.          
    return new SnakeSection(newFoodX,newFoodY,0);
  }
  
  class AdvanceTheSnakeTask extends TimerTask {
    public void run() {
        
      // Put stuff here that should happen during every advance.
      
      redSnake.move();             
      blueSnake.move();
      
      // Here, we check to see if the snakes have collided with each other.
      // This is done by checking whether the head of one snake (SnakeSection 0)
      // is equal in position to one of the SnakeSections of the competitor's snake.
      //
      // To make the game a little more interesting, we will define a collision to 
      // be the intersection of a snake's head with another snake's body. In particular,
      // we will NOT count it as a collision if the two snakes' heads move to the same
      // position at the same time. This will allow the snakes to pass through each other!
      // 
      // It is possible for both snakes to "crash" simultaneously, if, during the same
      // time step, each snake hits the other snake's body. In this case, neither player
      // is awarded any points.
 
      boolean redHitsBlue=blueSnake.checkBodyPositions(redSnake.snakeSecs[0]);
      boolean blueHitsRed=redSnake.checkBodyPositions(blueSnake.snakeSecs[0]);
      
      if (redHitsBlue && !blueHitsRed) {          // true if only red crashes.
        blueScore+=blueSnake.snakeLength;
        redCrashes++;
      }
      if (!redHitsBlue && blueHitsRed) {          // true if only blue crashes.
        redScore+=redSnake.snakeLength;
        blueCrashes++;
      }
      if (redHitsBlue && blueHitsRed) {           // true if both snakes crash simultaneously.
        blueCrashes++;
        redCrashes++;
      }
      if (redHitsBlue || blueHitsRed) {           // true if EITHER snake crashes.
        resetSnakes();
      }
      
      if (redCrashes==5 || blueCrashes==5) {      // game ends after one player has crashed 5 times.
        gameOver=true;
      }
      
      // Here, we check to see if the snake has eaten the current food.
      // Note that we will only check to see if the head of the snake (SnakeSection 0)
      // is in the same place as the food.
      //
      // Note that if both snakes get to the food simultaneously, they both
      // get to eat it.
      
      boolean newFood=false;
      if (redSnake.snakeSecs[0].match(foodPosition)) {
        redSnake.snakeLength+=foodValue;
        newFood=true;
      }
      if (blueSnake.snakeSecs[0].match(foodPosition)) {
        blueSnake.snakeLength+=foodValue;
        newFood=true;
      }
      if (newFood) {
        foodValue=(int) (Math.random()*8+1);       // Food has value from 1 to 9.
        foodPosition=getNewFoodPosition();
      }
      
      totalTimeSteps++;      
      if (totalTimeSteps%50 == 0) {                 // Update speed every 50 time steps.
        timer.cancel();                             // Cancel previous periodic events.
        if (millisecs>MIN_TIME_INTERVAL) 
          millisecs = (int) (millisecs * .9);       // Reduce current delay by 10%.  
        System.out.print(millisecs+" ");            // Diagnostic.
        startSnake(millisecs);
      }
      repaint();
    }
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);    
    if (gameOver) {
      g.setColor(Color.white);
      g.drawString("*********************************",300,200);
      g.drawString("*********************************",300,220);
      g.drawString("*********** Game over *********",300,240);
      g.drawString("Red score: "+redScore+"    Blue score: "+blueScore+" ",300,260);
      g.drawString("*********************************",300,280);
      g.drawString("*********************************",300,300);
      g.drawString("*** Hit any key to quit. ********",300,340);
      
      timer.cancel();          // We must cancel the periodic events scheduled by the timerTask.
    }
    
    // The body of the else paints the screen at each time step of the game.
    // It shows the score of each player, the number of crashes. Then it draws the snakes and
    // the food, with the value of the food in the food box.
    
    else {     
      // Show the score information.
      g.setColor(Color.red);
      g.drawString("Red Snake", 30, 15);
      g.drawString("Score: "+redScore+"  Crashes: "+redCrashes,4,30);
      g.setColor(Color.blue);
      g.drawString("Blue Snake", 680, 15);
      g.drawString("Score: "+blueScore+"  Crashes: "+blueCrashes,654,30);
      
      // Draw the snakes.
      redSnake.paint(g);    
      blueSnake.paint(g);
      
      // Draw food.
      g.setColor(Color.yellow);
      g.drawRect((int)foodPosition.x*20,(int)foodPosition.y*20,20,20);
      g.drawString(""+foodValue,(int)foodPosition.x*20+5,(int)foodPosition.y*20+15);
    }
  }
  
  public void keyTyped(KeyEvent e) {
    if (gameOver) {
      System.exit(0);
    }
    if (e.getKeyChar() =='h') {
      redSnake.snakeSecs[0].x=(int) (Math.random()*20);
      redSnake.snakeSecs[0].y=(int) (Math.random()*20);
    }
    if (e.getKeyChar()=='a') {              // Red snake left.
      redSnake.dirX=-2;
      redSnake.dirY=0;
    }
     else if (e.getKeyChar()=='d') {         // Red snake right.
      redSnake.dirX=1;
      redSnake.dirY=0;
    }
    else if (e.getKeyChar()=='w') {         // Red snake up.
      redSnake.dirX=0;
      redSnake.dirY=-1;
    }
    else if (e.getKeyChar()=='s') {         // Red snake down.
      redSnake.dirX=0;
      redSnake.dirY=1;
    }
    else if (e.getKeyChar()=='l') {         // Blue snake left.
      blueSnake.dirX=-1;
      blueSnake.dirY=0;
    }
    else if (e.getKeyChar()=='\'') {        // Blue snake right.
      blueSnake.dirX=1;
      blueSnake.dirY=0;
    }
    else if (e.getKeyChar()=='p') {         // Blue snake up.
      blueSnake.dirX=0;
      blueSnake.dirY=-1;
    }
    else if (e.getKeyChar()==';') {         // Blue snake down.
      blueSnake.dirX=0;
      blueSnake.dirY=1;
    }
  }

    // Ignore key which is held down.
    public void keyPressed(KeyEvent e) {
    }

    // Ignore key release events.
    public void keyReleased(KeyEvent e) {
    }

}