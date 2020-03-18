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

import ac.robinson.mediaphone.ancestors.models.SearchResultItem;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SearchService {
	@GET(OrigenesApi.API_ENDPOINT)
	Call<SearchResultItem> getSearchResults(@Query(OrigenesApi.PARAM_KEY) String key,
											@Query(OrigenesApi.PARAM_PASSWORD) String password,
											@Query(OrigenesApi.PARAM_TYPE) String type,
											@Query("page") int page,
											@Query("q") String query);
}
