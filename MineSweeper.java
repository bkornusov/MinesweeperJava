import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// to represent a cell
class Cell {
  ArrayList<Cell> neighbors = new ArrayList<Cell>(8);
  boolean isMine;
  boolean cleared;
  boolean flagged;

  // constructs a basic cell with an initial state
  Cell() {
    this.isMine = false;
    this.cleared = false;
    this.flagged = false;
  }

  // constructs a cell with a specific state
  Cell(boolean isMine, boolean cleared, boolean flagged) {
    this.isMine = isMine;
    this.cleared = cleared;
    this.flagged = flagged;
  }

  // adds a cell to the list of neighbors
  // EFFECT: adds a neighbor to the list of neighbors
  void addNeighbor(Cell c) {
    this.neighbors.add(c);
  }

  // makes this cell a bomb
  // EFFECT: sets the isMine field to true;
  void makeBomb() {
    this.isMine = true;
  }

  // returns the number of neighbors that are bombs of this cell;
  int numBombs() {
    int total = 0;
    for (Cell c : this.neighbors) {
      if (c.isMine) {
        total += 1;
      }
    }
    return total;
  }

  // draws this cell based on whether it is cleared, flagged, or neither
  WorldImage draw() {
    WorldImage base = new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
        new RectangleImage(20, 20, OutlineMode.SOLID, Color.GRAY));
    if (this.cleared) {
      if (this.numBombs() > 0) {
        return new OverlayImage(new TextImage(Integer.toString(this.numBombs()), Color.GREEN),
            base);
      }
      else {
        return base;
      }
    }
    else if (this.flagged) {
      return new OverlayImage(new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.ORANGE),
          base);
    }
    else {
      return new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
          new RectangleImage(20, 20, OutlineMode.SOLID, Color.BLUE));
    }
  }

  // clears the cell if it's not already cleared or flagged, and clears nearby
  // cells if it has no bomb around it
  // EFFECT: sets this.cleared to be true if the cell is not flagged or cleared
  // already
  void clear() {
    if (!(this.cleared || this.flagged)) {
      this.cleared = true;
      if (this.numBombs() <= 0) {
        for (Cell c : this.neighbors) {
          c.clear();
        }
      }
    }
  }

  // flags or unflags a cell
  // EFFECT: negates the current value of this.flagged
  void flag() {
    this.flagged = !this.flagged;
  }
}

// to represent the game
class Game extends World {
  int width;
  int height;
  int bombs;
  Random r;
  ArrayList<ArrayList<Cell>> board;

  // main constructor for Game class with a random seed
  Game(int width, int height, int bombs) {
    this.width = width;
    this.height = height;
    this.bombs = bombs;
    this.board = new ArrayList<ArrayList<Cell>>(height);
    this.r = new Random();
    this.initBoard();
    this.addBombs();
  }

  // Game class constructor with specified random seed
  Game(int width, int height, int bombs, Random r) {
    this.width = width;
    this.height = height;
    this.bombs = bombs;
    this.board = new ArrayList<ArrayList<Cell>>(height);
    this.r = r;
    this.initBoard();
    this.addBombs();
  }

  // Game class constructor with a specified board
  Game(ArrayList<ArrayList<Cell>> board, int bombs, Random r) {
    this.width = board.get(0).size();
    this.height = board.size();
    this.bombs = bombs;
    this.board = board;
    this.r = r;
  }

  // initializes the board with a bunch of cells and connects all the cells
  // together
  // EFFECT: sets each cell on the board to a new cell and adds all of the
  // neighbors
  void initBoard() {
    for (int row = 0; row < this.height; row++) {
      board.add(new ArrayList<Cell>(this.width));
      for (int col = 0; col < this.width; col++) {
        board.get(row).add(new Cell());
      }
    }

    for (int row = 0; row < this.height; row++) {
      board.add(new ArrayList<Cell>(this.width));
      for (int col = 0; col < this.width; col++) {
        ArrayList<Posn> neighbors = new Utils().getNeighbors(row, col, this.height, this.width);
        for (int i = 0; i < neighbors.size(); i++) {
          board.get(row).get(col)
              .addNeighbor(board.get(neighbors.get(i).x).get(neighbors.get(i).y));
        }
      }
    }
  }

  // adds the bombs to the board
  // EFFECT: updates given random cells to be bombs
  void addBombs() {
    ArrayList<Cell> cells = new Utils().flatten(this.board);

    for (int i = 0; i < this.bombs; i++) {
      cells.remove(this.r.nextInt(cells.size())).makeBomb();
    }
  }

