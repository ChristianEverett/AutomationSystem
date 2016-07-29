/**
 * 
 */
package com.pi.infrastructure;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * @author Christian Everett
 *
 */
public class HttpClient
{
	private String baseUrl = null;
	
	public HttpClient(String host) throws MalformedURLException
	{
		if(host == null)
			throw new MalformedURLException();
		
		this.baseUrl = host;
	}
	
	/**
	 * @param queryParams (arg1=var1&arg2=var2)
	 * @return Response body
	 * @throws IOException 
	 */
	public Response sendGet(String queryParams, String path) throws IOException
	{
		HttpURLConnection connection = createHTTPConnection(queryParams, path);
		
		connection.setRequestMethod("GET");
		
		String responseBody = sendRequest(connection);
		
		return new Response(responseBody, connection.getResponseCode());
	}

	/**
	 * @param queryParams (arg1=var1&arg2=var2)
	 * @param body of Post
	 * @throws IOException 
	 */
	public Response sendPost(String queryParams, String path, String resquestBody) throws IOException
	{
		HttpURLConnection connection = createHTTPConnection(queryParams, path);
		
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		
		DataOutputStream output = new DataOutputStream(connection.getOutputStream());
		output.writeBytes(resquestBody);
		output.flush();
		output.close();
		
		String responseBody = sendRequest(connection);
		
		return new Response(responseBody, connection.getResponseCode());
	}
	
	/**
	 * @param queryParams
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private HttpURLConnection createHTTPConnection(String queryParams, String path) throws IOException, MalformedURLException
	{
		String url = (path == null?baseUrl:baseUrl + path);
		url += (queryParams == null?"": "?" + queryParams);
		
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		return connection;
	}
	
	private String sendRequest(HttpURLConnection connection) throws IOException
	{
		String responseBody = "";
		Scanner body = new Scanner(new BufferedReader(new InputStreamReader(connection.getInputStream())));
		
		switch (connection.getResponseCode())
		{
		case HttpURLConnection.HTTP_OK:
			while(body.hasNextLine())
			{
				responseBody += body.nextLine();
			}
			break;

		default:
			break;
		}
		body.close();
		
		return responseBody;
	}
	
	public static List<NameValuePair> parseURLEncodedData(String data)
	{
		return URLEncodedUtils.parse(data, Charset.forName("utf8"));
	}
	
	public static String URLEncodeData(List<NameValuePair> params)
	{
		return URLEncodedUtils.format(params, Charset.forName("utf8"));
	}
	
	public class Response
	{
		String reponseBody;
		int statusCode;
		
		public Response(String responseBody, int statusCode)
		{
			this.reponseBody = responseBody;
			this.statusCode = statusCode;
		}
		
		/**
		 * @return the reponseBody
		 */
		public String getReponseBody()
		{
			return reponseBody;
		}
		/**
		 * @return the statusCode
		 */
		public int getStatusCode()
		{
			return statusCode;
		}
		
		public List<NameValuePair> parseURLEncodedData()
		{
			return URLEncodedUtils.parse(reponseBody, Charset.forName("utf8"));
		}
	}
}
