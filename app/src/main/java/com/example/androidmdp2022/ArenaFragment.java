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
    ImageButton forwardBtn, backRightBtn, backBtn, backLeftBtn, forwardRightBtn, forwardLeftBtn;
    //exploreResetButton, fastestResetButton;
    private static long exploreTimer, fastestTimer;
    ToggleButton imgRecButton, fastestButton;
    TextView exploreTimeTextView, fastestTimeTextView, robotStatusTextView;
    Switch phoneTiltSwitch;
    static Button fullcalibrateButton,calibrateButton;

    // for RPI
    Button sendToRPIBtn;

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
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_arena, container, false);

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
        //directionChangeImageBtn = view.findViewById(R.id.directionChangeImageBtn);
        //exploredImageBtn = view.findViewById(R.id.exploredImageBtn);
        //obstacleImageBtn = view.findViewById(R.id.obstacleImageBtn);
        //clearImageBtn = view.findViewById(R.id.clearImageBtn);
        //updateButton = view.findViewById(R.id.updateButton);
        setObstacleToggleBtn = view.findViewById(R.id.setObstacleToggleBtn);
        setObstacleDirectionToggleBtn = view.findViewById(R.id.setObstacleDirectionToggleBtn);

        // for RPI
        sendToRPIBtn = view.findViewById(R.id.sendToRPIBtn);

        //dropdownlist = view.findViewById(R.id.AC_dropdownlistDirection);
        //TODO Bluetooth comment
        //btStatus = view.findViewById(R.id.BTstatus);

        //TODO arrayAdapter
//        ArrayAdapter arrayAdapter = new ArrayAdapter(getContext(), R.layout.option_menu, directionOption);
//        dropdownlist.setText(arrayAdapter.getItem(0).toString(), false);
//        gridMap.setNewDirection(0);
//        dropdownlist.setAdapter(arrayAdapter);

//        dropdownlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                gridMap.setNewDirection(i);
//            }
//        });


        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Resetting map...");
                gridMap.resetMap();
                mObstacleListAdapter.clear();
                mObstacleList.setAdapter(mObstacleListAdapter);
                // TODO: uncommand for bluetooth and send command to RPI
                //BluetoothFragment.printMessage("RS");

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

//        setWaypointToggleBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showLog("Clicked setWaypointToggleBtn");
//                if (setWaypointToggleBtn.getText().equals("SET WAYPOINT"))
//                    showToast("Cancelled selecting waypoint");
//                else if (setWaypointToggleBtn.getText().equals("CANCEL")) {
//                    showToast("Please select waypoint");
//                    gridMap.setWaypointStatus(true);
//                    //gridMap.toggleCheckedBtn("setWaypointToggleBtn");
//                }
//                else
//                    showToast("Please select manual mode");
//                showLog("Exiting setWaypointToggleBtn");
//            }
//        });

//        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showLog("Clicked obstacleImageBtn");
//                if (!gridMap.getSetObstacleStatus()) {
//                    showToast("Please plot obstacles");
//                    gridMap.setSetObstacleStatus(true);
//                    //gridMap.toggleCheckedBtn("obstacleImageBtn");
//                }
//                else if (gridMap.getSetObstacleStatus())
//                    gridMap.setSetObstacleStatus(false);
//                showLog("Exiting obstacleImageBtn");
//            }
//        });

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
                    //gridMap.toggleCheckedBtn("setWaypointToggleBtn");
                }
            }
        });

        setObstacleDirectionToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked Obstacle Direction ToggleBtn");
                if (setObstacleDirectionToggleBtn.getText().equals("Set Obstacle Direction")) {
                    showToast("Cancelled selecting obstacle");
                    gridMap.setSetObstacleDirection(false);
                }
                else if (setObstacleDirectionToggleBtn.getText().equals("Cancel")) {
                    showToast("Please change obstacles direction");
                    gridMap.setSetObstacleDirection(true);
                    //gridMap.toggleCheckedBtn("setWaypointToggleBtn");
                }
            }
        });

        //For RPI
        sendToRPIBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Sending to RPI");
                gridMap.sendRPIMessage();
            }

        });


        // variable initialization for controller
        forwardBtn = view.findViewById(R.id.forwardImageBtn);
        backRightBtn = view.findViewById(R.id.backRightBtn);
        backBtn = view.findViewById(R.id.backImageBtn);
        backLeftBtn = view.findViewById(R.id.backLeftBtn);
        forwardRightBtn = view.findViewById(R.id.forwardRightBtn);
        forwardLeftBtn = view.findViewById(R.id.forwardLeftBtn);

