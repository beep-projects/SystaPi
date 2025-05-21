/*
* Copyright (c) 2021, The beep-projects contributors
* this file originated from https://github.com/beep-projects
* Do not remove the lines above.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see https://www.gnu.org/licenses/
*
*/
package de.freaklamarsch.systarest;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * A filter that adds Cross-Origin Resource Sharing (CORS) headers to HTTP responses.
 * This allows client-side JavaScript to make fetch calls to the server without being blocked by the browser's
 * same-origin policy.
 *
 * <p>Without this filter, browsers like Chrome will block fetch attempts with messages like:
 * "Access to fetch at 'http://systapi:1337/SystaREST/rawdata' from origin 'null'
 * has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is
 * present on the requested resource. If an opaque response serves your needs,
 * set the request's mode to 'no-cors' to fetch the resource with CORS disabled."</p>
 */

@Provider
public class CorsFilter implements ContainerResponseFilter {

    /**
     * Adds CORS headers to the HTTP response to allow cross-origin requests.
     *
     * @param requestContext the context of the incoming HTTP request
     * @param responseContext the context of the outgoing HTTP response
     * @throws IOException if an I/O error occurs
     */
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
		responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
		responseContext.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
		responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
	}
}
