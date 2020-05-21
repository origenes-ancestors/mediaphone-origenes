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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ac.robinson.mediaphone.MediaPhoneActivity;
import ac.robinson.mediaphone.R;
import ac.robinson.mediaphone.ancestors.api.ApiAuthentication;
import ac.robinson.mediaphone.ancestors.api.OrigenesApi;
import ac.robinson.mediaphone.ancestors.api.SearchService;
import ac.robinson.mediaphone.ancestors.models.SearchResultItem;
import ac.robinson.mediaphone.ancestors.models.ThumbnailImage;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoListActivity extends MediaPhoneActivity {

	private static final int ANCESTOR_DETAIL_VIEW = 423;
	private static final int PAGE_START = 0; // the number at which page numbers start (normally 0 or 1)
	private static final int ITEMS_PER_PAGE = 10; // make sure to match server

	private SearchView mSearchView;
	private ProgressBar mProgressBar;

	private PaginationAdapter mPaginationAdapter;

	private boolean mIsLoading;
	private boolean mIsLastPage;
	private int mCurrentPage;

	private String mCurrentQuery;

	private SearchService mSearchService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ancestor_list);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mSearchView = findViewById(R.id.search_view);
		mProgressBar = findViewById(R.id.main_progress);

		RecyclerView recyclerView = findViewById(R.id.recycler_view);
		mPaginationAdapter = new PaginationAdapter(new PaginationAdapter.ResultClickListener() {
			@Override
			public void onClick(ThumbnailImage result) {
				Intent ancestorIntent = new Intent(PhotoListActivity.this, PhotoDetailActivity.class);
				ancestorIntent.putExtra(PhotoDetailActivity.PHOTO_ID, result.getId());
				startActivityForResult(ancestorIntent, ANCESTOR_DETAIL_VIEW);
			}
		});

		GridLayoutManager gridLayoutManager = new GridLayoutManager(PhotoListActivity.this, 2);
		gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
			@Override
			public int getSpanSize(int position) {
				switch (mPaginationAdapter.getItemViewType(position)) {
					case PaginationAdapter.LOADING:
						return 2; // loading item takes up two cells
					case PaginationAdapter.ITEM:
						return 1;
					default:
						return -1;
				}
			}
		});
		recyclerView.setLayoutManager(gridLayoutManager);
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.setAdapter(mPaginationAdapter);
		recyclerView.addOnScrollListener(new PaginationScrollListener(gridLayoutManager) {
			@Override
			protected void loadMoreItems() {
				mIsLoading = true;
				mCurrentPage += 1;
				loadNextPage();
			}

			@Override
			public boolean isLastPage() {
				return mIsLastPage;
			}

			@Override
			public boolean isLoading() {
				return mIsLoading;
			}
		});

		mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				submitQuery(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String query) {
				if (TextUtils.isEmpty(query)) {
					submitQuery(query);
				}
				return true;
			}

			private void submitQuery(String query) {
				mCurrentQuery = query;
				loadFirstPage();
			}
		});

		((EditText) findViewById(R.id.login_text)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					handleLogin(v.getText().toString());
				}
				return false;
			}
		});

		mSearchService = OrigenesApi.getRetrofitClient().create(SearchService.class);
	}

	private Call<SearchResultItem> callSearchResultApi() {
		ApiAuthentication auth = ApiAuthentication.getInstance(PhotoListActivity.this);
		return mSearchService.getSearchResults(auth.getKey(), auth.getPassword(), OrigenesApi.TYPE_SEARCH, mCurrentPage,
				TextUtils
				.isEmpty(mCurrentQuery) ? "" : mCurrentQuery);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.logout, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		switch (itemId) {
			case R.id.menu_logout:
				handleLogin(null);
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void loginClick(View view) {
		switch (view.getId()) {
			case R.id.login_button:
				handleLogin(((EditText) findViewById(R.id.login_text)).getText().toString());
				break;
		}
	}

	private void handleLogin(String password) {
		ApiAuthentication.resetInstance();
		SharedPreferences mediaPhoneSettings = PreferenceManager.getDefaultSharedPreferences(PhotoListActivity.this);
		SharedPreferences.Editor editor = mediaPhoneSettings.edit();
		editor.putString(getString(R.string.key_api_password), password);
		editor.apply();

		((EditText) findViewById(R.id.login_text)).setText("");

		if (!TextUtils.isEmpty(password)) {
			findViewById(R.id.login_frame).setVisibility(View.GONE);
			loadFirstPage();
		} else {
			findViewById(R.id.login_frame).setVisibility(View.VISIBLE);
		}
	}

	private void handleSearchResponse(@NonNull Response<SearchResultItem> response, boolean firstPage) {
		if (!response.isSuccessful()) {
			if (response.errorBody() != null) {
				try {
					String responseString = response.errorBody().string();
					if (OrigenesApi.RESPONSE_AUTHENTICATION_REQUIRED.equals(responseString)) {
						handleLogin(null);
					}
				} catch (IOException e) {
					// TODO: handle failure? (unexpected exception)
				}
			} else {
				// TODO: handle failure? (unexpected null response)
			}
			return;
		}

		if (!firstPage) {
			mPaginationAdapter.removeLoadingFooter();
			mIsLoading = false;
		}

		List<ThumbnailImage> results;
		SearchResultItem resultsList = response.body();
		if (resultsList != null) {
			results = resultsList.getResults();
		} else {
			results = new ArrayList<>(); // empty list is better than failing
		}

		if (firstPage) {
			mProgressBar.setVisibility(View.GONE);
		}
		mPaginationAdapter.addAll(results);

		if (results.size() >= ITEMS_PER_PAGE) {
			mPaginationAdapter.addLoadingFooter();
		} else {
			mIsLastPage = true;
		}
	}

	private void loadFirstPage() {
		OrigenesApi.getOkHttpClient().dispatcher().cancelAll(); // cancel any previous queries

		mPaginationAdapter.clear();
		mIsLoading = false;
		mIsLastPage = false;
		mCurrentPage = PAGE_START;
		mProgressBar.setVisibility(View.VISIBLE);

		callSearchResultApi().enqueue(new Callback<SearchResultItem>() {
			@Override
			public void onResponse(@NonNull Call<SearchResultItem> call, @NonNull Response<SearchResultItem> response) {
				handleSearchResponse(response, true);
			}

			@Override
			public void onFailure(@NonNull Call<SearchResultItem> call, @NonNull Throwable t) {
				// TODO: handle failure (response == "error")
			}
		});
	}

	private void loadNextPage() {
		callSearchResultApi().enqueue(new Callback<SearchResultItem>() {
			@Override
			public void onResponse(@NonNull Call<SearchResultItem> call, @NonNull Response<SearchResultItem> response) {
				handleSearchResponse(response, false);
			}

			@Override
			public void onFailure(@NonNull Call<SearchResultItem> call, @NonNull Throwable t) {
				// TODO: handle failure (response == "error")
			}
		});
	}

	@Override
	protected void loadPreferences(SharedPreferences mediaPhoneSettings) {
		// nothing to do here
	}

	@Override
	protected void configureInterfacePreferences(SharedPreferences mediaPhoneSettings) {
		handleLogin(mediaPhoneSettings.getString(getString(R.string.key_api_password), null));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
		switch (requestCode) {
			case ANCESTOR_DETAIL_VIEW:
				if (resultCode != Activity.RESULT_OK) {
					break;
				}
				if (resultIntent != null) {
					if (resultIntent.hasExtra(PhotoDetailActivity.NEW_SEARCH_FILTER)) {
						CharSequence tagSearch = resultIntent.getCharSequenceExtra(PhotoDetailActivity.NEW_SEARCH_FILTER);
						if (tagSearch != null) {
							mSearchView.setQuery(tagSearch, true);
						}
					} else if (resultIntent.hasExtra(getString(R.string.extra_resource_url))) {
						setResult(Activity.RESULT_OK, resultIntent);
						finish();
					} else if (resultIntent.hasExtra(getString(R.string.extra_password_failure))) {
						handleLogin(null);
					}
				}
				break;

			default:
				super.onActivityResult(requestCode, resultCode, resultIntent);
				break;
		}
	}
}
