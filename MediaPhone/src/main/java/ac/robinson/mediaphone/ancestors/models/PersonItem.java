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

import ac.robinson.mediaphone.ancestors.api.ApiAuthentication;
import ac.robinson.mediaphone.ancestors.api.OrigenesApi;
import okhttp3.HttpUrl;

public class PersonItem {
	@SerializedName("id")
	@Expose
	private Integer mId;

	@SerializedName("name")
	@Expose
	private String mName;

	@SerializedName("description")
	@Expose
	private String mDescription;

	@SerializedName("photo")
	@Expose
	private Integer mPhoto;

	@SerializedName("appearsIn")
	@Expose
	private List<Integer> mAppearsInInternal = new ArrayList<>();
	private List<ThumbnailImage> mAppearsIn = new ArrayList<>();

	@SerializedName("ancestors")
	@Expose
	private List<LinkedPersonItem> mAncestors = new ArrayList<>();


	public Integer getId() {
		return mId;
	}

	public void setId(Integer id) {
		mId = id;
	}


	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}


	public String getDescription() {
		return mDescription;
	}

	public void setDescription(String description) {
		mDescription = description;
	}


	public Integer getPhoto() {
		return mPhoto;
	}

	public void setPhoto(Integer photo) {
		mPhoto = photo;
	}

	public String getImageLink(ApiAuthentication auth) {
		HttpUrl.Builder builder = OrigenesApi.getAuthenticatedImageBuilder(auth, getPhoto(),
				OrigenesApi.IMAGE_PARAM_SCALE_VALUE_PERSON);
		return builder.build().toString();
	}


	public List<ThumbnailImage> getAllPhotos() {
		if (mAppearsIn.size() <= 0) {
			for (int i : mAppearsInInternal) {
				mAppearsIn.add(new ThumbnailImage(i));
			}
		}
		return mAppearsIn;
	}

	public void setAllPhotos(List<ThumbnailImage> photos) {
		mAppearsInInternal.clear();
		mAppearsIn.clear();
		for (ThumbnailImage p : photos) {
			mAppearsInInternal.add(p.getId());
		}
	}


	public List<LinkedPersonItem> getAncestors() {
		return mAncestors;
	}

	public void setAncestors(List<LinkedPersonItem> ancestors) {
		mAncestors = ancestors;
	}
}
