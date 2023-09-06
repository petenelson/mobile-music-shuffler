package com.gungeeksoftware.MobileMusicShuffler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

 public class DialogBuilder {

	 static public AlertDialog getSimpleAlert(Context context, String title, String message, int resIcon)
	    {

		 	return getSimpleAlert(context, title, message, resIcon,
		 		
 				new DialogInterface.OnClickListener() {
				
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
		 		}
		 	);
		 	
		 
	    }

	 static public AlertDialog getSimpleAlert(Context context, String title, String message, int resIcon, DialogInterface.OnClickListener okButtonListener)
	     {
		 
	    	AlertDialog dialog = new AlertDialog.Builder(context).create();
			if (resIcon != 0)
				dialog.setIcon(resIcon);
			
			dialog.setTitle(title);
			dialog.setMessage(message);

			dialog.setButton("OK", okButtonListener);
			
			return dialog;
		 
	    }	 

 }

 