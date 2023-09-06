package com.gungeeksoftware.MobileMusicShuffler;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import com.gungeeksoftware.MobileMusicShuffler.R;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.GestureStore;
import android.gesture.Prediction;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class main extends Activity {
	
	
	NumberFormat _nf = NumberFormat.getInstance();
	
	final String TAG = "MobileMusicShuffler.main";
	final String KEY_CURRENT_PLAYLIST_NAME = "KEY_CURRENT_PLAYLIST_NAME";
	
	Button _button1;
	ImageButton _buttonPrev;
	ImageButton _buttonNext;
	ImageButton _buttonPlay;
	TextView _textArtist;
	TextView _textTitle;
	TextView _textPositionAndDuration;
	TextView _textPlaylistName;
	
	
	boolean _songsLoaded = false;
	boolean _serviceBound = false;
	
	boolean _prefPauseOnPhoneCall = false;
	boolean _prefHandleBluetooth = false;
	boolean _prefGesturesEnabled = false;
	boolean _prefNoButtons = false;
	String _prefGesture_RightToLeft = "";
	String _prefGesture_LeftToRight = "";
	String _prefGesture_DownArrow = "";
	String _prefGesture_Circle = "";
	String _prefGesture_Zigzag = "";
	String _prefGesture_RightArrow = "";
	String _prefGesture_LeftArrow = "";

	int GESTURE_COMMAND_NONE = 0;
	int GESTURE_COMMAND_PLAY = 1;
	int GESTURE_COMMAND_PREV = 2;
	int GESTURE_COMMAND_NEXT = 3;
	int GESTURE_COMMAND_ALL_SONGS = 4;
	int GESTURE_COMMAND_PLAYLISTS = 5;
	int GESTURE_COMMAND_NEXT_SAME = 6;
	

	
	boolean _showToastDebug = false;
	boolean _dieInAFire = false;
	int _playSongById = -1;
	long _playlistID = 0;
	String _playlistName = "";

	GestureLibrary gestureLibrary;
	
	String _searchQuery = "";
	
	
	// String [] _songs = null;
	
    MediaPlayerServiceConnection mediaPlayerConnection;

	Messenger _messenger;
    final Messenger _receiver = new Messenger(new IncomingHandler());    

    
    
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
//		Uri uri = MediaStore.Audio.Media.getContentUri("phoneStorage");
//		uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//		uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
//		Log.d(TAG, uri.toString());


        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
        {
        	AlertDialog dialog = new AlertDialog.Builder(this).create();
        	dialog.setIcon(R.drawable.stat_notify_sdcard_usb);
        	dialog.setTitle(R.string.error);
        	dialog.setMessage(getString(R.string.no_sd_card));
        	dialog.setButton("OK", new Dialog.OnClickListener() {
				
				public void onClick(DialogInterface arg0, int arg1) {
					finish();
					
				}
			});
        	

        	dialog.show();
        	return;
        }
        
        
        gestureLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
        gestureLibrary.setOrientationStyle(GestureStore.ORIENTATION_SENSITIVE);
        gestureLibrary.setSequenceType(GestureStore.SEQUENCE_SENSITIVE);

        bindGestures();
        

        _textArtist = (TextView)findViewById(R.id.textArtist);
        _textTitle = (TextView)findViewById(R.id.textTitle);
        _textPositionAndDuration = (TextView)findViewById(R.id.textPositionAndDuration);

        setPlayerButtonEvents();

        _playlistName = getString(R.string.all_songs);
        
        handleIntent(getIntent());

        
		
        
    }




	private void bindGestures() 
	{
       
        if (!gestureLibrary.load()) 
        {
        	showToast(getString(R.string.unable_to_load_gestures));
            finish();
        }
        
        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        gestures.setEnabled(_prefGesturesEnabled);
        
        gestures.addOnGesturePerformedListener(new OnGesturePerformedListener() {

        	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
        		ArrayList<Prediction> predictions = gestureLibrary.recognize(gesture);

        		// We want at least one prediction
        		if (predictions.size() > 0) {
        			Prediction prediction = predictions.get(0);
        			// We want at least some confidence in the result
        			if (prediction.score > 1.0) 
        			{
        				String name = prediction.name;
        				
        				// showToast(name);
        				
        				String [] gestureCommands = getResources().getStringArray(R.array.KEY_GESTURE_COMMANDS); 
        				String command = "";
        				
        				// Find out the command configured for the gesture 
        				if (name.equalsIgnoreCase(getString(R.string.gesture_left_to_right)))
        					command = _prefGesture_LeftToRight;
        				else if (name.equalsIgnoreCase(getString(R.string.gesture_right_to_left)))
        					command = _prefGesture_RightToLeft;
        				else if (name.equalsIgnoreCase(getString(R.string.gesture_down_arrow)))
        					command = _prefGesture_DownArrow;
        				else if (name.equalsIgnoreCase(getString(R.string.gesture_circle)))
        					command = _prefGesture_Circle;
        				else if (name.equalsIgnoreCase(getString(R.string.gesture_zigzag)))
        					command = _prefGesture_Zigzag;
        				else if (name.equalsIgnoreCase(getString(R.string.gesture_right_arrow)))
        					command = _prefGesture_RightArrow;
        				else if (name.equalsIgnoreCase(getString(R.string.gesture_left_arrow)))
        					command = _prefGesture_LeftArrow;
        				
        				
        				// showToast(command);
        				
        				// Perform the command
        				if (command.equalsIgnoreCase(gestureCommands[GESTURE_COMMAND_PLAY]))
        				{
        					sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_PLAY);
        					return;
        				}
        				else if (command.equalsIgnoreCase(gestureCommands[GESTURE_COMMAND_NEXT]))
        				{
        					sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_NEXT);
        					return;
        				}
        				else if (command.equalsIgnoreCase(gestureCommands[GESTURE_COMMAND_PREV]))
        				{
        					sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_PREV);
        					return;
        				}
        				else if (command.equalsIgnoreCase(gestureCommands[GESTURE_COMMAND_PLAYLISTS]))
        				{
        					showPlaylistDialog();
        					return;
        				}
        				else if (command.equalsIgnoreCase(gestureCommands[GESTURE_COMMAND_NEXT_SAME]))
        				{
        					sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_NEXT_SONG_BY_SAME_ARTIST);
        					return;
        				}
        				else if (command.equalsIgnoreCase(gestureCommands[GESTURE_COMMAND_ALL_SONGS]))
        				{
        					_playlistID = 0;
        					new LoadSongsTask().execute(_playlistID);
        					return;
        				}

        				

        			}
        		}
        	}

        });
	}
    
    
    
    
    @Override
	protected void onStart() {
    	setPreferences();
    	
        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        gestures.setEnabled(_prefGesturesEnabled);
    	
		super.onStart();
	}

    
    void setPreferences()
    {
    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    	Window window = getWindow();
    	
    	boolean keepScreenOn = pref.getBoolean(getString(R.string.KEY_KEEP_SCREEN_ON), false);
    	if (keepScreenOn)
    		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	else
    		window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
    	
    	boolean showOnLock = pref.getBoolean(getString(R.string.KEY_SHOW_ON_LOCK_SCREEN), false);
    	if (showOnLock)
    		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    	else
    		window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    	
    	
    	_prefPauseOnPhoneCall = pref.getBoolean(getString(R.string.KEY_PAUSE_ON_PHONE_CALL), false);
    	_prefHandleBluetooth = pref.getBoolean(getString(R.string.KEY_HANDLE_BLUETOOTH), false);
    	_prefGesturesEnabled = pref.getBoolean(getString(R.string.KEY_GESTURES_ENABLED), true);
    	_prefNoButtons = pref.getBoolean(getString(R.string.KEY_NO_BUTTONS), false);
    	
    	
    	for (int id : new int[] { R.id.tableButtons, R.id.tableRow1, R.id.buttonNext, R.id.buttonPlay, R.id.buttonPrev } )
    		findViewById(id).setVisibility(_prefNoButtons ? View.GONE : View.VISIBLE);
    			
    	
    	String [] gestureCommands = getResources().getStringArray(R.array.KEY_GESTURE_COMMANDS); 
    	
    	_prefGesture_LeftToRight = pref.getString(getString(R.string.KEY_GESTURE_LEFT_TO_RIGHT), gestureCommands[GESTURE_COMMAND_PREV]); 
    	_prefGesture_RightToLeft = pref.getString(getString(R.string.KEY_GESTURE_RIGHT_TO_LEFT), gestureCommands[GESTURE_COMMAND_NEXT]);
    	_prefGesture_DownArrow = pref.getString(getString(R.string.KEY_GESTURE_DOWN_ARROW), gestureCommands[GESTURE_COMMAND_PLAY]);
    	_prefGesture_Circle = pref.getString(getString(R.string.KEY_GESTURE_CIRCLE), gestureCommands[GESTURE_COMMAND_ALL_SONGS]);
    	_prefGesture_Zigzag = pref.getString(getString(R.string.KEY_GESTURE_ZIGZAG), gestureCommands[GESTURE_COMMAND_PLAYLISTS]);
    	_prefGesture_RightArrow = pref.getString(getString(R.string.KEY_GESTURE_RIGHT_ARROW), gestureCommands[GESTURE_COMMAND_NEXT_SAME]);
    	_prefGesture_LeftArrow = pref.getString(getString(R.string.KEY_GESTURE_LEFT_ARROW), gestureCommands[GESTURE_COMMAND_NONE]);
    			
    	if (_serviceBound && _messenger != null)
    		sendPreferencesToPlayer();

    }
    



	void bindToMediaPlayer()
    {
        if (!_serviceBound)
        {
        	if (mediaPlayerConnection == null)
        		mediaPlayerConnection = new MediaPlayerServiceConnection();
        		
        	
        	Intent intent = new Intent(main.this, MediaPlayerService.class);
        	_serviceBound = bindService(intent, 
	        		mediaPlayerConnection, 
	        		Context.BIND_AUTO_CREATE);
        	
        	
        }

    }
   
    
    void sendPlaySongByIdToPlayer(int songId)
    {
		Bundle bundle = new Bundle();
		bundle.putInt(MediaPlayerService.KEY_SONG_ID, songId);
		sendMessageToMediaPlayer(bundle, MediaPlayerService.MSG_SONG_PLAY_BY_ID);

    }
    
    



	void handleIntent(Intent intent)
	{
		
		
		String action = intent.getAction();

		
		if (Intent.ACTION_MAIN.equals(action))
			bindToMediaPlayer();
		
			
		
		
		 if (Intent.ACTION_SEARCH.equals(action)) 
		 {
			 _searchQuery = intent.getStringExtra(SearchManager.QUERY);
			 showSearchActivity();
		 }
		 	
		 
		 if (Intent.ACTION_VIEW.equals(action))
		 {
			 
			 Uri data = intent.getData();
			 if (data != null)
			 {
				 // Log.d(TAG, "onNewIntent " + data.toString());

				 String songID = data.getLastPathSegment();

				 try {
					 
					 Number id = _nf.parse(songID);
					 
					 if (id.intValue() > 0)
					 {
						 if (_serviceBound)
							 sendPlaySongByIdToPlayer(id.intValue());
						 else
						 {
							 _playSongById = id.intValue();	 
							 bindToMediaPlayer();
						 }
					 }
					 
					 
					 

				 } catch (Exception e) {
					 Log.e(TAG, "onNewIntent", e);
				 }

			 }
			 
			 bindToMediaPlayer();

		 }
	}

	

	@Override
	protected void onNewIntent(Intent intent) {

		setIntent(intent);
		handleIntent(intent);
		
	}



	@Override
	protected void onPause() {
		super.onPause();
	}


	@Override
	protected void onResume() {
		super.onResume();

	}


	@Override
	protected void onDestroy() {

		// Log.d(TAG, "onDestroy");
		
		if (_showToastDebug)
			showToast("onDestroy");
		
		if (_serviceBound)
			unbindFromService();
		
		super.onDestroy();
	}




	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			sendMessageToMediaPlayer(MediaPlayerService.MSG_TO_BACKGROUND_IF_PAUSED);
			unbindFromService();
		}
		
		
		return super.onKeyDown(keyCode, event);
	}






	class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) 
            {
            
        	case MediaPlayerService.MSG_DIE_IN_A_FIRE:
        		// The service has been slain.
        		// showToast("MediaPlayerService.MSG_DIE_IN_A_FIRE, finishing...");
        		finish();
        		break;

        	case MediaPlayerService.MSG_FEED_ME_SOME_SONGS:
            		new LoadSongsTask().execute(_playlistID);
            		break;
            
            	case MediaPlayerService.MSG_SONG_LIST:
            		Bundle bundle = msg.getData();
            		Song [] songs = (Song[])bundle.getSerializable(MediaPlayerService.KEY_SONGS);
            		if (songs != null)
            			setSongCount(songs.length, bundle.getString(MediaPlayerService.KEY_PLAYLIST_NAME));
            		break;
            
            	case MediaPlayerService.MSG_SERVICE_RUNNING:
            		boolean isRunning = msg.getData().getBoolean(MediaPlayerService.KEY_ARG1);
            		if (!isRunning)
            		{
            			Intent intent = new Intent(main.this, MediaPlayerService.class);
            			startService(intent);

            			// new LoadSongsTask().execute();
            			
            		}
            		else
            		{
            			// Update the main UI with data from the service
            			sendMessageToMediaPlayer(MediaPlayerService.MSG_SONG_LIST);
            			sendMessageToMediaPlayer(MediaPlayerService.MSG_CURRENT_SONG);
            			sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_IS_PLAYING);
            		}
            		break;
            
                case MediaPlayerService.MSG_CURRENT_SONG:
                	
                	Song song = (Song)msg.getData().getSerializable(MediaPlayerService.KEY_CURRENT_SONG);
                	displaySongInfo(song);
                    break;
                
                case MediaPlayerService.MSG_PLAYER_IS_PLAYING:
                	boolean isPlaying = msg.getData().getBoolean(MediaPlayerService.KEY_ARG1);
                	if (isPlaying)
                		setPlayButtonToPause();
                	else
                		setPlayButtonToPlay();
                    
                	break;
                	
                case MediaPlayerService.MSG_CURRENT_POSITION_AND_DURATION:
                	
                	Bundle data = msg.getData();
                	_textPositionAndDuration.setText(String.format("%d:%02d/%d:%02d", 
                			data.getInt(MediaPlayerService.KEY_POSITION_MINUTES), 
                			data.getInt(MediaPlayerService.KEY_POSITION_SECONDS), 
                			data.getInt(MediaPlayerService.KEY_DURATION_MINUTES),
                			data.getInt(MediaPlayerService.KEY_DURATION_SECONDS)));
                	
                default:
                    super.handleMessage(msg);
                    
            }
        }

    }

	private void showSearchActivity() 
	{
		unbindFromService();
		
		Intent intent = new Intent(this, SearchResults.class); 
		
		Bundle bundle = new Bundle();
		bundle.putString(SearchResults.KEY_QUERY, _searchQuery);
		intent.putExtras(bundle);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	

	class MediaPlayerServiceConnection implements ServiceConnection
	{


		public void onServiceConnected(ComponentName className, IBinder service) 
         {
			 Log.d(TAG, "onServiceConnected");
			 
         	_messenger = new Messenger(service);
         	
         	// Register ourselves with the service so we can send & receive messages
 			
         	sendMessageToMediaPlayer(MediaPlayerService.MSG_REGISTER_CLIENT);
         	
         	
 			
         	if (_showToastDebug)
         		showToast("onServiceConnected");


         	sendPreferencesToPlayer();
         	
         	
         	if (_playSongById > -1)
         	{
         		sendPlaySongByIdToPlayer(_playSongById);
         		_playSongById = -1;
         	}
         	
         }

         


		public void onServiceDisconnected(ComponentName className) 
         {
          	Log.d(TAG, "onServiceDisconnected");

          	_messenger = null;
         	
         	if (_showToastDebug)
         		showToast("onServiceDisconnected");

         }
	}
	
    

    
    
    void showToast(String message)
    {
    	Toast.makeText(main.this, TAG + " " + message, Toast.LENGTH_SHORT).show();
    }
    
    
    private void sendPreferencesToPlayer() 
    {
		Bundle bundle = new Bundle();
		bundle.putBoolean(MediaPlayerService.KEY_PREF_PAUSE_ON_PHONE_CALL, _prefPauseOnPhoneCall);
		bundle.putBoolean(MediaPlayerService.KEY_PREF_HANDLE_BLUETOOTH, _prefHandleBluetooth);
    	sendMessageToMediaPlayer(bundle, MediaPlayerService.MSG_PREFERENCES);
		
	}

    
    
    void sendMessageToMediaPlayer(Bundle bundle, int what)
    {
    	Message msg = Message.obtain(null, what);
        msg.replyTo = _receiver;
        
        if (bundle != null)
        	msg.setData(bundle);
        
        try 
        {
			_messenger.send(msg);
		} catch (RemoteException e) 
		{
			Log.e("onServiceConnected", e.toString(), e);
		}
    }    
    
    void sendMessageToMediaPlayer(int what)
    {
    	sendMessageToMediaPlayer(null, what);
    }
    
    void showPlaylistDialog()
    {
    	String[] projection = {
				MediaStore.Audio.Playlists._ID,
				MediaStore.Audio.Playlists.NAME
		};

		Cursor cursor = main.this.managedQuery(
				MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
				projection,
				null,
				null,
				MediaStore.Audio.Playlists.NAME);

		final Hashtable<CharSequence, Long> playlists = new Hashtable<CharSequence, Long>();
		ArrayList<String> lists = new ArrayList<String>();
		
		while(cursor.moveToNext())
		{
			// Log.d(TAG, "Playlist _ID: " + cursor.getString(0));
			// Log.d(TAG, "Playlist NAME: " + cursor.getString(1));
			playlists.put((CharSequence)cursor.getString(1), cursor.getLong(0));
			lists.add(cursor.getString(1));
		}

		if (playlists.size() == 0)
		{
			DialogBuilder
				.getSimpleAlert(this, getString(R.string.no_playlists), getString(R.string.no_playlists_defined), R.drawable.ic_dialog_info)
				.show();
			
			return;
		}
		
		
		final CharSequence [] items = new CharSequence[playlists.size()];
		
		for (int i = 0; i < playlists.size(); i++)
			items[i] = lists.get(i);
		
		
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.playlists);
		builder.setItems(items, new Dialog.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {

				_playlistName = (String)items[which];
				_playlistID = playlists.get(items[which]);
					
				
				dialog.dismiss();
				
				new LoadSongsTask().execute(_playlistID);
				
			}
		});
		
		
		AlertDialog dialog = builder.create();
		dialog.show();
		
		
    }
    
    
    void setPlayerButtonEvents()
    {
    	
        _buttonNext = (ImageButton)findViewById(R.id.buttonNext);
        _buttonPlay = (ImageButton)findViewById(R.id.buttonPlay);
        _buttonPrev = (ImageButton)findViewById(R.id.buttonPrev);
    	
    	
    	
        // Next
        _buttonNext.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{
				//Animation hyperspaceJump = AnimationUtils.loadAnimation(main.this, R.anim.fade_to_left);
				//hyperspaceJump.setDuration(250);
				
				//textTitle.startAnimation(hyperspaceJump);
				//_textArtist.startAnimation(hyperspaceJump);
				
				sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_NEXT);	
			}
		});

        _buttonNext.setOnLongClickListener(new OnLongClickListener() {
			
			public boolean onLongClick(View arg0) {
				sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_NEXT_SONG_BY_SAME_ARTIST);
				return true;
			}
		});
        
        // Previous
        _buttonPrev.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_PREV);
				
			}
		});

        // Play 
        _buttonPlay.setOnClickListener(new OnClickListener() {
			public void onClick(View v) 
			{
				sendMessageToMediaPlayer(MediaPlayerService.MSG_PLAYER_PLAY);
				
			}
		});

        
    }
    
    void setButtonsClickable(boolean clickable)
    {
    	
    	ImageButton [] buttons = new ImageButton [] { 
    			_buttonNext, 
    			_buttonPrev, 
    			_buttonPlay 
    	};
    	
    	for (ImageButton ib : buttons)
    		ib.setClickable(clickable);
    	
    	_buttonNext.setLongClickable(clickable);
    	
    }
    
    void setImageButtonImage(ImageButton button, int resDrawable)
    {
    	button.setImageResource(resDrawable);
    }
    
	void setPlayButtonToPause()
	{
		setImageButtonImage(_buttonPlay, R.drawable.ib_pause);
	}

	void setPlayButtonToPlay()
	{
		setImageButtonImage(_buttonPlay, R.drawable.ib_play);
	}

	
 
    
    
   class LoadSongsTask extends AsyncTask<Long, Void, Song []>
   {

	   ProgressDialog _pd = new ProgressDialog(main.this);
	   
	   
	   
		@Override
		protected void onPostExecute(Song [] result) 
		{
			songsLoaded(result, _playlistName);
			_pd.dismiss();
			super.onPostExecute(result);
		}



		@Override
		protected void onPreExecute() {
	
			_pd.setMessage(getString(R.string.loading_music));
			_pd.show();
			
			super.onPreExecute();
		}


		List<Song> GetSongs(Uri uri)
		{
			
			List<Song> list = new ArrayList<Song>();

			String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

			String[] projection = {
					MediaStore.Audio.Media._ID,
					MediaStore.Audio.Media.ARTIST,
					MediaStore.Audio.Media.TITLE,
					MediaStore.Audio.Media.DATA,
					MediaStore.Audio.Media.DISPLAY_NAME,
					MediaStore.Audio.Media.DURATION
			};

			Cursor cursor = main.this.managedQuery(
					uri,
					projection,
					selection,
					null,
					null);

			
			while(cursor.moveToNext())
			{
				Song song = new Song();
				song.ID = cursor.getInt(0);
				song.ARTIST = cursor.getString(1);
				song.TITLE = cursor.getString(2);
				song.DATA = cursor.getString(3);
				song.DISPLAY_NAME = cursor.getString(4);
				song.DURATION = cursor.getString(5);
				
				
				
				long duration = 0;
				try 
				{
					duration = _nf.parse(song.DURATION).longValue();
				} catch (ParseException e) 
				{
				
				}
				
				// don't load anything shorter than a second
				if (duration > 1000)
					list.add(song);

			}


			return list;
			
		}

		@Override
		protected Song [] doInBackground(Long... params) 
		{

			List<Song> list = new ArrayList<Song>();
			
			long playlistID = 0;

			if (params != null && params.length > 0)
				playlistID = params[0];

			
			if (playlistID > 0)
				list.addAll(GetSongs(MediaStore.Audio.Playlists.Members.getContentUri("external", playlistID)));
			else
			{
				try {
					list.addAll(GetSongs(MediaStore.Audio.Media.INTERNAL_CONTENT_URI));
				} catch (Exception e) {
					Log.e(TAG, "Error loading songs from " + MediaStore.Audio.Media.INTERNAL_CONTENT_URI.toString(), e);
				}
				
				try {
					list.addAll(GetSongs(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI));
				} catch (Exception e) {
					Log.e(TAG, "Error loading songs from " + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(), e);
				}
			}

			if (list.size() > 0)
			{
				Collections.shuffle(list);
				Song [] songArr = new Song[list.size()];
				
				return list.toArray(songArr);
			}
			else
				return new Song[0];

			
		}

   }
	   
  
   
   void setSongCount(int count, String playlistName)
   {
	   TextView textSongs = (TextView)findViewById(R.id.textNumberOfSongs);
	   textSongs.setText(playlistName + "\r\n" + _nf.format(count) + " " + getString(R.string.songs_loaded) );
   }
   
    
    void songsLoaded(Song [] songs, String playlistName)
    {
         
         if (songs != null && songs.length > 0)
         {
        	 
        	Log.i("Main", _nf.format(songs.length) + " songs loaded" );
       
        	setSongCount(songs.length, playlistName);
 	        
			try 
			{
				
				Bundle bundle = new Bundle(1);
				bundle.putSerializable(MediaPlayerService.KEY_SONGS, songs);
				bundle.putString(MediaPlayerService.KEY_PLAYLIST_NAME, playlistName);
				sendMessageToMediaPlayer(bundle, MediaPlayerService.MSG_LOAD_SONGS);
				
				
				
				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
 	      
 	        setButtonsClickable(songs.length > 0);

 	        
         }
         else
         {
        	 setButtonsClickable(false);

        	 
        	 AlertDialog dialog = DialogBuilder.getSimpleAlert(this, 
        			 getString(R.string.error), 
        			 getString(R.string.no_songs_to_play), 
        			 R.drawable.ic_dialog_alert_holo_light,
        			 
        			 new DialogInterface.OnClickListener() {
        		 		public void onClick(DialogInterface dialog, int which) {
        		 			dialog.dismiss();
        		 			finish();
        		 		}
        	 	}
        	 );
						
        	 dialog.show();
        	 
         }
    	
    }
    
    
    void displaySongInfo(Song song)
    {
		_textArtist.setText("");
		_textTitle.setText("");

		
		if (song != null)
		{
			_textArtist.setText(song.ARTIST);
			_textTitle.setText(song.TITLE);
		}

    }
    
    
  
    
    
    @Override
	protected void onSaveInstanceState(Bundle outState) {

    	outState.putSerializable(KEY_CURRENT_PLAYLIST_NAME, _playlistName);
		super.onSaveInstanceState(outState);
	}

    
    
    
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

		if (savedInstanceState != null)
			_playlistName = savedInstanceState.getString(KEY_CURRENT_PLAYLIST_NAME) ;
		
		super.onRestoreInstanceState(savedInstanceState);
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = new MenuInflater(main.this);
		inflater.inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	void showQuitDialog()
	{
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setIcon(R.drawable.icon_mms);
		dialog.setTitle(R.string.app_name);
		dialog.setMessage(getString(R.string.sure_want_to_quit));
		
		// Yes
		dialog.setButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (_serviceBound)
				{
					_dieInAFire = true;
					dialog.dismiss();
					unbindFromService();
					finish();
					
				}
				
			}
		});

		
		dialog.setButton2(getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();				
			}
		});

		
		dialog.show();
		
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		switch (item.getItemId())
		{
			case R.id.itemReshuffle:
				_playlistID = 0;
				new LoadSongsTask().execute(_playlistID);
				break;
			
			case R.id.itemQuit:
				showQuitDialog();
				break;

			case R.id.itemAbout:
				showAboutDialog();
				break;

			case R.id.itemPreferences:
				
				Intent intent = new Intent(this, Preferences.class);
				startActivity(intent);
				
				break;
				
			case R.id.itemPlaylists:
				showPlaylistDialog();
				break;

		}


		return super.onMenuItemSelected(featureId, item);
	}



	
	void showAboutDialog()
	{
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setIcon(R.drawable.icon_mms);
		dialog.setTitle(R.string.app_name);
		
		StringBuilder builder = new StringBuilder();
		builder.append(getString(R.string.version));
		builder.append(" ");
		
		String versionName = "";
		
		try {

			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),0);
			versionName = packageInfo.versionName;

		} catch (NameNotFoundException e) {
			versionName = "Cannot load Version!";
		}
		
		builder.append(versionName);

			
		builder.append("\r\n\r\n");
		builder.append(getString(R.string.app_author));
		builder.append(": Pete Nelson <pete@petenelson.com>");
		
		
		dialog.setMessage(builder.toString());
		
		// Yes
		dialog.setButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				
			}
		});

		
		dialog.setButton2(getString(R.string.visit_www), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
				Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(getString(R.string.app_home_page_url)));
				startActivity(browserIntent);
				
				dialog.dismiss();				
			}
		});

		
		dialog.show();
		
	}



	void unbindFromService()
	{
		
		if (_serviceBound)
		{
			Log.d(TAG, "unbindFromService");
			
			if (_showToastDebug)
				showToast("unbindFromService");

			if (_messenger != null) 
			{
				if (_dieInAFire)
				{
					sendMessageToMediaPlayer(MediaPlayerService.MSG_DIE_IN_A_FIRE);
					_dieInAFire = false;
				}
				else
				{
					sendMessageToMediaPlayer(MediaPlayerService.MSG_UNREGISTER_CLIENT);
				}
			}
			
			unbindService(mediaPlayerConnection);
			mediaPlayerConnection = null;
			_serviceBound = false;
			
		}
	}
	

	
	 
}