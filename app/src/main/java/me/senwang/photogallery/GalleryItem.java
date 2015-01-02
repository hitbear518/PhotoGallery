package me.senwang.photogallery;

public class GalleryItem {

	private String mId;
	private String mCaption;
	private String mUrl;
    private String mOwner;

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public String getPhotoPageUrl() {
        return "http://www.flickr.com/photos/" + mOwner + "/" + mId;
    }

    public String getId() {
		return mId;
	}

	public void setId(String id) {
		mId = id;
	}

	public String getCaption() {
		return mCaption;
	}

	public void setCaption(String caption) {
		mCaption = caption;
	}

	public String getUrl() {
		return mUrl;
	}

	public void setUrl(String url) {
		mUrl = url;
	}

	@Override
	public String toString() {
		return mCaption;
	}
}
