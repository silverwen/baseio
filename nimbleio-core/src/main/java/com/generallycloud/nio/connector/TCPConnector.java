package com.generallycloud.nio.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import com.generallycloud.nio.TimeoutException;
import com.generallycloud.nio.common.CloseUtil;
import com.generallycloud.nio.common.LifeCycleUtil;
import com.generallycloud.nio.common.MessageFormatter;
import com.generallycloud.nio.component.NIOContext;
import com.generallycloud.nio.component.ServerConfiguration;
import com.generallycloud.nio.component.Session;
import com.generallycloud.nio.component.TCPSelectorLoop;
import com.generallycloud.nio.component.concurrent.UniqueThread;
import com.generallycloud.nio.component.concurrent.Waiter;

public class TCPConnector extends AbstractIOConnector {

	private TCPSelectorLoop	selectorLoop;
	private UniqueThread	selectorLoopThread;
	private Waiter			waiter	= new Waiter();

	protected void connect(NIOContext context, InetSocketAddress socketAddress) throws IOException {

		SocketChannel channel = SocketChannel.open();

		channel.configureBlocking(false);

		this.selectorLoop = new ClientTCPSelectorLoop(context, this);

		this.selectorLoop.register(context, channel);

		channel.connect(socketAddress);

		this.selectorLoopThread = new UniqueThread(selectorLoop, getServiceDescription() + "(Selector)");

		this.selectorLoopThread.start();

		if (waiter.await(30000)) {

			CloseUtil.close(this);

			Object o = waiter.getPayload();

			if (o instanceof Exception) {

				Exception t = (Exception) o;

				throw new TimeoutException(MessageFormatter.format(
						"connect faild,connector:{},nested exception is {}", this, t.getMessage()), t);
			}

			if (o == null) {
				throw new TimeoutException("time out");
			}
		}
	}

	protected void finishConnect(Session session, IOException exception) {

		if (exception == null) {

			this.session = session;

			this.waiter.setPayload(null);

			if (waiter.isTimeouted()) {
				CloseUtil.close(this);
			}
		} else {

			this.waiter.setPayload(exception);
		}
	}

	public InetSocketAddress getServerSocketAddress() {
		return this.serverAddress;
	}

	protected UniqueThread getSelectorLoopThread() {
		return selectorLoopThread;
	}

	protected int getSERVER_PORT(ServerConfiguration configuration) {
		return configuration.getSERVER_TCP_PORT();
	}

	protected void setIOService(NIOContext context) {
		context.setTCPService(this);
	}

	protected void close(NIOContext context) {

		LifeCycleUtil.stop(selectorLoopThread);

		CloseUtil.close(session);
	}

	public String getServiceDescription() {
		return "TCP:" + serverAddress.toString();
	}

}