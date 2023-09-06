package com.gungeeksoftware.MobileMusicShuffler;

import java.util.ArrayList;
import java.util.List;

import com.gungeeksoftware.MobileMusicShuffler.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SearchResults extends ListActivity {

	final String TAG = "MobileMediaShuffler SearchResults";
	
	Song [] _songs;
	
	final static String KEY_SONGS = "Songs";
	final static String KEY_QUERY = "Query";
	final static String KEY_SELECTED_SONG_ID = "SelectedSongId";
	
	final static int REQUEST_SHOW_SEARCH_RESULTS = 46432;
	
	final static int RESULT_SONG_SELECTED = 4906;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		Log.d(TAG, "onCreate");
		
		super.onCreate(savedInstanceState);
		
		String query = "";
		Bundle bundle = null;
		
		if (savedInstanceState != null)
			bundle = savedInstanceState;

		if (bundle == null)
			bundle = getIntent().getExtras();
		
		if (bundle != null)
			query = bundle.getString(KEY_QUERY);


		_songs = filterSongs(query);
		
		this.setListAdapter(new SongListAdapter(this));
		
	}

	@Override
	protected void onNewIntent(Intent intent) {

		setIntent(intent);
		handleIntent(intent);
		
		
	}

	void handleIntent(Intent intent)
	{
	
	}
	
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) 
	{

		closeSearchResults((int)id);
	}

	
	void closeSearchResults(int returnSelectedSongId)
	{
	
		Intent intent = new Intent(getBaseContext(), main.class);
		intent.setAction(Intent.ACTION_VIEW);
		// intent.putExtra(KEY_SELECTED_SONG_ID, id);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setData(Uri.parse("content://com.petenelson.MobileMusicShuffler.SongProvider/songs/" + returnSelectedSongId));
		startActivity(intent);
		
		finish();

		
	}






	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			closeSearchResults(0);
			return true;
		}
		else
			return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}








	private Song[] filterSongs(String query) 
	{
		Song [] songs = new Song[0];
		
		if (query == null || query == "")
		{
			AlertDialog dialog = DialogBuilder.getSimpleAlert(this, getString(R.string.error), getString(R.string.query_required), R.drawable.stat_sys_warning);
			dialog.show();
		}
		else
		{
	
			List<Song> list = new ArrayList<Song>();


			SongProvider songProvider = new SongProvider(this);
			
			Cursor cursor = songProvider.query(null, null, null, new String[] { query }, null);
			if (cursor != null)
			{
				startManagingCursor(cursor);

				while(cursor.moveToNext())
				{
					Song song = new Song();
					song.ID = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
					song.TITLE = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
					song.ARTIST = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_2));

					list.add(song);

				}

				Song [] songArr = new Song[list.size()];

				songArr = list.toArray(songArr);

				songs = songArr;

			}
		}
		
		return songs;
		
	}




	class SongListAdapter extends BaseAdapter
	{
		private LayoutInflater _inflater;

		public SongListAdapter(Context context) 
		{
			_inflater = LayoutInflater.from(context);
		}
				 
		
		public int getCount() {
			return _songs.length;
		}

		public Object getItem(int arg0) {
			return _songs[arg0];
		}

		public long getItemId(int arg0) {
			return _songs[arg0].ID;
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder;
			
			 if (convertView == null) 
			 {
				 convertView = _inflater.inflate(R.layout.search_results_item, null);
				 
				 holder = new ViewHolder();
				 holder.Artist = (TextView) convertView.findViewById(R.id.textSearchArtist);
				 holder.Title = (TextView) convertView.findViewById(R.id.textSearchTitle);

				 convertView.setTag(holder);
			 } 
			 else 
			 {
				 holder = (ViewHolder) convertView.getTag();
			 }
			 
		 	 holder.Artist.setText(_songs[position].ARTIST);
		 	 holder.Title.setText(_songs[position].TITLE);
			 
			 return convertView;

		}
		
		
	}
	
	static class ViewHolder 
	{
			 TextView Title;
			 TextView Artist;
	}

	

}
