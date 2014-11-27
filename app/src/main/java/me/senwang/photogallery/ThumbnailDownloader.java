package me.senwang.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThumbnailDownloader<Token> extends HandlerThread {
	public static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;

	public interface Listener<T> {
		void onThumbnailDownloaded(T t, Bitmap thumbnail);
	}

	private Handler mHandler;
	private Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());

	private Handler mResponseHandler;
	private Listener<Token> mListener;

	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		mResponseHandler = responseHandler;
	}

	public void setListener(Listener<Token> listener) {
		mListener = listener;
	}

	@Override
	protected void onLooperPrepared() {
		super.onLooperPrepared();
		mHandler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				if (msg.what == MESSAGE_DOWNLOAD) {
					Token token = (Token) msg.obj;
					Log.i(TAG, "Got a request for url: " + requestMap.get(token));
					handleRequest(token);
					return true;
				} else {
					return false;
				}
			}
		});
	}

	public void queueThumbnail(Token token, String url) {
		Log.i(TAG, "Got an URL: " + url);
		requestMap.put(token, url);

		mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
	}

	private void handleRequest(final Token token) {
		final String url = requestMap.get(token);
		if (url == null) {
			return;
		}

		try {
			byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
			final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			Log.i(TAG, "Bitmap created");

			mResponseHandler.post(new Runnable() {
				@Override
				public void run() {
					if (requestMap.get(token) != url) {
						return;
					}

					requestMap.remove(token);
					mListener.onThumbnailDownloaded(token, bitmap);
				}
			});
		} catch (IOException e) {
			Log.e(TAG, "Error downloading image", e);
		}
	}

	public void clearQueue() {
		mHandler.removeMessages(MESSAGE_DOWNLOAD);
		requestMap.clear();
	}
}
