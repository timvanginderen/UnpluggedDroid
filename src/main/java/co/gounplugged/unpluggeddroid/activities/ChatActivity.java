package co.gounplugged.unpluggeddroid.activities;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.viewpagerindicator.UnderlinePageIndicator;

import java.util.ArrayList;
import java.util.List;

import co.gounplugged.unpluggeddroid.R;
import co.gounplugged.unpluggeddroid.adapters.ContactRecyclerViewAdapter;
import co.gounplugged.unpluggeddroid.adapters.MessageRecyclerViewAdapter;
import co.gounplugged.unpluggeddroid.application.BaseApplication;
import co.gounplugged.unpluggeddroid.db.ConversationDatabaseAccess;
import co.gounplugged.unpluggeddroid.exceptions.NotFoundInDatabaseException;
import co.gounplugged.unpluggeddroid.fragments.MessageInputFragment;
import co.gounplugged.unpluggeddroid.fragments.SearchContactFragment;
import co.gounplugged.unpluggeddroid.listeners.RecyclerItemClickListener;
import co.gounplugged.unpluggeddroid.models.Contact;
import co.gounplugged.unpluggeddroid.models.Conversation;
import co.gounplugged.unpluggeddroid.models.Message;
import co.gounplugged.unpluggeddroid.models.Profile;
import co.gounplugged.unpluggeddroid.services.OpenPGPBridgeService;
import co.gounplugged.unpluggeddroid.utils.ContactUtil;
import co.gounplugged.unpluggeddroid.utils.ConversationUtil;
import co.gounplugged.unpluggeddroid.utils.MessageUtil;
import co.gounplugged.unpluggeddroid.utils.NotificationHelper;
import de.greenrobot.event.EventBus;


public class ChatActivity extends BaseActivity {

    private static final int NOTIFICATION_ID = 001;
    public static final String EXTRA_CONVERSATION_ID = "ChatActivity.EXTRA_CONVERSATION_ID";

    // Debug
    private final String TAG = "ChatActivity";

    // Constants
    public static final int VIEWPAGE_MESSAGE_INPUT = 0;
    public static final int VIEWPAGE_SEARCH_CONTACT = 1;

    private int mCurrentViewPage = VIEWPAGE_MESSAGE_INPUT;

    // GUI
    private ViewPager mViewPager;
    private UnderlinePageIndicator mUnderlinePageIndicator;

    private OpenPGPBridgeService mOpenPGPBridgeService;
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
    ;
    private boolean mIsBoundToOpenPGP = false;

    private Conversation mSelectedConversation;

    private MessageRecyclerViewAdapter mMessageRecyclerViewAdapter;
    private ContactRecyclerViewAdapter mContactRecyclerViewAdapter;

    private MessageInputFragment mMessageInputFragment;
    private SearchContactFragment mSearchContactFragment;

    private String mSavedQuery = null;


    public static Intent newLaunchIntent(Context context) {
        Intent intent = new Intent(context, ChatActivity.class);
        // instead of launching a new instance all other activities on top of it will be closed
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    /*
        Return the last selected conversation. Null if no last conversation.
     */
    public synchronized Conversation getLastSelectedConversation() {
        if (mSelectedConversation != null) // refresh conversation as it may have changed
            ConversationUtil.refresh(getApplicationContext(), mSelectedConversation);
        if (mSelectedConversation == null) {
            long cid = Profile.getLastConversationId();
            // try to load last conversation saved to profile
            if (cid != Profile.LAST_SELECTED_CONVERSATION_UNSET_ID) {
                try {
                    mSelectedConversation = ConversationUtil.findById(getApplicationContext(), cid);
                } catch (NotFoundInDatabaseException e) {
                    // That conversation got erased from the db
                    mSelectedConversation = getTopConversation();
                }
            } else {
                mSelectedConversation = getTopConversation();
            }
        }
        return mSelectedConversation;
    }

    /**
     * Get the conversation at the top of the list
     * @return
     */
    private Conversation getTopConversation() {
        List<Conversation> conversations = ConversationUtil.getAll(getApplicationContext());
        if (conversations != null && conversations.size() > 0)
            return mSelectedConversation = conversations.get(0);
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat_new);
        Log.d(TAG, "onCreate");

        getLastSelectedConversation();
        loadGui();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Conversation c = null;
        try {
            c = ConversationUtil.findById(this, 1);
        } catch (NotFoundInDatabaseException e) {
            e.printStackTrace();
        }
        Message message = MessageUtil.create(this, c, "Dit is example text", Message.TYPE_INCOMING,
                System.currentTimeMillis());
        Notification notification = NotificationHelper.buildIncomingMessageNotification(this, message);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(NOTIFICATION_ID, notification);

        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

    }

