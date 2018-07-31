package cz.destil.moodsync.light;

import android.graphics.Color;
import android.util.Log;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent;
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryCallback;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeState;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.clip.ClipResponse;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.destil.moodsync.R;
import cz.destil.moodsync.activity.MainActivity;
import cz.destil.moodsync.core.App;
import cz.destil.moodsync.core.BaseAsyncTask;
import cz.destil.moodsync.core.Config;
import cz.destil.moodsync.event.ErrorEvent;
import cz.destil.moodsync.event.SuccessEvent;
import cz.destil.moodsync.util.Toas;
//import lifx.java.android.client.LFXClient;
//import lifx.java.android.entities.LFXHSBKColor;
//import lifx.java.android.entities.LFXTypes;
//import lifx.java.android.light.LFXTaggedLightCollection;
//import lifx.java.android.network_context.LFXNetworkContext;
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor;
import com.squareup.otto.Bus;

/**
 * Controller which controls Hue lights.
 *
 * @author Dennis Leroy Wigand (xwavedw@gmail.com)
 */
public class LightsControllerHue {

    private static final String TAG = "LightsControllerHue";

    private static final int TIMEOUT = 5000;
    private static LightsControllerHue sInstance;
//    private LFXNetworkContext mNetworkContext;
    private boolean mWorkingFine;
    private boolean mDisconnected;
    private int mPreviousColor = -1;

    private Bridge bridge;
    private BridgeDiscovery bridgeDiscovery;
    private List<BridgeDiscoveryResult> bridgeDiscoveryResults;

//    private Bus bus;

    enum UIState {
        Idle,
        BridgeDiscoveryRunning,
        BridgeDiscoveryResults,
        Connecting,
        Pushlinking,
        Connected
    }

    public static LightsControllerHue get() {
        if (sInstance == null) {
            sInstance = new LightsControllerHue();
        }
        return sInstance;
    }

//    public void setBus(Bus bus) {
//        this.bus = bus;
//    }

