package org.odyssey.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.odyssey.databasemodel.ArtistModel;
import org.odyssey.views.GridItem;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.SectionIndexer;

public class ArtistsAdapter extends BaseAdapter implements SectionIndexer {

    private static final String TAG = "OdysseyArtistsAdapter";

    private Context mContext;
    private ArrayList<String> mSectionList;
    private ArrayList<Integer> mSectionPositions;
    private HashMap<Character, Integer> mPositionSectionMap;

    private List<ArtistModel> mModelData;

    private GridView mRootGrid;

    private int mScrollSpeed = 0;

    public ArtistsAdapter(Context context, GridView rootGrid) {
        super();

        mSectionList = new ArrayList<String>();
        mSectionPositions = new ArrayList<Integer>();
        mPositionSectionMap = new HashMap<Character, Integer>();
        mModelData = new ArrayList<ArtistModel>();
        mContext = context;
        mRootGrid = rootGrid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ArtistModel artist = mModelData.get(position);
        String label = artist.getArtistName();
        String imageURL = artist.getArtURL();

        if (convertView != null) {
            GridItem gridItem = (GridItem) convertView;
            gridItem.setText(label);
            gridItem.setImageURL(imageURL);
        } else {
            convertView = new GridItem(mContext, label, imageURL, new android.widget.AbsListView.LayoutParams(mRootGrid.getColumnWidth(), mRootGrid.getColumnWidth()));
        }

        if (mScrollSpeed == 0) {
            ((GridItem) convertView).startCoverImageTask();
        }
        return convertView;
    }

    /**
     * Swaps the model of this adapter. This sets the dataset on which the
     * adapter creates the GridItems. This should generally be safe to call.
     * Clears old section data and model data and recreates sectionScrolling
     * data.
     * 
     * @param albums
     *            Actual model data
     */
    public void swapModel(List<ArtistModel> artists) {
        Log.v(TAG, "Swapping data model");
        if (artists == null) {
            mModelData.clear();
        } else {
            mModelData = artists;
        }
        // create sectionlist for fastscrolling

        mSectionList.clear();
        mSectionPositions.clear();
        mPositionSectionMap.clear();
        if (mModelData.size() > 0) {
            char lastSection = 0;

            ArtistModel currentArtist = mModelData.get(0);

            lastSection = currentArtist.getArtistName().toUpperCase().charAt(0);

            mSectionList.add("" + lastSection);
            mSectionPositions.add(0);
            mPositionSectionMap.put(lastSection, mSectionList.size() - 1);

            for (int i = 1; i < getCount(); i++) {

                currentArtist = mModelData.get(i);

                char currentSection = currentArtist.getArtistName().toUpperCase().charAt(0);

                if (lastSection != currentSection) {
                    mSectionList.add("" + currentSection);

                    lastSection = currentSection;
                    mSectionPositions.add(i);
                    mPositionSectionMap.put(currentSection, mSectionList.size() - 1);
                }

            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (sectionIndex >= 0 && sectionIndex < mSectionPositions.size()) {
            return mSectionPositions.get(sectionIndex);
        }
        return 0;
    }

    @Override
    public int getSectionForPosition(int pos) {

        ArtistModel artist = (ArtistModel) getItem(pos);

        String artistsName = artist.getArtistName();

        char artistSection = artistsName.toUpperCase().charAt(0);

        if (mPositionSectionMap.containsKey(artistSection)) {
            int sectionIndex = mPositionSectionMap.get(artistSection);
            return sectionIndex;
        }

        return 0;
    }

    @Override
    public Object[] getSections() {

        return mSectionList.toArray();
    }

    @Override
    public int getCount() {
        return mModelData.size();
    }

    @Override
    public Object getItem(int position) {
        return mModelData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setScrollSpeed(int speed) {
        mScrollSpeed = speed;
    }

}