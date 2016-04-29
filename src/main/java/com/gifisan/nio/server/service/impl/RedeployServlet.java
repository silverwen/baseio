package com.gifisan.nio.server.service.impl;

import com.gifisan.nio.component.Configuration;
import com.gifisan.nio.component.Parameters;
import com.gifisan.nio.component.future.ServerReadFuture;
import com.gifisan.nio.server.NIOContext;
import com.gifisan.nio.server.RESMessage;
import com.gifisan.nio.server.ServerContext;
import com.gifisan.nio.server.service.NIOServlet;
import com.gifisan.nio.server.session.IOSession;

public class RedeployServlet extends NIOServlet {

	private String	username	= null;
	private String	password	= null;
	
	public void accept(IOSession session,ServerReadFuture future) throws Exception {
		Parameters param = future.getParameters();
		String username = param.getParameter("username");
		String password = param.getParameter("password");

		boolean result = this.username.equals(username) && this.password.equals(password);
		if (result) {
			ServerContext context = (ServerContext) session.getContext();
			RESMessage message =  context.redeploy()  ? RESMessage.R_SUCCESS : RESMessage.R_FAIL;
			future.write(message.toString());
		} else {
			future.write(RESMessage.R_UNAUTH.toString());
		}
		session.flush(future);

	}

	public void initialize(NIOContext context, Configuration config) throws Exception {
		this.username = config.getProperty("username");
		this.password = config.getProperty("password");
	}

}