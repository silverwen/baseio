/*
 * Copyright 2015-2017 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.generallycloud.nio.container.jms.server;

import java.util.HashMap;
import java.util.Map;

import com.generallycloud.nio.codec.protobase.future.ProtobaseReadFuture;
import com.generallycloud.nio.common.Logger;
import com.generallycloud.nio.common.LoggerFactory;
import com.generallycloud.nio.component.SocketSession;
import com.generallycloud.nio.component.concurrent.AbstractEventLoop;
import com.generallycloud.nio.container.ApplicationContextUtil;
import com.generallycloud.nio.container.authority.Authority;
import com.generallycloud.nio.container.jms.Message;

public abstract class AbstractProductLine extends AbstractEventLoop implements MessageQueue {

	protected MQContext					context;
	protected MessageStorage				storage;
	protected long						dueTime;
	protected Map<String, ConsumerQueue>	consumerMap;
	private Logger						logger	= LoggerFactory.getLogger(AbstractProductLine.class);

	public AbstractProductLine(MQContext context) {

		this.context = context;

		this.storage = new MessageStorage();

		this.consumerMap = new HashMap<String, ConsumerQueue>();

		this.dueTime = context.getMessageDueTime();
	}

	// TODO 处理剩下的message 和 receiver
	@Override
	protected void doStop() {
	}

	public MQContext getContext() {
		return context;
	}

	@Override
	public void pollMessage(SocketSession session, ProtobaseReadFuture future, MQSessionAttachment attachment) {

		if (attachment.getConsumer() != null) {
			return;
		}

		Authority authority = ApplicationContextUtil.getAuthority(session);

		String queueName = authority.getUuid();

		// 来自终端类型
		context.addReceiver(queueName);

		ConsumerQueue consumerQueue = getConsumerQueue(queueName);

		Consumer consumer = new Consumer(consumerQueue, attachment, session, future, queueName);

		attachment.setConsumer(consumer);

		consumerQueue.offer(consumer);
	}

	protected ConsumerQueue getConsumerQueue(String queueName) {

		ConsumerQueue consumerQueue = consumerMap.get(queueName);

		if (consumerQueue == null) {

			synchronized (consumerMap) {

				consumerQueue = consumerMap.get(queueName);

				if (consumerQueue == null) {
					consumerQueue = createConsumerQueue();
					consumerMap.put(queueName, consumerQueue);
				}
			}
		}
		return consumerQueue;
	}

	protected abstract ConsumerQueue createConsumerQueue();

	@Override
	public void offerMessage(Message message) {

		storage.offer(message);
	}

	protected void filterUseless(Message message) {
		long now = System.currentTimeMillis();
		long dueTime = this.dueTime;

		if (now - message.getTimestamp() > dueTime) {
			// 消息过期了
			logger.debug(">>>> message invalidate : {}", message);
			return;
		}
		this.offerMessage(message);
	}

	public void setDueTime(long dueTime) {
		this.dueTime = dueTime;
	}
}
