package com.gungeeksoftware.MobileMusicShuffler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
// import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.gungeeksoftware.MobileMusicShuffler.R;


public class MediaPlayerService extends Service {

	
	private static final String TAG = "MobileMusicShufflerService";
	
	final Messenger _receiver = new Messenger(new IncomingHandler());
	
    private AudioManager mAudioManager;
    private ComponentName mRemoteControlResponder;
    private static Method mRegisterMediaButtonEventReceiver;
    private static Method mUnregisterMediaButtonEventReceiver;
    
    // private static Method mOnAudioFocusChangeListener;

    Notification _notification;
	BroadcastReceiver _sdCardReceiver;

	BroadcastReceiver _allReceiver;

	
	BroadcastReceiver _headsetReceiver;
	BroadcastReceiver _mediaButtonReceiver;
	

	MediaPlayer _mediaPlayer;
	NotificationManager _nm;

	NumberFormat _nf = NumberFormat.getInstance();
	
	boolean _running = false;
	boolean _publishPositionAndDuration = false;
	boolean _pauseOnPhoneCall = false;
	// boolean _HandleBluetoothCommands = false;
	
	Handler _handler = new Handler();

	Song [] _songs = null;
	int _currentSong = -1;
	String _playlistName = "";
	
	boolean _playerIsReset = true;
	boolean _notificationVisible = false;
	
	boolean _showDebugToasts = false;
	boolean _logDebug = true;

	TelephonyManager _telephonyManager;



	ArrayList<Messenger> _clients = new ArrayList<Messenger>();
	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_CURRENT_SONG_INDEX = 3;
	static final int MSG_LOAD_SONGS = 4;
	static final int MSG_PLAYER_STOP = 5;
	static final int MSG_PLAYER_PAUSE = 6;
	static final int MSG_PLAYER_IS_PLAYING = 7;
	static final int MSG_PLAYER_NEXT = 8;
	static final int MSG_PLAYER_PREV = 9;
	static final int MSG_PLAYER_PLAY = 10;
	static final int MSG_CURRENT_SONG = 11;
	static final int MSG_SONG_LIST = 12;
	static final int MSG_SERVICE_RUNNING = 13;
	static final int MSG_TO_BACKGROUND_IF_PAUSED = 13;
	static final int MSG_CURRENT_POSITION_AND_DURATION = 14;
	static final int MSG_SONG_PLAY_BY_ID = 15;
	static final int MSG_PLAYER_NEXT_SONG_BY_SAME_ARTIST = 16;
	static final int MSG_FEED_ME_SOME_SONGS = 17;
	static final int MSG_DIE_IN_A_FIRE = 18;
	static final int MSG_PREFERENCES = 19;
	
	

	static final String KEY_SONGS = "Songs";
	static final String KEY_CURRENT_SONG = "CurrentSong";
	static final String KEY_CURRENT_SONG_INDEX = "CurrentSongIndex";
	static final String KEY_SONG_ID = "SongID";
	static final String KEY_PREF_PAUSE_ON_PHONE_CALL = "PauseOnPhoneCall";
	static final String KEY_PREF_HANDLE_BLUETOOTH = "HandleBluetoothCommands";
	static final String KEY_ARG1 = "arg1";
	static final String KEY_POSITION_SECONDS = "positionSeconds";
	static final String KEY_POSITION_MINUTES = "positionMinutes";
	static final String KEY_DURATION_SECONDS = "durationSeconds";
	static final String KEY_DURATION_MINUTES = "durationMinutes";
	static final String KEY_PLAYLIST_NAME = "PlaylistName";

	
	 
	
	
	private static void initializeRemoteControlRegistrationMethods() {
		   try {
		      if (mRegisterMediaButtonEventReceiver == null) {
		         mRegisterMediaButtonEventReceiver = AudioManager.class.getMethod(
		               "registerMediaButtonEventReceiver",
		               new Class[] { ComponentName.class } );
		      }
		      if (mUnregisterMediaButtonEventReceiver == null) {
		         mUnregisterMediaButtonEventReceiver = AudioManager.class.getMethod(
		               "unregisterMediaButtonEventReceiver",
		               new Class[] { ComponentName.class } );
		      }
		      /* success, this device will take advantage of better remote */
		      /* control event handling                                    */
		   } catch (NoSuchMethodException nsme) {
		      /* failure, still using the legacy behavior, but this app    */
		      /* is future-proof!                                          */
		   }
		}
	
