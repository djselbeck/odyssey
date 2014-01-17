package org.odyssey.manager;

import java.lang.ref.WeakReference;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

/*
 * Loaderclass for covers
 */
public class AsyncLoader extends AsyncTask<AsyncLoader.CoverViewHolder, Void, Drawable> {

	private CoverViewHolder cover; 
	
	/*
	 * Wrapperclass for covers
	 */
	public static class CoverViewHolder {
		public String imagePath;
		public ImageView coverView;
		public WeakReference<LruCache<String, Drawable>> coverCache;
	}

	@Override
	protected Drawable doInBackground(CoverViewHolder... params) {

		cover = params[0];
		
		Drawable tempImage = Drawable.createFromPath(params[0].imagePath);
		
		return tempImage;
		
	}
	
	@Override
	protected void onPostExecute(Drawable result){
		
		super.onPostExecute(result);
		
		cover.coverCache.get().put(cover.imagePath, result);
		
		cover.coverView.setImageDrawable(result);	
		
	}
	
}