package org.odyssey.views;

import org.odyssey.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.GridView;
import android.widget.LinearLayout;

public class AlbumItem extends LinearLayout {
	private static final String TAG = "OdysseyAlbumItem";
	private int mPosition;
	private final GridView mParentGV;
	
	public AlbumItem(Context context) {
		super(context);
		mParentGV = null;
	}
	
	public AlbumItem(Context context, int position, GridView parent ) {
		super(context);
		mParentGV = parent;
		mPosition  = position;
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.item_albums, this, true);
		
		this.findViewById(R.id.albumMenuButton).setOnClickListener(new MenuButtonHandler());
		setOnClickListener(new ItemClickHandler(this));
		setOnTouchListener(new ItemSelectedHandler(this));
	}
	
	private class ItemClickHandler implements OnClickListener {
		AlbumItem mParentItem;
		
		public ItemClickHandler(AlbumItem item) {
			super();
			mParentItem = item;
		}
		
		@Override
		public void onClick(View v) {
			Log.v(TAG,"Item clicked: " + mPosition);
			mParentGV.performItemClick(mParentItem, mPosition, -1);
		}
		
	}
	
	private class ItemSelectedHandler implements OnTouchListener {
		AlbumItem mParent;
		public ItemSelectedHandler(AlbumItem  parent) {
			super();
			mParent = parent;
		}
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			if ( event.getAction() == MotionEvent.ACTION_DOWN) {
				Log.v(TAG,"Item touched: " + mPosition);
				mParent.setBackground(mParentGV.getSelector());
			}
			return false;
		}
		
	}
	
	private class MenuButtonHandler implements OnClickListener {

		@Override
		public void onClick(View v) {
			Log.v(TAG,"Menu button clicked:" + mPosition);
		}
	}
	
	public void setPosition(int position) {
		mPosition = position;
	}

}