//        exploreTimeTextView = view.findViewById(R.id.exploreTimeTextView);
//        fastestTimeTextView = view.findViewById(R.id.fastestTimeTextView);
          imgRecButton = view.findViewById(R.id.imgRecToogleButton);
//        fastestButton = view.findViewById(R.id.fastestToggleBtn);
//        exploreResetButton = view.findViewById(R.id.exploreResetImageBtn);
//        fastestResetButton = view.findViewById(R.id.fastestResetImageBtn);
        //phoneTiltSwitch = view.findViewById(R.id.phoneTiltSwitch);
        //fullcalibrateButton = view.findViewById(R.id.fullcalibrateButton);
        //calibrateButton = view.findViewById(R.id.calibrateButton);



        // Button Listener
        forwardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveForwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");
                    // TODO: uncommand for bluetooth and send command to RPI
                    BluetoothFragment.printMessage("W");
                    refreshLabel();
                    //"W" is used for communication with AMDTOOL
//                    MainActivity.printMessage("W");
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
                    //gridMap.moveRobot("forward");
                    gridMap.moveRobot("right");
                    gridMap.moveRobot("forward");
                    gridMap.moveRobot("left");
//                    gridMap.moveRobot("forward");
//                    gridMap.moveRobot("forward");
                    // TODO: uncommand for bluetooth and send command to RPI
                    BluetoothFragment.printMessage("E");
                    refreshLabel();
                    updateStatus("turning forward right");
                    //"A" is used for communication with AMDTOOL
//                    MainActivity.printMessage("A");

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
                    //gridMap.moveRobot("forward");
                    gridMap.moveRobot("left");
                    gridMap.moveRobot("forward");
                    gridMap.moveRobot("right");
//                    gridMap.moveRobot("forward");
//                    gridMap.moveRobot("forward");
//                    gridMap.moveRobot("forward");
                    // TODO: uncommand for bluetooth and send command to RPI
                    BluetoothFragment.printMessage("Q");
                    refreshLabel();
                    updateStatus("turning forward left");
                    //"A" is used for communication with AMDTOOL
//                    MainActivity.printMessage("A");

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
                    //BluetoothFragment.printMessage("cmd:right");
                    BluetoothFragment.printMessage("D");

                    refreshLabel();
                    //"D" is used for communication with AMDTOOL
//                    MainActivity.printMessage("D");
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
                    gridMap.moveRobot("back");
                    //"S" is used for communication with AMDTOOL
                    // MainActivity.printMessage("S");
                    // TODO: uncommand for bluetooth and send command to RPI
                    BluetoothFragment.printMessage("S");
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
                    //"A" is used for communication with AMDTOOL
//                    MainActivity.printMessage("A");
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
                String[][] mapArray = new String[20][20];
                OutputStream out = null;
                for (int i = 0; i < 20; i++) {
                    for (int j = 0; j < 20; j++) {
                        mapArray[i][j] = "O";
                    }
                }

                for(int[] coord : gridMap.getObstacleCoord()){
                    //System.out.println(String.format("PRINT COORDS: [%d][%d]",coord[0],coord[1]));
                    if(gridMap.returnObstacleFacing(coord[0],coord[1])=="UP") {
                        mapArray[coord[1]-1][coord[0]-1] = "N";
                    } else if(gridMap.returnObstacleFacing(coord[0],coord[1]).contains("DOWN")) {
                        mapArray[coord[1]-1][coord[0]-1] = "S";
                    } else if(gridMap.returnObstacleFacing(coord[0],coord[1]).contains("RIGHT")) {
                        mapArray[coord[1]-1][coord[0]-1] = "E";
                    } else if(gridMap.returnObstacleFacing(coord[0],coord[1]).contains("LEFT")) {
                        mapArray[coord[1]-1][coord[0]-1] = "W";
                    } else mapArray[coord[1]-1][coord[0]-1] = "X";

                    //System.out.println("mapArray[0][1]" + mapArray[coord[0]-1][coord[1]-1]);
                    //System.out.println(String.format("LOGIC mapArray[%d][%d] %b",coord[0],20-coord[1],mapArray[coord[0]][20-coord[1]]));
                    //System.out.println("PRINT ARRAY" + mapArray);
                    //System.out.println(String.format("CHANGED mapArray[%d][%d] %b",coord[0],coord[1],mapArray[coord[0]][coord[1]]));
                    Log.d(TAG, "successfully updated 2d array");
                }
