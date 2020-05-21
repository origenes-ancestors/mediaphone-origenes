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

import javax.annotation.Nullable;

import ac.robinson.mediaphone.ancestors.api.ApiAuthentication;
import ac.robinson.mediaphone.ancestors.api.OrigenesApi;
import okhttp3.HttpUrl;

public class LinkedPersonItem {
	@SerializedName("id")
	@Expose
	private Integer mId;

	@SerializedName("name")
	@Expose
	private String mName;

	@SerializedName("photo")
	@Expose
	private Integer mPhoto;

	// note: this object is reused in various places; in some cases photoCrop will be null as it does not apply
	@SerializedName("photoCrop")
	@Expose
	@Nullable
	private String mPhotoCrop;

	// note: this object is reused in various places; in some cases relationship will be null as it does not apply
	@SerializedName("relationship")
	@Expose
	@Nullable
	private String mRelationship;


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


	public String getPhotoCrop() {
		return mPhotoCrop;
	}

	public void setPhotoCrop(String photoCrop) {
		mPhotoCrop = photoCrop;
	}


	public String getRelationship() {
		return mRelationship;
	}

	public void setRelationship(String relationship) {
		mRelationship = relationship;
	}
}
