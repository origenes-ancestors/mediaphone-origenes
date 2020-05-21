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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.IOException;

import ac.robinson.mediaphone.MediaPhoneActivity;
import ac.robinson.mediaphone.R;
import ac.robinson.mediaphone.ancestors.api.ApiAuthentication;
import ac.robinson.mediaphone.ancestors.api.OrigenesApi;
import ac.robinson.mediaphone.ancestors.api.PersonService;
import ac.robinson.mediaphone.ancestors.models.LinkedPersonItem;
import ac.robinson.mediaphone.ancestors.models.PersonItem;
import ac.robinson.mediaphone.ancestors.models.ThumbnailImage;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonDetailActivity extends MediaPhoneActivity {

	public static final String PERSON_ID = "person_id";

	private PersonService mPersonService;

	private int mSelectedPersonId;
	private String mSelectedPersonUrl;

	// TODO: support zooming via https://github.com/facebook/fresco/tree/master/samples/zoomable/src/main/java/com
	// /facebook/samples/zoomable
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ancestor_family);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mPersonService = OrigenesApi.getRetrofitClient().create(PersonService.class);

		// TODO: handle screen rotation to avoid repeated queries (note: now less important as we cache all queries)
		loadPerson();
	}

	private Call<PersonItem> callPersonApi() {
		ApiAuthentication auth = ApiAuthentication.getInstance(PersonDetailActivity.this);
		return mPersonService.getPerson(auth.getKey(), auth.getPassword(), OrigenesApi.TYPE_PERSON, mSelectedPersonId);
	}

	// TODO: remove this ridiculousness and cache views
	private void clearActivity() {
		((SimpleDraweeView) findViewById(R.id.ancestor_image)).setImageResource(0);
		((LinearLayout) findViewById(R.id.person_photo_holder)).removeAllViews();
		((TextView) findViewById(R.id.ancestor_name)).setText("");
		((TextView) findViewById(R.id.ancestor_description)).setText("");
		((LinearLayout) findViewById(R.id.person_ancestor_holder)).removeAllViews();
	}

	private void loadPerson() {
		Intent intent = getIntent();
		if (!intent.hasExtra(PERSON_ID)) {
			finish(); // no selection
			return;
		}

		mSelectedPersonId = intent.getIntExtra(PERSON_ID, 0);
		if (mSelectedPersonId <= 0) {
			finish(); // no selection
			return;
		}

		mSelectedPersonUrl = null;
		callPersonApi().enqueue(new Callback<PersonItem>() {
			@Override
			public void onResponse(@NonNull Call<PersonItem> call, @NonNull Response<PersonItem> response) {
				if (!response.isSuccessful()) {
					if (response.errorBody() != null) {
						try {
							String responseString = response.errorBody().string();
							if (OrigenesApi.RESPONSE_AUTHENTICATION_REQUIRED.equals(responseString)) {
								Intent result = new Intent();
								result.putExtra(getString(R.string.extra_password_failure), true);
								setResult(Activity.RESULT_OK, result);
								finish();
							}
						} catch (IOException e) {
							// TODO: handle failure? (unexpected exception)
						}
					} else {
						// TODO: handle failure? (unexpected null response)
					}
					return;
				}

				PersonItem person = response.body();
				if (person == null) {
					// TODO: properly handle error (parse failure? person not found?)
					finish(); // no selection
					return;
				}

				// some people don't have photos - no need to try to load these
				Uri photoUri = null;
				if (person.getPhoto() >= 0) {
					mSelectedPersonUrl = person.getImageLink(ApiAuthentication.getInstance(PersonDetailActivity.this));
					photoUri = Uri.parse(mSelectedPersonUrl);
				}
				// SimpleDraweeView seems to have an issue where the placeholder image isn't shown unless we set a value,
				// so we do that here even if it is null
				((SimpleDraweeView) findViewById(R.id.ancestor_image)).setImageURI(photoUri);

				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				if (inflater != null) {
					LinearLayout holderLayout = findViewById(R.id.person_photo_holder);
					for (ThumbnailImage photo : person.getAllPhotos()) {
						View personLayout = inflater.inflate(R.layout.ancestor_photo, holderLayout, false);

						// we don't check for IDs <= 0 here as if a person is in a photo that photo is guaranteed to exist
						SimpleDraweeView personPhoto = personLayout.findViewById(R.id.photo_person_photo);
						personPhoto.setImageURI(Uri.parse(photo.getImageLink(ApiAuthentication.getInstance(PersonDetailActivity.this))));

						personLayout.setTag(photo.getId());

						personLayout.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent photoIntent = new Intent();
								photoIntent.putExtra(PhotoDetailActivity.PHOTO_ID, (int) v.getTag());
								setResult(Activity.RESULT_OK, photoIntent);
								finish();
							}
						});

						holderLayout.addView(personLayout);
					}
				}

				TextView personNameView = findViewById(R.id.ancestor_name);
				if (!TextUtils.isEmpty(person.getName())) {
					personNameView.setText(person.getName());
				} else {
					personNameView.setVisibility(View.GONE);
				}

				TextView personDescriptionView = findViewById(R.id.ancestor_description);
				if (!TextUtils.isEmpty(person.getDescription())) {
					personDescriptionView.setText(person.getDescription());
				} else {
					personDescriptionView.setVisibility(View.GONE);
				}

				if (inflater != null) {
					LinearLayout holderLayout = findViewById(R.id.person_ancestor_holder);
					for (LinkedPersonItem ancestor : person.getAncestors()) {
						View personLayout = inflater.inflate(R.layout.ancestor_detail_person, holderLayout, false);

						// some ancestors don't have photos - no need to try to load these
						if (person.getId() >= 0) {
							SimpleDraweeView personPhoto = personLayout.findViewById(R.id.photo_person_photo);
							personPhoto.setImageURI(Uri.parse(ancestor.getImageLink(ApiAuthentication.getInstance(PersonDetailActivity.this))));
						}

						((TextView) personLayout.findViewById(R.id.photo_person_text)).setText(getString(R.string.ancestor_relationship, ancestor
								.getName(), ancestor.getRelationship()));
						personLayout.setTag(ancestor.getId());

						personLayout.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								clearActivity();
								Intent newIntent = new Intent();
								newIntent.putExtra(PERSON_ID, (int) v.getTag());
								setIntent(newIntent);
								loadPerson();
							}
						});

						holderLayout.addView(personLayout);
					}
				}
			}

			@Override
			public void onFailure(@NonNull Call<PersonItem> call, @NonNull Throwable t) {
				// TODO: handle failure (response == "error")
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.finished_editing, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_back_without_editing:
			case R.id.menu_finished_editing:
				Intent result = new Intent();
				result.putExtra(getString(R.string.extra_resource_url), mSelectedPersonUrl);
				setResult(Activity.RESULT_OK, result);
				finish();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void loadPreferences(SharedPreferences mediaPhoneSettings) {
		// nothing to do here
	}

	@Override
	protected void configureInterfacePreferences(SharedPreferences mediaPhoneSettings) {
		// nothing to do here
	}

	public void handleButtonClicks(View view) {
		// TODO: anything to do here? (R.id.ancestor_image SimpleDraweeView click)
	}
}