//                for(int i = 0; i<mapArray.length; i++){
//                    for(int j = 0; j<mapArray.length; j++){
//                        System.out.println(i + ", " + j + " " + mapArray[i][j] + ",");
//                    }
//                    System.out.println("\n");
//                }

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
//                            DataOutputStream writer = new DataOutputStream(urlConnection.getOutputStream ());
//                            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(writer, StandardCharsets.UTF_8));
//                            bufferedWriter.write(arrayObj.toString());
                            Log.d(TAG, "POST REQUEST SENT");

//                           InputStream inputStream = urlConnection.getInputStream();

//                            writer.flush ();
//                            writer.close ();
                            int code = urlConnection.getResponseCode();
                            System.out.println(code);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            Log.d(TAG, "failed to connect w localhost");
                        }
                    }
                });
                showLog("Clicked Image Recognition ToggleBtn");
                BluetoothFragment.printMessage("START");
//                ToggleButton imgRecToggleBtn = (ToggleButton) v;
//                if (imgRecToggleBtn.getText().equals("IMAGE RECOGNITION")) {
//                    showToast("Image Recognition timer stop!");
//                    robotStatusTextView.setText("Image Recognition Stopped");
//                    timerHandler.removeCallbacks(timerRunnableExplore);
//                }
//                else if (imgRecToggleBtn.getText().equals("STOP")) {
//                    showToast("Image Recognition timer start!");
//                    // TODO: uncommand for bluetooth and send command to RPI
//                    //BluetoothFragment.printMessage("IR");
//                    robotStatusTextView.setText("Image Recognition Started");
//                    exploreTimer = System.currentTimeMillis();
//                    timerHandler.postDelayed(timerRunnableExplore, 0);
//                }
//                else {
//                    showToast("Else statement: " + imgRecToggleBtn.getText());
//                }
                showLog("Exiting Image Recognition ToggleBtn");
            }
        });

//        exploreResetButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showLog("Clicked exploreResetImageBtn");
//                showToast("Reseting image recognition  time...");
//                exploreTimeTextView.setText("00:00");
//                robotStatusTextView.setText("Not Available");
//                if(imgRecButton.isChecked())
//                    imgRecButton.toggle();
//                timerHandler.removeCallbacks(timerRunnableExplore);
//                showLog("Exiting exploreResetImageBtn");            }
//        });

//        fastestButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showLog("Clicked fastestToggleBtn");
//                ToggleButton fastestToggleBtn = (ToggleButton) v;
//                if (fastestToggleBtn.getText().equals("FASTEST")) {
//                    showToast("Fastest timer stop!");
//                    robotStatusTextView.setText("Fastest Path Stopped");
//                    timerHandler.removeCallbacks(timerRunnableFastest);
//                }
//                else if (fastestToggleBtn.getText().equals("STOP")) {
//                    showToast("Fastest timer start!");
//                    // TODO: uncommand for bluetooth and send command to RPI
//                    //BluetoothFragment.printMessage("FS");
//                    robotStatusTextView.setText("Fastest Path Started");
//                    fastestTimer = System.currentTimeMillis();
//                    timerHandler.postDelayed(timerRunnableFastest, 0);
//                }
//                else
//                    showToast(fastestToggleBtn.getText().toString());
//                showLog("Exiting fastestToggleBtn");            }
//        });
//
//        fastestResetButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showLog("Clicked fastestResetImageBtn");
//                showToast("Reseting fastest time...");
//                fastestTimeTextView.setText("00:00");
//                robotStatusTextView.setText("Not Available");
//                if (fastestButton.isChecked())
//                    fastestButton.toggle();
//                timerHandler.removeCallbacks(timerRunnableFastest);
//                showLog("Exiting fastestResetImageBtn");            }
//        });

        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

