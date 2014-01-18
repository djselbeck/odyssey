package org.odyssey.manager;


import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;

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
		public String labelText;
		public ImageView coverView;
		public TextView labelView;
		public AsyncLoader task;
	}

	@Override
	protected Drawable doInBackground(CoverViewHolder... params) {

		cover = params[0];
		
		if(cover.imagePath != null)
		{
			Drawable tempImage = Drawable.createFromPath(params[0].imagePath);
			
			return tempImage;
		}
		
		return null;
		
	}
	
	@Override
	protected void onPostExecute(Drawable result){
		
		super.onPostExecute(result);
		
		// set cover if exists
		if(result != null){		
			cover.coverView.setImageDrawable(result);	
		}
		
		// always set label
		cover.labelView.setText(cover.labelText);
		
	}
	
}