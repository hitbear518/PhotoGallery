package me.senwang.photogallery;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

	private static final String TAG = PhotoGalleryFragment.class.getSimpleName();

	private GridView mGridView;
	private ArrayList<GalleryItem> mItems;
	private ThumbnailDownloader<ImageView> mThumbnailThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);

		updateItems();

		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
		mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
			@Override
			public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
				if (isVisible()) {
					imageView.setImageBitmap(thumbnail);
				}
			}
		});
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

		mGridView = (GridView) v.findViewById(R.id.grid_view);

		setupAdapter();

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GalleryItem item = (GalleryItem) mGridView.getItemAtPosition(position);

                Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
                Intent i = new Intent(getActivity(), PhotoPageActivity.class);
                i.setData(photoPageUri);
                startActivity(i);
            }
        });

		return v;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		MenuItem searchItem = menu.findItem(R.id.menu_item_search);
		SearchView searchView = (SearchView) searchItem.getActionView();

		SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo searchableInfo = searchManager.getSearchableInfo(getActivity().getComponentName());
		searchView.setSearchableInfo(searchableInfo);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		if (PollService.isServiceAlarmOn(getActivity())) {
			toggleItem.setTitle(R.string.stop_polling);
		} else {
			toggleItem.setTitle(R.string.start_polling);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_search:
			getActivity().onSearchRequested();
			return true;
		case R.id.menu_item_clear:
			PreferenceManager.getDefaultSharedPreferences(getActivity())
					.edit()
					.putString(PhotoGalleryActivity.PREF_SEARCH_QUERY, null)
					.commit();
			updateItems();
			return true;
		case R.id.menu_item_toggle_polling:
			boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
			PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
			getActivity().invalidateOptionsMenu();
			return true;
		default:
			return false;
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mThumbnailThread.clearQueue();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailThread.quit();
		Log.i(TAG, "Background thread destroyed");
	}

	public void updateItems() {
		new FetchItemsTask().execute();
	}

	private void setupAdapter() {
		if (getActivity() == null || mGridView == null) return;

		if (mItems != null) {
			mGridView.setAdapter(new GalleryItemAdapter(getActivity(), mItems));
		} else {
			mGridView.setAdapter(null);
		}
	}

	private class FetchItemsTask extends AsyncTask<Void, Void, ArrayList<GalleryItem>> {
		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {
			Activity activity = getActivity();
			if (activity == null) {
				return new ArrayList<GalleryItem>();
			}

			String query = PreferenceManager.getDefaultSharedPreferences(getActivity())
					.getString(PhotoGalleryActivity.PREF_SEARCH_QUERY, null);

			if (query != null) {
				return new FlickrFetchr().search(query);
			} else {
				return new FlickrFetchr().fetchItems();
			}
		}

		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			mItems = items;
			setupAdapter();
		}
	}

	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {

		public GalleryItemAdapter(Context context, List<GalleryItem> objects) {
			super(context, R.layout.gallery_item, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView imageView;

			if (convertView == null) {
				imageView = (ImageView) LayoutInflater.from(getContext()).inflate(R.layout.gallery_item, parent, false);
			} else {
				imageView = (ImageView) convertView;
			}

			imageView.setImageResource(android.R.drawable.progress_indeterminate_horizontal);
			GalleryItem item = getItem(position);
			mThumbnailThread.queueThumbnail(imageView, item.getUrl());

			return imageView;
		}
	}
}