//        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showLog("Clicked manualAutoToggleBtn");
//                if (manualAutoToggleBtn.getText().equals("MANUAL")) {
//                    try {
//                        gridMap.setAutoUpdate(true);
//                        autoUpdate = true;
//                        gridMap.toggleCheckedBtn("None");
//                        //updateButton.setClickable(false);
//                        //updateButton.setTextColor(Color.GRAY);
//                        //ControlFragment.getCalibrateButton().setClickable(false);
//                        //ControlFragment.getCalibrateButton().setTextColor(Color.GRAY);
//                        manualAutoToggleBtn.setText("AUTO");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    showToast("AUTO mode");
//                }
//                else if (manualAutoToggleBtn.getText().equals("AUTO")) {
//                    try {
//                        gridMap.setAutoUpdate(false);
//                        autoUpdate = false;
//                        gridMap.toggleCheckedBtn("None");
//                        //updateButton.setClickable(true);
//                        //updateButton.setTextColor(Color.BLACK);
//                        //ControlFragment.getCalibrateButton().setClickable(true);
//                        //ControlFragment.getCalibrateButton().setTextColor(Color.BLACK);
//                        manualAutoToggleBtn.setText("MANUAL");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    showToast("MANUAL mode");
//                }
//                showLog("Exiting manualAutoToggleBtn");
//            }
//        });

//        phoneTiltSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//                if (gridMap.getAutoUpdate()) {
//                    updateStatus("Please press 'MANUAL'");
//                    phoneTiltSwitch.setChecked(false);
//                }
//                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
//                    if(phoneTiltSwitch.isChecked()){
//                        showToast("Tilt motion control: ON");
//                        phoneTiltSwitch.setPressed(true);
//
//                        mSensorManager.registerListener(ArenaFragment.this, mSensor, mSensorManager.SENSOR_DELAY_NORMAL);
//                        sensorHandler.post(sensorDelay);
//                    }else{
//                        showToast("Tilt motion control: OFF");
//                        showLog("unregistering Sensor Listener");
//                        try {
//                            mSensorManager.unregisterListener(ArenaFragment.this);
//                        }catch(IllegalArgumentException e) {
//                            e.printStackTrace();
//                        }
//                        sensorHandler.removeCallbacks(sensorDelay);
//                    }
//                } else {
//                    updateStatus("Please press 'STARTING POINT'");
//                    phoneTiltSwitch.setChecked(false);
//                }
//                if(phoneTiltSwitch.isChecked()){
//                    compoundButton.setText("TILT ON");
//                }else
//                {
//                    compoundButton.setText("TILT OFF");
//                }
//            }
//        });
        //TODO Bluetooth
        mObstacleList = view.findViewById(R.id.obstacleList);
        mObstacleListAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        mObstacleList.setAdapter(mObstacleListAdapter);


//        mStartingPiontFAB.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showLog("Clicked setStartPoint");
//                if(isSetStartingPiont == false)
//                {
//                    isSetStartingPiont = true;
//                    gridMap.setStartCoordStatus(isSetStartingPiont);
//                    mStartingPiontFAB.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.xmark));
//                    showToast("Please select starting point");
//                }
//                else
//                {
//                    isSetStartingPiont = false;
//                    gridMap.setStartCoordStatus(isSetStartingPiont);
//                    mStartingPiontFAB.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.start_32));
//                    showToast("Cancelled selecting starting point");
//                }
//                showLog("Exiting setStartPoint");
//            }
//        });


        //txtStartingPoint = view.findViewById(R.id.txtStartingPoint);


