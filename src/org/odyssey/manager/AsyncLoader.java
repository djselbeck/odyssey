package org.odyssey.manager;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * Loaderclass for covers
 */
public class AsyncLoader extends AsyncTask<AsyncLoader.CoverViewHolder, Void, Bitmap> {

	private CoverViewHolder cover;

	/*
	 * Wrapperclass for covers
	 */
	public static class CoverViewHolder {
		public String imagePath;
		// public String labelText;
		public WeakReference<ImageView> coverViewReference;
		public TextView labelView;
		public AsyncLoader task;
		public WeakReference<LruCache<String, Bitmap>> cache;
	}

	@Override
	protected Bitmap doInBackground(CoverViewHolder... params) {

		cover = params[0];

		if (cover.imagePath != null) {
			// Bitmap tempImage = BitmapFactory.decodeFile(params[0].imagePath);
			//
			// return tempImage;

			return decodeSampledBitmapFromResource(params[0].imagePath, cover.coverViewReference.get().getWidth(), cover.coverViewReference.get().getHeight());
		}

		return null;

	}

	public Bitmap decodeSampledBitmapFromResource(String pathName, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(pathName, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		Bitmap retBitmap = BitmapFactory.decodeFile(pathName, options);
		if (cover.cache != null) {
			cover.cache.get().put(cover.imagePath, retBitmap);
		}
		return retBitmap;
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	@Override
	protected void onPostExecute(Bitmap result) {

		super.onPostExecute(result);

		// set cover if exists
		if (cover.coverViewReference != null && result != null) {
			cover.coverViewReference.get().setImageBitmap(result);
		}

		// always set label
		// cover.labelView.setText(cover.labelText);

	}

}