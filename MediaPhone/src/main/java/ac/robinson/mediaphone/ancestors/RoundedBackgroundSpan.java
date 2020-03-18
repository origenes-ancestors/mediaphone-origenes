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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;

public class RoundedBackgroundSpan extends ReplacementSpan {


	private final int mBackgroundColor;
	private final int mTextColor;
	private final int mPaddingLeft;
	private final int mPaddingRight;
	private final int mMarginLeft;
	private final int mMarginRight;

	/**
	 * Add rounded background for text in TextView.
	 *
	 * @param backgroundColor background color
	 * @param textColor       text color
	 * @param paddingLeft     padding left(including background)
	 * @param paddingRight    padding right(including background)
	 * @param marginLeft      margin left(not including background)
	 * @param marginRight     margin right(not including background)
	 */
	public RoundedBackgroundSpan(int backgroundColor, int textColor, int paddingLeft, int paddingRight, int marginLeft,
								 int marginRight) {
		mBackgroundColor = backgroundColor;
		mTextColor = textColor;
		mPaddingLeft = paddingLeft;
		mPaddingRight = paddingRight;
		mMarginLeft = marginLeft;
		mMarginRight = marginRight;
	}

	@Override
	public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
		return (int) (mMarginLeft + mPaddingLeft + paint.measureText(text, start, end) + mPaddingRight + mMarginRight);
	}

	@Override
	public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
					 @NonNull Paint paint) {
		float width = paint.measureText(text, start, end);
		RectF rect = new RectF(
				x + mMarginLeft, top, x + width + mMarginLeft + mPaddingLeft + mPaddingRight, bottom - mMarginLeft);
		paint.setColor(mBackgroundColor);
		canvas.drawRoundRect(rect, rect.height() / 4, rect.height() / 4, paint);
		paint.setColor(mTextColor);
		canvas.drawText(text, start, end, x + mMarginLeft + mPaddingLeft, y + paint.getFontMetricsInt().descent / 2f, paint);
	}
}
