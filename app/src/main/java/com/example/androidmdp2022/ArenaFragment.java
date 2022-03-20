package com.example.androidmdp2022;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.SENSOR_SERVICE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ArenaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ArenaFragment extends Fragment implements SensorEventListener {

    // Declaration Variables
    String [] directionOption = {"NONE", "UP", "DOWN", "LEFT", "RIGHT"};

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "ArenaFragment";

    ImageButton resetMapBtn, updateButton;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn;
    ToggleButton setStartPointToggleBtn, setWaypointToggleBtn, setObstacleToggleBtn, setObstacleDirectionToggleBtn;
    Switch manualAutoToggleBtn;
    private static final boolean autoUpdate = false;
    public static boolean manualUpdateRequest = false;

    String fobsstring,fexpstring;

    private static GridMap gridMap;
    static TextView xAxisTextView, yAxisTextView, directionAxisTextView;

    // Control Button
    ImageButton forwardBtn, backRightBtn, backBtn, backLeftBtn, forwardRightBtn, forwardLeftBtn, timerButton;
    private static long exploreTimer, fastestTimer;
    ToggleButton imgRecButton, fastestButton;

    TextView exploreTimeTextView, fastestTimeTextView, robotStatusTextView;
    Switch phoneTiltSwitch;
    static Button fullcalibrateButton,calibrateButton;

    // for RPI
    ImageButton sendToRPIBtn;

    TextView btStatus;

    private ListView mObstacleList;
    private static ArrayAdapter<String> mObstacleListAdapter;

    private Sensor mSensor;
    private SensorManager mSensorManager;

    // Timer
    static Handler timerHandler = new Handler();

    AutoCompleteTextView dropdownlist;

    // new floating action button
    FloatingActionButton mExpandFAB, mStartingPiontFAB, mRestMapFAB, mSendToRPIFAB, mStartImageRecFAB, mStartFastestFAB;
    // to check whether sub FABs are visible or not
    Boolean isAllFabsVisible = false;
    static Boolean isSetStartingPiont = false;
    TextView txtStartingPoint;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ArenaFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ArenaFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ArenaFragment newInstance(String param1, String param2) {
        ArenaFragment fragment = new ArenaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_arena, container, false);

        // Map
        gridMap = new GridMap(getContext());
        gridMap = view.findViewById(R.id.MapView);

        xAxisTextView =  view.findViewById(R.id.xValue);
        yAxisTextView =  view.findViewById(R.id.yValue);
        directionAxisTextView =  view.findViewById(R.id.directionValue);
        robotStatusTextView = view.findViewById(R.id.robotStatusText);

        fobsstring = getArguments().getString("Obstacle");
        fexpstring = getArguments().getString("Explored");

        resetMapBtn = view.findViewById(R.id.resetButton);
        setStartPointToggleBtn = view.findViewById(R.id.startingPointButton);
        setObstacleToggleBtn = view.findViewById(R.id.setObstacleToggleBtn);
        setObstacleDirectionToggleBtn = view.findViewById(R.id.setObstacleDirectionToggleBtn);

        // for RPI
        sendToRPIBtn = view.findViewById(R.id.sendToRPIBtn);

        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Resetting map...");
                gridMap.resetMap();
                mObstacleListAdapter.clear();
                mObstacleList.setAdapter(mObstacleListAdapter);

                // Writing data to SharedPreferences
                SharedPreferences settings = getActivity().getSharedPreferences("Shared Preferences", MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.remove("imagestored").commit();
                editor.clear().commit();
                String imagestringstored = settings.getString("imagestored", "");
                System.out.println("removing of string: " + imagestringstored);

            }
        });

        setStartPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setStartPointToggleBtn");
                if (!setStartPointToggleBtn.isChecked())
                {
                    gridMap.setStartCoordStatus(false);
                    showToast("Cancelled selecting starting point");
                }
                else if (setStartPointToggleBtn.isChecked() && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setStartPointToggleBtn");
            }
        });

        setObstacleToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked obstacleToggleBtn");
                if (setObstacleToggleBtn.getText().equals("Set Obstacle")) {
                    showToast("Cancelled selecting obstacle");
                    gridMap.setSetObstacleStatus(false);
                }
                else if (setObstacleToggleBtn.getText().equals("Cancel")) {
                    showToast("Please plot obstacles");
                    gridMap.setSetObstacleStatus(true);
                }
            }
        });

        setObstacleDirectionToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked Obstacle Direction ToggleBtn");
                if (setObstacleDirectionToggleBtn.getText().equals("Set Obs Direction")) {
                    showToast("Cancelled selecting obstacle");
                    gridMap.setSetObstacleDirection(false);
                }
                else if (setObstacleDirectionToggleBtn.getText().equals("Cancel")) {
                    showToast("Please change obstacles direction");
                    gridMap.setSetObstacleDirection(true);
                }
            }
        });

        //For RPI
        sendToRPIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Exiting Image Recognition ToggleBtn");
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                String[][] mapArray = new String[20][20];
                OutputStream out = null;
                for (int i = 0; i < 20; i++) {
                    for (int j = 0; j < 20; j++) {
                        mapArray[i][j] = "O";
                    }
                }

                for(int[] coord : gridMap.getObstacleCoord()){
                    if(gridMap.returnObstacleFacing(coord[0],coord[1])=="UP") {
                        mapArray[coord[1]-1][coord[0]-1] = "N";
                    } else if(gridMap.returnObstacleFacing(coord[0],coord[1]).contains("DOWN")) {
                        mapArray[coord[1]-1][coord[0]-1] = "S";
                    } else if(gridMap.returnObstacleFacing(coord[0],coord[1]).contains("RIGHT")) {
                        mapArray[coord[1]-1][coord[0]-1] = "E";
                    } else if(gridMap.returnObstacleFacing(coord[0],coord[1]).contains("LEFT")) {
                        mapArray[coord[1]-1][coord[0]-1] = "W";
                    } else mapArray[coord[1]-1][coord[0]-1] = "X";

                    Log.d(TAG, "successfully updated 2d array");
                }

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Gson gson = new Gson();
                        JsonObject jsonObj = new JsonObject();
                        JsonElement jsonMapArray = gson.toJsonTree(mapArray);
                        jsonObj.add("arena", jsonMapArray);
                        String jsonStr = gson.toJson(jsonObj);

                        try {
                            URL url = new URL("http://192.168.3.13:3000/set_arena");
                            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                            Log.d(TAG, "HTTP connection created");
                            urlConnection.setRequestMethod("POST");
                            urlConnection.addRequestProperty("Accept", "application/json");
                            urlConnection.addRequestProperty("Content-Type", "application/json");
                            urlConnection.setDoOutput(true);
                            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream(), StandardCharsets.UTF_8);
                            out.write(jsonStr);
                            out.flush();
                            out.close();
                            urlConnection.connect();
                            Log.d(TAG, "POST REQUEST SENT");

                            int code = urlConnection.getResponseCode();
                            System.out.println(code);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            Log.d(TAG, "failed to connect w localhost");
                        }
                    }
                });
                showLog("Clicked Send to RPI Btn");
                showToast("Sending Map Array to RPI!");
            }

        });


        // variable initialization for controller
        forwardBtn = view.findViewById(R.id.forwardImageBtn);
        backRightBtn = view.findViewById(R.id.backRightBtn);
        backBtn = view.findViewById(R.id.backImageBtn);
        backLeftBtn = view.findViewById(R.id.backLeftBtn);
        forwardRightBtn = view.findViewById(R.id.forwardRightBtn);
        forwardLeftBtn = view.findViewById(R.id.forwardLeftBtn);

          imgRecButton = view.findViewById(R.id.imgRecToogleButton);
          timerButton = view.findViewById(R.id.timerButton);

        // Button Listener
        forwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveForwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    // TODO: uncommand for bluetooth and send command to RPI
                    BluetoothFragment.printMessage("S");
                    refreshLabel();
                    //"W" is used for communication with AMDTOOL
                    if (gridMap.getValidPosition())
                        updateStatus("moving forward");
                    else
                        updateStatus("Unable to move forward");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting moveForwardImageBtn");
            }
        });
        forwardRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked forwardRightBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right");
                    gridMap.moveRobot("forward");
                    gridMap.moveRobot("left");
                    BluetoothFragment.printMessage("E");
                    refreshLabel();
                    updateStatus("turning forward right");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting forwardRightBtn");
            }
        });
        forwardLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked forwardLeftBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left");
                    gridMap.moveRobot("forward");
                    gridMap.moveRobot("right");
                    BluetoothFragment.printMessage("Q");
                    refreshLabel();
                    updateStatus("turning forward left");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting forwardLeftBtn");
            }
        });

        backRightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked backRightBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    gridMap.moveRobot("left");
                    gridMap.moveRobot("back");
                    gridMap.moveRobot("back");
                    gridMap.moveRobot("back");
                    gridMap.moveRobot("back");
                    // TODO: uncommand for bluetooth and send command to RPI
                    BluetoothFragment.printMessage("D");
                    refreshLabel();
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting backRightBtn");
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveBackwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");
                    BluetoothFragment.printMessage("W");
                    refreshLabel();
                    if (gridMap.getValidPosition())
                        updateStatus("moving backward");
                    else
                        updateStatus("Unable to move backward");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting moveBackwardImageBtn");
            }
        });

        backLeftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnLeftImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    gridMap.moveRobot("right");
                    gridMap.moveRobot("back");
                    gridMap.moveRobot("back");
                    gridMap.moveRobot("back");
                    gridMap.moveRobot("back");
                    // TODO: uncommand for bluetooth and send command to RPI
                    BluetoothFragment.printMessage("A");
                    refreshLabel();
                    updateStatus("turning back left");

                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting backLeftBtn" +
                        "");
            }
        });

        imgRecButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                showLog("Sending START to RPI");
                BluetoothFragment.printMessage("START");
                updateStatus("Sending START to RPI");
            }
        });

        timerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                showLog("Clicked on wk 9 button");
                BluetoothFragment.printMessage("FP");
                updateStatus("Starting Fastest Path NOW");
            }
        });

        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mObstacleList = view.findViewById(R.id.obstacleList);
        mObstacleListAdapter = new ArrayAdapter<>(getContext(), R.layout.obstacleitem);
        mObstacleList.setAdapter(mObstacleListAdapter);
        return view;
    }

    public static void removeObstacleFromList(String message)
    {
        mObstacleListAdapter.remove(message);
    }
    // Note ArenaBTMessage change to printing obstacle coordinate
    public static void updateObstacleList(String message)
    {
        mObstacleListAdapter.add(message);
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateStatus(String message) {
        Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP,0, 0);
        toast.show();
    }

    public static void refreshLabel() {
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0]-1));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1]-1));
    }

    public void refreshDirection(String direction) {
        gridMap.setRobotDirection(direction);
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
    }

    // Timer
    Runnable timerRunnableExplore = new Runnable() {
        @Override
        public void run() {
            long millisExplore = System.currentTimeMillis() - exploreTimer;
            int secondsExplore = (int) (millisExplore / 1000);
            int minutesExplore = secondsExplore / 60;
            secondsExplore = secondsExplore % 60;
            exploreTimeTextView.setText(String.format("%02d:%02d", minutesExplore, secondsExplore));
            timerHandler.postDelayed(this, 500);
        }
    };

    Runnable timerRunnableFastest = new Runnable() {
        @Override
        public void run() {
            long millisFastest = System.currentTimeMillis() - fastestTimer;
            int secondsFastest = (int) (millisFastest / 1000);
            int minutesFastest = secondsFastest / 60;
            secondsFastest = secondsFastest % 60;
            fastestTimeTextView.setText(String.format("%02d:%02d", minutesFastest, secondsFastest));
            timerHandler.postDelayed(this, 500);
        }
    };

    Handler sensorHandler = new Handler();
    boolean sensorFlag= false;

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {
            sensorFlag = true;
            sensorHandler.postDelayed(this,1000);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        showLog("SensorChanged X: "+x);
        showLog("SensorChanged Y: "+y);
        showLog("SensorChanged Z: "+z);

        if(sensorFlag) {
            if (y < -2) {
                showLog("Sensor Move Forward Detected");
                gridMap.moveRobot("forward");
                refreshLabel();
            } else if (y > 2) {
                showLog("Sensor Move Backward Detected");
                gridMap.moveRobot("back");
                refreshLabel();
            } else if (x > 2) {
                showLog("Sensor Move Left Detected");
                gridMap.moveRobot("left");
                refreshLabel();
            } else if (x < -2) {
                showLog("Sensor Move Right Detected");
                gridMap.moveRobot("right");
                refreshLabel();
            }
        }
        sensorFlag = false;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }


    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            showLog("receivedMessage: message --- " + message);

            if (message.equals("W")){
                robotStatusTextView.setText("Robot Moving Forward");
            }
            else if (message.equals("S")){
                robotStatusTextView.setText("Robot Reversing");
            }
            else if (message.equals("A")){
                robotStatusTextView.setText("Robot Turning Left");
            }
            else if (message.equals("D")){
                robotStatusTextView.setText("Robot Turning Right");
            }
            else if (message.equals("RR")){
                robotStatusTextView.setText("Robot Ready To Start");
            }
            else if (message.equals("LT")){
                robotStatusTextView.setText("Robot Looking For Target");
            }

            // Try getting update image
            // First Case
            if(message.contains("TARGET")){ // example String: “TARGET,<x>,<y>,<Target ID>”
                int startingIndex = message.indexOf("<");
                int endingIndex = message.indexOf(">");
                int xCoord = Integer.parseInt(message.substring(startingIndex + 1, endingIndex))+1;

                startingIndex = message.indexOf("<", endingIndex+1);
                endingIndex = message.indexOf(">", endingIndex+1);
                int yCoord = Integer.parseInt(message.substring(startingIndex+1, endingIndex))+1;
                startingIndex = message.indexOf("<", endingIndex+1);
                endingIndex = message.indexOf(">", endingIndex+1);
                String targetID = message.substring(startingIndex+1, endingIndex);
                String obstacleNo = String.valueOf(gridMap.returnObstacleId(xCoord, yCoord));
                if(Integer.parseInt(obstacleNo) == -100)
                {
                    Toast.makeText(getContext(), "Obstacle doesn't exist.", Toast.LENGTH_SHORT).show();
                }
                // to count the number of <
                char check = '<';
                int count = 0;

                for (int i = 0; i < message.length(); i++) {
                    if (message.charAt(i) == check) {
                        count++;
                    }
                }

                // if count is equal to 3 == second case
                    Toast.makeText(getContext(), "Obstacle No " + obstacleNo + " detected as " + targetID, Toast.LENGTH_SHORT).show();
                    // TODO: need update
                    gridMap.updateImageNumberCell(Integer.parseInt(obstacleNo), targetID);
            }

            if(message.contains("ROBOT")){
                int startingIndex = message.indexOf("<");
                int endingIndex = message.indexOf(">");
                String xCoord = message.substring(startingIndex + 1, endingIndex);

                startingIndex = message.indexOf("<", endingIndex+1);
                endingIndex = message.indexOf(">", endingIndex+1);
                String yCoord = message.substring(startingIndex+1, endingIndex);

                startingIndex = message.indexOf("<", endingIndex+1);
                endingIndex = message.indexOf(">", endingIndex+1);
                String direction = message.substring(startingIndex+1, endingIndex);

                // set directions from N S E W to up down left right
                if(direction.equals("1")){
                    direction="up";
                } else if(direction.equals("3")){
                    direction="down";
                } else if(direction.equals("0")){
                    direction="right";
                } else if(direction.equals("2")) {
                    direction = "left";
                }
                else{
                    direction="up";
                }


                //validate the robot input to prevent out of arena zone
                if(Integer.parseInt(xCoord) > 0 && Integer.parseInt(xCoord) < 19 && Integer.parseInt(yCoord) > 0 && Integer.parseInt(yCoord) < 19) {

                    // remove current robot
                    // get current coordinate
                    int[] curCoord = gridMap.getCurCoord(); // robot current coordinate this.setOldRobotCoord(curCoord[0], curCoord[1]);

                    // conditions
                    if (curCoord[0] != -1 && curCoord[1] != -1) {
                        // set old coordinate to type unexplored
                        // TODO: need update
                        gridMap.unsetOldRobotCoord(curCoord[0], curCoord[1]);
                        // set new robot direction
                        // TODO: need update
                        gridMap.setCurCoord(Integer.parseInt(xCoord) + 1, Integer.parseInt(yCoord) + 1, direction);
                    } else {
                        // Show Error Message or Alternatively allow draw robot w/o selecting robot start direction
                        // ToDo: show error message or allows putting the robot w/o setting start point
                        Toast.makeText(getContext(), "Please set start point of the robot first", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    Toast.makeText(getContext(), "Robot is out of arena zone. x: " + xCoord + " y: " + yCoord, Toast.LENGTH_SHORT).show();
                }
            }

            sharedPreferences();
            String receivedText = BluetoothFragment.getBTdeviceName() + ": " + message;
            editor.putString("message", receivedText);
            editor.commit();
            BluetoothFragment.refreshMessageReceived();
        }
    };

    public void sharedPreferences() {
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
}