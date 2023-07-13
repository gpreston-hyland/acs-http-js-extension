package com.alfresco.se.extension.js.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.net.MalformedURLException;
import org.json.*;

import com.alfresco.se.aws.auth.AWS4SignerBase;
import com.alfresco.se.aws.auth.AWS4SignerForAuthorizationHeader;
import com.alfresco.se.aws.util.BinaryUtils;
//import com.alfresco.se.http_js.platformsample.DemoComponent;
import com.alfresco.se.aws.util.HttpUtils;

//import software.amazon.awssdk.utils.BinaryUtils;

import org.alfresco.repo.processor.BaseProcessorExtension;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class HTTPRequest extends BaseProcessorExtension {

    private static Log logger = LogFactory.getLog(HTTPRequest.class);
    
	private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    
    public String get(String urlString) throws IOException {
      return execute(urlString, (String)null, (String)null, GET, (String)null, (String)null);
    }
    
    public InputStream getStream(String urlString) throws IOException {
      return executeGetStream(urlString, (String)null, (String)null, GET, (String)null, (String)null);
    }
    
    public String get(String urlString, String name, String password) throws IOException {
      return execute(urlString, (String)null, (String)null, GET, name, password);
    }
    
    public InputStream getStream(String urlString, String name, String password) throws IOException {
      return executeGetStream(urlString, (String)null, (String)null, GET, name, password);
    }
    
    public String post(String urlString, String content, String contentType, String name, String password) throws IOException {
      return execute(urlString, content, contentType, POST, name, password);
    }
    
    public InputStream postStream(String urlString, String content, String contentType, String name, String password) throws IOException {
      return executeGetStream(urlString, content, contentType, POST, name, password);
    }
    
    public String postaws(String urlString, String content, String contentType, String awsAuthData ) throws IOException {
    	return executeAWS(urlString, content, contentType, POST, awsAuthData);
    }
    
    public String put(String urlString, String content, String contentType, String name, String password) throws IOException {
      return execute(urlString, content, contentType, PUT, name, password);
    }
    
    public InputStream putStream(String urlString, String content, String contentType, String name, String password) throws IOException {
      return executeGetStream(urlString, content, contentType, PUT, name, password);
    }
    
    public String execute(String urlString, String content, String contentType, String httpMethod, String name, String password) throws IOException {
      HttpURLConnection urlConnection = buildConnection(urlString, httpMethod);
      if (name != null && password != null)
        setHttpBasicAuthentication(urlConnection, name, password); 
      if (httpMethod.equals(POST) && content != null && content.length() > 0) {
        urlConnection.setDoOutput(true);
        writeRequestContent(urlConnection, content, contentType);
      } else if (httpMethod.equals(PUT) && content != null && content.length() > 0) {
        urlConnection.setDoOutput(true);
        writeRequestContent(urlConnection, content, contentType);
      } else {
        urlConnection.setDoOutput(false);
      } 
      return sendRequest(urlConnection);
    }
    
    public InputStream executeGetStream(String urlString, String content, String contentType, String httpMethod, String name, String password) throws IOException {
      HttpURLConnection urlConnection = buildConnection(urlString, httpMethod);
      if (name != null && password != null)
        setHttpBasicAuthentication(urlConnection, name, password); 
//      if ("POST".equals(httpMethod) && content != null && content.length() > 0) {
      if (httpMethod.equals(POST) && content != null && content.length() > 0) {
        urlConnection.setDoOutput(true);
        writeRequestContent(urlConnection, content, contentType);
      } else if (httpMethod.equals(PUT) && content != null && content.length() > 0) {
        urlConnection.setDoOutput(true);
        writeRequestContent(urlConnection, content, contentType);
      } else {
        urlConnection.setDoOutput(false);
      } 
      return sendRequestGetStream(urlConnection);
    }
    
    public String executeAWS(String urlString, String content, String contentType, String httpMethod, String awsAuthData) {
    	
    	URL endpointUrl = null;
    	try {
    		endpointUrl = new URL(urlString);
    	} catch (MalformedURLException e) {
    		logger.error("** Unable to parse AWS Service Endpoint: " + e.getMessage());
    	}
    	JSONObject authData = new JSONObject(awsAuthData);
    	
    	byte[] contentHash = AWS4SignerBase.hash(content);
    	String contentHashString = BinaryUtils.toHex(contentHash);
    	
    	Map<String,String> headers = new HashMap<String, String>();
    	headers.put("x-amz-content-sha256", contentHashString);
    	headers.put("content-length","" + content.length());
    	if (contentType == null || contentType.length() == 0 ) {
    		headers.put("content-type", "application/x-amz-json-1.1");
    	} else {
    		headers.put("content-type", contentType);
    	}
    	headers.put("x-amz-target", authData.getString("amzTarget"));
    	
    	AWS4SignerForAuthorizationHeader signer = new AWS4SignerForAuthorizationHeader(endpointUrl, httpMethod,
    			authData.getString("service"),authData.getString("region"));
    	
    	String authorization = signer.computeSignature(headers, null, contentHashString, authData.getString("accessKey"), authData.getString("secretKey"));
    	headers.put("Authorization", authorization);
    	String response = HttpUtils.invokeHttpRequest(endpointUrl, httpMethod, headers, content);
    	System.out.println(response.toString());
    	
    	return response;
    }
    private void writeRequestContent(HttpURLConnection urlConnection, String content, String contentType) throws IOException {
      if (contentType != null)
        urlConnection.setRequestProperty("Content-Type", contentType); 
      OutputStreamWriter out = null;
      try {
        out = new OutputStreamWriter(urlConnection.getOutputStream(), "ASCII");
        out.write(content);
        out.flush();
      } finally {
        if (out != null)
          try {
            out.close();
          } catch (Exception exception) {} 
      } 
    }
    
    private void setHttpBasicAuthentication(HttpURLConnection urlConnection, String name, String password) {
      String authString = name + ":" + name;
      byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
      String authStringEnc = new String(authEncBytes);
      urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
    }
    
    private String sendRequest(HttpURLConnection urlConnection) throws IOException {
      return readResponse(sendRequestGetStream(urlConnection));
    }
    
    private InputStream sendRequestGetStream(HttpURLConnection urlConnection) throws IOException {
      int responseCode = urlConnection.getResponseCode();
      if (responseCode != 200 && responseCode != 201)
        throw new IOException("RESPONSE CODE:" + responseCode + "\n" + urlConnection.getResponseMessage()); 
      return urlConnection.getInputStream();
    }
    
    private HttpURLConnection buildConnection(String urlString, String httpMethod) throws IOException {
      URL url = new URL(urlString);
      HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
      urlConnection.setRequestMethod(httpMethod);
      return urlConnection;
    }
    
    private String readResponse(InputStream is) throws IOException {
      String var3;
      StringBuffer sb = new StringBuffer();
      try {
        int i;
        while ((i = is.read()) != -1)
          sb.append((char)i); 
        var3 = sb.toString();
      } finally {
        try {
          is.close();
        } catch (IOException iOException) {}
      } 
      return var3;
    }




//    public void main(String[] args) {
////      try {
////        HttpRequest req = new HttpRequest();
////        System.out.println(req.get("http://192.168.99.223:3333/usersarray"));
////        System.out.println("------");
////        System.out.println(req.get("http://192.168.99.223:9090/activiti-app/api/enterprise/process-definitions", "rui", "rui"));
////        System.out.println("------");
////        System.out.println(req.post("http://192.168.99.223:3333/profile/1234", (String)null, (String)null, (String)null, (String)null));
////        System.out.println("------");
////        System.out.println(req.post("http://192.168.99.223:9090/activiti-app/api/enterprise/process-instances", "{\"name\":\"test\",\"processDefinitionId\":\"FOI-1:10:12091\",\"values\":{\"name\":\"xpto\"}}", "application/json", "rui", "rui"));
////      } catch (Exception var2) {
////        var2.printStackTrace();
////      } 
//    }
  
}
