package com.gungeeksoftware.MobileMusicShuffler;

import java.text.Collator;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
// import android.util.Log;


public class SongProvider extends ContentProvider 
{

	String TAG = "SongProvider";

	Context _context;
	
	public SongProvider() {
	
	}
	
	public SongProvider(Context context) {
		_context = context;
	}
	
	
	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		
		
		 
		
		return 0;
	}

	@Override
	public String getType(Uri arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri arg0, ContentValues arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		_context = getContext();
		return false;
	}

	@Override
	public Cursor query(Uri arg0, String[] arg1, String arg2, String[] searchQuery,
			String arg4) {

		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		
		String query = "";

		if (searchQuery != null && searchQuery.length > 0)
			query = searchQuery[0];
		
		if (query.length() < 2)
			return null;
		
		
		StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Media.TITLE + " != '' AND ");
        where.append(MediaStore.Audio.Media.IS_MUSIC + " != 0 AND ");
        
        // Add in the filtering constraints
        String [] keywords = null;
        String [] searchWords = null;
        
        if (query != null) 
        {
            searchWords = query.split(" ");
            keywords = new String[searchWords.length];
            
            Collator col = Collator.getInstance();
				
            col.setStrength(Collator.IDENTICAL);
            
            
            for (int i = 0; i < searchWords.length; i++) {
                // keywords[i] = '%' + MediaStore.Audio.keyFor(searchWords[i]) + '%';
            	
            	if (searchWords[i].contains("%"))
            		searchWords[i] = searchWords[i].replace("%", "");
            		
            	keywords[i] = '%' + searchWords[i] + '%';
            }
            
            where.append(" ( ");
            for (int i = 0; i < searchWords.length; i++) 
            {
            	if (i != 0)
            		where.append(" AND ");
            	where.append(" (");
                where.append(MediaStore.Audio.Media.ARTIST + "||");
                where.append(MediaStore.Audio.Media.TITLE + " LIKE ?");
                where.append(")");
            }
            where.append(" ) ");
        }
        
		
		String selection =  where.toString(); // " title != '' AND (artist||title like ?)"; where.toString();  // "  (artist||title like ? AND artist||title like ?) "; // where.toString();
		
		// Log.d(TAG, "selection: " + selection);
		
		String[] projection = {
				BaseColumns._ID,   
                MediaStore.Audio.Media.MIME_TYPE, 
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.IS_MUSIC
		};
		
		

		
		
			
		Cursor cursor = _context.getContentResolver().query(
				uri,
				projection,
				selection,
				keywords,
				MediaStore.Audio.Media.TITLE);


		MatrixCursor songCursor = new MatrixCursor(new String[] {
		   BaseColumns._ID,
		   SearchManager.SUGGEST_COLUMN_TEXT_1,
		   SearchManager.SUGGEST_COLUMN_TEXT_2,
		   SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
		});
		
		while(cursor.moveToNext())
		{
			String title = cursor.getString(4);
			String artist = cursor.getString(2);
			int id = cursor.getInt(0);
			
			// if (stringContainsAllKeywords(artist + " " + title, searchWords))
				songCursor.addRow(new Object[] { id , title, artist, id });
		}
			

		cursor.close();		
		
		
		return songCursor;

	}
	
	
	
//	boolean stringContainsAllKeywords(String value, String [] keywords)
//	{
//		if (keywords == null || keywords.length == 0)
//			return false;
//		
//		value = value.toLowerCase();
//
//		for(String s : keywords)
//		{
//			if (!value.contains(s.toLowerCase()))
//				return false;
//		}
//
//		return true;
//		
//	}
	

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	
	
}
