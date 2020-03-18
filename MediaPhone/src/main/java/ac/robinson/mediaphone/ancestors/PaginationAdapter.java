/*
 *  Copyright (C) 2020 Simon Robinson
 *
 *  This file is part of Com-Me.
 *
 *  Com-Me is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  Com-Me is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with Com-Me.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ac.robinson.mediaphone.ancestors;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import ac.robinson.mediaphone.R;
import ac.robinson.mediaphone.ancestors.api.ApiAuthentication;
import ac.robinson.mediaphone.ancestors.models.ThumbnailImage;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// based on https://github.com/Suleiman19/Android-Pagination-with-RecyclerView/pull/4
public class PaginationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	static final int ITEM = 0;
	static final int LOADING = 1;

	private List<ThumbnailImage> mAncestorResults;

	private ResultClickListener mResultClickListener;
	private boolean mLoadingFooterAdded = false;

	interface ResultClickListener {
		void onClick(ThumbnailImage result);
	}

	PaginationAdapter(ResultClickListener clickListener) {
		mResultClickListener = clickListener;
		mAncestorResults = new ArrayList<>();
	}

	public List<ThumbnailImage> getAncestors() {
		return mAncestorResults;
	}

	public void setAncestors(List<ThumbnailImage> movieResults) {
		this.mAncestorResults = movieResults;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		RecyclerView.ViewHolder viewHolder;
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		switch (viewType) {
			case ITEM:
				viewHolder = getViewHolder(parent, inflater);
				break;
			case LOADING:
			default:
				View loadingView = inflater.inflate(R.layout.ancestor_list_progress, parent, false);
				viewHolder = new LoadingViewHolder(loadingView);
				break;
		}
		return viewHolder;
	}

	@NonNull
	private RecyclerView.ViewHolder getViewHolder(ViewGroup parent, LayoutInflater inflater) {
		RecyclerView.ViewHolder viewHolder;
		View listItem = inflater.inflate(R.layout.ancestor_list_item, parent, false);
		viewHolder = new AncestorVewHolder(listItem);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		ThumbnailImage result = mAncestorResults.get(position);
		switch (getItemViewType(position)) {
			case ITEM:
				final AncestorVewHolder ancestorViewHolder = (AncestorVewHolder) holder;
				ancestorViewHolder.mAncestorImage.setImageURI(result.getImageLink(ApiAuthentication.getInstance(ancestorViewHolder.mAncestorImage
						.getContext())), SquareRelativeLayout.VIEW_WIDTH);
				break;
			case LOADING:
				// Do nothing
				break;
		}
	}

	@Override
	public int getItemCount() {
		return mAncestorResults == null ? 0 : mAncestorResults.size();
	}

	@Override
	public int getItemViewType(int position) {
		return (position == mAncestorResults.size() - 1 && mLoadingFooterAdded) ? LOADING : ITEM;
	}


	public void add(ThumbnailImage item) {
		mAncestorResults.add(item);
		notifyItemInserted(mAncestorResults.size() - 1);
	}

	void addAll(List<ThumbnailImage> itemList) {
		for (ThumbnailImage item : itemList) {
			add(item);
		}
	}

	public void remove(ThumbnailImage item) {
		int position = mAncestorResults.indexOf(item);
		if (position > -1) {
			mAncestorResults.remove(position);
			notifyItemRemoved(position);
		}
	}

	public boolean isEmpty() {
		return getItemCount() == 0;
	}

	void clear() {
		mLoadingFooterAdded = false;
		while (getItemCount() > 0) {
			remove(getItem(0));
		}
	}

	void addLoadingFooter() {
		mLoadingFooterAdded = true;
		add(new ThumbnailImage(-1)); // temporary item
	}

	void removeLoadingFooter() {
		mLoadingFooterAdded = false;

		int position = mAncestorResults.size() - 1;
		ThumbnailImage result = getItem(position);

		if (result != null && result.getId() == -1) {
			mAncestorResults.remove(position);
			notifyItemRemoved(position);
		}
	}

	private ThumbnailImage getItem(int position) {
		return mAncestorResults.get(position);
	}


	protected class AncestorVewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		private SimpleDraweeView mAncestorImage;

		AncestorVewHolder(View itemView) {
			super(itemView);
			mAncestorImage = itemView.findViewById(R.id.ancestor_list_image);
			itemView.setOnClickListener(AncestorVewHolder.this);
		}

		@Override
		public void onClick(View view) {
			mResultClickListener.onClick(mAncestorResults.get(getLayoutPosition()));
		}
	}

	protected static class LoadingViewHolder extends RecyclerView.ViewHolder {
		LoadingViewHolder(View itemView) {
			super(itemView);
		}
	}
}