//        mRestMapFAB.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showLog("Clicked resetMapBtn");
//                showToast("Reseting map...");
//                gridMap.resetMap();
//                // TODO: uncommand for bluetooth and send command to RPI
//                //BluetoothFragment.printMessage("RS");
//
//                // Writing data to SharedPreferences
//                SharedPreferences settings = getActivity().getSharedPreferences("Shared Preferences", MODE_PRIVATE);
//                SharedPreferences.Editor editor = settings.edit();
//                editor.remove("imagestored").commit();
//                editor.clear().commit();
//                String imagestringstored = settings.getString("imagestored", "");
//                System.out.println("removing of string: " + imagestringstored);
//
//            }
//        });

//        mSendToRPIFAB.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showToast("Sending to RPI");
//                gridMap.sendRPIMessage();
//            }
//        });
//
//        mStartImageRecFAB.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //TODO Bluetooth
//                //BluetoothFragment.printMessage("IR");
//                robotStatusTextView.setText("Image Recognition Started");
//                showToast("Start Image Recognition");
//            }
//        });
//
//        mStartFastestFAB.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //TODO Bluetooth
//                //BluetoothFragment.printMessage("FS");
//                robotStatusTextView.setText("Fastest Path Started");
//                showToast("Start Fastest Challenge");
//            }
//        });


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
        //directionAxisTextView.setText(sharedPreferences.getString("direction",""));
    }

    public void refreshDirection(String direction) {
        gridMap.setRobotDirection(direction);
        directionAxisTextView.setText(sharedPreferences.getString("direction",""));
        //TODO Bluetooth
        //BluetoothFragment.printMessage("Direction is set to " + direction);
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
                //BluetoothFragment.printMessage("W1|");
                //BluetoothFragment.printMessage("f");
            } else if (y > 2) {
                showLog("Sensor Move Backward Detected");
                gridMap.moveRobot("back");
                refreshLabel();
                //BluetoothFragment.printMessage("S1|");
                //BluetoothFragment.printMessage("r");
            } else if (x > 2) {
                showLog("Sensor Move Left Detected");
                gridMap.moveRobot("left");
                refreshLabel();
                //BluetoothFragment.printMessage("A|");
                //BluetoothFragment.printMessage("tl");
            } else if (x < -2) {
                showLog("Sensor Move Right Detected");
                gridMap.moveRobot("right");
                refreshLabel();
                //BluetoothFragment.printMessage("D|");
                //BluetoothFragment.printMessage("tr");
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

            //TODO: update robot status when receiving
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
            // TODO: need change the receiving using x,y instead of obstacle number
            // For RPI
//            if(message.contains("TARGET")) // example String: “TARGET, <x>, <y>, <Traget ID>”
//            {
//                int startingIndex = message.indexOf("<");
//                int endingIndex = message.indexOf(">");
//                int x = Integer.parseInt(message.substring(startingIndex + 1, endingIndex));
//
//                startingIndex = message.indexOf("<", endingIndex+1);
//                endingIndex = message.indexOf(">", endingIndex+1);
//                int y = Integer.parseInt(message.substring(startingIndex+1, endingIndex));
//
//                startingIndex = message.indexOf("<", endingIndex+1);
//                endingIndex = message.indexOf(">", endingIndex+1);
//                String targetID = message.substring(startingIndex+1, endingIndex);
//
//                // to count the number of <
//                char check = '<';
//                int count = 0;
//
//                for (int i = 0; i < message.length(); i++) {
//                    if (message.charAt(i) == check) {
//                        count++;
//                    }
//                }
//
//                // if count is equal to 4 == second case
//                if(count == 4){ // additional <Direction Facing change>
//                    startingIndex = message.indexOf("<", endingIndex+1);
//                    endingIndex = message.indexOf(">", endingIndex+1);
//                    String obstacleFacing = message.substring(startingIndex+1, endingIndex);
//                    Toast.makeText(getContext(), "x: " + x + " y: " + y + " ImageID: " + targetID + " on direction " + obstacleFacing, Toast.LENGTH_SHORT).show();
//                    // TODO: need update
//                    gridMap.updateImageNumberCellRPI(x, y , targetID, obstacleFacing);
//                    // if count is not equal 3 == first case
//                } else {
//                    Toast.makeText(getContext(), "x: " + x + " y: " + y + " ImageID: " + targetID, Toast.LENGTH_SHORT).show();
//                    // TODO: need update
//                    gridMap.updateImageNumberCellRPI(x, y , targetID);
//                }
//
//            }

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
//                if(count == 3){ // additional <Direction Facing change>
//                    Toast.makeText(getContext(), "Obstacle No " + obstacleNo + " detected as " + targetID + " on direction " + obstacleFacing, Toast.LENGTH_SHORT).show();
//                    // TODO: need update
//                    gridMap.updateImageNumberCell(Integer.parseInt(obstacleNo), targetID, obstacleFacing);
//                    // if count is not equal 3 == first case
//                } else {
                    Toast.makeText(getContext(), "Obstacle No " + obstacleNo + " detected as " + targetID, Toast.LENGTH_SHORT).show();
                    // TODO: need update
                    gridMap.updateImageNumberCell(Integer.parseInt(obstacleNo), targetID);
                //}
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
                String direction = message.substring(startingIndex+1);

                // set directions from N S E W to up down left right
                if(direction.equals("N")){
                    direction="up";
                } else if(direction.equals("S")){
                    direction="down";
                } else if(direction.equals("E")){
                    direction="right";
                } else if(direction.equals("W")) {
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

//            try {
//                if (message.length() > 7 && message.startsWith("grid", 2)) {
//                    String resultString = "";
//                    String amdString = message.substring(11,message.length()-2);
//                    showLog("amdString: " + amdString);
//                    BigInteger hexBigIntegerExplored = new BigInteger(amdString, 16);
//                    String exploredString = hexBigIntegerExplored.toString(2);
//
//                    while (exploredString.length() < 400)
//                        exploredString = "0" + exploredString;
//
//                    for (int i=0; i<exploredString.length(); i=i+20) {
//                        int j=0;
//                        String subString = "";
//                        while (j<20) {
//                            subString = subString + exploredString.charAt(j+i);
//                            j++;
//                        }
//                        resultString = subString + resultString;
//                    }
//                    hexBigIntegerExplored = new BigInteger(resultString, 2);
//                    resultString = hexBigIntegerExplored.toString(16);
//
//                    JSONObject amdObject = new JSONObject();
//                    amdObject.put("explored", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
//                    amdObject.put("length", amdString.length()*4);
//                    amdObject.put("obstacle", resultString);
//                    JSONArray amdArray = new JSONArray();
//                    amdArray.put(amdObject);
//                    JSONObject amdMessage = new JSONObject();
//                    amdMessage.put("map", amdArray);
//                    message = String.valueOf(amdMessage);
//                    showLog("Executed for AMD message, message: " + message);
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            try {
//                if (message.length() > 8 && message.startsWith("image", 2)) {
//                    JSONObject jsonObject = new JSONObject(message);
//                    JSONArray jsonArray = jsonObject.getJSONArray("image");
//                    gridMap.drawImageNumberCell(jsonArray.getInt(0),jsonArray.getInt(1),jsonArray.getInt(2));
//                    showLog("Image Added for index: " + jsonArray.getInt(0) + "," +jsonArray.getInt(1));
//                }
//            } catch (JSONException e) {
//                showLog("Adding Image Failed");
//            }

            // TODO: need update Not sure if needed or not
//            if (gridMap.getAutoUpdate() || MapFragment.manualUpdateRequest) {
//                try {
//                    gridMap.setReceivedJsonObject(new JSONObject(message));
//                    gridMap.updateMapInformation();
//                    MapFragment.manualUpdateRequest = false;
//                    showLog("messageReceiver: try decode successful");
//                } catch (JSONException e) {
//                    showLog("messageReceiver: try decode unsuccessful");
//                }
//            }
            sharedPreferences();
            //String receivedText = sharedPreferences.getString("message", "") + "\n" + message;

            String receivedText = BluetoothFragment.getBTdeviceName() + ": " + message;
            editor.putString("message", receivedText);
            editor.commit();
            BluetoothFragment.refreshMessageReceived();
            //BluetoothMessagesListAdapter.add(receivedText);
        }
    };

    public void sharedPreferences() {
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

}