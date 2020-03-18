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

public class PhotoItem {
	@SerializedName("id")
	@Expose
	private Integer mId;

	@SerializedName("description")
	@Expose
	private String mDescription;

	@SerializedName("tags")
	@Expose
	private String[] mTags;

	@SerializedName("people")
	@Expose
	private List<LinkedPersonItem> mPeople = new ArrayList<>();


	public Integer getId() {
		return mId;
	}

	public void setId(Integer id) {
		mId = id;
	}

	public String getImageLink(ApiAuthentication auth) {
		HttpUrl.Builder builder = OrigenesApi.getAuthenticatedImageBuilder(auth, getId(),
				OrigenesApi.IMAGE_PARAM_SCALE_VALUE_FULL);
		return builder.build().toString();
	}


	public String getDescription() {
		return mDescription;
	}

	public void setDescription(String description) {
		mDescription = description;
	}


	public String[] getTags() {
		return mTags;
	}

	public void setTags(String[] tags) {
		mTags = tags;
	}


	public List<LinkedPersonItem> getTaggedPeople() {
		return mPeople;
	}

	public void setTaggedPeople(List<LinkedPersonItem> people) {
		mPeople = people;
	}
}