    static {
        initializeRemoteControlRegistrationMethods();
    }
	 
    private void registerRemoteControl() {
        try {
            if (mRegisterMediaButtonEventReceiver == null) {
                return;
            }
            mRegisterMediaButtonEventReceiver.invoke(mAudioManager,
                    mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected " + ie);
        }
    }
    
    private void unregisterRemoteControl() {
        try {
            if (mUnregisterMediaButtonEventReceiver == null) {
                return;
            }
            mUnregisterMediaButtonEventReceiver.invoke(mAudioManager,
                    mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            System.err.println("unexpected " + ie);  
        }
    }
    
//    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
//        public void onAudioFocusChange(int focusChange) {
//            // 
//        	
//        	
//            switch (focusChange) {
//                case AudioManager.AUDIOFOCUS_LOSS:
//                    Log.v(TAG, "AudioFocus: received AUDIOFOCUS_LOSS");
//                    break;
//                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
//                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//                    Log.v(TAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
//                    break;
//                case AudioManager.AUDIOFOCUS_GAIN:
//                    Log.v(TAG, "AudioFocus: received AUDIOFOCUS_GAIN");
//                    break;
//                default:
//                    Log.e(TAG, "Unknown audio focus change code");
//            }
//        }
//    };
    
    
    
	private PhoneStateListener _phoneStateListener = new PhoneStateListener() {

	    @Override
	    public void onCallStateChanged(int state, String incomingNumber) {
	        super.onCallStateChanged(state, incomingNumber);

	        switch (state) 
	        {

		        case TelephonyManager.CALL_STATE_RINGING:
	    			if (_pauseOnPhoneCall)
	    			{
	        			pause();
	        			sendIsPlayingToClients();
	    			}
	                break;

	        }
	    }

	};

	
	
	void sendMessageToClients(Bundle data, int what)
	{
		Message message = Message.obtain(null, what);
		message.setData(data);

		sendMessageToClients(message);
	}
	
	void sendMessageToClients(boolean arg1, int what)
	{
		Bundle bundle = new Bundle(1);
		bundle.putBoolean(KEY_ARG1, arg1);
		sendMessageToClients(bundle, what);
	}
	

	void sendMessageToClients(int arg1, int what)
	{
		Message message = Message.obtain(null, what);
		message.arg1 = arg1;
		sendMessageToClients(message);
	}

	void sendMessageToClients(int what)
	{
		sendMessageToClients(0, what);
	}
		
		

	
	void sendMessageToClients(Message msg)
	{
		 for (int i=_clients.size()-1; i>=0; i--) {
			 try 
			 {
				 _clients.get(i).send(msg);
				 
			 } catch (RemoteException e) {
				 // The client is dead.  Remove it from the list;
				 // we are going through the list from back to front
				 // so this is safe to do inside the loop.
				 _clients.remove(i);
			 }

		 }
	
	}
	
    void showToast(String message)
    {
    	Toast.makeText(MediaPlayerService.this, message, Toast.LENGTH_SHORT).show();
    }

    void setPositionDurationPublishing(boolean publish)
    {
    	if (!publish)
    	{
    		// stop	
    		_publishPositionAndDuration = false;
    		_handler.removeCallbacks(_runnableUpdateSong);

    	}
    	else
    	{
    		// start
    		_publishPositionAndDuration = true;
    		_handler.removeCallbacks(_runnableUpdateSong);
    	    _handler.postDelayed(_runnableUpdateSong, 350);

    	}
    	
    }
	
	
	 class IncomingHandler extends Handler 
	 {
		 @Override
		 public void handleMessage(Message msg) 
		 {
			 
			 switch (msg.what) 
			 {
			 
			 	case MSG_DIE_IN_A_FIRE:
			 		try{
			 		_clients.remove(msg.replyTo);
			 		} catch(Exception e) {}

			 		dieInAFire();
			 		break;
			 
				 case MSG_REGISTER_CLIENT:
					 _clients.add(msg.replyTo);
					 sendMessageToClients(_running, MSG_SERVICE_RUNNING);
					 
					 if (_songs == null || _songs.length == 0)
					 {
						 sendMessageToClients(MSG_FEED_ME_SOME_SONGS);
					 }
					 else
					 {
						 sendSongListToClients();
						 sendIsPlayingToClients();
						 sendCurrentSongToClients();
						 setPositionDurationPublishing(true);
						 
						 if (isPlaying())
							 sendCurrentSongNotification();
							 
						 

					 }
					 
					 break;
	
				 case MSG_UNREGISTER_CLIENT:
					 _clients.remove(msg.replyTo);
					 
					 if (_clients.size() == 0)
						 setPositionDurationPublishing(false);
					 
					 break;

				 case MSG_CURRENT_SONG:
					 sendCurrentSongToClients();
					 break;
					
				 case MSG_CURRENT_SONG_INDEX:
					 int songIndex = msg.getData().getInt(KEY_CURRENT_SONG_INDEX);
					 if (songIndex >= 0 && songIndex < _songs.length)
					 {
						 _currentSong = songIndex;
						 playSong(songIndex);
					 }
					 
					 break;

				 case MSG_SONG_LIST:
					 sendSongListToClients();
					 break;
					 
					 

				 case MSG_PREFERENCES:
					 _pauseOnPhoneCall = msg.getData().getBoolean(KEY_PREF_PAUSE_ON_PHONE_CALL, false);
					 
					 boolean handleBluetoothCommands = msg.getData().getBoolean(KEY_PREF_HANDLE_BLUETOOTH, false);
					 
					 if (handleBluetoothCommands)
						 registerRemoteControl();
					 else
						 unregisterRemoteControl();
					 
					 
					 break;

				 case MSG_SONG_PLAY_BY_ID:
					 int songID = msg.getData().getInt(KEY_SONG_ID);
					 if (songID != -1)
					 {
						 for (int i = 0; i < _songs.length; i++)
						 {
							 if (_songs[i].ID == songID)
							 {
								 _currentSong = i;
								 playSong(i);
								 sendCurrentSongToClients();
								 sendIsPlayingToClients();
								 break;
							 }
						 }
						 
					 }
					 break;
					 
					 
				 case MSG_PLAYER_NEXT_SONG_BY_SAME_ARTIST:
					 nextSongBySameArtist();
					 break;

					 
				 case MSG_LOAD_SONGS:
					 Bundle bundle = msg.getData();
					 _songs = (Song[])bundle.getSerializable(KEY_SONGS);
					 _playlistName = bundle.getString(KEY_PLAYLIST_NAME);
					 if (_songs != null && _songs.length > 0)
					 {
						 clearNotification();

						 stop();
						 _currentSong = 0;
						 sendCurrentSongToClients();
						 sendCurrentSongPositionDurationToClient(_currentSong);
						 sendIsPlayingToClients();
						 
					 }
					 break;

				 case MSG_PLAYER_NEXT:
					 nextSong();
					 sendCurrentSongToClients();
					 break;
					 
				 case MSG_PLAYER_PREV:
					 handlePrevSong();
					 break;

				 case MSG_PLAYER_PAUSE:
					 pause();
					 break;
					 
				 case MSG_PLAYER_IS_PLAYING:
					 sendMessageToClients(isPlaying(), MediaPlayerService.MSG_PLAYER_IS_PLAYING);
					 break;
					 
				 case MSG_PLAYER_PLAY:
					 handlePlayMessage();
					 break;
					 
				 case MSG_PLAYER_STOP:
					 stop();
					 break;
					 
				 case MSG_TO_BACKGROUND_IF_PAUSED:
					 
					 if (!isPlaying())
					 {
						 clearNotification();
						 setPositionDurationPublishing(false);
						 stopForeground(true);
						 
						 _running = false;
						 
						 Log.d(TAG, "sent to background");
						 
					 }
					 break;
					 
					 
					 
				 default:
					 super.handleMessage(msg);
			 }
		 }


		 
		 
		 

		private void sendSongListToClients() {
			Bundle bundle = new Bundle();
			bundle.putSerializable(KEY_SONGS, _songs);
			bundle.putString(KEY_PLAYLIST_NAME, _playlistName);
			sendMessageToClients(bundle, MSG_SONG_LIST);
		}

		private void nextSongBySameArtist() 
		{
			 Song song = getSong(_currentSong);
			 if (song != null)
			 {
				 String artist = song.ARTIST;
				 int currentSong = _currentSong;
				 
				 int foundSondIndex = -1;
				 
				 
				 for (int i = currentSong + 1; i < _songs.length; i++)
				 {
					 if (artist.equalsIgnoreCase(_songs[i].ARTIST))
					 {
						 foundSondIndex = i;
						 break;
					 }
				 }
				 
				 // we got to the end of the array, start back at the beginning
				 if (foundSondIndex == -1)
				 {
					 for (int i = 0; i < currentSong && i < _songs.length; i++)
					 {
						 if (artist.equalsIgnoreCase(_songs[i].ARTIST))
						 {
							 foundSondIndex = i;
							 break;
						 }
					 }
				 }

				 if (foundSondIndex > -1)
				 {
					 _currentSong = foundSondIndex;
					 if (isPlaying())
						 playSong(_currentSong);
					 else
						 stop();

					 sendCurrentSongToClients();
				 }

				 
				 
			 }
		}
	 }

	 
	 private void dieInAFire() 
	 {
		 stop();

		 if (_sdCardReceiver != null)
		 {
			 try 
			 {
				 unregisterReceiver(_sdCardReceiver);
			 } catch(Exception ex) {}
		 }


		 if (_headsetReceiver != null)
		 {
			 try {
				 unregisterReceiver(_headsetReceiver);

			 } catch(Exception ex) {}
		 }
		 if (_mediaButtonReceiver != null)
		 {
			 try {
				 unregisterReceiver(_mediaButtonReceiver);

			 } catch(Exception ex) {}
		 }

		 Log.d(TAG, "I am slain.");

		 stopSelf();
	 }

	
		
	 void handlePrevSong()
	 {
		 // if we're past a couple seconds into the song, seek back to the beginning


		 try {
			 if (isPlaying() && _mediaPlayer.getCurrentPosition() > 2000)
			 {
				 _mediaPlayer.seekTo(0);
			 }
			 else
			 {
				 prevSong();
				 sendCurrentSongToClients();
			 }

		 } catch (Exception e) {
			 e.printStackTrace();
			 Log.e(TAG, "MSG_PLAYER_PREV", e);
		 }


	 }
	
	
	void nextSong()
	{
		_currentSong++;
		if (_currentSong + 1 > _songs.length)
			_currentSong = 0;

		if (isPlaying())
			playSong(_currentSong);
		else
			stop(false);

		
		sendCurrentSongPositionDurationToClient(_currentSong);
		if (_notificationVisible)
			sendCurrentSongNotification();

	}

	boolean playSong(int songIndex)
	{
		final String METHOD = "[playSong] ";
		
		if (_logDebug)
			Log.d(TAG, "playSong(" + songIndex + ")");
		
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
        {
        	if (_logDebug)
        		Log.d(TAG, METHOD + "SD card no longer mounted");
        		
	 		dieInAFire();
        }
		
        
        
		Song song = getSong(songIndex);
		if (song != null)
		{

			
			if (_logDebug)
				Log.d(TAG, METHOD + "song.DATA = " + song.DATA);
			
			
			try
			{
				stop();
				
				if (!_running)
					startService();
				
				_mediaPlayer.setDataSource(song.DATA);
				_mediaPlayer.prepare();
				_mediaPlayer.start();

				_playerIsReset = false;
				
				sendCurrentSongNotification();
				
				setPositionDurationPublishing(true);
				
				return true;

			}
			catch (IOException e)
			{
				Log.e(TAG, "playSong", e);
				songError(songIndex);
			}
			catch (Exception e)
			{
				Log.e(TAG, "playSong", e);
				songError(songIndex);
			}
		}
		else
		{
			if (_logDebug)
				Log.d(TAG, METHOD + "song was null");
		}

		return false;
	}
	
	
	
	Song getSong(int songIndex)
	{
		try
		{
			return _songs[songIndex];
		}
		catch (Exception e)
		{
			return null;
		}
	}

	void stop()
	{
		stop(true);
	}
	
	
	void stop(boolean clearNotification)
	{
		if (_mediaPlayer != null)
		{
			if (isPlaying())
				_mediaPlayer.stop();

			setPositionDurationPublishing(false);

			resetPlayer();
			
		}

		if (clearNotification)
			clearNotification();

	}
	
	void clearNotification()
	{
		_nm.cancelAll();
		_notificationVisible = false;
	}
	
	void resetPlayer()
	{
		_mediaPlayer.reset();
		_playerIsReset = true;
	}
	

	public long getCurrentPosition()
	{
		return _mediaPlayer.getCurrentPosition();
	}
	
	public void seekTo(int msec)
	{
		_mediaPlayer.seekTo(msec);
		sendCurrentSongPositionDurationToClient(_currentSong);
	}
    
	public void pause()
	{
		if (isPlaying())
		{
			_mediaPlayer.pause();
			setPositionDurationPublishing(false);
		}
	}
	
	public void start()
	{
		if (!isPlaying())
		{
			_mediaPlayer.start();
			setPositionDurationPublishing(true);
		}
	}
	



	public void prevSong()
	{
		_currentSong--;
		if (_currentSong == -1 && _songs != null)
			_currentSong = _songs.length - 1;

		if (isPlaying())
			playSong(_currentSong);
		else
			stop(false);
		
		sendCurrentSongPositionDurationToClient(_currentSong);

		if (_notificationVisible)
			sendCurrentSongNotification();


	}


	public boolean isPlaying()
	{
		boolean isPlaying = false;
		
		try {
			isPlaying = _mediaPlayer != null && _mediaPlayer.isPlaying();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return isPlaying;
	}




	public int getCurrentSong()
	{
		return _currentSong;
	}

	public void setCurrentSong(int songIndex)
	{
		_currentSong = songIndex;
	}

	
	void sendCurrentSongNotification()
	{
		Song song = getSong(_currentSong);
		if (song != null)
		{
		
			String text = song.TITLE + "\n" + song.ARTIST;
			
			
			
	        Intent intent = new Intent(this, main.class);
	        intent.setAction(Intent.ACTION_MAIN);
	        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0 );
	        
	        // Set the info for the views that show in the notification panel.
			if (_notification == null)
				_notification = new Notification(R.drawable.ic_menu_play_clip, "", System.currentTimeMillis());

			
	        _notification.when = System.currentTimeMillis();
	        _notification.setLatestEventInfo(getApplicationContext(), getString(R.string.app_name), 
	        		text, contentIntent);
			
	        
			_nm.notify(R.layout.main, _notification);
			
			_notificationVisible = true;
			
			
		}
		
	}
	
	void handlePlayMessage()
	{
		int currentPosition = _mediaPlayer.getCurrentPosition();
		
		if (isPlaying())
		{
			_mediaPlayer.pause();
			sendIsPlayingToClients();
			setPositionDurationPublishing(false);
		}
		else if (currentPosition > 10 && !_playerIsReset)
		{
			// should be in a paused state here
			_mediaPlayer.start();
			sendIsPlayingToClients();
			sendCurrentSongNotification();
			setPositionDurationPublishing(true);
			

		}
		else 
		{
			setPositionDurationPublishing(false);

			
			if (_currentSong == -1 && _songs != null && _songs.length > 0)
				_currentSong = 0;
			
			
			if (_currentSong == -1)
			{
				sendIsPlayingToClients();
			}
			else
			{
				
				if (playSong(_currentSong))
				{
					sendIsPlayingToClients();
					sendCurrentSongToClients();
					setPositionDurationPublishing(true);

				}
				
			}
			
		}
	}
	
	
	
	
	

	
	@Override
	public IBinder onBind(Intent arg0) {
 		return _receiver.getBinder();
	}

	
	@Override
	public void onCreate() {
		
		Log.d(TAG, "onCreate");
		
		if (_showDebugToasts)
			showToast("MediaPlayerService onCreate");
		
		_nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		
		_mediaPlayer = new MediaPlayer();
		_mediaPlayer.setLooping(false);
		
		
		
		
        _mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			
			public void onCompletion(MediaPlayer mp) {
				nextSong();
				playSong(_currentSong);

				sendCurrentSongToClients();
				
			}
		});
        
        _mediaPlayer.setOnErrorListener(new OnErrorListener() {
			
			public boolean onError(MediaPlayer mp, int what, int extra) 
			{
				Log.e(TAG, "MediaPlayer onError " + String.format("what: {0}, extra: {1}", what, extra));

				songError(_currentSong);
				
				return true;
			}
		});
        
        _telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        _telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        
		
        _sdCardReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(TAG, "_sdCardReceiver: onReceive " + intent.getAction());
				
				// I am slain.
				sendMessageToClients(MSG_DIE_IN_A_FIRE);
				dieInAFire();
				
			}
		};
        
        IntentFilter filter = new IntentFilter(); 
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        
        filter.addDataScheme("file");
        registerReceiver(_sdCardReceiver, filter);
        

		// Handles unplugging of a headset/aux cable
        _headsetReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(TAG, "_headsetReceiver: onReceive " + intent.getAction());
				
				int state = intent.getIntExtra("state", 0);
				if (state == 0)
				{
					pause();
					sendIsPlayingToClients();
				}
				
			}
		};
        
        IntentFilter filterHeadset = new IntentFilter(); 
        filterHeadset.addAction(Intent.ACTION_HEADSET_PLUG);
		registerReceiver(_headsetReceiver, filterHeadset);
		
		
		
		 mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
	     mRemoteControlResponder = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());

	     
	     // mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
	     
	     
        _mediaButtonReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Bundle bundle = intent.getExtras();
				handleMediaButton(bundle);
			}
		};
        
        IntentFilter filterMediaButton = new IntentFilter(); 
        filterMediaButton.addAction(RemoteControlReceiver.INTENT_MEDIA_BUTTON);
		registerReceiver(_mediaButtonReceiver, filterMediaButton);


		
		
		
		
		// For seeing what we get		
