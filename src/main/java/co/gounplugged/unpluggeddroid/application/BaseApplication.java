package co.gounplugged.unpluggeddroid.application;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.facebook.stetho.Stetho;

import java.util.List;

import co.gounplugged.unpluggeddroid.api.APICaller;
import co.gounplugged.unpluggeddroid.db.ConversationDatabaseAccess;
import co.gounplugged.unpluggeddroid.db.DatabaseAccess;
import co.gounplugged.unpluggeddroid.managers.ThrowManager;
import co.gounplugged.unpluggeddroid.models.Conversation;
import co.gounplugged.unpluggeddroid.models.Mask;
import co.gounplugged.unpluggeddroid.models.Profile;
import co.gounplugged.unpluggeddroid.services.EdgenetClientService;
import co.gounplugged.unpluggeddroid.services.OpenPGPBridgeService;
import co.gounplugged.unpluggeddroid.utils.ContactUtil;
import co.gounplugged.unpluggeddroid.utils.MaskUtil;

/**
 * Serves as global application instance
 */
public class BaseApplication extends Application {
    private static final String TAG = "BaseApplication";
    public static final String SMS_DEFAULT_APPLICATION = "sms_default_application";

    private APICaller mApiCaller;
    private List<Mask> mKnownMasks;
    private List<Conversation> mRecentConversations;

    private OpenPGPBridgeService mOpenPGPBridgeService;
    private boolean mIsBoundToOpenPGP = false;
    private ServiceConnection mOpenPGPBridgeConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OpenPGPBridgeService.LocalBinder binder = (OpenPGPBridgeService.LocalBinder) service;
            mOpenPGPBridgeService = binder.getService();
            mIsBoundToOpenPGP = true;
            Log.d(TAG, "bound to pgp bridge");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIsBoundToOpenPGP = false;
            Log.d(TAG, "unbound from pgp bridge");
        }
    };

    public static final BaseApplication getInstance(Context c) {
        return (BaseApplication) c.getApplicationContext();
    }

    public static class App {

        public static ThrowManager ThrowManager;
    }
    /**
     * Get new masks from api or cache on app start
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "APPLICATION STARTED");

        // Initialize Stetho, a debug bridge that will enable us to inspect the application
        // (via chrome developper tools) and enable a command line interfact to app internals.
        Stetho.initialize(Stetho.newInitializerBuilder(this)
                .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))
                .build());

        // Eager initialization of certain managers
        initManagers();

        Profile.loadProfile(getApplicationContext());
        mApiCaller = new APICaller(getApplicationContext());

        switch (Profile.getApplicationState()) {
            case (Profile.APPLICATION_STATE_UNINITALIZED):
                break;
            case (Profile.APPLICATION_STATE_INITALIZED):
                seedKnownMasks();
                break;
        }

        //todo threading
        mRecentConversations = new ConversationDatabaseAccess(getApplicationContext()).getRecentConversations();

        Log.d(TAG, "APPLICATION PROGRESSED");

        startService(new Intent(this, EdgenetClientService.class));
        startService(new Intent(this, OpenPGPBridgeService.class));

        bindService(
                new Intent(this, OpenPGPBridgeService.class),
                mOpenPGPBridgeConnection,
                Context.BIND_AUTO_CREATE);
    }

    public void unbindFromPGPService() {
        if (mIsBoundToOpenPGP) {
            unbindService(mOpenPGPBridgeConnection);
            mIsBoundToOpenPGP = false;
        }
    }

    public OpenPGPBridgeService getOpenPGPBridgeService() {
        return mOpenPGPBridgeService;
    }

    private void initManagers() {
        App.ThrowManager = new ThrowManager(getApplicationContext());
    }

    public List<Conversation> getRecentConversations() {
        return mRecentConversations;
    }

    public void addRecentConversation(Conversation conversation) {
        mRecentConversations.add(0, conversation);
    }
    public void removeRecentConversation(Conversation conversation) {
        mRecentConversations.remove(conversation);
    }

    public void refreshKnownMasks() {
        mKnownMasks = null;
        seedKnownMasks();
    }

    public void seedKnownMasks() {
        if(mKnownMasks == null) mKnownMasks = MaskUtil.getCachedMasks(getApplicationContext());
        if(mKnownMasks.isEmpty()) mApiCaller.getMasks(Profile.getCountryCodeFilter());
    }

    public List<Mask> getKnownMasks() {
        seedKnownMasks();
        return mKnownMasks;
    }

    public void setKnownMasks(List<Mask> knownMasks) {
        this.mKnownMasks = knownMasks;
    }

    public void loadContacts() {
        if(Profile.areContactsSynced()) return;
        ContactUtil.loadContactsInThread(getApplicationContext());
        Profile.setContactsSynced(true);
    }

    public boolean isDefaultSMSApp() {
        String defaultApplication = Settings.Secure.getString(getContentResolver(),  SMS_DEFAULT_APPLICATION);
        String thisApplication = getApplicationContext().getPackageName();

        return defaultApplication.equals(thisApplication);
    }
}
