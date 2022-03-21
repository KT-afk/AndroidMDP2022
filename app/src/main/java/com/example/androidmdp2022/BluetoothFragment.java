package com.example.androidmdp2022;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class BluetoothFragment extends Fragment {
    //for debugging
    private static final String MY_TAG = " bluetooth_fragment";

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private Set<BluetoothDevice> availableDevices;

    private static final UUID theUUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private static BluetoothDevice btDevice;

    private String connectedDeviceName;
    private static ArrayAdapter<String> btMessagesListAdapter;
    private final BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(MY_TAG, "broadcastReceiver1: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(MY_TAG, "broadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(MY_TAG, "broadcastReceiver1: STATE ON");

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(MY_TAG, "broadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };
    private final BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(MY_TAG, "broadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(MY_TAG, "broadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(MY_TAG, "broadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(MY_TAG, "broadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(MY_TAG, "broadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };
    // To discover bluetooth devices
    private final BroadcastReceiver broadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(MY_TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if (deviceName == null) {
                    Log.d(MY_TAG, "the device address: " + device.getAddress());
                    availableDevices.add(device);
                    availableDevicesListAdapter.add(device.getAddress());
                } else {
                    availableDevices.add(device);
                    availableDevicesListAdapter.add(device.getName());
                    Log.d(MY_TAG, "the device name: " + device.getName());
                }
                availableDevicesListAdapter.notifyDataSetChanged();
                Log.d(MY_TAG, "onReceive: " + device.getName() + " : " + device.getAddress());
            }
        }
    };
    // for bluetooth pairing
    private final BroadcastReceiver broadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(MY_TAG, "BOND_BONDED.");
                    Toast.makeText(getContext(), "Successfully paired with " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    BluetoothFragment.btDevice = mDevice;
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(MY_TAG, "BOND_BONDING.");
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(MY_TAG, "BOND_NONE.");
                }
            }
        }
    };
    private Button scanBTbutton;
    private ArrayAdapter<String> pairedDevicesListAdapter;
    private ArrayAdapter<String> availableDevicesListAdapter;
    private ListView pairedDevicesListView;
    private ListView availableDevicesListView;
    //NEW
    BluetoothService bluetoothConnection;
    private Button disconnectBTButton;
    private EditText sendMessageField;
    private ImageButton sendMessageButton;
    Runnable reconnectionRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!BluetoothService.btConnectStatus) {
                    startBTConnect(btDevice, theUUID);
                    Toast.makeText(getContext(), "Reconnection Success", Toast.LENGTH_SHORT).show();
                }
                reconnectionHandler.removeCallbacks(reconnectionRunnable);
                retryConnection = false;
            } catch (Exception e) {
                Toast.makeText(getContext(), "Failed to reconnect, trying in 5 second", Toast.LENGTH_SHORT).show();
            }
        }
    };
    private Switch switchBT;

    // Declaration Variables
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    ProgressDialog myDialog;

    boolean isDisconnectBtn = false;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types and number of parameters
    public static BluetoothFragment newInstance(String param1, String param2) {
        BluetoothFragment fragment = new BluetoothFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private TextView tvConStatus;

    boolean retryConnection = false;
    Handler reconnectionHandler = new Handler();
    private TextView tvDeviceName;
    // Unconnected devices
    private ConstraintLayout unconnectedLayout;
    // connected devices
    private ConstraintLayout connectedLayout;
    // for auto reconnecting bluetooth
    private final BroadcastReceiver broadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if (status.equals("connected")) {
                try {
                    myDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                Log.d(MY_TAG, "broadcastReceiver5: Device now connected to " + mDevice.getName());
                Toast.makeText(getContext(), "Device now connected to " + mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("conStatus", "Connected to " + mDevice.getName());
                updateConnectedStatus(mDevice.getName());
            } else if (status.equals("disconnected") && !retryConnection) {
                Log.d(MY_TAG, "broadcastReceiver5: Disconnected from " + mDevice.getName());
                Toast.makeText(getContext(), "Disconnected from " + mDevice.getName(), Toast.LENGTH_LONG).show();
                bluetoothConnection = new BluetoothService(getContext());
                sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("conStatus", "Disconnected");
                updateDisconnectStatus();
                editor.commit();

                if (!isDisconnectBtn) {
                    try {

                        myDialog.show();
                    } catch (Exception e) {
                        Log.d(MY_TAG, "BluetoothPopUp: broadcastReceiver5 Dialog show failure");
                    }

                    retryConnection = true;
                    reconnectionHandler.postDelayed(reconnectionRunnable, 5000);
                }
            }
            editor.commit();
        }
    };
    private ListView btMessages;

    public BluetoothFragment() {
    }

    private static void showLog(String message) {
        Log.d(MY_TAG, message);
    }

    // Send message to bluetooth
    public static void printMsg(String message) {
        showLog("Entering printMessage");
        if (BluetoothService.btConnectStatus) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothService.write(bytes);
        }
        showLog(message);
        //for the message chat List
        btMessagesListAdapter.add("Grp 18: " + message);
        showLog("Exiting printMessage");
    }

    // for obstacle message
    public static void printMsg(String name, int x, int y, String direction) throws JSONException {
        showLog("Entering printMessage");

        JSONObject jsonObject = new JSONObject();
        String message;

        switch (name) {
            //"starting" case:
            case "Obstacle":
                jsonObject.put(name, name);
                jsonObject.put("x", Integer.toString(x));
                jsonObject.put("y", Integer.toString(y));
                message = name + " (" + x + "," + y + "," + direction + ")";
                break;
            case "Obstacle direction change":
                jsonObject.put(name, name);
                jsonObject.put("x", Integer.toString(x));
                jsonObject.put("y", Integer.toString(y));
                message = name + " (" + x + "," + y + "," + direction + ")";
                break;
            default:
                message = "Unexpected default for printMessage: " + name;
                break;
        }

        if (BluetoothService.btConnectStatus) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothService.write(bytes);
        }
        showLog("Exiting printMessage");
        btMessagesListAdapter.add("Grp 18: " + message);
    }

    public static void refreshMessageReceived() {
        btMessagesListAdapter.add(sharedPreferences.getString("message", ""));
        btMessagesListAdapter.notifyDataSetChanged();
    }

    public static String getBTName() {
        return btDevice.getName();
    }

    protected void showSoftwareKeyboard(boolean showKeyboard) {
        final Activity activity = getActivity();
        final InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), showKeyboard ? InputMethodManager.SHOW_FORCED : InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //  Stop Bluetooth discovery
        bluetoothAdapter.cancelDiscovery();
    }

    private void getPairedDevices() {
        // set bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //  Get paired devices and display on list
        pairedDevices = bluetoothAdapter.getBondedDevices();
        Log.d("pairedDevices", Integer.toString(pairedDevices.size()));
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice : pairedDevices) {
                pairedDevicesListAdapter.add(pairedDevice.getName());
            }
            pairedDevicesListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        availableDevices = new HashSet<>();
        bluetoothConnection = new BluetoothService(getContext());
        checkBTPermission();
    }

    //@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_bluetooth2, container, false);

        //  Instantiate layouts
        unconnectedLayout = view.findViewById(R.id.unconnectedBTContainer);
        connectedLayout = view.findViewById(R.id.connectedBTContainer);

        switchBT = view.findViewById(R.id.switch_BT);
        scanBTbutton = view.findViewById(R.id.scanBTbutton);
        tvDeviceName = view.findViewById(R.id.deviceName);
        tvConStatus = view.findViewById(R.id.connectionStatus);
        pairedDevicesListView = view.findViewById(R.id.pairedBTDevicesList);
        availableDevicesListView = view.findViewById(R.id.connectedBTDevicesList);

        //  Instantiate connected layout
        disconnectBTButton = view.findViewById(R.id.disconnectBTButton);
        btMessages = view.findViewById(R.id.BTMessages);
        btMessagesListAdapter = new ArrayAdapter<>(getContext(), R.layout.obstacleitem);
        btMessages.setAdapter(btMessagesListAdapter);
        sendMessageField = view.findViewById(R.id.sendMessageField);
        sendMessageButton = view.findViewById(R.id.sendMessageButton);

        // set the adapter for paired devices
        pairedDevicesListAdapter = new ArrayAdapter<>(getContext(), R.layout.obstacleitem);
        pairedDevicesListView.setAdapter(pairedDevicesListAdapter);

        // set the adapter for discovered devices
        availableDevicesListAdapter = new ArrayAdapter<>(getContext(), R.layout.obstacleitem);
        availableDevicesListView.setAdapter(availableDevicesListAdapter);

        getPairedDevices();

        //NEW
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(broadcastReceiver4, filter);

        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver5, filter2);

        Log.d("bluetoothAdapter", Boolean.toString(bluetoothAdapter.isEnabled()));
        if(bluetoothAdapter.isEnabled())
        {
            switchBT.setChecked(true);
            pairedDevicesListView.setVisibility(View.VISIBLE);
        }
        else
        {
            updateDisconnectStatus();
            pairedDevicesListView.setVisibility(View.GONE);
        }
        switchBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (switchBT.isChecked()) {
                    pairedDevicesListView.setVisibility(View.VISIBLE);
                    checkBTPermission();

                    if (bluetoothAdapter == null) {
                        switchBT.setChecked(false);
                        Toast.makeText(getContext(), "Bluetooth is not supported in this device.", Toast.LENGTH_SHORT).show();
                    } else {
                        if (!bluetoothAdapter.isEnabled()) {
                            Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivity(enableBTintent);

                            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                            getContext().registerReceiver(broadcastReceiver1, BTIntent);

                            IntentFilter discoverIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                            getContext().registerReceiver(broadcastReceiver2, discoverIntent);
                        }
                        if (bluetoothAdapter.isEnabled()) {
                            Log.d(MY_TAG, "enableDisableBT: disabling Bluetooth");
                            bluetoothAdapter.disable();

                            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                            getContext().registerReceiver(broadcastReceiver1, BTIntent);
                        }
                    }
                }
                else
                {
                    bluetoothAdapter.disable();
                    availableDevicesListAdapter.clear();
                    availableDevicesListAdapter.notifyDataSetChanged();
                    pairedDevicesListView.setVisibility(View.GONE);
                    updateDisconnectStatus();
                }
            }
        });


        //  Scan scanBTbutton action
        scanBTbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(MY_TAG,"btn_scan click");
                //sets the device to be discoverable for five minutes
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);

                // clear list adapter and hashset
                availableDevicesListAdapter.clear();
                availableDevices.clear();
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                checkBTPermission();
                bluetoothAdapter.startDiscovery();

                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                getContext().registerReceiver(broadcastReceiver3, discoverDevicesIntent);
            }
        });

        // paired devices on click
        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                String chosenDeviceName = ((TextView) view).getText().toString();
                for (BluetoothDevice pairedDevice : pairedDevices) {
                    if (pairedDevice.getName().equalsIgnoreCase(chosenDeviceName)) {
                        //bluetoothConnection.connect(pairedDevice);
                        btDevice = pairedDevice;
                        showLog("pairedDevicesListView on click " + btDevice.getName());
                        isDisconnectBtn = false;
                        startConnect();
                        break;
                    }
                }
            }
        });

        // discovered devices on clicked
        availableDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String chosenDeviceName = ((TextView) view).getText().toString();
                for (BluetoothDevice device : availableDevices) {
                    if (device==null || device.getName()==null)
                        continue;
                    if (device.getName().equalsIgnoreCase(chosenDeviceName)) {
                        availableDevicesListAdapter.clear();
                        availableDevices.clear();
                        btDevice = device;
                        isDisconnectBtn = false;
                        startConnect();
                        break;
                    }
                }
            }
        });

        //  Disconnect scanBTbutton action
        disconnectBTButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothFragment.printMsg("BT_DC");
                //TODO: Disconnect function
                bluetoothConnection.disconnect();
                isDisconnectBtn = true;
            }
        });

        //  Send Message scanBTbutton action
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked sendTextBtn");
                String sentText = "" + sendMessageField.getText().toString();
                showSoftwareKeyboard(false);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("message", sharedPreferences.getString("message", "") + '\n' + sentText);
                editor.commit();
                btMessagesListAdapter.add("Group 18: " + sentText);
                sendMessageField.setText("");

                if (BluetoothService.btConnectStatus) {
                    byte[] bytes = sentText.getBytes(Charset.defaultCharset());
                    BluetoothService.write(bytes);
                }
                showLog("Exiting sendTextBtn");

            }
        });

        myDialog = new ProgressDialog(getContext());
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return view;
    }

    private void updateConnectedStatus(String deviceName) {
        tvDeviceName.setText(deviceName);
        tvConStatus.setText("CONNECTED");

        connectedDeviceName = deviceName;

        scanBTbutton.setVisibility(View.GONE);
        unconnectedLayout.setVisibility(View.GONE);
        connectedLayout.setVisibility(View.VISIBLE);

        //this is dark forest green
        tvDeviceName.setTextColor(Color.parseColor("#055303"));
        //this is pastel red
        tvConStatus.setTextColor(Color.parseColor("#055303"));
    }

    private void updateDisconnectStatus() {
        tvDeviceName.setText("No Device");
        tvConStatus.setText("DISCONNECTED");

        scanBTbutton.setVisibility(View.VISIBLE);
        unconnectedLayout.setVisibility(View.VISIBLE);
        connectedLayout.setVisibility(View.GONE);
        //this is pastel red
        tvDeviceName.setTextColor(Color.parseColor("#630727"));
        tvConStatus.setTextColor(Color.parseColor("#630727"));

        btMessagesListAdapter.clear();
    }

    // Check Bluetooth permission
    public void checkBTPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            int permissionCheck = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.BLUETOOTH_ADMIN) + ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.BLUETOOTH);

            if(permissionCheck != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(getActivity(), new String[]
                        {
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH

                        }, 1);
            }

        }
    }

    public void startConnect() {
        startBTConnect(btDevice, theUUID);
    }

    public void startBTConnect(BluetoothDevice device, UUID uuid) {
        Log.d(MY_TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection");
        bluetoothConnection.startClientThread(device, uuid);
    }

    @Override
    public void onPause() {
        Log.d(MY_TAG, "onPause: called");
        super.onPause();
        try {
            getContext().unregisterReceiver(broadcastReceiver1);
            getContext().unregisterReceiver(broadcastReceiver2);
            getContext().unregisterReceiver(broadcastReceiver3);
            getContext().unregisterReceiver(broadcastReceiver4);
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }
}