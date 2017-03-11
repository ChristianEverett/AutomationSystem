/**
 * 
 */
package com.pi.infrastructure.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
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
	private static final String APPLICATION_JSON = "application/json";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String GET = "GET";
	private static final String POST = "POST";
	private String baseUrl = null;

	public HttpClient(String host) throws MalformedURLException
	{
		if (host == null)
			throw new MalformedURLException();

		this.baseUrl = host;
	}

	/**
	 * @param queryParams
	 *            (arg1=var1&arg2=var2)
	 * @return Response body
	 * @throws IOException
	 */
	public Response sendGet(String queryParams, String path) throws IOException
	{
		HttpURLConnection connection = createHTTPConnection(queryParams, path);

		connection.setRequestMethod(GET);

		String responseBody = sendRequest(connection);

		return new Response(responseBody, connection.getResponseCode());
	}

	/**
	 * @param queryParams
	 *            (arg1=var1&arg2=var2)
	 * @param path
	 * @param resquestBody
	 * @throws IOException
	 */
	public Response sendPostJson(String queryParams, String path, String resquestBody) throws IOException
	{
		HttpURLConnection connection = createHTTPConnection(queryParams, path);

		connection.setRequestMethod(POST);
		connection.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
		connection.setDoOutput(true);

		try (DataOutputStream output = new DataOutputStream(connection.getOutputStream()))
		{
			output.writeBytes(resquestBody);
			output.flush();

			String responseBody = sendRequest(connection);

			return new Response(responseBody, connection.getResponseCode());
		}
		catch (IOException e)
		{
			throw e;
		}
	}

	/**
	 * @param queryParams
	 *            (arg1=var1&arg2=var2)
	 * @return Response body
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public ObjectResponse sendGetObject(String queryParams, String path) throws IOException, ClassNotFoundException
	{
		Object responseBody = null;
		HttpURLConnection connection = createHTTPConnection(queryParams, path);

		connection.setRequestMethod(GET);

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
		{
			try (ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream()))
			{
				responseBody = inputStream.readObject();
			}
			catch (IOException e)
			{
				throw e;
			}
		}

		return new ObjectResponse(responseBody, connection.getResponseCode());
	}

	/**
	 * @param queryParams
	 *            (arg1=var1&arg2=var2)
	 * @param path
	 * @param resquestBody
	 *            java object
	 * @throws Exception
	 */
	public ObjectResponse sendPostObject(String queryParams, String path, Serializable resquestBody) throws Exception
	{
		Object responseBody = null;
		HttpURLConnection connection = createHTTPConnection(queryParams, path);

		connection.setRequestMethod(POST);
		connection.setDoOutput(true);

		try (ObjectOutputStream output = new ObjectOutputStream(connection.getOutputStream()))
		{
			output.writeObject(resquestBody);
			output.flush();
		}
		catch (IOException e)
		{
			throw e;
		}

		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
		{
			try (ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream()))
			{
				responseBody = inputStream.readObject();
			}
			catch (EOFException e)
			{
			}
			catch (IOException | ClassNotFoundException e)
			{
				throw e;
			}
		}

		return new ObjectResponse(responseBody, connection.getResponseCode());
	}

	/**
	 * @param queryParams
	 * @param path
	 * @return HttpURLConnection
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private HttpURLConnection createHTTPConnection(String queryParams, String path) throws IOException, MalformedURLException
	{
		String url = (path == null ? baseUrl : baseUrl + path);
		url += (queryParams == null || queryParams.equals("") ? "" : "?" + queryParams);

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		return connection;
	}

	private String sendRequest(HttpURLConnection connection) throws IOException
	{
		String responseBody = "";
		try (Scanner body = new Scanner(new BufferedReader(new InputStreamReader(connection.getInputStream()))))
		{
			switch (connection.getResponseCode())
			{
			case HttpURLConnection.HTTP_OK:
				while (body.hasNextLine())
				{
					responseBody += body.nextLine();
				}
				break;

			default:
				break;
			}

			return responseBody;
		}
		catch (IOException e)
		{
			throw e;
		}
	}

	public static List<NameValuePair> parseURLEncodedData(String data)
	{
		return URLEncodedUtils.parse(data, Charset.forName("utf8"));
	}

	public static String URLEncodeData(List<NameValuePair> params)
	{
		return URLEncodedUtils.format(params, Charset.forName("utf8"));
	}

	public static HashMap<String, String> URLEncodedDataToHashMap(String data)
	{
		HashMap<String, String> map = new HashMap<>();

		List<NameValuePair> pairs = parseURLEncodedData(data);

		for (NameValuePair nameValuePair : pairs)
		{
			map.put(nameValuePair.getName(), nameValuePair.getValue());
		}

		return map;
	}

	public class Response
	{
		private String response;
		private int statusCode;

		public Response(String responseBody, int statusCode)
		{
			this.response = responseBody;
			this.statusCode = statusCode;
		}

		/**
		 * @return the reponseBody
		 */
		public String getReponseBody()
		{
			return response;
		}

		/**
		 * @return the statusCode
		 */
		public int getStatusCode()
		{
			return statusCode;
		}

		public boolean isHTTP_OK()
		{
			return HttpURLConnection.HTTP_OK == statusCode;
		}

		public List<NameValuePair> parseURLEncodedData()
		{
			return URLEncodedUtils.parse(response, Charset.forName("utf8"));
		}
	}

	public class ObjectResponse extends Response
	{
		private Object responseObject = null;

		public ObjectResponse(Object responseBody, int statusCode)
		{
			super("", statusCode);
			responseObject = responseBody;
		}

		/**
		 * @return the responseObject
		 */
		public Object getResponseObject()
		{
			return responseObject;
		}
	}
}
