package co.gounplugged.unpluggeddroid.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import co.gounplugged.unpluggeddroid.R;
import co.gounplugged.unpluggeddroid.adapters.MessageAdapter;
import co.gounplugged.unpluggeddroid.application.BaseApplication;
import co.gounplugged.unpluggeddroid.exceptions.InvalidConversationException;
import co.gounplugged.unpluggeddroid.exceptions.NotFoundInDatabaseException;
import co.gounplugged.unpluggeddroid.fragments.MessageInputFragment;
import co.gounplugged.unpluggeddroid.fragments.SearchContactFragment;
import co.gounplugged.unpluggeddroid.models.Contact;
import co.gounplugged.unpluggeddroid.models.Conversation;
import co.gounplugged.unpluggeddroid.models.Message;
import co.gounplugged.unpluggeddroid.models.Profile;
import co.gounplugged.unpluggeddroid.services.OpenPGPBridgeService;
import co.gounplugged.unpluggeddroid.utils.ConversationUtil;
import co.gounplugged.unpluggeddroid.widgets.ConversationContainer;
import co.gounplugged.unpluggeddroid.widgets.infiniteviewpager.InfinitePagerAdapter;
import co.gounplugged.unpluggeddroid.widgets.infiniteviewpager.InfiniteViewPager;
import de.greenrobot.event.EventBus;


public class ChatActivity extends BaseActivity {
	// Debug
	private final String TAG = "ChatActivity";
	
	// Constants
    public static final String EXTRA_MESSAGE = "message";

	// GUI
	private MessageAdapter mChatArrayAdapter;
	private ListView mChatListView;
    private InfiniteViewPager mViewPager;
    private ConversationContainer mConversationContainer;
    private ImageView mImageViewDropZoneChats, mImageViewDropZoneDelete;

    private Conversation mSelectedConversation;
    private Conversation mClickedConversation;  //TODO refactor global var

    private ConversationContainer.ConversationListener conversationListener = new ConversationContainer.ConversationListener() {

        //TODO: clear distinction / proper naming for selecting a conversation and switching to conversation
        @Override
        public void onConversationClicked(Conversation conversation) {
            Log.i(TAG, "onConversationClicked: " + conversation.toString());
            mClickedConversation = conversation;
            showDropZones();
        }
    };

    /*
        Return the last selected conversation. Null if no last conversation.
     */
    public Conversation getLastSelectedConversation() {
        if(mSelectedConversation != null) ConversationUtil.refresh(getApplicationContext(), mSelectedConversation);
        if(mSelectedConversation == null) {
            long cid = Profile.getLastConversationId();
            if(cid != Profile.LAST_SELECTED_CONVERSATION_UNSET_ID) {
                try {
                    mSelectedConversation = ConversationUtil.findById(getApplicationContext(), cid);
                } catch (NotFoundInDatabaseException e) {
                    e.printStackTrace();
                }
            } else {
                List<Conversation> conversations = ConversationUtil.getAll(getApplicationContext());
                if(conversations != null && conversations.size() > 0)
                    mSelectedConversation = conversations.get(0);
            }
        }
        return mSelectedConversation;
    }

