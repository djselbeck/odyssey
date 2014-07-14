package org.odyssey.views;

import java.lang.ref.WeakReference;

import org.odyssey.R;
import org.odyssey.manager.AsyncLoader;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class GridItem extends RelativeLayout {

    private AsyncLoader.CoverViewHolder mHolder;
    private static final String TAG = "OdysseyAlbumGridItem";
    private boolean mCoverDone = false;

    public GridItem(Context context, String text, String imageURL, android.view.ViewGroup.LayoutParams layoutParams) {
        super(context);
        setLayoutParams(layoutParams);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.item_albums, this, true);
        setLayoutParams(layoutParams);
        ((TextView) this.findViewById(R.id.textViewAlbumItem)).setText(text);

        mHolder = new AsyncLoader.CoverViewHolder();
        mHolder.coverViewReference = new WeakReference<ImageView>((ImageView) this.findViewById(R.id.imageViewAlbum));
        mHolder.coverViewSwitcher = new WeakReference<ViewSwitcher>((ViewSwitcher) this.findViewById(R.id.albumgridSwitcher));
        mHolder.imagePath = imageURL;

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mHolder.task != null) {
            mHolder.task.cancel(true);
            mHolder.task = null;
        }
    }

    public void startCoverImageTask() {
        if (mHolder.imagePath != null && mHolder.task == null && !mCoverDone) {
            mCoverDone = true;
            mHolder.task = new AsyncLoader();
            mHolder.task.execute(mHolder);
        }
    }

}
