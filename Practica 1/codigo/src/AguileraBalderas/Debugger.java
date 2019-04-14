package AguileraBalderas;

import core.game.StateObservation;
import tools.Vector2d;

import java.awt.*;

public class Debugger {
  int pixelsPerBlock;
  int mapHeightPixels;
  int mapWidthPixels;
  int mapHeightBlocks;
  int mapWidthBlocks;

  public Debugger(StateObservation stateObs) {
    Dimension pixels = stateObs.getWorldDimension();
    pixelsPerBlock = stateObs.getBlockSize();
    mapHeightPixels = pixels.height;
    mapWidthPixels = pixels.width;
    mapHeightBlocks = (mapHeightPixels / pixelsPerBlock);
    mapWidthBlocks = (mapWidthPixels / pixelsPerBlock);
  }

  public Vector2d cellToPosition(int x, int y){
    return new Vector2d(x * pixelsPerBlock, y * pixelsPerBlock);
  }

  public Vector2d positionToCell(Vector2d position){
    int x = ((int) position.x) / pixelsPerBlock;
    int y = ((int) position.y) / pixelsPerBlock;
    return new Vector2d(x, y);
  }

  void drawCell(Graphics2D graphics, Color color, Vector2di cell) {
    drawCell(graphics, color,cell.x, cell.y);
  }

  void drawCell(Graphics2D graphics, Color color, int cellX, int cellY) {
    Vector2d pos = cellToPosition(cellX, cellY);
    graphics.setColor(color);
    graphics.fillRect((int) (pos.x), (int) (pos.y), pixelsPerBlock, pixelsPerBlock);
  }
}
