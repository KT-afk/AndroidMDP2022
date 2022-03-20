package com.example.androidmdp2022;

import static java.lang.String.valueOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;

public class GridMap extends View {

    public GridMap(Context c) {
        super(c);
        initMap();
    }

    SharedPreferences sharedPreferences;

    private final Paint blackPaint = new Paint();
    private final Paint imageColor = new Paint();
    private final Paint obstacleColor = new Paint();
    private final Paint robotColor = new Paint();
    private final Paint endColor = new Paint();
    private final Paint startColor = new Paint();
    private final Paint waypointColor = new Paint();
    private final Paint unexploredColor = new Paint();
    private final Paint exploredColor = new Paint();
    private final Paint arrowColor = new Paint();
    //private Paint fastestPathColor = new Paint();
    private final Paint imageLine = new Paint();
    private final Paint imageLineConfirm = new Paint();

    private static JSONObject receivedJsonObject = new JSONObject();
    private static JSONObject mapInformation;
    private static JSONObject backupMapInformation;
    private static String robotDirection = "None";
    private static int[] startCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};
    private static int[] waypointCoord = new int[]{-1, -1};
    private static ArrayList<String[]> arrowCoord = new ArrayList<>();
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>();
    private static boolean autoUpdate = false;
    private static boolean canDrawRobot = false;
    private static boolean setWaypointStatus = false;
    private static boolean startCoordStatus = false;
    private static boolean setObstacleStatus = false;
    private static boolean unSetCellStatus = false;
    private static boolean setExploredStatus = false;
    private static boolean validPosition = false;
    private static final boolean waypointNew = false;
    private boolean newEndCoord = false;
    private static boolean setObstacleDirection = false;

    private static final String TAG = "GridMap";
    private static final int COL = 20;
    private static final int ROW = 20;
    private static float cellSize;
    private static Cell[][] cells;
    private Canvas can;

    private boolean mapDrawn = false;
    public static String publicMDFExploration;
    public static String publicMDFObstacle;

    private static final int[] selectedObsCoord = new int[3];
    private static boolean obsSelected = false;
    private static ArrayList<Cell> oCellArr = new ArrayList<Cell>();

    int newDirection = -1; // 0:None 1: Up, 2: Down, 3: Left, 4:Right
    int switchDirection = -1; // 0:None 1: Up, 2: Down, 3: Left, 4:Right
    String[] directionList = new String[] {"NONE","UP", "DOWN", "LEFT", "RIGHT"};
    private static String obsSelectedFacing = null; // <-- newly added
    private static String obsTargetImageID = null; // <-- newly added
    private static int[] obstacleNoArray = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};

    //For RPI message
    String rpiRobot = "";
    String rpiObstacle;

    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initMap();
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        imageColor.setColor(Color.MAGENTA);
        obstacleColor.setColor(Color.BLACK);
        robotColor.setColor(Color.CYAN);
        endColor.setColor(Color.GREEN);
        startColor.setColor(Color.CYAN);
        waypointColor.setColor(Color.parseColor("#fefdca"));
        waypointColor.setColor(Color.parseColor("#fefdca"));
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
        showLog("Redrawing map");

        //CREATE CELL COORDINATES
        Log.d(TAG,"Creating Cell");

        if (!mapDrawn) {
            String[] dummyArrowCoord = new String[3];
            dummyArrowCoord[0] = "1";
            dummyArrowCoord[1] = "1";
            dummyArrowCoord[2] = "dummy";
            arrowCoord.add(dummyArrowCoord);
            this.createCell();
            // TODO: Remove end coordinate
            //this.setEndCoord(COL-1, ROW-1);
            newEndCoord=true;
            showLog("Map drawn");
        }

        drawIndividualCell(canvas);
        drawHorizontalLines(canvas);
        drawVerticalLines(canvas);
        drawGridNumber(canvas);

        if(newEndCoord==true){
            int endcol = COL-1;
            int endrow = ROW-1;
            RectF endrect = new RectF(endcol * cellSize, endrow * cellSize, (endcol + 1) * cellSize, (endrow + 1) * cellSize);
            mapDrawn = true;
            newEndCoord=false;
        }

        if (getCanDrawRobot())
            drawRobot(canvas, curCoord);
        showLog("Exiting onDraw");
    }
    public int returnObstacleId(int x, int y){
        if(x > 21 || y > 21)
            return -100;
        return cells[x][y].obstacleNo; }
    public String returnObstacleFacing(int x, int y){
        return cells[x][y].obstacleFacing;
    }

    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");

        for (int x = 1; x <= COL; x++)
            for (int y = 1; y <= ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    if (cells[x][y].getId() == -1) {
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);

                        // this is for drawing numbers in obstacle and using oCellArr arraylist for drag and drop
                        if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("image")) {
                            boolean written = false;
                            for (int a = 0; a < oCellArr.size(); a++) {
                                if (cells[x][y] == oCellArr.get(a)) {

                                    // TODO: NEW Target ID
                                    if(cells[x][y].targetID == null) {
                                        canvas.drawText(Integer.toString(cells[x][y].obstacleNo), cells[x][y].startX + (cellSize / 3.2f), cells[x][y].startY + (cellSize / 1.5f), exploredColor);
                                    } else{
                                        Paint textPaint2 = new Paint();
                                        textPaint2.setTextSize(20);
                                        textPaint2.setColor(Color.WHITE);
                                        textPaint2.setTextAlign(Paint.Align.CENTER);
                                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, blackPaint);
                                        canvas.drawText(cells[x][y].targetID, (cells[x][y].startX+cells[x][y].endX)/2, cells[x][y].endY + (cells[x][y].startY-cells[x][y].endY)/4, textPaint2);
                                    }
                                    written = true;
                                    break;
                                }
                            }

                            if (written == false) {

                                // TODO: NEW Obstacle ID
                                oCellArr.add(cells[x][y]);
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
    // Obstacle Face - Function to convert drawable to bitmap  @ GridMap.java (start)
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    } // end

    public void drawImageNumberCell(int id,int x, int y) {
        cells[x+1][19-y].setType("image");
        cells[x+1][19-y].setId(id);
        this.invalidate();
    }

    public void setCellType(int x, int y, String type){
        cells[x+1][19-y].setType(type);
        cells[x+1][19-y].setId(-1);
        this.invalidate();
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
    public void drawRobotIfEmpty(){

    }

    public void updateImageNumberCell(int obstacleNo, String targetID, String obstacleFacing){
        // find the obstacle no which has the same id
        for (int x = 1; x <= COL; x++)
            for (int y = 1; y <= ROW; y++)
                for (int i = 0; i < this.getArrowCoord().size(); i++)
                    if (cells[x][y].obstacleNo == obstacleNo && cells[x][y].type == "obstacle") {
                        cells[x][y].targetID = targetID;
                        cells[x][y].isDirection = true;
                        if(obstacleFacing.contains("UP")) {
                            cells[x][y].setobstacleFacing("UP");
                        }
                        if(obstacleFacing.contains("DOWN")) {
                            cells[x][y].setobstacleFacing("DOWN");
                        }
                        if(obstacleFacing.contains("RIGHT")) {
                            cells[x][y].setobstacleFacing("RIGHT");
                        }
                        if(obstacleFacing.contains("LEFT")) {
                            cells[x][y].setobstacleFacing("LEFT");

                        }
                        break;
                    }
        this.invalidate();
    }

    // For RPI
        public void updateImageNumberCellRPI(int x, int y, String targetID, String obstacleFacing)
        {
            cells[x+1][19-y].targetID = targetID;
            cells[x+1][19-y].isDirection = true;
            if(obstacleFacing.contains("N")) {
                cells[x+1][19-y].setobstacleFacing("UP");
            }
            if(obstacleFacing.contains("S")) {
                cells[x+1][19-y].setobstacleFacing("DOWN");
            }
            if(obstacleFacing.contains("E")) {
                cells[x+1][19-y].setobstacleFacing("RIGHT");
            }
            if(obstacleFacing.contains("W")) {
                cells[x+1][19-y].setobstacleFacing("LEFT");

            }
            this.invalidate();
        }
    public void updateImageNumberCellRPI(int x, int y, String targetID)
    {
        cells[x+1][19-y].targetID = targetID;
        this.invalidate();
    }

    private void drawHorizontalLines(Canvas canvas) {
        for (int y = 1; y <= ROW+1; y++)
            canvas.drawLine(cells[1][y].startX, cells[1][y].startY - (cellSize / 30), cells[20][y].endX, cells[20][y].startY - (cellSize / 30), blackPaint);
    }

    private void drawVerticalLines(Canvas canvas) {
        for (int x = 0; x <= COL; x++)
            canvas.drawLine(cells[x][1].startX - (cellSize / 30) + cellSize, cells[x][1].startY - (cellSize / 30), cells[x][1].startX - (cellSize / 30) + cellSize, cells[x][20].endY + (cellSize / 30), blackPaint);
    }

    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        for (int x = 1; x <= COL; x++) {
            if (x > 9)
                canvas.drawText(Integer.toString(x-1), cells[x][0].startX + (cellSize / 5), cells[x][0].startY + (cellSize / 3), blackPaint);
            else
                canvas.drawText(Integer.toString(x-1), cells[x][0].startX + (cellSize / 3), cells[x][0].startY + (cellSize / 3), blackPaint);
        }
        for (int y = 1; y <= ROW; y++) {
            if (y > 10)
                canvas.drawText(Integer.toString(y-1), cells[0][y].startX + (cellSize / 2.5f), cells[0][y].startY + (cellSize / 2), blackPaint);
            else
                canvas.drawText(Integer.toString(y-1), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 2), blackPaint);
        }
        showLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas, int[] curCoord) {
        showLog("Entering drawRobot");
        if(newEndCoord){
            int endcol = 19;
            int endrow = 19;
            RectF endrect = new RectF(endcol * cellSize, endrow * cellSize, (endcol + 1) * cellSize, (endrow + 1) * cellSize);
            newEndCoord=false;
        }
        for (int y = curCoord[1] - 1; y < curCoord[1] + 1; y++){
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor );
        }
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++){
            canvas.drawLine(cells[x][curCoord[1] - 1].startX - (cellSize / 30) + cellSize, cells[x][curCoord[1] - 1].startY, cells[x][curCoord[1] + 1].startX - (cellSize / 30) + cellSize, cells[x][curCoord[1] + 1].endY, robotColor);
        }
        int col = curCoord[0];
        int row = curCoord[1];
        RectF rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);

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

    public void setNewDirection(int newDirection)
    {
        this.newDirection = newDirection;
    }

    public static String binaryToHex(String bin) {
        String hex;
        BigInteger b = new BigInteger(bin, 2);
        hex=b.toString(16);
        return hex;
    }
    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public boolean getMapDrawn() {
        return mapDrawn;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    public boolean getValidPosition() {
        return validPosition;
    }

    public void setUnSetCellStatus(boolean status) {
        unSetCellStatus = status;
    }

    public boolean getUnSetCellStatus() {
        return unSetCellStatus;
    }

    public void setSetObstacleDirection(boolean status)
    {
        setObstacleDirection = status;
    }

    public boolean getSetObstacleDirection()
    {
        return setObstacleDirection;
    }

    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    public boolean getExploredStatus() {
        return setExploredStatus;
    }

    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    public void setWaypointStatus(boolean status) {
        setWaypointStatus = status;
    }

    public boolean getCanDrawRobot() {
        return canDrawRobot;
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

    public void setEndCoord(int col, int row) {
        showLog("Entering setEndCoord");
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");

        showLog("Exiting setEndCoord");
    }

    public void setStartCoord(int col, int row){
        showLog("Entering setStartCoord");
        startCoord[0] = col;
        startCoord[1] = row;
        String direction = getRobotDirection();
        if(direction.equals("None")) {
            direction = "up";
        }
        if (this.getStartCoordStatus())
            this.setCurCoord(col, row, direction);
        showLog("Exiting setStartCoord");
    }

    private int[] getStartCoord() {
        return startCoord;
    }

    public void setCurCoord(int col, int row, String direction) {
        showLog("Entering setCurCoord");
        curCoord[0] = col;
        curCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxis(col, row, direction);

        //row = 20- row;
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        showLog("Exiting setCurCoord");
    }

    public int[] getCurCoord() {
        return curCoord;
    }

    private void calculateDimension() {
        this.setCellSize(getWidth()/(COL+1));
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

    private void setOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        showLog("Exiting setOldRobotCoord");
    }

    public void unsetOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        //oldRow = 20- oldRow;
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("unexplored");
        showLog("Exiting setOldRobotCoord");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
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
        GridMap.obstacleCoord.add(obstacleCoord);
        cells[col][row].setType("obstacle");
        // set obstacle No
        for(int i = 0; i<obstacleNoArray.length; i++){
            if(obstacleNoArray[i] != -1){
                if(cells[col][row].obstacleNo == -1){
                    cells[col][row].obstacleNo = obstacleNoArray[i]; // assign obstacle no
                    String random = col + "" + row;
                    obstacleNoArray[i] = -1; // set index to marked as used
                    ArenaFragment.updateObstacleList( " Obstacle No: " + cells[col][row].obstacleNo + "\t\tX: "+ (col-1) + "\t\tY: " + (row -1) +  "\n Direction: " + cells[col][row].getobstacleFacing());
                    break;
                }
            }
        }
        showLog("Exiting setObstacleCoord");
        // TODO: uncommand for bluetooth

        if(!obsSelected)
        {
            try {
                BluetoothFragment.printMessage("Obstacle", (col-1), (row-1), getObstacleDirectionText(newDirection) + "\n");
                BluetoothFragment.printMessage("Obstacle"+ " Column: "+ (col-1) + " Row: "+ (row -1) + " Direction: " + getObstacleDirectionText(newDirection) + "\n");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
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
                    this.paint = obstacleColor;
                    break;
                case "robot":
                    this.paint = robotColor;
                    break;
                case "end":
                    this.paint = endColor;
                    break;
                case "start":
                    this.paint = startColor;
                    break;
                case "waypoint":
                    this.paint = waypointColor;
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
                /*case "fastestPath":
                    this.paint = fastestPathColor;
                    break;*/
                case "image":
                    this.paint = imageColor;
                    break;
                case "id":
                    this.paint = obstacleColor;
                    break;
                default:
                    showLog("setTtype default: " + type);
                    break;
            }
        }

        // Obstacle Face @ GridMap.Java -> class Cell
        public void setobstacleFacing(String obstacleFacing) {
            this.obstacleFacing = obstacleFacing;
        }
        public String getobstacleFacing() {
            return this.obstacleFacing;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    // For RPI
    String rpiConvertDirection(String direction)
    {
        String direction_NSEW = "";
        switch (direction)
        {
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

    //For RPI
    public void sendRPIMessage()
    {
        if(rpiRobot.equals("") == false) {
            String message = rpiRobot;

            for (int x = 1; x <= COL; x++) {
                for (int y = 1; y <= ROW; y++) {
                    for (int i = 0; i < this.getArrowCoord().size(); i++) {

                        if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("image")) {

                            message = message + (x - 1) + "," + (y - 1) + "," + rpiConvertDirection(cells[x][y].getobstacleFacing()) + ",";
                        }

                    }
                }
            }

            // remove the last coma
            StringBuffer messageBuffer = new StringBuffer(message);
            messageBuffer.deleteCharAt(messageBuffer.length() - 1);
            //TODO Bluetooth Comment
        }
        else
        {
            Toast.makeText(this.getContext(), "Please set the Starting Position of the robot", Toast.LENGTH_LONG).show();
        }


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
            if (startCoordStatus) {
                if (canDrawRobot) {
                    int[] startCoord = this.getStartCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.convertRow(startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++)
                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                    }
                }
                else
                    canDrawRobot = true;
                this.setStartCoord(column, row);
                startCoordStatus = false;
                String direction = getRobotDirection();
                if(direction.equals("None")) {
                    direction = "up";
                }
                try {
                    int directionInt = 0;
                    if(direction.equals("up")){
                        directionInt = 0;
                    } else if(direction.equals("left")) {
                        directionInt = 3;
                    } else if(direction.equals("right")) {
                        directionInt = 1;
                    } else if(direction.equals("down")) {
                        directionInt = 2;
                    }
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
                // reset setStartPointFAB icon
                if (ArenaFragment.isSetStartingPiont)
                {
                    ArenaFragment.isSetStartingPiont = false;
                }

                this.invalidate();
                return true;
            }
            if (setObstacleStatus) { // setting the position of the obstacle


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
            if (unSetCellStatus) { // TODO: remove obstacle not yet use (ontouch on the map to remove)
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][row].setType("unexplored");
                for (int i=0; i<obstacleCoord.size(); i++) {
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row){
                        // TODO: uncoomand for bluetooth
                        //BluetoothFragment.printMessage("RemovedObstacle, " + "<" + valueOf(obstacleCoord.get(i)[0] -1) + "," + valueOf(Math.abs(obstacleCoord.get(i)[1]) - 1) + ">");
                        //BluetoothFragment.printMessage("RemovedObstacle, " + "<" + valueOf(selectedObsCoord[0] - 1) + ">, <" + valueOf(Math.abs(selectedObsCoord[1]) - 1) + ">, <" + cells[selectedObsCoord[0]][20 - selectedObsCoord[1]].obstacleNo + ">");
                        obstacleNoArray[cells[column][row].obstacleNo - 1] = cells[column][row].obstacleNo; // unset obstacle no by assigning number back to array
                        cells[column][row].obstacleNo = -1;
                        obstacleCoord.remove(i);
                        if(oCellArr.get(oCellArr.size()-1) == cells[column][row]){
                            oCellArr.remove(oCellArr.size()-1);
                            //oCellArrDirection.remove(oCellArrDirection.size()-1);
                        }
                    }
                }
                this.invalidate();
                return true;
            }
            if(setObstacleDirection)
            {
                showLog("Enter set obstacle direction");


                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
                mBuilder.setTitle("Select Obstacle Direction");
                mBuilder.setSingleChoiceItems(directionList, switchDirection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switchDirection = i;
                    }
                });
                mBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Create the new cell
                        ArenaFragment.removeObstacleFromList( " Obstacle No: " + cells[column][row].obstacleNo + "\t\tX: "+ (column-1) + "\t\tY: " + (row-1) +  "\n Direction: " + cells[column][row].getobstacleFacing());
                        switch (switchDirection)
                        {
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

                        ArenaFragment.updateObstacleList( "Obstacle No: " + cells[column][row].obstacleNo + "\t\tX: "+ (column-1) + "\t\tY: " + (row-1) +  "\t\tDirection: " + cells[column][row].getobstacleFacing());
                        invalidate();

                        try {
                            BluetoothFragment.printMessage("Obstacle direction change",column,(row), getObstacleDirectionText(switchDirection));
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
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row){
                        selectedObsCoord[0] = column;
                        selectedObsCoord[1] = row;
                        for (int x = 0; x < oCellArr.size(); x++) {
                            if (oCellArr.get(x) == cells[column][row]) {
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
                    BluetoothFragment.printMessage("Obstacle", (column-1), (row-1), getObstacleDirectionText(newDirection) + "\n");
                    BluetoothFragment.printMessage("Obstacle"+ " Column: "+ (column-1) + " Row: "+ (row-1) + " Direction: " + getObstacleDirectionText(newDirection) + "\n");
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
                    ArenaFragment.removeObstacleFromList(obstacleRemove);
                    // TODO: NEW obstacle
                    showLog("RemovedObstacle, " + "<" + (selectedObsCoord[0] - 1) + ">, <" + (Math.abs(selectedObsCoord[1]) - 1) + ">, <" + cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo + ">");
                    obstacleNoArray[cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo - 1] = cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo; // unset obstacle no by assigning number back to array
                    cells[selectedObsCoord[0]][selectedObsCoord[1]].obstacleNo = -1;
                    cells[selectedObsCoord[0]][selectedObsCoord[1]].setType("unexplored");
                    // Remove obstacle facing direction
                    obsSelectedFacing = cells[selectedObsCoord[0]][selectedObsCoord[1]].getobstacleFacing(); // <-- newly added
                    cells[selectedObsCoord[0]][selectedObsCoord[1]].setobstacleFacing(null); // <-- newly added
                    // Remove target ID
                    showLog(""+selectedObsCoord[0] + " " + (selectedObsCoord[1]));
                    obsTargetImageID = cells[selectedObsCoord[0]][selectedObsCoord[1]].targetID; // <-- newly added
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
                        oCellArr.set(selectedObsCoord[2], cells[column][row]);
                        selectedObsCoord[0] = column;
                        selectedObsCoord[1] = row;
                        // TODO: new Obstacle
                        // Add obstacle facing direction
                        cells[column][row].setobstacleFacing(obsSelectedFacing); // <-- newly added
                        // Add target ID
                        cells[column][row].targetID = obsTargetImageID;  // <-- newly added

                        this.setObstacleCoord(column, row);
                    }
                    //If selection is outside the grid
                    else if (column < 1 || row < 1 || column > 20 || row > 20) {
                        obsSelected = false;
                        //Remove from oCellArr
                        if (oCellArr.get(oCellArr.size() - 1) == cells[selectedObsCoord[0]][selectedObsCoord[1]]) {
                            oCellArr.remove(oCellArr.size() - 1);

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

    public void toggleCheckedBtn(String buttonName) {
        ToggleButton setStartPointToggleBtn = ((Activity)this.getContext()).findViewById(R.id.startingPointButton);
        ToggleButton setObstacleBtn = ((Activity)this.getContext()).findViewById(R.id.setObstacleToggleBtn);

        if (!buttonName.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartCoordStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!buttonName.equals("setObstacleToggleBtn"))
            if (setObstacleBtn.isChecked()) {
                this.setSetObstacleStatus(false);
                setObstacleBtn.toggle();
            }

    }

    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusText);
        updateRobotAxis(1, 1, "None");
        robotStatusTextView.setText("Not Available");
        SharedPreferences.Editor editor = sharedPreferences.edit();

        this.toggleCheckedBtn("None");

        receivedJsonObject = null;
        backupMapInformation = null;
        startCoord = new int[]{-1, -1};
        curCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};
        robotDirection = "None";
        autoUpdate = false;
        arrowCoord = new ArrayList<>();
        obstacleCoord = new ArrayList<>();
        waypointCoord = new int[]{-1, -1};
        mapDrawn = false;
        canDrawRobot = false;
        validPosition = false;
        oCellArr = new ArrayList<>();
        rpiObstacle = "";
        rpiRobot = "";
        //oCellArrDirection = new ArrayList<>();

        // newly added
        obstacleNoArray = new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}; //reset obstacle no array


        showLog("Exiting resetMap");
        this.invalidate();
    }

    public void changeDirection(String direction){
        showLog("Entering changeDirection");
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;
        switch(robotDirection){
            case "up":
                switch(direction){
                    case "forward right":
                    case "back right":
                        robotDirection = "right";
                        break;
                    case "forward left":
                    case "back left":
                        robotDirection = "left";
                        break;
                }
                break;
            case "right":
                switch(direction){
                    case "forward right":
                    case "back right":
                        robotDirection = "down";
                        break;
                    case "forward left":
                    case "back left":
                        robotDirection = "up";
                        break;
                }
                break;
            case "left":
                switch(direction){
                    case "forward right":
                    case "back right":
                        robotDirection = "up";
                        break;
                    case "forward left":
                    case "back left":
                        robotDirection = "down";
                        break;
                }
                break;
            case "down":
                switch(direction){
                    case "forward right":
                    case "back right":
                        robotDirection = "left";
                        break;
                    case "forward left":
                    case "back left":
                        robotDirection = "right";
                        break;
                }
                break;
        }
    }
    // TODO: need find a way to make the turn like a car using angle (clock)
    public void moveRobot(String direction) {
        showLog("Entering moveRobot");
        setValidPosition(false);
        int[] curCoord = this.getCurCoord();
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        this.setOldRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getOldRobotCoord();
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        switch (robotDirection) {
            //Actual moveable space 2-19
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != ROW -1) {
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
            case "down":
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
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }

    public void printRobotStatus(String message) {
        TextView robotStatusTextView = ((Activity)this.getContext()).findViewById(R.id.robotStatusText);
        robotStatusTextView.setText(message);
    }

    public static void setPublicMDFExploration(String msg) {
        publicMDFExploration = msg;
    }

    public static void setPublicMDFObstacle(String msg) {
        publicMDFObstacle = msg;
    }

    public static String getPublicMDFExploration() {
        return publicMDFExploration;
    }

    public static String getPublicMDFObstacle() {
        return publicMDFObstacle;
    }


}
