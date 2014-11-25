package me.senwang.photogallery;

import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class FlickrFetchr {

	private static final String TAG = FlickrFetchr.class.getSimpleName();

	public static final String END_POINT = "https://api.flickr.com/services/rest/";
	public static final String API_KEY = "a2f8e893d64a620ea9de75216d11576a";
	public static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
	public static final String PARAM_EXTRAS = "extras";

	public static final String EXTRA_SMALL_URL = "url_s";

	public static final String XML_PHOTO = "photo";

	byte[] getUrlBytes(String urlSpec) throws IOException {
		InputStream in = null;
		HttpURLConnection connection = null;
		try {
			URL url = new URL(urlSpec);
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);

			in = connection.getInputStream();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
			}
			return out.toByteArray();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (in != null) {
				in.close();
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public String getUrl(String urlSpec) throws IOException {
		return new String(getUrlBytes(urlSpec));
	}

	public void fetchItems() {
		String url = Uri.parse(END_POINT).buildUpon()
				.appendQueryParameter("method", METHOD_GET_RECENT)
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.build().toString();
		try {
			String xmlString = getUrl(url);
			Log.i(TAG, "Received xml: " + xmlString);
		} catch (IOException e) {
			Log.e(TAG, "Failed to fetch items", e);
		}
	}

	public ArrayList<GalleryItem> parseItems() {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();

		String url = Uri.parse(END_POINT).buildUpon()
				.appendQueryParameter("api_key", API_KEY)
				.appendQueryParameter("method", METHOD_GET_RECENT)
				.appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
				.build().toString();
		try {
			String xmlString = getUrl(url);
			Log.i(TAG, "Received XML: " + xmlString);
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlString));

			parseItems(items, parser);
		} catch (IOException e) {
			Log.e(TAG, "Failed to fetch items", e);
		} catch (XmlPullParserException e) {
			Log.e(TAG, "Failed to fetch items", e);
		}
		return items;

	}

	private void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser) throws IOException, XmlPullParserException {
		int eventType = parser.next();

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG && XML_PHOTO.equals(parser.getName())) {
				String id = parser.getAttributeValue(null, "id");
				String caption = parser.getAttributeValue(null, "title");
				String smallUrl = parser.getAttributeValue(null, EXTRA_SMALL_URL);

				GalleryItem item = new GalleryItem();
				item.setId(id);
				item.setCaption(caption);
				item.setUrl(smallUrl);
				items.add(item);
			}

			eventType = parser.next();
		}
	}
}
