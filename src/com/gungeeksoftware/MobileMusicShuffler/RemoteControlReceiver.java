package com.gungeeksoftware.MobileMusicShuffler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
	
	public static final String INTENT_MEDIA_BUTTON = "com.gungeeksoftware.MobileMusicShuffler.MEDIA_BUTTON";
	public static final String INTENT_MEDIA_BUTTON_KEY_CODE = "com.gungeeksoftware.MobileMusicShuffler.MEDIA_BUTTON_KEY_CODE";
	public static final String INTENT_MEDIA_BUTTON_KEY_ACTION = "com.gungeeksoftware.MobileMusicShuffler.MEDIA_BUTTON_KEY_ACTION";
	public static final String INTENT_MEDIA_BUTTON_KEY_DOWNTIME = "com.gungeeksoftware.MobileMusicShuffler.MEDIA_BUTTON_KEY_DOWNTIME";
	
	final String TAG = "RemoteControlReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
    	
    	Log.d(TAG, intent.getAction());
    	
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
        	
        	 KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

        	  Intent i = new Intent();
              i.setAction(INTENT_MEDIA_BUTTON);
              i.putExtra(INTENT_MEDIA_BUTTON_KEY_CODE, event.getKeyCode());
              i.putExtra(INTENT_MEDIA_BUTTON_KEY_ACTION, event.getAction());
              i.putExtra(INTENT_MEDIA_BUTTON_KEY_DOWNTIME, event.getDownTime());
              context.sendBroadcast(i);

        }
    }
    
	
}
