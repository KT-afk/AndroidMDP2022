package com.example.androidmdp2022;

import static java.lang.String.valueOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import org.json.JSONException;

import java.util.ArrayList;

public class GridMap extends View {

    public GridMap(Context c) {
        super(c);
        initMap();
    }

    SharedPreferences sharedPreferences;

    private final Paint blackPaint = new Paint();
    private static final boolean clearCellStatus = false;
    private static int[] startingCoord = new int[]{-1, -1};
    private final Paint unexploredColor = new Paint();
    private final Paint exploredColor = new Paint();
    private final Paint arrowColor = new Paint();
    private final Paint imageLine = new Paint();
    private final Paint imageLineConfirm = new Paint();
    private static int[] currentCoord = new int[]{-1, -1};
    private static int[] previousCoord = new int[]{-1, -1};
    private static ArrayList<int[]> obsCoord = new ArrayList<>();
    private static String robotDirection = "None";
    private static boolean drawableRobot = false;
    private static boolean startingCoordStatus = false;
    private static boolean setObsStatus = false;
    private static ArrayList<String[]> arrowCoord = new ArrayList<>();
    private static boolean setObsDirection = false;
    private static boolean autoUpdate = false;
    private static ArrayList<Cell> cellArr = new ArrayList<Cell>();
    private static String obsImageTargetID = null; // <-- newly added
    private static int[] obsNoArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private final Paint imageColour = new Paint();
    private static boolean setExploredStatus = false;
    private static boolean validPosition = false;
    private boolean newEndCoord = false;
    private final Paint obsColour = new Paint();

    private static final String TAG = "GridMap";
    private static final int COL = 20;
    private static final int ROW = 20;
    private static float cellSize;
    private static Cell[][] cells;

    private boolean mapDrawn = false;

    private static final int[] selectedObsCoord = new int[3];
    private static boolean obsSelected = false;
    private final Paint robotColour = new Paint();

    int newDirection = -1; // 0:None 1: Up, 2: Down, 3: Left, 4:Right
    private final Paint endingColour = new Paint();
    private final Paint startingColour = new Paint();
    private static String obsSelectedFacing = null; // <-- newly added
    String[] directionList = new String[]{"NONE", "UP", "DOWN", "LEFT", "RIGHT"};
    int changeDirection = -1; // 0:None 1: Up, 2: Down, 3: Left, 4:Right

    //For RPI message
    String rpiRobot = "";
    String rpiObstacle;

    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initMap();
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        imageColour.setColor(Color.MAGENTA);
        obsColour.setColor(Color.BLACK);
        robotColour.setColor(Color.CYAN);
        endingColour.setColor(Color.GREEN);
        startingColour.setColor(Color.CYAN);
        unexploredColor.setColor(0xa7bdff);
        exploredColor.setColor(Color.WHITE);
        arrowColor.setColor(Color.BLACK);

        imageLine.setStyle(Paint.Style.STROKE);
        imageLine.setColor(Color.YELLOW);
        imageLine.setStrokeWidth(2); // <-- line thickness

        imageLineConfirm.setStyle(Paint.Style.STROKE);
        imageLineConfirm.setColor(Color.YELLOW);
        imageLineConfirm.setStrokeWidth(5); // <-- line thickness

        // get shared preferences
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    private void initMap() {
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        showLog("Entering onDraw");
        super.onDraw(canvas);

        //CREATE CELL COORDINATES
        Log.d(TAG,"Creating Cell");

        if (!mapDrawn) {
            String[] dummyArrowCoord = new String[3];
            dummyArrowCoord[0] = "1";
            dummyArrowCoord[1] = "1";
            dummyArrowCoord[2] = "dummy";
            arrowCoord.add(dummyArrowCoord);
            this.createCell();
            newEndCoord = true;
            showLog("Map drawn");
        }

        drawIndivCell(canvas);
        drawHoriLines(canvas);
        drawVertiLines(canvas);
        drawGridNo(canvas);

        if (newEndCoord == true) {
            mapDrawn = true;
            newEndCoord = false;
        }

        if (getCanDrawRobot())
            drawRobot(canvas, currentCoord);
        showLog("Exiting onDraw");
    }

    public int returnObsId(int x, int y) {
        if (x > 21 || y > 21)
            return -100;
        return cells[x][y].obstacleNo;
    }

    public String returnObstacleFacing(int x, int y) {
        return cells[x][y].obstacleFacing;
    }

    private void drawIndivCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");

