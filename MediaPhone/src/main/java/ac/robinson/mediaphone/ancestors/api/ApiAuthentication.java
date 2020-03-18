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

package ac.robinson.mediaphone.ancestors.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import ac.robinson.mediaphone.R;

public class ApiAuthentication {
	private static ApiAuthentication sApiAuthentication = null;

	private final String mKey;
	private final String mPassword;

	private ApiAuthentication(String key, String password) {
		mKey = key;
		mPassword = password;
	}

	public String getKey() {
		return mKey;
	}

	public String getPassword() {
		return mPassword;
	}

	public static ApiAuthentication getInstance(Context context) {
		if (sApiAuthentication == null) {
			SharedPreferences mediaPhoneSettings = PreferenceManager.getDefaultSharedPreferences(context);
			String password = mediaPhoneSettings.getString(context.getString(R.string.key_api_password), null);
			sApiAuthentication = new ApiAuthentication(context.getString(R.string.api_key), TextUtils.isEmpty(password) ? "" :
					password);
		}
		return sApiAuthentication;
	}

	public static void resetInstance() {
		sApiAuthentication = null;
	}
}