    public void changeColor(int color) {
        if (mWorkingFine && color != mPreviousColor) {
            Log.w(TAG, "Change Color to " + color);
            BridgeState bridgeState = bridge.getBridgeState();
            List<LightPoint> lights = bridgeState.getLights();
            for (final LightPoint light : lights) {
                final LightState lightState = new LightState();
                // TODO what about duraltion?
                int[] rgbColors = new int[1];
                rgbColors[0] = color;
                double[][] ret = HueColor.bulkConvertToXY(rgbColors, light);
                if (ret.length == 1) {
                    lightState.setXY(ret[0][0], ret[0][1]);
                    //TODO birghtness? Config.LIFX_BRIGHTNESS
                    light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
                        @Override
                        public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
                            if (returnCode == ReturnCode.SUCCESS) {
                                Log.w(TAG, "Changed hue of light " + light.getIdentifier() + " to " + lightState.getHue());
                            } else {
                                Log.e(TAG, "Error changing hue of light " + light.getIdentifier());
                                for (HueError error : errorList) {
                                    Log.e(TAG, error.toString());
                                }
                            }
                        }
                    });
                }
            }
            mPreviousColor = color;
        }
    }

    public void init() {
        Log.w(TAG, "Init called");
        mWorkingFine = false;
        mDisconnected = false;
//        mNetworkContext = LFXClient.getSharedInstance(App.get()).getLocalNetworkContext();
        //TODO we may also need a network listener
//        mNetworkContext.addNetworkContextListener(new LFXNetworkContext.LFXNetworkContextListener() {
//            @Override
//            public void networkContextDidConnect(LFXNetworkContext networkContext) {
//                mDisconnected = false;
//            }
//
//            @Override
//            public void networkContextDidDisconnect(LFXNetworkContext networkContext) {
//                if (!mDisconnected && mWorkingFine) {
//                    mWorkingFine = false;
//                    Toas.t(R.string.lifx_disconnected);
//                    MainActivity.BUS.post(new ErrorEvent(R.string.lifx_disconnected));
//                }
//            }
//
//            @Override
//            public void networkContextDidAddTaggedLightCollection(LFXNetworkContext networkContext, LFXTaggedLightCollection collection) {
//                startRocking();
//            }
//
//            @Override
//            public void networkContextDidRemoveTaggedLightCollection(LFXNetworkContext networkContext, LFXTaggedLightCollection collection) {
//            }
//        });
    }

    private void startRocking() {
        Log.w(TAG, "rocking called");
        //java.lang.IllegalStateException: Event bus [Bus "default"] accessed from non-main thread null
//        MainActivity.BUS.post(new SuccessEvent());
//        if (bus != null) {
        App.bus().post(new SuccessEvent());
//        }

        BridgeState bridgeState = bridge.getBridgeState();
        List<LightPoint> lights = bridgeState.getLights();
        for (final LightPoint light : lights) {
            final LightState lightState = new LightState();
            Log.w(TAG, "Set lights on!");
            lightState.setOn(true);
            light.updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
                @Override
                public void handleCallback(Bridge bridge, ReturnCode returnCode, List<ClipResponse> list, List<HueError> errorList) {
                    if (returnCode == ReturnCode.SUCCESS) {
                        Log.w(TAG, "Changed hue of light " + light.getIdentifier() + " to " + lightState.getHue());
                        mWorkingFine = true;
                    } else {
                        Log.e(TAG, "Error changing hue of light " + light.getIdentifier());
                        for (HueError error : errorList) {
                            Log.e(TAG, error.toString());
                        }
                        mWorkingFine = false;
                    }
                }
            });
        }
    }

    public void start() {
        // Connect to a bridge or start the bridge discovery
        String bridgeIp = getLastUsedBridgeIp();
        Log.w(TAG, "Checkf or last ip; " + bridgeIp);
        if (!mWorkingFine) {
            if (bridgeIp == null) {
                startBridgeDiscovery();
            } else {
                connectToBridge(bridgeIp);
            }
        } else {
            startRocking();
        }
    }

    public void stop() {
        mDisconnected = true;
        if (bridge != null && mWorkingFine) {
            Log.w(TAG, "disconnect from bridge");
            disconnectFromBridge();
        }
    }

    public void signalStop() {
        int color = App.get().getResources().getColor(android.R.color.white);
        //TODO faster 100?
        Log.w(TAG, "signal stop change color");
        changeColor(color);
    }

    /**
     * Use the KnownBridges API to retrieve the last connected bridge
     * @return Ip address of the last connected bridge, or null
     */
    private String getLastUsedBridgeIp() {
        List<KnownBridge> bridges = KnownBridges.getAll();

        if (bridges.isEmpty()) {
            return null;
        }

        return Collections.max(bridges, new Comparator<KnownBridge>() {
            @Override
            public int compare(KnownBridge a, KnownBridge b) {
                return a.getLastConnected().compareTo(b.getLastConnected());
            }
        }).getIpAddress();
    }

    /**
     * Start the bridge discovery search
     * Read the documentation on meethue for an explanation of the bridge discovery options
     */
    private void startBridgeDiscovery() {
        Log.w(TAG, "Start Discovery!");
        Toas.t("Start Discovery!");
        disconnectFromBridge();
        bridgeDiscovery = new BridgeDiscovery();
        // ALL Include [UPNP, IPSCAN, NUPNP] but in some nets UPNP and NUPNP is not working properly
        Log.w(TAG, "Scanning");
        bridgeDiscovery.search(BridgeDiscovery.BridgeDiscoveryOption.ALL, bridgeDiscoveryCallback);
//        MainActivity.BUS.post(new ErrorEvent(R.string.connecting_to_hue));
    }

    /**
     * Disconnect a bridge
     * The hue SDK supports multiple bridge connections at the same time,
     * but for the purposes of this demo we only connect to one bridge at a time.
     */
    private void disconnectFromBridge() {
        if (bridge != null) {
            Log.w(TAG, "Disconnect from bridge");
            bridge.disconnect();
            bridge = null;
        }
    }

    /**
     * The callback that receives the results of the bridge discovery
     */
    private BridgeDiscoveryCallback bridgeDiscoveryCallback = new BridgeDiscoveryCallback() {
        @Override
        public void onFinished(final List<BridgeDiscoveryResult> results, final ReturnCode returnCode) {
            // Set to null to prevent stopBridgeDiscovery from stopping it
            bridgeDiscovery = null;
//            Toas.t("onFinished: " + returnCode);
            Log.w(TAG, "Finished Scanning callback: " + returnCode);

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
                    if (returnCode == ReturnCode.SUCCESS) {
                        bridgeDiscoveryResults = results;

                        //TODO HACK: Connect to first bridge
                        Log.w(TAG, "Bridges found: " + results.size());
                        if (results.size() > 0) {
                            Log.w(TAG, "connectToBridge");
//                            Toas.t("Found bridges: " + results.size());
                            connectToBridge(results.get(0).getIP());
                        } else {
                            Log.w(TAG, "no bridges discovered");
                            App.bus().post(new ErrorEvent(R.string.discovery_no_hue_bridge));
                        }
//                        updateUI(UIState.BridgeDiscoveryResults, "Found " + results.size() + " bridge(s) in the network.");
                    } else if (returnCode == ReturnCode.STOPPED) {
                        Log.w(TAG, "no bridges discovered");
                        App.bus().post(new ErrorEvent(R.string.discovery_no_hue_bridge));
                    } else {
                        Log.w(TAG, "hue bridge error");
                        App.bus().post(new ErrorEvent(R.string.discovery_hue_bridge_error));
                    }
//                }
//            });
        }
    };

    /**
     * Use the BridgeBuilder to create a bridge instance and connect to it
     */
    private void connectToBridge(String bridgeIp) {
        stopBridgeDiscovery();
        disconnectFromBridge();

        bridge = new BridgeBuilder("app name", "device name")
                .setIpAddress(bridgeIp)
                .setConnectionType(BridgeConnectionType.LOCAL)
                .setBridgeConnectionCallback(bridgeConnectionCallback)
                .addBridgeStateUpdatedCallback(bridgeStateUpdatedCallback)
                .build();
        Log.e(TAG, "New bridge created");

        bridge.connect();
    }

    /**
     * Stops the bridge discovery if it is still running
     */
    private void stopBridgeDiscovery() {
        if (bridgeDiscovery != null) {
            Log.w(TAG, "Stop bridge from discovery");
            bridgeDiscovery.stop();
            bridgeDiscovery = null;
        }
    }

    /**
     * The callback that receives bridge connection events
     */
    private BridgeConnectionCallback bridgeConnectionCallback = new BridgeConnectionCallback() {
        @Override
        public void onConnectionEvent(BridgeConnection bridgeConnection, ConnectionEvent connectionEvent) {
            Log.w(TAG, "Connection event: " + connectionEvent);

            switch (connectionEvent) {
                case LINK_BUTTON_NOT_PRESSED:
//                    Toas.t("Press the link button to authenticate.");
                    Log.e(TAG, "Press the link button to authenticate.");
//                    MainActivity.BUS.post(new ErrorEvent(R.string.auth_hue_bridge));
                    break;

                case COULD_NOT_CONNECT:
//                    Toas.t("Could not connect.");
                    Log.e(TAG, "Could not connect.");
//                    MainActivity.BUS.post(new ErrorEvent(R.string.no_connection_hue_bridge));
                    break;

                case CONNECTION_LOST:
//                    Toas.t("Connection lost. Attempting to reconnect.");
                    Log.e(TAG, "Connection lost. Attempting to reconnect.");
//                    MainActivity.BUS.post(new ErrorEvent(R.string.connection_lost_hue_bridge));
                    break;

                case CONNECTION_RESTORED:
//                    Toas.t("Connection restored.");
                    Log.e(TAG, "Connection restored.");
//                    MainActivity.BUS.post(new ErrorEvent(R.string.connection_restored_hue_bridge));
                    break;

                case DISCONNECTED:
                    // User-initiated disconnection.
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onConnectionError(BridgeConnection bridgeConnection, List<HueError> list) {
            // TODO
            for (HueError error : list) {
                Log.e(TAG, "Connection error: " + error.toString());
//                MainActivity.BUS.post(new ErrorEvent(R.string.connection_lost_hue_bridge));
            }
        }
    };

    /**
     * The callback the receives bridge state update events
     */
    private BridgeStateUpdatedCallback bridgeStateUpdatedCallback = new BridgeStateUpdatedCallback() {
        @Override
        public void onBridgeStateUpdated(Bridge bridge, BridgeStateUpdatedEvent bridgeStateUpdatedEvent) {
            Log.w(TAG, "Bridge state updated event: " + bridgeStateUpdatedEvent);

            switch (bridgeStateUpdatedEvent) {
                case INITIALIZED:
                    // The bridge state was fully initialized for the first time.
                    // It is now safe to perform operations on the bridge state.
//                    updateUI(UIState.Connected, "Connected!");
                    Log.w(TAG, "Conected HUE");
                    startRocking();
                    break;

                case LIGHTS_AND_GROUPS:
                    // At least one light was updated.
                    break;

                default:
                    break;
            }
        }
    };
}
