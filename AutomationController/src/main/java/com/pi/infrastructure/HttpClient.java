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
	private String url = null;
	
	public HttpClient(String host) throws MalformedURLException
	{
		if(host == null)
			throw new MalformedURLException();
		
		this.url = host;
	}
	
	/**
	 * @param queryParams (arg1=var1&arg2=var2)
	 * @return Response body
	 * @throws IOException 
	 */
	public Response sendGet(String queryParams) throws IOException
	{
		queryParams = (queryParams == null?"":queryParams);
		
		HttpURLConnection connection = (HttpURLConnection) new URL(url + "?" + queryParams).openConnection();
		
		connection.setRequestMethod("GET");
		
		String responseBody = sendRequest(connection);
		
		return new Response(responseBody, connection.getResponseCode());
	}

	/**
	 * @param queryParams (arg1=var1&arg2=var2)
	 * @param body of Post
	 * @throws IOException 
	 */
	public Response sendPost(String queryParams, String resquestBody) throws IOException
	{
		queryParams = (queryParams == null?"":queryParams);
		resquestBody = (resquestBody == null?"":resquestBody);
		
		HttpURLConnection connection = (HttpURLConnection) new URL(url + "?" + queryParams).openConnection();
		
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		
		DataOutputStream output = new DataOutputStream(connection.getOutputStream());
		output.writeBytes(resquestBody);
		output.flush();
		output.close();
		
		String responseBody = sendRequest(connection);
		
		return new Response(responseBody, connection.getResponseCode());
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
		return responseBody;
	}
	
	public static List<NameValuePair> parseURLEncodedData(String data)
	{
		return URLEncodedUtils.parse(data, Charset.forName("utf8"));
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
	}
}