  // creates a worldscene for this game state
  public WorldScene makeScene() {
    WorldScene output = new WorldScene(this.width * 20, this.height * 20);
    for (int row = 0; row < this.board.size(); row++) {
      for (int col = 0; col < this.board.get(row).size(); col++) {
        output.placeImageXY(this.board.get(row).get(col).draw(), 20 * col + 10, 20 * row + 10);
      }
    }
    return output;
  }

  // checks whether the game is finished
  boolean isDone() {
    for (ArrayList<Cell> row : board) {
      for (Cell c : row) {
        if ((c.isMine && !c.flagged) || (!c.isMine && !c.cleared)) {
          return false;
        }
      }
    }
    return true;
  }

  // handles the onMousePressed events, on left click clears boxes, on right click
  // flags them
  // EFFECT: updates the game cells by clearing or flagging them
  public void onMouseClicked(Posn pos, String buttonName) {
    if (buttonName.equals("LeftButton")) {
      int x = pos.x / 20;
      int y = pos.y / 20;
      Cell c = board.get(y).get(x);
      if (c.isMine) {
        this.endOfWorld("Kaboom");
      }
      else {
        c.clear();
      }
      if (this.isDone()) {
        this.endOfWorld("Win");
      }
    }
    else if (buttonName.equals("RightButton")) {
      int x = pos.x / 20;
      int y = pos.y / 20;
      board.get(y).get(x).flag();
      if (this.isDone()) {
        this.endOfWorld("Win");
      }
    }
  }

  // Adds text to display if the game is won or lost and ends the game
  public WorldScene lastScene(String msg) {
    WorldScene output = this.makeScene();
    if (msg.equals("Kaboom")) {
      output.placeImageXY(new TextImage("Kaboom! You Lose", Color.RED), this.width * 10,
          this.height * 10);
    }
    else if (msg.equals("Win")) {
      output.placeImageXY(new TextImage("Yay! You Win", Color.GREEN), this.width * 10,
          this.height * 10);
    }
    return output;
  }
}

// Utility functions like flatten and getNeighbors
class Utils {

  // flattens a List of Lists into a single list
  <T> ArrayList<T> flatten(ArrayList<ArrayList<T>> list) {
    ArrayList<T> output = new ArrayList<T>();
    for (int i = 0; i < list.size(); i++) {
      for (int j = 0; j < list.get(i).size(); j++) {
        output.add(list.get(i).get(j));
      }
    }
    return output;
  }

  // produces a list of the positions of a given cell's neighbors
  // only adds neighbors that are valid positions
  ArrayList<Posn> getNeighbors(int row, int col, int height, int width) {
    ArrayList<Posn> output = new ArrayList<Posn>(8);
    for (int i = Math.max(0, row - 1); i <= Math.min(height - 1, row + 1); i++) {
      for (int j = Math.max(0, col - 1); j <= Math.min(width - 1, col + 1); j++) {
        if (!(i == row && j == col)) {
          output.add(new Posn(i, j));
        }
      }
    }
    return output;
  }
}

// Examples class for the Minesweeper Game
class ExamplesGame {

  ArrayList<ArrayList<Integer>> list1 = new ArrayList<ArrayList<Integer>>(
      Arrays.asList(new ArrayList<Integer>(Arrays.asList(1, 2, 3)),
          new ArrayList<Integer>(Arrays.asList(4, 5, 6)),
          new ArrayList<Integer>(Arrays.asList(7, 8, 9))));
  ArrayList<Integer> list2 = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
  ArrayList<ArrayList<Integer>> list3 = new ArrayList<ArrayList<Integer>>(Arrays
      .asList(new ArrayList<Integer>(Arrays.asList(1)), new ArrayList<Integer>(Arrays.asList(2))));
  ArrayList<Integer> list4 = new ArrayList<Integer>(Arrays.asList(1, 2));

  Cell cell1 = new Cell();
  Cell cell2 = new Cell();
  Cell cell3 = new Cell();
  Cell cell4 = new Cell();
  Cell cell5 = new Cell();
  Cell cell6 = new Cell();

  Cell gameCell1 = new Cell();
  Cell gameCell2 = new Cell();
  Cell gameCell3 = new Cell();
  Cell gameCell4 = new Cell();