//        _allReceiver = new BroadcastReceiver() {
//			
//			@Override
//			public void onReceive(Context context, Intent intent) {
//				Log.d(TAG, "_allReceiver onReceive getAction:  " + intent.getAction());
//				Log.d(TAG, "_allReceiver onReceive getDataString:  " + intent.getDataString());
//				Log.d(TAG, "_allReceiver onReceive getPackage:  " + intent.getPackage());
//				
//				Bundle bundle = intent.getExtras();
//				if (bundle != null)
//					Log.d(TAG, "_allReceiver onReceive bundle:  " + bundle.toString());
//
//
//			}
//		};
        
//        IntentFilter allFilter = new IntentFilter();
//		registerReceiver(_allReceiver, allFilter);

		
        
	}

	void handleMediaButton(Bundle bundle)
	{
		int action = bundle.getInt(RemoteControlReceiver.INTENT_MEDIA_BUTTON_KEY_ACTION, 0);
		int keycode = bundle.getInt(RemoteControlReceiver.INTENT_MEDIA_BUTTON_KEY_CODE, 0);
		// long downTime = bundle.getInt(RemoteControlReceiver.INTENT_MEDIA_BUTTON_KEY_DOWNTIME, 0);

		if (action == KeyEvent.ACTION_UP)
		{
		
			switch (keycode)
			{
				case KeyEvent.KEYCODE_HEADSETHOOK:
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					handlePlayMessage();
					break;
			
				case KeyEvent.KEYCODE_MEDIA_STOP:
					stop();
					break;

				case KeyEvent.KEYCODE_MEDIA_NEXT:
					nextSong();
					sendCurrentSongToClients();

					break;

				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					handlePrevSong();
					break;
			}
		}
	
	
	}
	
	
	void songError(int songIndex)
	{
		Song song = getSong(songIndex);
		if (song != null)
			showToast(getString(R.string.error_playing_song) + ": " + song.TITLE + " " + song.ARTIST);
		
		sendIsPlayingToClients(false);
		_nm.cancelAll();

	}
	
	void sendIsPlayingToClients()
	{
		sendIsPlayingToClients(isPlaying());
	}
	
	void sendIsPlayingToClients(boolean playing)
	{
		sendMessageToClients(playing, MSG_PLAYER_IS_PLAYING);
	}

	void sendCurrentSongToClients()
	{
		if (_currentSong >= 0 && _songs != null && _songs.length > 0 && _currentSong < _songs.length)
		{
			Bundle bundle = new Bundle(1);
			bundle.putSerializable(KEY_CURRENT_SONG, getSong(_currentSong));
			
			sendMessageToClients(bundle, MSG_CURRENT_SONG);
		}
	}
	

	void sendCurrentSongIndexToClients()
	{
		sendMessageToClients(_currentSong, MSG_CURRENT_SONG_INDEX);
	}

	
	@Override
	public void onDestroy() 
	{
		
		Log.d(TAG, "onDestroy");

		if (_showDebugToasts)
			showToast("MedaPlayerService onDestroy ");

		try {
			if (_telephonyManager != null)
				_telephonyManager.listen(_phoneStateListener, PhoneStateListener.LISTEN_NONE);
		} catch (Exception e) {
		}

		

		try {
			if (_mediaPlayer != null)
				_mediaPlayer.release();
		} catch (Exception e) {
		}

		try {
			if (_nm != null)
				_nm.cancelAll();
		} catch (Exception e) {
		}
		
		
		try
		{
			unregisterRemoteControl();
		}
		catch(Exception e) {}

		_running = false;			


		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		return START_STICKY;
	}
	
	
	void startService()
	{
		
		

        // Set the icon, scrolling text and timestamp

		_notification = new Notification(R.drawable.ic_menu_play_clip, "",
	            System.currentTimeMillis());
		
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, main.class), 0);

        // Set the info for the views that show in the notification panel.
        _notification.setLatestEventInfo(this, "",
                       "", contentIntent);

        _notification.flags |= Notification.FLAG_ONGOING_EVENT;
        _notification.flags |= Notification.FLAG_NO_CLEAR;
        
        
		startForeground(R.layout.main, _notification);		
		

		
		_running = true;
		

		
		
	}
	


	void sendCurrentSongPositionDurationToClient(int songIndex)
	{
		Song song = getSong(songIndex);
		if (song != null)
		{
		
			long duration = 0;
			try {
				duration = (Long) _nf.parse(song.DURATION);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			long position = 0;
			
			if (!_playerIsReset)
			{
				try {
					position = _mediaPlayer.getCurrentPosition();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			int durationSeconds = (int) (duration / 1000);
			int durationMinutes = durationSeconds / 60;
			durationSeconds     = durationSeconds % 60;

			int positionSeconds = (int) (position / 1000);
			int positionMinutes = positionSeconds / 60;
			positionSeconds     = positionSeconds % 60;

			Bundle bundle = new Bundle(4);
			bundle.putInt(KEY_POSITION_SECONDS, positionSeconds);
			bundle.putInt(KEY_POSITION_MINUTES, positionMinutes);
			bundle.putInt(KEY_DURATION_SECONDS, durationSeconds);
			bundle.putInt(KEY_DURATION_MINUTES, durationMinutes);

			// Log.d(TAG, "Sending pos/dur to clients");

			sendMessageToClients(bundle, MSG_CURRENT_POSITION_AND_DURATION);
		}
	}
	

	private Runnable _runnableUpdateSong = new Runnable() {

		public void run() 
		{

			if (!_publishPositionAndDuration)
				return;

			sendCurrentSongPositionDurationToClient(_currentSong);

			_handler.postDelayed(this, 350);

		}

	};


	
}