        for (int x = 1; x <= COL; x++)
            for (int y = 1; y <= ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    if (cells[x][y].getId() == -1) {
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);

                        // this is for drawing numbers in obstacle and using cellArr arraylist for drag and drop
                        if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("image")) {
                            boolean written = false;
                            for (int a = 0; a < cellArr.size(); a++) {
                                if (cells[x][y] == cellArr.get(a)) {

                                    // TODO: NEW Target ID
                                    if (cells[x][y].targetID == null) {
                                        canvas.drawText(Integer.toString(cells[x][y].obstacleNo), cells[x][y].startX + (cellSize / 3.2f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
                                    } else {
                                        Paint textPaint2 = new Paint();
                                        textPaint2.setTextSize(20);
                                        textPaint2.setColor(Color.WHITE);
                                        textPaint2.setTextAlign(Paint.Align.CENTER);
                                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, blackPaint);
                                        canvas.drawText(cells[x][y].targetID, (cells[x][y].startX + cells[x][y].endX) / 2, cells[x][y].endY + (cells[x][y].startY - cells[x][y].endY) / 4, textPaint2);
                                    }
                                    written = true;
                                    break;
                                }
                            }

                            if (written == false) {

                                // TODO: NEW Obstacle ID
                                cellArr.add(cells[x][y]);
                                if(cells[x][y].targetID == null){
                                    canvas.drawText(Integer.toString(cells[x][y].obstacleNo), cells[x][y].startX + (cellSize / 3.2f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
                                } else {
                                    Paint textPaint2 = new Paint();
                                    textPaint2.setTextSize(20);
                                    textPaint2.setColor(Color.WHITE);
                                    textPaint2.setTextAlign(Paint.Align.CENTER);
                                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, blackPaint);
                                    canvas.drawText(cells[x][y].targetID, (cells[x][y].startX+cells[x][y].endX)/2, cells[x][y].endY + (cells[x][y].startY-cells[x][y].endY)/4, textPaint2);
                                }

                            }
                        }
                    }
                    else {
                        // this part draw the numbers out on the map
                        Paint textPaint = new Paint();
                        textPaint.setTextSize(20);
                        textPaint.setColor(Color.WHITE);
                        textPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);
                        canvas.drawText(valueOf(cells[x][y].getId()),(cells[x][y].startX+cells[x][y].endX)/2, cells[x][y].endY + (cells[x][y].startY-cells[x][y].endY)/4, textPaint);
                    }

        // Obstacle Face --> Assign Drawable to the cells @ GridMap.Java (Start)
        for (int x = 1; x <= COL; x++)
            for (int y = 1; y <= ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++) {
                    if (cells[x][y].obstacleFacing != null) {
                        if (cells[x][y].obstacleFacing == "UP" && cells[x][y].isDirection == false ) {
                            canvas.drawRect(cells[x][y].startX + 2 , cells[x][y].startY + 1, cells[x][y].endX, cells[x][y].endY - (cellSize / 1.1f), imageLine);
                        }
                        if (cells[x][y].obstacleFacing == "DOWN" && cells[x][y].isDirection == false) {
                            canvas.drawRect(cells[x][y].startX + 2, cells[x][y].startY + (cellSize / 1f) - 2, cells[x][y].endX, cells[x][y].endY - 1, imageLine);
                        }
                        if (cells[x][y].obstacleFacing == "LEFT" && cells[x][y].isDirection == false) {
                            canvas.drawRect(cells[x][y].startX + 1, cells[x][y].startY + 2, cells[x][y].endX - (cellSize / 1.1f), cells[x][y].endY, imageLine);
                        }
                        if (cells[x][y].obstacleFacing == "RIGHT" && cells[x][y].isDirection == false) {
                            canvas.drawRect(cells[x][y].startX + (cellSize / 1f) -2, cells[x][y].startY, cells[x][y].endX -1, cells[x][y].endY, imageLine);
                        }

                        // for confirm direction display
                        if (cells[x][y].obstacleFacing == "UP" && cells[x][y].isDirection == true ) {


                            canvas.drawRect(cells[x][y].startX + 2 , cells[x][y].startY + 1, cells[x][y].endX, cells[x][y].endY - (cellSize / 1.1f), imageLineConfirm);
                        }
                        if (cells[x][y].obstacleFacing == "DOWN" && cells[x][y].isDirection == true) {

                            canvas.drawRect(cells[x][y].startX + 2, cells[x][y].startY + (cellSize / 1f) - 2, cells[x][y].endX, cells[x][y].endY - 1, imageLineConfirm);
                        }
                        if (cells[x][y].obstacleFacing == "LEFT" && cells[x][y].isDirection == true) {
                            canvas.drawRect(cells[x][y].startX + 1, cells[x][y].startY + 2, cells[x][y].endX - (cellSize / 1.1f), cells[x][y].endY, imageLineConfirm);
                        }
                        if (cells[x][y].obstacleFacing == "RIGHT" && cells[x][y].isDirection == true) {
                            canvas.drawRect(cells[x][y].startX + (cellSize / 1f) -2, cells[x][y].startY, cells[x][y].endX -1, cells[x][y].endY, imageLineConfirm);
                        }
                    } // End
                }

