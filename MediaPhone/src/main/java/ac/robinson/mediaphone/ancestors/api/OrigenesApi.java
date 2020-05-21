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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.File;
import java.io.IOException;

import ac.robinson.mediaphone.MediaPhone;
import ac.robinson.mediaphone.MediaPhoneApplication;
import ac.robinson.util.IOUtilities;
import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OrigenesApi {
	private static OkHttpClient sOkHttp = null;
	private static Retrofit sRetrofit = null;

	public static final String BASE_URL = "https://digitaleconomytoolkit.org/origenes/";
	public static final String API_ENDPOINT = "api.php";

	// global response bodies (failures)
	public static final String RESPONSE_ERROR = "error";
	public static final String RESPONSE_AUTHENTICATION_REQUIRED = "auth";

	// global query parameters
	public static final String PARAM_KEY = "k";
	public static final String PARAM_PASSWORD = "p";
	public static final String PARAM_ID = "id";
	public static final String PARAM_TYPE = "type";

	// global type options
	public static final String TYPE_SEARCH = "search";
	public static final String TYPE_PHOTO = "photo";
	public static final String TYPE_PERSON = "person";
	public static final String TYPE_IMAGE = "image";

	// image parameters
	public static final String IMAGE_PARAM_SCALE = "scale";
	public static final String IMAGE_PARAM_SCALE_VALUE_FULL = "full"; // the full-resolution source image (watermarked)
	public static final String IMAGE_PARAM_SCALE_VALUE_THUMB = "thumb"; // a thumbnail-sized version of the image (smart crop)
	public static final String IMAGE_PARAM_SCALE_VALUE_PERSON = "person"; // a thumbnail-sized version of a person's photo

	public static OkHttpClient getOkHttpClient() {
		if (sOkHttp == null) {
			OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
			if (MediaPhone.DEBUG) {
				clientBuilder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
			}

			// cache results - see: https://stackoverflow.com/a/52943003/1993220 (all other methods are outdated and not working)
			final Context context = MediaPhoneApplication.mCacheContext.get();
			File cacheDirectory = IOUtilities.getNewCachePath(context, MediaPhone.APPLICATION_NAME + "cache", true, false);
			if (context != null && cacheDirectory != null) {
				Cache cache = new Cache(cacheDirectory, 20 * 1024 * 1024); // 20 MB cache size
				clientBuilder.addInterceptor(sOfflineInterceptor);
				clientBuilder.addNetworkInterceptor(sOnlineInterceptor);
				clientBuilder.cache(cache);
			}

			sOkHttp = clientBuilder.build();
		}
		return sOkHttp;
	}

	public static Retrofit getRetrofitClient() {
		if (sRetrofit == null) {
			sRetrofit = new Retrofit.Builder().client(getOkHttpClient())
					.addConverterFactory(GsonConverterFactory.create())
					.baseUrl(BASE_URL)
					.build();
		}
		return sRetrofit;
	}

	public static HttpUrl.Builder getAuthenticatedImageBuilder(ApiAuthentication auth, int imageId, String imageScale) {
		HttpUrl.Builder builder = getRetrofitClient().baseUrl().newBuilder();
		builder.addPathSegment(API_ENDPOINT);
		builder.addQueryParameter(PARAM_KEY, auth.getKey());
		builder.addQueryParameter(PARAM_PASSWORD, auth.getPassword());
		builder.addQueryParameter(PARAM_TYPE, TYPE_IMAGE);
		builder.addQueryParameter(OrigenesApi.PARAM_ID, String.valueOf(imageId));
		builder.addQueryParameter(OrigenesApi.IMAGE_PARAM_SCALE, imageScale);
		return builder;
	}

	private static Interceptor sOnlineInterceptor = new Interceptor() {
		@Override
		public okhttp3.Response intercept(Chain chain) throws IOException {
			okhttp3.Response response = chain.proceed(chain.request());
			int maxAge = 60; // read from cache for 60 seconds even if there is internet connection
			return response.newBuilder().header("Cache-Control", "public, max-age=" + maxAge).removeHeader("Pragma").build();
		}
	};

	private static Interceptor sOfflineInterceptor = new Interceptor() {
		@Override
		public okhttp3.Response intercept(Chain chain) throws IOException {
			Request request = chain.request();

			final Context context = MediaPhoneApplication.mCacheContext.get();
			boolean isOnline = context != null && isOnline(context);

			if (!isOnline) {
				int maxStale = 60 * 60 * 24 * 30; // offline cache available for 30 days
				request = request.newBuilder()
						.header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
						.removeHeader("Pragma")
						.build();
			}
			return chain.proceed(request);
		}
	};

	private static boolean isOnline(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = null;
		if (connectivityManager != null) {
			networkInfo = connectivityManager.getActiveNetworkInfo();
		}
		return (networkInfo != null && networkInfo.isConnected());
	}
}
