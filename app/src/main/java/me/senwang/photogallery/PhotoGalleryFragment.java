package me.senwang.photogallery;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

	private static final String TAG = PhotoGalleryFragment.class.getSimpleName();

	private GridView mGridView;
	private ArrayList<GalleryItem> mItems;
	private ThumbnailDownloader<ImageView> mThumbnailThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);

		new FetchItemsTask().execute();
		mThumbnailThread = new ThumbnailDownloader<ImageView>();
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

		mGridView = (GridView) v.findViewById(R.id.grid_view);

		setupAdapter();

		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mThumbnailThread.quit();
		Log.i(TAG, "Background thread destroyed");
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
			return new FlickrFetchr().parseItems();
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

			imageView.setImageResource(R.drawable.ic_launcher);
			GalleryItem item = getItem(position);
			mThumbnailThread.queueThumbnail(imageView, item.getUrl());

			return imageView;
		}
	}
}