        showLog("Exiting drawIndividualCell");
    }

    public void updateImageNumberCell(int obstacleNo, String targetID){
        // find the obstacle no which has the same id
        for (int x = 1; x <= COL; x++)
            for (int y = 1; y <= ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    if (cells[x][y].obstacleNo == obstacleNo) {
                        cells[x][y].targetID = targetID;
                        cells[x][y].setType("image");
                    }
        this.invalidate();
    }

    private void drawHoriLines(Canvas canvas) {
        for (int y = 1; y <= ROW + 1; y++)
            canvas.drawLine(cells[1][y].startX, cells[1][y].startY - (cellSize / 30), cells[20][y].endX, cells[20][y].startY - (cellSize / 30), blackPaint);
    }

    private void drawVertiLines(Canvas canvas) {
        for (int x = 0; x <= COL; x++)
            canvas.drawLine(cells[x][1].startX - (cellSize / 30) + cellSize, cells[x][1].startY - (cellSize / 30), cells[x][1].startX - (cellSize / 30) + cellSize, cells[x][20].endY + (cellSize / 30), blackPaint);
    }

    private void drawGridNo(Canvas canvas) {
        showLog("Entering drawGridNo");
        for (int x = 1; x <= COL; x++) {
            if (x > 9)
                canvas.drawText(Integer.toString(x - 1), cells[x][0].startX + (cellSize / 5), cells[x][0].startY + (cellSize / 3), blackPaint);
            else
                canvas.drawText(Integer.toString(x - 1), cells[x][0].startX + (cellSize / 3), cells[x][0].startY + (cellSize / 3), blackPaint);
        }
        for (int y = 1; y <= ROW; y++) {
            if (y > 10)
                canvas.drawText(Integer.toString(y - 1), cells[0][y].startX + (cellSize / 2.5f), cells[0][y].startY + (cellSize / 2), blackPaint);
            else
                canvas.drawText(Integer.toString(y-1), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 2), blackPaint);
        }
        showLog("Exiting drawGridNo");
    }

    private void drawRobot(Canvas canvas, int[] curCoord) {
        showLog("Entering drawRobot");
        if(newEndCoord){
            newEndCoord=false;
        }
        for (int y = curCoord[1] - 1; y < curCoord[1] + 1; y++){
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColour);
        }
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++){
            canvas.drawLine(cells[x][curCoord[1] - 1].startX - (cellSize / 30) + cellSize, cells[x][curCoord[1] - 1].startY, cells[x][curCoord[1] + 1].startX - (cellSize / 30) + cellSize, cells[x][curCoord[1] + 1].endY, robotColour);
        }

        switch (this.getRobotDirection()) {
            case "up":
                //left drawn line
                canvas.drawLine(cells[curCoord[0] - 1][curCoord[1] + 1].startX, cells[curCoord[0] - 1][curCoord[1] + 1].endY, (cells[curCoord[0]][curCoord[1] - 1].startX + cells[curCoord[0]][curCoord[1] - 1].endX) / 2, cells[curCoord[0]][curCoord[1] - 1].startY, blackPaint);
                //right drawn line
                canvas.drawLine((cells[curCoord[0]][curCoord[1] - 1].startX + cells[curCoord[0]][curCoord[1] - 1].endX) / 2, cells[curCoord[0]][curCoord[1] - 1].startY, cells[curCoord[0] + 1][curCoord[1] + 1].endX, cells[curCoord[0] + 1][curCoord[1] + 1].endY, blackPaint);
                break;
            case "down":
                canvas.drawLine(cells[curCoord[0] - 1][curCoord[1] - 1].startX, cells[curCoord[0] - 1][curCoord[1] - 1].startY, (cells[curCoord[0]][curCoord[1] + 1].startX + cells[curCoord[0]][curCoord[1] + 1].endX) / 2, cells[curCoord[0]][curCoord[1] + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0]][curCoord[1] + 1].startX + cells[curCoord[0]][curCoord[1] + 1].endX) / 2, cells[curCoord[0]][curCoord[1] + 1].endY, cells[curCoord[0] + 1][curCoord[1] - 1].endX, cells[curCoord[0] + 1][curCoord[1] - 1].startY, blackPaint);
                break;
            case "right":
                canvas.drawLine(cells[curCoord[0] - 1][curCoord[1] - 1].startX, cells[curCoord[0] - 1][curCoord[1] - 1].startY, cells[curCoord[0] + 1][curCoord[1]].endX, cells[curCoord[0] + 1][curCoord[1] - 1].endY + (cells[curCoord[0] + 1][curCoord[1]].endY - cells[curCoord[0] + 1][curCoord[1] - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] + 1][curCoord[1]].endX, cells[curCoord[0] + 1][curCoord[1] - 1].endY + (cells[curCoord[0] + 1][curCoord[1]].endY - cells[curCoord[0] + 1][curCoord[1] - 1].endY) / 2, cells[curCoord[0] - 1][curCoord[1] + 1].startX, cells[curCoord[0] - 1][curCoord[1] + 1].endY, blackPaint);
                break;
            case "left":
                canvas.drawLine(cells[curCoord[0] + 1][curCoord[1] - 1].endX, cells[curCoord[0] + 1][curCoord[1] - 1].startY, cells[curCoord[0] - 1][curCoord[1]].startX, cells[curCoord[0] - 1][curCoord[1] - 1].endY + (cells[curCoord[0] - 1][curCoord[1]].endY - cells[curCoord[0] - 1][curCoord[1] - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] - 1][curCoord[1]].startX, cells[curCoord[0] - 1][curCoord[1] - 1].endY + (cells[curCoord[0] - 1][curCoord[1]].endY - cells[curCoord[0] - 1][curCoord[1] - 1].endY) / 2, cells[curCoord[0] + 1][curCoord[1] + 1].endX, cells[curCoord[0] + 1][curCoord[1] + 1].endY, blackPaint);
                break;
            case "upright": // NE
                //NE
                //left drawn line
                canvas.drawLine(cells[curCoord[0] - 1][curCoord[1]].startX, cells[curCoord[0] - 1][curCoord[1]].endY, (cells[curCoord[0] + 1][curCoord[1] - 1].startX + cells[curCoord[0] + 2][curCoord[1] - 1 ].endX) / 2, cells[curCoord[0] + 2][curCoord[1] - 1].startY, blackPaint);
                canvas.drawLine((cells[curCoord[0] + 1][curCoord[1] - 1].startX + cells[curCoord[0] + 2][curCoord[1] - 1].endX) / 2, cells[curCoord[0] + 1][curCoord[1] - 1].startY, cells[curCoord[0] - 1][curCoord[1]].endX, cells[curCoord[0] - 1][curCoord[1] + 1].endY, blackPaint);

                break;
            case "upleft": // NW
                //NW
                //left drawn line
                canvas.drawLine(cells[curCoord[0] + 1][curCoord[1] + 1].startX, cells[curCoord[0] + 1][curCoord[1] + 1].endY, (cells[curCoord[0] - 2][curCoord[1]].startX + cells[curCoord[0] - 1][curCoord[1]].endX) / 2, cells[curCoord[0] - 1][curCoord[1] - 1].startY, blackPaint);
                canvas.drawLine((cells[curCoord[0] - 2][curCoord[1]].startX + cells[curCoord[0] - 1][curCoord[1]].endX) / 2, cells[curCoord[0]][curCoord[1] - 1].startY, cells[curCoord[0] + 1][curCoord[1]].endX, cells[curCoord[0] + 1][curCoord[1]].endY, blackPaint);

                break;
            case "downright": // SE
                // SE
                canvas.drawLine(cells[curCoord[0] - 1][curCoord[1]].startX, cells[curCoord[0] - 1][curCoord[1]].startY, (cells[curCoord[0] + 1][curCoord[1] + 1].startX + cells[curCoord[0] + 2][curCoord[1] + 1].endX) / 2, cells[curCoord[0] + 1][curCoord[1] + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0] + 1][curCoord[1] + 1].startX + cells[curCoord[0] + 2][curCoord[1] + 1].endX) / 2, cells[curCoord[0]][curCoord[1] + 1].endY, cells[curCoord[0] - 1][curCoord[1] - 1].endX, cells[curCoord[0] - 1][curCoord[1] - 1].startY, blackPaint);

                break;

            case "downleft": // SW
                // SW
                canvas.drawLine(cells[curCoord[0] + 1][curCoord[1] - 1].startX, cells[curCoord[0] + 1][curCoord[1] - 1].startY, (cells[curCoord[0] - 2][curCoord[1] + 1].startX + cells[curCoord[0] - 1][curCoord[1] + 1].endX) / 2, cells[curCoord[0] - 1][curCoord[1] + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0] - 2][curCoord[1] + 1].startX + cells[curCoord[0] - 1][curCoord[1] + 1].endX) / 2, cells[curCoord[0]][curCoord[1] + 1].endY, cells[curCoord[0] + 1][curCoord[1]].endX, cells[curCoord[0] + 1][curCoord[1]].startY, blackPaint);

                break;
            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    private ArrayList<String[]> getArrowCoord() {
        return arrowCoord;
    }

    public String getRobotDirection() {
        return robotDirection;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    public boolean getValidPosition() {
        return validPosition;
    }

    public void setSetObsDirection(boolean status) {
        setObsDirection = status;
    }

    public void setSetObstacleStatus(boolean status) {
        setObsStatus = status;
    }




    private boolean getStartingCoordStatus() {
        return startingCoordStatus;
    }

    public void setStartingCoordStatus(boolean status) {
        startingCoordStatus = status;
    }


    public boolean getCanDrawRobot() {
        return drawableRobot;
    }

    private void createCell() {
        showLog("Entering cellCreate");
        cells = new Cell[COL + 2][ROW + 2];
        this.calculateDimension();
        cellSize = this.getCellSize();

        for (int x = 0; x <= COL +1; x++)
            for (int y = 0; y <= ROW +1; y++)
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
        showLog("Exiting createCell");
    }

    public void setStartingCoord(int col, int row) {
        showLog("Entering setStartingCoord");
        startingCoord[0] = col;
        startingCoord[1] = row;
        String direction = getRobotDirection();
        if (direction.equals("None")) {
            direction = "up";
        }
        if (this.getStartingCoordStatus())
            this.setCurrentCoord(col, row, direction);
        showLog("Exiting setStartingCoord");
    }

    private int[] getStartingCoord() {
        return startingCoord;
    }

    public void setCurrentCoord(int col, int row, String direction) {
        showLog("Entering setCurrentCoord");
        currentCoord[0] = col;
        currentCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxis(col, row, direction);

        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        showLog("Exiting setCurrentCoord");
    }

    public int[] getCurrentCoord() {
        return currentCoord;
    }

    private void calculateDimension() {
        this.setCellSize(getWidth() / (COL + 1));
    }

    private int convertRow(int row) {
        return row;
    }

    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    private float getCellSize() {
        return cellSize;
    }

    private void setPreRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setPreRobotCoord");
        previousCoord[0] = oldCol;
        previousCoord[1] = oldRow;
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        showLog("Exiting setPreRobotCoord");
    }

    public void unsetPreRobotCoord(int oldCol, int oldRow) {
        showLog("Entering unsetPreRobotCoord");
        previousCoord[0] = oldCol;
        previousCoord[1] = oldRow;
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("unexplored");
        showLog("Exiting unsetPreRobotCoord");
    }

    private int[] getPreRobotCoord() {
        return previousCoord;
    }

    public void setRobotDirection(String direction) {
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();
    }

    private void updateRobotAxis(int col, int row, String direction) {
        TextView xAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.xValue);
        TextView yAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.yValue);
        TextView directionAxisTextView =  ((Activity)this.getContext()).findViewById(R.id.directionValue);

        xAxisTextView.setText(valueOf(col-1));
        yAxisTextView.setText(valueOf(row-1));
        if(direction.equals("up"))
        {
            directionAxisTextView.setText("N");
        }
        else if (direction.equals("down"))
        {
            directionAxisTextView.setText("S");
        }
        else if (direction.equals("right"))
        {
            directionAxisTextView.setText("E");
        }
        else if (direction.equals("left"))
        {
            directionAxisTextView.setText("W");
        }
        else if (direction.equals("upleft"))
        {
            directionAxisTextView.setText("NW");
        }
        else if (direction.equals("upright"))
        {
            directionAxisTextView.setText("NE");
        }
        else if (direction.equals("downleft"))
        {
            directionAxisTextView.setText("SW");
        }
        else if (direction.equals("downright"))
        {
            directionAxisTextView.setText("SE");
        }
        else
        {
            directionAxisTextView.setText("None");
        }
    }

    private void setObstacleCoord(int col, int row) {
        showLog("Entering setObstacleCoord");
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obsCoord.add(obstacleCoord);
        cells[col][row].setType("obstacle");
        // set obstacle No
        for (int i = 0; i < obsNoArray.length; i++) {
            if (obsNoArray[i] != -1) {
                if (cells[col][row].obstacleNo == -1) {
                    cells[col][row].obstacleNo = obsNoArray[i]; // assign obstacle no
                    String random = col + "" + row;
                    obsNoArray[i] = -1; // set index to marked as used
                    MapFragment.updateObstacleList(" Obstacle No: " + cells[col][row].obstacleNo + "\t\tX: " + (col - 1) + "\t\tY: " + (row - 1) + "\n Direction: " + cells[col][row].getobstacleFacing());
                    break;
                }
            }
        }
        showLog("Exiting setObstacleCoord");
        // TODO: uncommand for bluetooth

        if(!obsSelected)
        {
            try {
                BluetoothFragment.printMsg("Obstacle", (col - 1), (row - 1), getObstacleDirectionText(newDirection) + "\n");
                BluetoothFragment.printMsg("Obstacle" + " Column: " + (col - 1) + " Row: " + (row - 1) + " Direction: " + getObstacleDirectionText(newDirection) + "\n");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<int[]> getObstacleCoord() {
        return obsCoord;
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private String getObstacleDirectionText(int inDirection)
    {
        String direction = "";
        switch (inDirection)
        {
            case 0:
                direction = "NONE";
                break;
            case 1:
                direction = "UP";
                break;
            case 2:
                direction = "DOWN";
                break;
            case 3:
                direction = "LEFT";
                break;
            case 4:
                direction = "RIGHT";
                break;
        }

        return direction;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showLog("Entering onTouchEvent");
        //column = 1-20, row = 1-20
        int column = (int) (event.getX() / cellSize);
        int row = this.convertRow((int) (event.getY() / cellSize));
        ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.startingPointButton);

        // && column<=20 && row<=20 && column>=1 && row>=1 is the validation when placing in the map
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false && column<=20 && row<=20 && column>=1 && row>=1) {
            if (startingCoordStatus) {
                if (drawableRobot) {
                    int[] startCoord = this.getStartingCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.convertRow(startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++)
                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                    }
                } else
                    drawableRobot = true;
                this.setStartingCoord(column, row);
                startingCoordStatus = false;
                String direction = getRobotDirection();
                if (direction.equals("None")) {
                    direction = "up";
                }
                try {
                    // TODO: uncoomand for Bluetooth
                    // For RPI
                    rpiRobot = "Al," + (row - 1) + "," + (column - 1) + "," + rpiConvertDirection(direction.toUpperCase()) + ",";
                    showLog("rpiRobot: " + rpiRobot);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                updateRobotAxis(column, row, direction);
                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();
                if (MapFragment.isSetStartingPoint) {
                    MapFragment.isSetStartingPoint = false;
                }

                this.invalidate();
                return true;
            }
            if (setObsStatus) { // setting the position of the obstacle


                // TODO: New direction
                cells[column][row].setobstacleFacing(getObstacleDirectionText(newDirection));

                this.setObstacleCoord(column, row);

                this.invalidate();
                return true;

            }
            if (setExploredStatus) {
                cells[column][row].setType("explored");
                this.invalidate();
                return true;
            }
            if (clearCellStatus) {
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][row].setType("unexplored");
                for (int i = 0; i < obstacleCoord.size(); i++) {
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row) {
                        //BluetoothFragment.printMessage("RemovedObstacle, " + "<" + valueOf(obsCoord.get(i)[0] -1) + "," + valueOf(Math.abs(obsCoord.get(i)[1]) - 1) + ">");
                        //BluetoothFragment.printMessage("RemovedObstacle, " + "<" + valueOf(selectedObsCoord[0] - 1) + ">, <" + valueOf(Math.abs(selectedObsCoord[1]) - 1) + ">, <" + cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo + ">");
                        obsNoArray[cells[column][row].obstacleNo - 1] = cells[column][row].obstacleNo; // unset obstacle no by assigning number back to array
                        cells[column][row].obstacleNo = -1;
                        obstacleCoord.remove(i);
                        if (cellArr.get(cellArr.size() - 1) == cells[column][row]) {
                            cellArr.remove(cellArr.size() - 1);
                            //oCellArrDirection.remove(oCellArrDirection.size()-1);
                        }
                    }
                }
                this.invalidate();
                return true;
            }
            if (setObsDirection) {
                showLog("Enter set obstacle direction");


                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
                mBuilder.setTitle("Select Obstacle Direction");
                mBuilder.setSingleChoiceItems(directionList, changeDirection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        changeDirection = i;
                    }
                });
                mBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Create the new cell
                        MapFragment.removeObstacleFromList(" Obstacle No: " + cells[column][row].obstacleNo + "\t\tX: " + (column - 1) + "\t\tY: " + (row - 1) + "\n Direction: " + cells[column][row].getobstacleFacing());
                        switch (changeDirection) {
                            case 0:
                                cells[column][row].setobstacleFacing("NONE");
                                break;
                            case 1:
                                cells[column][row].setobstacleFacing("UP");
                                break;
                            case 2:
                                cells[column][row].setobstacleFacing("DOWN");
                                break;
                            case 3:
                                cells[column][row].setobstacleFacing("LEFT");
                                break;
                            case 4:
                                cells[column][row].setobstacleFacing("RIGHT");
                                break;
                        }

                        MapFragment.updateObstacleList("Obstacle No: " + cells[column][row].obstacleNo + "\t\tX: " + (column - 1) + "\t\tY: " + (row - 1) + "\t\tDirection: " + cells[column][row].getobstacleFacing());
                        invalidate();

                        try {
                            BluetoothFragment.printMsg("Obstacle direction change", column, (row), getObstacleDirectionText(changeDirection));
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }

                        dialogInterface.dismiss();
                    }
                });

                // check if the cell selected is obstacle or not
                if(cells[column][row].type.equals("obstacle") || cells[column][row].type.equals("image")) {

                    AlertDialog dialog = mBuilder.create();
                    dialog.show();
                }

                this.invalidate();
                showLog("Exit set obstacle direction");
                return true;
            }
            // selection of obstacle in the map
            if (obsSelected == false){
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                for (int i=0; i<obstacleCoord.size(); i++)
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row) {
                        selectedObsCoord[0] = column;
                        selectedObsCoord[1] = row;
                        for (int x = 0; x < cellArr.size(); x++) {
                            if (cellArr.get(x) == cells[column][row]) {
                                selectedObsCoord[2] = x;
                            }
                        }
                        obsSelected = true;
                        return true;
                    }
            }
        }
        // when touch event is release from the map
        else if (event.getAction() == MotionEvent.ACTION_UP && this.getAutoUpdate() == false) {
            if(obsSelected) {
                obsSelected = false;
                Log.d("obsSelected", Boolean.toString(obsSelected));
                try {
                    BluetoothFragment.printMsg("Obstacle", (column - 1), (row - 1), getObstacleDirectionText(newDirection) + "\n");
                    BluetoothFragment.printMsg("Obstacle" + " Column: " + (column - 1) + " Row: " + (row - 1) + " Direction: " + getObstacleDirectionText(newDirection) + "\n");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        // moving obstacle around or out of the map
        else if (event.getAction() == MotionEvent.ACTION_MOVE && this.getAutoUpdate() == false) {
            if (obsSelected) {
                boolean occupied = false;
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                for (int i = 0; i < obstacleCoord.size(); i++) {
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row) {
                        occupied = true;
                    }
                }
                if (occupied == false) {

                    String obstacleRemove = " Obstacle No: " + cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo + "\t\tX: "+ (selectedObsCoord[0] - 1) + "\t\tY: " + (selectedObsCoord[1] - 1) +  "\n Direction: ";
                    if(cells[selectedObsCoord[0]][selectedObsCoord[1]].getobstacleFacing() != null)
                        obstacleRemove += cells[selectedObsCoord[0]][selectedObsCoord[1]].getobstacleFacing();
                    MapFragment.removeObstacleFromList(obstacleRemove);
                    // TODO: NEW obstacle
                    showLog("RemovedObstacle, " + "<" + (selectedObsCoord[0] - 1) + ">, <" + (Math.abs(selectedObsCoord[1]) - 1) + ">, <" + cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo + ">");
                    obsNoArray[cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo - 1] = cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo; // unset obstacle no by assigning number back to array
                    cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo = -1;
                    cells[selectedObsCoord[0]][selectedObsCoord[1]].setType("unexplored");
                    // Remove obstacle facing direction
                    obsSelectedFacing = cells[selectedObsCoord[0]][selectedObsCoord[1]].getobstacleFacing(); // <-- newly added
                    cells[selectedObsCoord[0]][selectedObsCoord[1]].setobstacleFacing(null); // <-- newly added
                    // Remove target ID
                    showLog("" + selectedObsCoord[0] + " " + (selectedObsCoord[1]));
                    obsImageTargetID = cells[selectedObsCoord[0]][selectedObsCoord[1]].targetID; // <-- newly added
                    cells[selectedObsCoord[0]][selectedObsCoord[1]].targetID = null; // <-- newly added

                    //Remove from obstacles coord arraylist
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] == selectedObsCoord[0] && obstacleCoord.get(i)[1] == selectedObsCoord[1]) {
                            obstacleCoord.remove(i);
                        }
                    }
                    //If selection is within the grid
                    if (column <= 20 && row <= 20 && column >= 1 && row >= 1) {
                        //Create the new cell
                        cellArr.set(selectedObsCoord[2], cells[column][row]);
                        selectedObsCoord[0] = column;
                        selectedObsCoord[1] = row;
                        // TODO: new Obstacle
                        // Add obstacle facing direction
                        cells[column][row].setobstacleFacing(obsSelectedFacing); // <-- newly added
                        // Add target ID
                        cells[column][row].targetID = obsImageTargetID;  // <-- newly added

                        this.setObstacleCoord(column, row);
                    }
                    //If selection is outside the grid
                    else if (column < 1 || row < 1 || column > 20 || row > 20) {
                        obsSelected = false;
                        //Remove from cellArr
                        if (cellArr.get(cellArr.size() - 1) == cells[selectedObsCoord[0]][selectedObsCoord[1]]) {
                            cellArr.remove(cellArr.size() - 1);

                            // Remove obstacle facing direction
                            cells[selectedObsCoord[0]][selectedObsCoord[1]].setobstacleFacing(null); //<-- newly added
                            // Remove target ID
                            cells[selectedObsCoord[0]][selectedObsCoord[1]].targetID = null; // <-- newly added
                        }
                    }
                    this.invalidate();
                    return true;
                }
            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    // For RPI
    String rpiConvertDirection(String direction) {
        String direction_NSEW = "";
        switch (direction) {
            case "UP":
                direction_NSEW = "N";
                break;
            case "DOWN":
                direction_NSEW = "S";
                break;
            case "LEFT":
                direction_NSEW = "W";
                break;
            case "RIGHT":
                direction_NSEW = "E";
                break;
            default:
                direction_NSEW = "NONE";
        }
        return direction_NSEW;
    }


    public void toggleCheckedBtn(String buttonName) {
        ToggleButton setStartPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.startingPointButton);
        ToggleButton setObstacleBtn = ((Activity) this.getContext()).findViewById(R.id.setObstacleToggleBtn);

        if (!buttonName.equals("setStartingPointBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartingCoordStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!buttonName.equals("setObstacleBtn"))
            if (setObstacleBtn.isChecked()) {
                this.setSetObstacleStatus(false);
                setObstacleBtn.toggle();
            }

    }

    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusText);
        updateRobotAxis(1, 1, "None");
        robotStatusTextView.setText("Not Available");
        SharedPreferences.Editor editor = sharedPreferences.edit();

        this.toggleCheckedBtn("None");

        startingCoord = new int[]{-1, -1};
        currentCoord = new int[]{-1, -1};
        previousCoord = new int[]{-1, -1};
        robotDirection = "None";
        autoUpdate = false;
        arrowCoord = new ArrayList<>();
        obsCoord = new ArrayList<>();
        mapDrawn = false;
        drawableRobot = false;
        validPosition = false;
        cellArr = new ArrayList<>();
        rpiObstacle = "";
        rpiRobot = "";

        // newly added
        obsNoArray = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15}; //reset obstacle no array


        showLog("Exiting resetMap");
        this.invalidate();
    }

    public void moveRobot(String direction) {
        showLog("Entering moveRobot");
        setValidPosition(false);
        int[] curCoord = this.getCurrentCoord();
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        this.setPreRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getPreRobotCoord();
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        switch (robotDirection) {
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != ROW - 1) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "right";
                        break;
                    case "back":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "left";
                        break;
                    default:
                        robotDirection = "error up";
                        break;
                }
                break;
            case "right":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != COL-1) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "down";
                        break;
                    case "back":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "up";
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "back":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "left";
                        break;
                    case "back":
                        if (curCoord[1] != COL-1) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "right";
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "left":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "up";
                        break;
                    case "back":
                        if (curCoord[0] != COL-1) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "down";
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoord";
                break;
        }
        if (getValidPosition())
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] != x || obstacleCoord.get(i)[1] != y)
                            setValidPosition(true);
                        else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurrentCoord(curCoord[0], curCoord[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurrentCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }



    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;
        int id = -1;
        String obstacleFacing = "";

        String targetID = null;
        int obstacleNo = -1;

        boolean isDirection = false;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obsColour;
                    break;
                case "robot":
                    this.paint = robotColour;
                    break;
                case "end":
                    this.paint = endingColour;
                    break;
                case "start":
                    this.paint = startingColour;
                    break;
                case "unexplored":
                    this.paint = unexploredColor;
                    break;
                case "explored":
                    this.paint = exploredColor;
                    break;
                case "arrow":
                    this.paint = arrowColor;
                    break;
                case "image":
                    this.paint = imageColour;
                    break;
                default:
                    showLog("setTtype default: " + type);
                    break;
            }
        }

        public void setobstacleFacing(String obstacleFacing) {
            this.obstacleFacing = obstacleFacing;
        }

        public String getobstacleFacing() {
            return this.obstacleFacing;
        }

        public int getId() {
            return this.id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

}
