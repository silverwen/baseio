package test;

import java.io.IOException;

import com.gifisan.mtp.client.NIOClient;
import com.gifisan.mtp.client.Response;
import com.gifisan.mtp.servlet.test.TestExceptionServlet;

public class TestException {
	
	
	public static void main(String[] args) throws IOException {

		long timeout = 999100000;
		String serviceKey = TestExceptionServlet.SERVICE_NAME;
		String param = ClientUtil.getParamString();
		NIOClient client = ClientUtil.getClient();
		
		client.connect();
		Response response = client.request(serviceKey, param, timeout);
		client.close();
		
		System.out.println(response.getContent());
	}
}