    @Override
    protected synchronized void onResume() {
        super.onResume();
        EventBus.getDefault().removeStickyEvent(Message.class);
        EventBus.getDefault().registerSticky(this);


        long conversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1);
        if (conversationId == -1)
            return;


        //don't re-fetch if conversation is already loaded
        if (mSelectedConversation != null && mSelectedConversation.getId() == conversationId)
            return;

        mSelectedConversation = fetchConversation(conversationId);
        updateActivityViews();


//        Conversation c = null;
//        try {
//            c = ConversationUtil.findById(this, 6);
//        } catch (NotFoundInDatabaseException e) {
//            e.printStackTrace();
//        }
//        Message message = MessageUtil.create(this, c, "Dit is example text", Message.TYPE_INCOMING,
//                System.currentTimeMillis());
//        Notification notification = NotificationHelper.buildIncomingMessageNotification(this, message);
//
//        NotificationManager mNotificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        // mId allows you to update the notification later on.
//        mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

    private Conversation fetchConversation(long conversationId) {
        ConversationDatabaseAccess databaseAccess = new ConversationDatabaseAccess(getApplicationContext());
        return databaseAccess.getById(conversationId);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // getIntent() should always return the most recent
        setIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private static final String BUNDLE_KEY_VIEW_PAGE = "co.gounplugged.unpluggeddroid.mCurrentView";
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(BUNDLE_KEY_VIEW_PAGE, mCurrentViewPage);
    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentViewPage = savedInstanceState.getInt(BUNDLE_KEY_VIEW_PAGE);
        toggleRecyclerView(mCurrentViewPage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onEventMainThread(Message message) {
        mMessageRecyclerViewAdapter.addMessage(message);
    }


    public void filterContacts(String query) {
        mSavedQuery = query;
        if (null == mContactRecyclerViewAdapter)
            return;
        mContactRecyclerViewAdapter.filter(query);
    }

    private void loadGui() {
        setupDrawer(NAVIGATION_MAIN_HOME);

        getSupportActionBar().setTitle(getConversationName());

        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        List<Fragment> fragments = new ArrayList<>();
        mMessageInputFragment = (MessageInputFragment) Fragment.instantiate(getApplicationContext(),
                MessageInputFragment.class.getName(), getIntent().getExtras());
        mSearchContactFragment = (SearchContactFragment) Fragment.instantiate(getApplicationContext(),
                SearchContactFragment.class.getName(), getIntent().getExtras());
        fragments.add(mMessageInputFragment);
        fragments.add(mSearchContactFragment);

        FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager(), fragments);
        mViewPager.setAdapter(adapter);

        mUnderlinePageIndicator = (UnderlinePageIndicator) findViewById(R.id.indicator);
//        mUnderlinePageIndicator.setViewPager(mViewPager);
//        mUnderlinePageIndicator.setCurrentItem(0);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                Log.i(TAG, (position % 2 == 0 ? "input" : "search") + "-fragment in viewpager selected");
                mCurrentViewPage = position;
                toggleRecyclerView(position);
                switch (position) {
                    case VIEWPAGE_MESSAGE_INPUT:
                        getSupportActionBar().setTitle(getConversationName());
                        break;
                    case VIEWPAGE_SEARCH_CONTACT:
                        getSupportActionBar().setTitle("Contacts");
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        mMessageRecyclerViewAdapter = new MessageRecyclerViewAdapter(this, mSelectedConversation);

        //Load contacts in adapter
        if(Build.VERSION.SDK_INT >= 11)
            new LoadContactsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            new LoadContactsTask().execute();

        recyclerView.setAdapter(mMessageRecyclerViewAdapter);
        // Only hackish click-listener for contacts in recyclerview for now
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Contact c = mContactRecyclerViewAdapter.getContact(position);
                        addConversation(c);
                        mSearchContactFragment.clearInput();
                        //Re-order menu
                    }
                })
        );
    }

    private String getConversationName() {
        String name = (mSelectedConversation == null) ?
                getString(R.string.app_name) : mSelectedConversation.getName();
        return name;
    }

    private void toggleRecyclerView(int position) {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
//        mUnderlinePageIndicator.setCurrentItem(position);
        switch (position) {
            case VIEWPAGE_MESSAGE_INPUT:
                recyclerView.setAdapter(mMessageRecyclerViewAdapter);
                break;
            case VIEWPAGE_SEARCH_CONTACT:
                recyclerView.setAdapter(mContactRecyclerViewAdapter);
                break;
        }

    }

    private void establishSecondLine(Conversation conversation) {
        BaseApplication app = (BaseApplication) getApplicationContext();
        OpenPGPBridgeService openPGPBridgeService = app.getOpenPGPBridgeService();

        if(openPGPBridgeService != null) {
            BaseApplication.App.ThrowManager.ensureKreweEstablished(conversation, openPGPBridgeService);
        }
    }


    public void addConversation(Contact contact) {
        Conversation newConversation;

        try {
            newConversation = ConversationUtil.findByParticipant(contact, getApplicationContext());
        } catch(NotFoundInDatabaseException e) {
            try {
                newConversation = ConversationUtil.createConversation(contact, getApplicationContext());
            } catch (Conversation.InvalidConversationException e1) {
                //TODO let user know something went wrong
                return;
            }
        }

        establishSecondLine(newConversation);

        mSelectedConversation = newConversation;
        Profile.setLastConversationId(mSelectedConversation.getId());

        //update ui with new convo
        updateActivityViews();
        addNewConversationToSubMenu(mSelectedConversation);

    }

    private void updateActivityViews() {
        mMessageRecyclerViewAdapter.setConversation(mSelectedConversation);
        toggleRecyclerView(VIEWPAGE_MESSAGE_INPUT);
        mViewPager.setCurrentItem(VIEWPAGE_MESSAGE_INPUT, true);
        getSupportActionBar().setTitle(mSelectedConversation.getParticipant().getName());
        mMessageInputFragment.updateViews();
    }

    public OpenPGPBridgeService getOpenPGPBridgeService() {
        return mOpenPGPBridgeService;
    }

    static class FragmentPagerAdapter extends android.support.v4.app.FragmentPagerAdapter {

        private final List<Fragment> mViewPagerFragments;

        public FragmentPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mViewPagerFragments = fragments;
        }

        @Override
        public Fragment getItem(int index) {
            return mViewPagerFragments.get(index);
        }

        @Override
        public int getCount() {
            return mViewPagerFragments.size();
        }

        //Always return POSITION_NONE from getItemPosition() method. Which means: "Fragment must be always recreated"
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "BLABLABLABALBALA";
        }
    }

    private class LoadContactsTask extends AsyncTask<Void, Void, List<Contact>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<Contact> doInBackground(Void... params) {
            return ContactUtil.getCachedContacts(getApplicationContext());
        }

        @Override
        protected void onPostExecute(List<Contact> contacts) {
            super.onPostExecute(contacts);
            mContactRecyclerViewAdapter = new ContactRecyclerViewAdapter(getApplicationContext(), contacts);

            if (!TextUtils.isEmpty(mSavedQuery))
                mContactRecyclerViewAdapter.filter(mSavedQuery);

            //todo activity could be killed at this point!
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
            if (mCurrentViewPage == VIEWPAGE_SEARCH_CONTACT)
                recyclerView.setAdapter(mContactRecyclerViewAdapter);
        }


    };

}