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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ac.robinson.mediaphone.MediaPhoneActivity;
import ac.robinson.mediaphone.R;
import ac.robinson.mediaphone.ancestors.api.ApiAuthentication;
import ac.robinson.mediaphone.ancestors.api.OrigenesApi;
import ac.robinson.mediaphone.ancestors.api.PhotoService;
import ac.robinson.mediaphone.ancestors.models.LinkedPersonItem;
import ac.robinson.mediaphone.ancestors.models.PhotoItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhotoDetailActivity extends MediaPhoneActivity {

	public static final String PHOTO_ID = "photo_id";
	public static final String NEW_SEARCH_FILTER = "new_search_filter";

	private static final int PERSON_DETAIL_VIEW = 424;

	private PhotoService mPhotoService;

	private int mSelectedPhotoId;
	private String mSelectedPhotoUrl;

	// TODO: support zooming via https://github.com/facebook/fresco/tree/master/samples/zoomable/src/main/java/com/facebook
	// TODO: /samples/zoomable
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ancestor_detail);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mPhotoService = OrigenesApi.getRetrofitClient().create(PhotoService.class);

		// TODO: handle screen rotation to avoid repeated queries (note: now less important as we cache all queries)
		loadPhoto();
	}

	private Call<PhotoItem> getPhotoApiCall() {
		ApiAuthentication auth = ApiAuthentication.getInstance(PhotoDetailActivity.this);
		return mPhotoService.getPhoto(auth.getKey(), auth.getPassword(), OrigenesApi.TYPE_PHOTO, mSelectedPhotoId);
	}

	// TODO: remove this ridiculousness and cache views
	private void clearActivity() {
		((SimpleDraweeView) findViewById(R.id.ancestor_image)).setImageResource(0);
		((TextView) findViewById(R.id.ancestor_description)).setText("");
		((TextView) findViewById(R.id.ancestor_tags)).setText("");
		((LinearLayout) findViewById(R.id.photo_person_holder)).removeAllViews();
	}

	private void loadPhoto() {
		Intent intent = getIntent();
		if (!intent.hasExtra(PHOTO_ID)) {
			finish(); // no selection
			return;
		}

		mSelectedPhotoId = intent.getIntExtra(PHOTO_ID, 0);
		if (mSelectedPhotoId <= 0) {
			finish(); // no selection
			return;
		}

		mSelectedPhotoUrl = null;
		getPhotoApiCall().enqueue(new Callback<PhotoItem>() {
			@Override
			public void onResponse(@NonNull Call<PhotoItem> call, @NonNull Response<PhotoItem> response) {
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

				PhotoItem photo = response.body();
				if (photo == null) {
					// TODO: properly handle error (parse failure? photo not found?)
					finish(); // no selection
					return;
				}

				mSelectedPhotoUrl = photo.getImageLink(ApiAuthentication.getInstance(PhotoDetailActivity.this));
				if (!TextUtils.isEmpty(mSelectedPhotoUrl)) {
					((SimpleDraweeView) findViewById(R.id.ancestor_image)).setImageURI(Uri.parse(mSelectedPhotoUrl));
				}
				TextView photoDescriptionView = findViewById(R.id.ancestor_description);
				if (!TextUtils.isEmpty(photo.getDescription())) {
					photoDescriptionView.setText(photo.getDescription());
				} else {
					photoDescriptionView.setVisibility(View.GONE);
				}

				// TODO: should we just return as a string if we're going to join anyway?
				String photoTags = TextUtils.join("|", photo.getTags());
				TextView photoTagsView = findViewById(R.id.ancestor_tags);
				if (!TextUtils.isEmpty(photoTags)) {
					createTags(photoTagsView, photoTags);
				} else {
					photoTagsView.setVisibility(View.GONE);
				}

				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				if (inflater != null) {
					LinearLayout holderLayout = findViewById(R.id.photo_person_holder);
					for (LinkedPersonItem person : photo.getTaggedPeople()) {
						View personLayout = inflater.inflate(R.layout.ancestor_detail_person, holderLayout, false);

						// we don't check for IDs >= 0 here as if a person is in a photo that photo is guaranteed to exist
						SimpleDraweeView personPhoto = personLayout.findViewById(R.id.photo_person_photo);
						personPhoto.setImageURI(Uri.parse(person.getImageLink(ApiAuthentication.getInstance(PhotoDetailActivity.this))));

						((TextView) personLayout.findViewById(R.id.photo_person_text)).setText(person.getName());
						personLayout.setTag(person.getId());

						personLayout.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent ancestorIntent = new Intent(PhotoDetailActivity.this, PersonDetailActivity.class);
								ancestorIntent.putExtra(PersonDetailActivity.PERSON_ID, (int) v.getTag());
								startActivityForResult(ancestorIntent, PERSON_DETAIL_VIEW);
							}
						});

						holderLayout.addView(personLayout);
					}
				}
			}

			@Override
			public void onFailure(@NonNull Call<PhotoItem> call, @NonNull Throwable t) {
				// TODO: handle failure (response == "error")
			}
		});
	}

	private void createTags(TextView tagsView, String tagString) {

		/* TODO: now that base API level is 14, we can use Chip instead of this hacky approach, e.g.:
			<com.google.android.material.chip.Chip
			android:layout_width="wrap_content"
			android:layout_height="wrap_content"
			android:text="@string/hello_world"/>
		 */

		// TODO: alternative for tags view: https://stackoverflow.com/a/49973416/1993220
		String regex = "([^|]+)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(tagString);
		SpannableStringBuilder stringBuilder = new SpannableStringBuilder(tagString);
		final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (layoutInflater != null) {
			while (matcher.find()) {
				final TextView oneWord = (TextView) layoutInflater.inflate(R.layout.ancestor_tag, null);
				final int begin = matcher.start();
				final int end = matcher.end();
				oneWord.setText(tagString.substring(begin, end));
				BitmapDrawable tagDrawable = (BitmapDrawable) convertViewToDrawable(oneWord);
				tagDrawable.setBounds(0, 0, tagDrawable.getIntrinsicWidth(), tagDrawable.getIntrinsicHeight());

				// Resources resources = getResources();
				// int padding = resources.getDimensionPixelSize(R.dimen.ancestor_tag_padding);
				// int margin = resources.getDimensionPixelSize(R.dimen.ancestor_tag_margin);
				// RoundedBackgroundSpan backgroundSpan = new RoundedBackgroundSpan(resources.getColor(R.color
				// .primary),
				// resources.getColor(android.R.color.white), padding, padding, margin, margin);

				stringBuilder.setSpan(new ImageSpan(tagDrawable), begin, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				stringBuilder.setSpan(new ClickableSpan() {
					@Override
					public void onClick(@NonNull View widget) {
						if (!(widget instanceof TextView)) {
							return;
						}
						TextView textView = (TextView) widget;
						if (!(textView.getText() instanceof Spanned)) {
							return;
						}
						widget.playSoundEffect(SoundEffectConstants.CLICK);

						Spanned spanned = (Spanned) textView.getText();
						int start = spanned.getSpanStart(this);
						int end = spanned.getSpanEnd(this);

						Intent result = new Intent();
						result.putExtra(NEW_SEARCH_FILTER, spanned.subSequence(start, end));
						setResult(Activity.RESULT_OK, result);
						finish();
					}
				}, begin, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			replaceAll(stringBuilder, Pattern.compile("\\|"), " ");
			tagsView.setText(stringBuilder);
			tagsView.setMovementMethod(LinkMovementMethod.getInstance());
		}
	}

	public static void replaceAll(SpannableStringBuilder sb, Pattern pattern, String replacement) {
		Matcher m = pattern.matcher(sb);
		int start = 0;
		while (m.find(start)) {
			sb.replace(m.start(), m.end(), replacement);
			start = m.start() + replacement.length();
		}
	}

	public static Object convertViewToDrawable(View view) {
		int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		view.measure(spec, spec);
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		Bitmap tagBackground = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
		Canvas tagCanvas = new Canvas(tagBackground);
		tagCanvas.translate(-view.getScrollX(), -view.getScrollY());
		view.draw(tagCanvas);
		view.setDrawingCacheEnabled(true);
		Bitmap cacheBitmap = view.getDrawingCache();
		Bitmap viewBitmap = cacheBitmap.copy(Bitmap.Config.ARGB_8888, true);
		view.destroyDrawingCache();
		return new BitmapDrawable(viewBitmap);
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
				result.putExtra(getString(R.string.extra_resource_url), mSelectedPhotoUrl);
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
		// TODO: handle image clicks on R.id.ancestor_image SimpleDraweeView (e.g., add cropping visualisation; zooming etc)
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
		switch (requestCode) {
			case PERSON_DETAIL_VIEW:
				if (resultCode != Activity.RESULT_OK) {
					break;
				}
				if (resultIntent != null) {
					if (resultIntent.hasExtra(getString(R.string.extra_resource_url))) {
						setResult(Activity.RESULT_OK, resultIntent);
						finish();

					} else if (resultIntent.hasExtra(PHOTO_ID)) {
						clearActivity();
						Intent newIntent = new Intent();
						newIntent.putExtra(PHOTO_ID, resultIntent.getIntExtra(PHOTO_ID, 0));
						setIntent(newIntent);
						loadPhoto();
					} else if (resultIntent.hasExtra(getString(R.string.extra_password_failure))) {
						setResult(Activity.RESULT_OK, resultIntent);
						finish();
					}
				}
				break;

			default:
				super.onActivityResult(requestCode, resultCode, resultIntent);
				break;
		}
	}
}
