package me.senwang.photogallery;

import android.app.Fragment;
import android.app.SearchManager;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;


public class PhotoGalleryActivity extends SingleFragmentActivity {

	public static final String TAG = PhotoGalleryActivity.class.getSimpleName();

	public static final String PREF_SEARCH_QUERY = "searchQuery";

	@Override
	protected Fragment createFragment() {
		return new PhotoGalleryFragment();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		PhotoGalleryFragment fragment = (PhotoGalleryFragment) getFragmentManager()
				.findFragmentById(R.id.fragment_container);
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			Log.i(TAG, "Received a new search query: " + query);

			PreferenceManager.getDefaultSharedPreferences(this)
					.edit()
					.putString(PREF_SEARCH_QUERY, query)
					.commit();
		}
		fragment.updateItems();
	}
}