  Cell gameCell5 = new Cell(true, false, true);
  Cell gameCell6 = new Cell(false, false, false);
  Cell gameCell7 = new Cell(false, true, false);
  Cell gameCell8 = new Cell(false, true, false);
  Cell gameCell9 = new Cell(false, true, false);
  Cell gameCell10 = new Cell(false, true, false);

  Cell gameCell11 = new Cell();
  Cell gameCell12 = new Cell();
  Cell gameCell13 = new Cell();
  Cell gameCell14 = new Cell();
  Cell gameCell15 = new Cell();
  Cell gameCell16 = new Cell();

  WorldImage base = new OverlayImage(new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
      new RectangleImage(20, 20, OutlineMode.SOLID, Color.GRAY));

  WorldImage flagged = new OverlayImage(
      new EquilateralTriangleImage(10, OutlineMode.SOLID, Color.ORANGE), base);
  WorldImage cleared1 = new OverlayImage(new TextImage(Integer.toString(1), Color.GREEN), base);
  WorldImage cleared0 = new OverlayImage(
      new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
      new RectangleImage(20, 20, OutlineMode.SOLID, Color.GRAY));
  WorldImage uncleared = new OverlayImage(
      new RectangleImage(20, 20, OutlineMode.OUTLINE, Color.BLACK),
      new RectangleImage(20, 20, OutlineMode.SOLID, Color.BLUE));
  WorldScene drawn;

  Game newGame;
  Game newGame2;
  Game game2;

  // to initialize the data
  void initData() {
    cell1 = new Cell();
    cell2 = new Cell();
    cell3 = new Cell();
    cell4 = new Cell();
    cell5 = new Cell();
    cell6 = new Cell();
    gameCell1 = new Cell();
    gameCell2 = new Cell();
    gameCell3 = new Cell();
    gameCell4 = new Cell();
    gameCell11 = new Cell();
    gameCell12 = new Cell();
    gameCell13 = new Cell();
    gameCell14 = new Cell();
    gameCell15 = new Cell();
    gameCell16 = new Cell();

    cell1.addNeighbor(cell2);
    cell1.addNeighbor(cell3);
    cell3.addNeighbor(cell4);
    cell4.addNeighbor(cell5);
    cell4.makeBomb();
    cell5.makeBomb();
    cell6.makeBomb();

    gameCell1.addNeighbor(gameCell2);
    gameCell1.addNeighbor(gameCell3);
    gameCell1.addNeighbor(gameCell4);
    gameCell2.addNeighbor(gameCell1);
    gameCell2.addNeighbor(gameCell3);
    gameCell2.addNeighbor(gameCell4);
    gameCell3.addNeighbor(gameCell1);
    gameCell3.addNeighbor(gameCell2);
    gameCell3.addNeighbor(gameCell4);
    gameCell4.addNeighbor(gameCell1);
    gameCell4.addNeighbor(gameCell2);
    gameCell4.addNeighbor(gameCell3);
    gameCell3.makeBomb();

    gameCell11.addNeighbor(gameCell12);
    gameCell11.addNeighbor(gameCell14);
    gameCell11.addNeighbor(gameCell15);
    gameCell12.addNeighbor(gameCell11);
    gameCell12.addNeighbor(gameCell13);
    gameCell12.addNeighbor(gameCell14);
    gameCell12.addNeighbor(gameCell15);
    gameCell12.addNeighbor(gameCell16);
    gameCell13.addNeighbor(gameCell12);
    gameCell13.addNeighbor(gameCell15);
    gameCell13.addNeighbor(gameCell16);
    gameCell14.addNeighbor(gameCell11);
    gameCell14.addNeighbor(gameCell12);
    gameCell14.addNeighbor(gameCell15);
    gameCell15.addNeighbor(gameCell11);
    gameCell15.addNeighbor(gameCell12);
    gameCell15.addNeighbor(gameCell13);
    gameCell15.addNeighbor(gameCell14);
    gameCell15.addNeighbor(gameCell16);
    gameCell16.addNeighbor(gameCell12);
    gameCell16.addNeighbor(gameCell13);
    gameCell16.addNeighbor(gameCell15);
    gameCell15.makeBomb();

    newGame = new Game(2, 2, 1, new Random(1));
    newGame2 = new Game(3, 2, 1, new Random(2));
  }

  // to test the addNeighbor method
  void testAddNeighbor(Tester t) {
    this.initData();
    t.checkExpect(this.cell1.neighbors, new ArrayList<Cell>(Arrays.asList(this.cell2, this.cell3)));
    t.checkExpect(this.cell3.neighbors, new ArrayList<Cell>(Arrays.asList(this.cell4)));
    t.checkExpect(this.cell4.neighbors, new ArrayList<Cell>(Arrays.asList(this.cell5)));
  }

