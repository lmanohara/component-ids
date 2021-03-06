/*******************************************************************************
 * Copyright (c) 2015-2016, WSO2.Telco Inc. (http://www.wso2telco.com) 
 * 
 * All Rights Reserved. WSO2.Telco Inc. licences this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.wso2telco.entity;

import java.io.IOException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
//import org.apache.log4j.Logger;

import com.wso2telco.util.EncryptAES;
import com.wso2telco.util.FileUtil;

 
// TODO: Auto-generated Javadoc
 
 
/**
 * The Class Endpoints.
 */
@Path("/")
public class Endpoints {

	// private static final Logger LOG =
	 
	/** Removed the below log impl to facilitate PRODEV-173 - operator-proxy-service- standardize
	/** The Constant file. 
	// Logger.getLogger(Endpoints.class.getName());
	private static final File file = new File(FileUtil.getApplicationProperty("log_file"));
	*/
	
    /** The log. */
    private static Log log = LogFactory.getLog(Endpoints.class); 
	 
	 
	/**
	 * Instantiates a new endpoints.
	 */
	public Endpoints() {

	}

	 
	 
	/**
	 * Redirect to authorize endpoint.
	 *
	 * @param hsr the hsr
	 * @param response the response
	 * @param headers the headers
	 * @param ui the ui
	 * @param jsonBody the json body
	 * @throws SQLException the SQL exception
	 * @throws RemoteException the remote exception
	 * @throws Exception the exception
	 */
	@GET
	@Path("/oauth2/authorize")
	// @Produces("application/json")
	public void RedirectToAuthorizeEndpoint(@Context HttpServletRequest hsr, @Context HttpServletResponse response,
			@Context HttpHeaders headers, @Context UriInfo ui, String jsonBody) throws SQLException, RemoteException,
			Exception {

		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();

		String queryString = "";
		for (Entry<String, List<String>> entry : queryParams.entrySet()) {
			queryString = queryString + entry.getKey().toString() + "=" + entry.getValue().get(0) + "&";
		}

		// Read the operator from config
		String operator = FileUtil.getApplicationProperty("operator");

		String msisdn = null;
		String ipAddress = null;

		if (headers != null) {
			for (String header : headers.getRequestHeaders().keySet()) {
				if (log.isDebugEnabled()) {
					log.debug("Header:" + header + "Value:" + headers.getRequestHeader(header));
				}
			}
		}

		if (headers.getRequestHeader(FileUtil.getApplicationProperty("msisdn_header")) != null) {
			msisdn = headers.getRequestHeader(FileUtil.getApplicationProperty("msisdn_header")).get(0);
		} else {
			// nop
		}

		if (headers.getRequestHeader(FileUtil.getApplicationProperty("ip_header")) != null) {
			ipAddress = headers.getRequestHeader(FileUtil.getApplicationProperty("ip_header")).get(0);
		}
		// Encrypt MSISDN
		msisdn = EncryptAES.encrypt(msisdn);

		// URL encode
		msisdn = URLEncoder.encode(msisdn, "UTF-8");

		String authorizeURL = null;

		// Reconstruct AuthURL
		if (ipAddress != null) {

			authorizeURL = FileUtil.getApplicationProperty("authorizeURL") + queryString + "msisdn_header=" + msisdn
					+ "&" + "ipAddress=" + ipAddress + "&" + "operator=" + operator;
		} else {
			authorizeURL = FileUtil.getApplicationProperty("authorizeURL") + queryString + "msisdn_header=" + msisdn
					+ "&" + "operator=" + operator;
		}
		if (log.isDebugEnabled()) {
			log.debug("authorizeURL : " + authorizeURL);
		}

		response.sendRedirect(authorizeURL);

	}

	 
	/** Removed the below log impl to facilitate PRODEV-173 - operator-proxy-service- standardize
	/**
	 * Log.
	 *
	 * @param text the text
	 
	private static void log(String text) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
			out.println(text);
		} catch (IOException e) {
			System.err.println(e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	 */
	 
	/**
	 * Call oauth2.
	 *
	 * @param oauthURL the oauth url
	 * @param msisdn the msisdn
	 * @param ipAddress the ip address
	 * @return the string
	 */
	private String callOauth2(String oauthURL, String msisdn, String ipAddress) {
		String redirectURL = null;

		try {
			HttpGet get = new HttpGet(oauthURL);

			CloseableHttpClient httpclient = null;
			CloseableHttpResponse response = null;

			httpclient = HttpClients.createDefault();

			get.setHeader("msisdn", msisdn);
			get.setHeader("ipaddress", ipAddress);

			response = httpclient.execute(get);
			
			if (log.isDebugEnabled()) {
			log.debug("Status = " + response.getStatusLine().toString());
			}

			Header[] headers = response.getAllHeaders();

			for (Header header : headers) {
				
				if (log.isDebugEnabled()) {
					log.debug("Key : " + header.getName() + " ,Value : " + header.getValue());
				}
				if (header.getName().equals("Location")) {
					redirectURL = header.getValue();
				}
			}

		} catch (IOException ex) {
			// // LOG.info("Error occurred " + ex);
			log.error("Error occurred " + ex);
		}
		return redirectURL;
	}

}