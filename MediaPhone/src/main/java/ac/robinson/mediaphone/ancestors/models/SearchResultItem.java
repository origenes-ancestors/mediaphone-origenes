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

package ac.robinson.mediaphone.ancestors.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SearchResultItem {
	@SerializedName("page")
	@Expose
	private Integer mPage;

	@SerializedName("results")
	@Expose
	private List<Integer> mResultsInternal = new ArrayList<>();
	private List<ThumbnailImage> mResults = new ArrayList<>();


	public Integer getPage() {
		return mPage;
	}

	public void setPage(Integer page) {
		mPage = page;
	}


	public List<ThumbnailImage> getResults() {
		if (mResults.size() <= 0) {
			for (int i : mResultsInternal) {
				mResults.add(new ThumbnailImage(i));
			}
		}
		return mResults;
	}

	public void setResults(List<ThumbnailImage> photos) {
		mResultsInternal.clear();
		mResults.clear();
		for (ThumbnailImage p : photos) {
			mResultsInternal.add(p.getId());
		}
	}
}