  // to test the makeBomb method
  void testMakeBomb(Tester t) {
    this.initData();
    t.checkExpect(this.cell4.isMine, true);
    t.checkExpect(this.cell5.isMine, true);
    t.checkExpect(this.cell1.isMine, false);
    t.checkExpect(this.cell2.isMine, false);
  }

  // to test the initBoard method
  void testInitBoard(Tester t) {
    this.initData();
    // initBoard() is called by the constructor which is called in initData()
    // all neighbors are set up in initData() as well
    Utils u = new Utils();
    t.checkExpect(u.flatten(this.newGame.board),
        new ArrayList<Cell>(Arrays.asList(gameCell1, gameCell2, gameCell3, gameCell4)));
    t.checkExpect(u.flatten(this.newGame2.board), new ArrayList<Cell>(
        Arrays.asList(gameCell11, gameCell12, gameCell13, gameCell14, gameCell15, gameCell16)));
  }

  // to test the addBombs method
  void testAddBombs(Tester t) {
    this.initData();
    this.newGame.addBombs(); // adds another bomb in the first position
    this.gameCell1.makeBomb();
    this.newGame2.addBombs(); // adds another bomb in a random position
    this.gameCell11.makeBomb();
    Utils u = new Utils();
    t.checkExpect(u.flatten(this.newGame.board),
        new ArrayList<Cell>(Arrays.asList(gameCell1, gameCell2, gameCell3, gameCell4)));
    t.checkExpect(u.flatten(this.newGame2.board), new ArrayList<Cell>(
        Arrays.asList(gameCell11, gameCell12, gameCell13, gameCell14, gameCell15, gameCell16)));
  }

  // to test the numBombs method
  void testNumBombs(Tester t) {
    this.initData();
    t.checkExpect(this.cell1.numBombs(), 0);
    t.checkExpect(this.cell4.numBombs(), 1);
  }

  // to test the flatten method
  void testFlatten(Tester t) {
    Utils u = new Utils();
    t.checkExpect(u.flatten(list1), list2);
    t.checkExpect(u.flatten(list3), list4);
  }

  // to test the getNeighbors method
  void testGetNeighbors(Tester t) {
    Utils u = new Utils();
    t.checkExpect(u.getNeighbors(3, 3, 11, 11),
        new ArrayList<Posn>(Arrays.asList(new Posn(2, 2), new Posn(2, 3), new Posn(2, 4),
            new Posn(3, 2), new Posn(3, 4), new Posn(4, 2), new Posn(4, 3), new Posn(4, 4))));
    t.checkExpect(u.getNeighbors(0, 0, 10, 10),
        new ArrayList<Posn>(Arrays.asList(new Posn(0, 1), new Posn(1, 0), new Posn(1, 1))));
    t.checkExpect(u.getNeighbors(10, 10, 11, 11),
        new ArrayList<Posn>(Arrays.asList(new Posn(9, 9), new Posn(9, 10), new Posn(10, 9))));
    t.checkExpect(u.getNeighbors(3, 0, 10, 10), new ArrayList<Posn>(Arrays.asList(new Posn(2, 0),
        new Posn(2, 1), new Posn(3, 1), new Posn(4, 0), new Posn(4, 1))));
  }

