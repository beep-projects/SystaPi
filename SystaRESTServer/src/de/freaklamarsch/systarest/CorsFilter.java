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
 * Implements {@link ContainerResponseFilter} to add Cross-Origin Resource Sharing (CORS)
 * headers to HTTP responses. This enables client-side JavaScript applications, served from
 * a different origin than this server, to make API calls without being blocked by the
 * browser's same-origin policy.
 *
 * <p>For example, without these headers, a browser might block a fetch request with an error message like:
 * "Access to fetch at 'http://systapi:1337/SystaREST/rawdata' from origin 'http://localhost:3000'
 * has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the
 * requested resource."</p>
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    /**
     * Modifies the outgoing HTTP response to include standard CORS headers.
     * These headers instruct browsers to permit requests from any origin ("*"),
     * allow credentials, specify permissible headers, and define allowed HTTP methods.
     *
     * @param requestContext  The context of the incoming HTTP request (not directly used in this filter but required by the interface).
     * @param responseContext The context of the outgoing HTTP response, used to add headers.
     * @throws IOException    If an I/O error occurs during header manipulation (though unlikely in this implementation).
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
