package org.odyssey.manager;

import java.lang.ref.WeakReference;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * Loaderclass for covers
 */
public class AsyncLoader extends
		AsyncTask<AsyncLoader.CoverViewHolder, Void, Drawable> {

	private CoverViewHolder cover;

	/*
	 * Wrapperclass for covers
	 */
	public static class CoverViewHolder {
		public String imagePath;
		// public String labelText;
		public ImageView coverView;
		public TextView labelView;
		public AsyncLoader task;
		public WeakReference<LruCache<String, Drawable>> cache;
	}

	@Override
	protected Drawable doInBackground(CoverViewHolder... params) {

		cover = params[0];

		if (cover.imagePath != null) {
			Drawable tempImage = Drawable.createFromPath(params[0].imagePath);

			return tempImage;
		}

		return null;

	}

	@Override
	protected void onPostExecute(Drawable result) {

		super.onPostExecute(result);

		// set cover if exists
		if (result != null) {
			cover.cache.get().put(cover.imagePath, result);
			cover.coverView.setImageDrawable(result);
		}

		// always set label
		// cover.labelView.setText(cover.labelText);

	}

}