  // to test the draw method
  void testDraw(Tester t) {
    gameCell5.addNeighbor(gameCell6);
    gameCell5.addNeighbor(gameCell8);
    gameCell5.addNeighbor(gameCell9);
    gameCell6.addNeighbor(gameCell5);
    gameCell6.addNeighbor(gameCell7);
    gameCell6.addNeighbor(gameCell8);
    gameCell6.addNeighbor(gameCell9);
    gameCell6.addNeighbor(gameCell10);
    gameCell7.addNeighbor(gameCell6);
    gameCell7.addNeighbor(gameCell9);
    gameCell7.addNeighbor(gameCell10);
    gameCell8.addNeighbor(gameCell5);
    gameCell8.addNeighbor(gameCell6);
    gameCell8.addNeighbor(gameCell9);
    gameCell9.addNeighbor(gameCell5);
    gameCell9.addNeighbor(gameCell6);
    gameCell9.addNeighbor(gameCell7);
    gameCell9.addNeighbor(gameCell8);
    gameCell9.addNeighbor(gameCell10);
    gameCell10.addNeighbor(gameCell9);
    gameCell10.addNeighbor(gameCell6);
    gameCell10.addNeighbor(gameCell7);
    ArrayList<ArrayList<Cell>> board = new ArrayList<ArrayList<Cell>>();
    board.add(new ArrayList<Cell>());
    board.add(new ArrayList<Cell>());
    board.get(0).add(gameCell5);
    board.get(0).add(gameCell6);
    board.get(0).add(gameCell7);
    board.get(1).add(gameCell8);
    board.get(1).add(gameCell9);
    board.get(1).add(gameCell10);
    game2 = new Game(board, 1, new Random(1));
    drawn = new WorldScene(60, 40);
    drawn.placeImageXY(flagged, 10, 10);
    drawn.placeImageXY(uncleared, 30, 10);
    drawn.placeImageXY(cleared1, 10, 30);
    drawn.placeImageXY(cleared1, 30, 30);
    drawn.placeImageXY(cleared0, 50, 10);
    drawn.placeImageXY(cleared0, 50, 30);

    t.checkExpect(gameCell5.draw(), flagged);
    t.checkExpect(gameCell6.draw(), uncleared);
    t.checkExpect(gameCell7.draw(), cleared0);
    t.checkExpect(gameCell8.draw(), cleared1);
    t.checkExpect(game2.makeScene(), drawn);
  }

  // tests the clear method and flood filling
  void testClear(Tester t) {
    this.initData();
    t.checkExpect(cell1.cleared, false);
    t.checkExpect(cell2.cleared, false);
    t.checkExpect(cell3.cleared, false);
    t.checkExpect(cell4.cleared, false);
    cell1.clear();
    t.checkExpect(cell1.cleared, true);
    t.checkExpect(cell2.cleared, true);
    t.checkExpect(cell3.cleared, true);
    t.checkExpect(cell4.cleared, false);
  }

  // tests the flag method
  void testFlag(Tester t) {
    this.initData();
    t.checkExpect(cell1.flagged, false);
    cell1.flag();
    t.checkExpect(cell1.flagged, true);
    cell1.flag();
    t.checkExpect(cell1.flagged, false);
  }

  // tests the onMousePressed handler
  void testOnMouse(Tester t) {
    this.initData();
    this.newGame.onMouseClicked(new Posn(1, 1), "LeftButton");
    this.gameCell1.clear();
    t.checkExpect(this.newGame.board.get(0).get(0), gameCell1);
    this.newGame.onMouseClicked(new Posn(0, 21), "RightButton");
    this.gameCell3.flag();
    t.checkExpect(this.newGame.board.get(1).get(0), gameCell3);
  }

  // tests the isDone method
  void testIsDone(Tester t) {
    this.initData();
    ArrayList<ArrayList<Cell>> board = new ArrayList<ArrayList<Cell>>();
    board.add(new ArrayList<Cell>());
    board.add(new ArrayList<Cell>());
    board.get(0).add(gameCell1);
    board.get(0).add(gameCell2);
    board.get(1).add(gameCell3);
    board.get(1).add(gameCell4);
    Game newGame1 = new Game(board, 1, new Random(1));
    t.checkExpect(newGame1.isDone(), false);
    gameCell1.clear();
    gameCell2.clear();
    gameCell4.clear();
    gameCell3.flag();
    t.checkExpect(newGame1.isDone(), true);
  }

  // tests the lastScene method
  void testLastScene(Tester t) {
    this.initData();
    WorldScene scene = newGame.makeScene();
    WorldScene lossScene = newGame.makeScene();
    WorldScene winScene = newGame.makeScene();
    WorldImage loss = new TextImage("Kaboom! You Lose", Color.RED);
    lossScene.placeImageXY(loss, 20, 20);
    WorldImage win = new TextImage("Yay! You Win", Color.GREEN);
    winScene.placeImageXY(win, 20, 20);
    t.checkExpect(newGame.lastScene("Kaboom"), lossScene);
    t.checkExpect(newGame.lastScene("Win"), winScene);
    t.checkExpect(newGame.lastScene("blah"), scene);
  }

  // plays the game
  void testBigBang() {
    World game = new Game(20, 20, 50);
    int w = 400;
    int h = 400;
    game.bigBang(w, h);
  }
  
}

class MineSweeper {
  
  public static void main(String[] args) {
    ExamplesGame startGame = new ExamplesGame();
    startGame.testBigBang();
  }
}