    private void setLastConversation() {
        Profile.setLastConversationId(mSelectedConversation.id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        hideActionBar();

        getLastSelectedConversation();
        mChatArrayAdapter = new MessageAdapter(this, mSelectedConversation);
    	loadGui();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ((BaseApplication) getApplicationContext()).seedKnownMasks();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected synchronized void onResume() {
    	super.onResume();
        mConversationContainer.setConversationListener(conversationListener);

        EventBus.getDefault().removeStickyEvent(Message.class);
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mConversationContainer.removeConversationListener(conversationListener);
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }

    public void onEventMainThread(Message message) {
        mChatArrayAdapter.addMessage(message);
    }

    private void loadGui() {
        // Setup navigation-drawer
        final String[] menu = getResources().getStringArray(R.array.navigation_menu);
        ArrayAdapter<String> drawerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menu);

        final DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        final ListView navList = (ListView) findViewById(R.id.drawer);
        navList.setAdapter(drawerAdapter);
        navList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int pos,long id){
                switch (pos) {
                    case 0:
                        startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                        return;
                    case 1:
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                        return;

                }
            }
        });


        // Input/Search infinite-viewpager
        mViewPager = (InfiniteViewPager) findViewById(R.id.viewpager);

        // It is only possible to achieve wrapping when you have at least 4 pages.
        // This is because of the way the ViewPager creates, destroys, and displays the pages.
        // No fix for the general case has been found.
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(Fragment.instantiate(getApplicationContext(), MessageInputFragment.class.getName(), getIntent().getExtras()));
        fragments.add(Fragment.instantiate(getApplicationContext(), SearchContactFragment.class.getName(), getIntent().getExtras()));
        fragments.add(Fragment.instantiate(getApplicationContext(), MessageInputFragment.class.getName(), getIntent().getExtras()));
        fragments.add(Fragment.instantiate(getApplicationContext(), SearchContactFragment.class.getName(), getIntent().getExtras()));

        FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager(), fragments);
        PagerAdapter wrappedAdapter = new InfinitePagerAdapter(adapter);

        mViewPager.setAdapter(wrappedAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                Log.i(TAG, (position % 2 == 0 ? "input" : "search") + "-fragment in viewpager selected");
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        //drop zone views & animations
        mImageViewDropZoneDelete = (ImageView) findViewById(R.id.iv_drop_zone_delete);
        mImageViewDropZoneChats = (ImageView) findViewById(R.id.iv_drop_zone_chats);

        //Check for drop-events on drop-zones
        mImageViewDropZoneDelete.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    //should always be called
                    case DragEvent.ACTION_DRAG_ENDED:
                        hideDropZones();
                        break;
                    case DragEvent.ACTION_DROP:
                        Log.i(TAG, "Conversation dropped on mImageViewDropZoneDelete.");
                        Collection<Message> messages = new ArrayList<>();
//                        mChatArrayAdapter.setMessages(new ArrayList<>(messages));
                        break;
                }
                return true;
            }
        });
        mImageViewDropZoneChats.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DROP:
                        Log.i(TAG, "Conversation dropped on mChatListView.");
                        replaceSelectedConversation(mClickedConversation);
                        break;
                }
                return true;
            }
        });


        // Chat log //todo extract
        mChatListView = (ListView) findViewById(R.id.lv_chats);
        mChatListView.setAdapter(mChatArrayAdapter);

        //Conversations
        mConversationContainer = (ConversationContainer) findViewById(R.id.conversation_container);
        mConversationContainer.setConversationsAllBut(mSelectedConversation);

        mChatArrayAdapter.setConversation(mSelectedConversation);
    }

    private void replaceSelectedConversation(Conversation newConversation) {
        if(!hasConversationChanged(newConversation)) return;
        if(mSelectedConversation != null) mConversationContainer.addConversation(mSelectedConversation);

        mConversationContainer.removeConversation(newConversation);
        mSelectedConversation = newConversation;
        setLastConversation();

        mChatArrayAdapter.setConversation(mSelectedConversation);
    }


    private void showDropZones() {
        mImageViewDropZoneChats.setVisibility(View.VISIBLE);
        mImageViewDropZoneDelete.setVisibility(View.VISIBLE);
    }

    private void hideDropZones() {
        mImageViewDropZoneDelete.setVisibility(View.GONE);
        mImageViewDropZoneChats.setVisibility(View.GONE);
    }

    //todo refactor
    public MessageAdapter getChatArrayAdapter() {
        return mChatArrayAdapter;
    }


    public void addConversation(Contact contact) {
        Conversation newConversation;

        try {
            newConversation = ConversationUtil.findByParticipant(contact, getApplicationContext());
        } catch(NotFoundInDatabaseException e) {
            try {
                newConversation = ConversationUtil.createConversation(contact, getApplicationContext());
            } catch (InvalidConversationException e1) {
                //TODO let user know something went wrong
                return;
            }
        }

       replaceSelectedConversation(newConversation);
    }

    private boolean hasConversationChanged(Conversation newConversation) {
        if(newConversation == null) return false;
        if(mSelectedConversation == null) return true;
        if(mSelectedConversation.equals(newConversation)) return false;
        return true;
    }

    private class FragmentPagerAdapter extends android.support.v4.app.FragmentPagerAdapter {

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
    }

}