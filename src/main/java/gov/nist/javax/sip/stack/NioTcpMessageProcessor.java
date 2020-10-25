package gov.nist.javax.sip.stack;

import gov.nist.core.HostPort;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NIO implementation for TCP.
 */
public class NioTcpMessageProcessor extends ConnectionOrientedMessageProcessor {
	private static final Logger logger = Logger.getLogger(NioTcpMessageProcessor.class.getName());

	protected Selector selector;
	protected Thread selectorThread;
	protected NIOHandler nioHandler;

	protected ServerSocketChannel channel;

	// Cache the change request here, the selector thread will read it when it wakes
	// up and execute the request
	private final List<ChangeRequest> changeRequests = new LinkedList<>();

	// Data send over a socket is cached here before hand, the selector thread will
	// take it later for physical send
	private final Map<SocketChannel, List<ByteBuffer>> pendingData = new WeakHashMap<>();

	public static class ChangeRequest {
		public static final int REGISTER = 1;
		public static final int CHANGEOPS = 2;

		public SocketChannel socket;
		public int type;
		public int ops;

		public ChangeRequest(SocketChannel socket, int type, int ops) {
			this.socket = socket;
			this.type = type;
			this.ops = ops;
		}

		@Override
		public String toString() {
			return socket + " type = " + type + " ops = " + ops;
		}
	}

	private SocketChannel initiateConnection(InetSocketAddress address, int timeout) throws IOException {
		// We use blocking outbound connect just because it's pure pain to deal with
		// http://stackoverflow.com/questions/204186/java-nio-select-returns-without-selected-keys-why
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(true);

		logger.log(Level.FINEST, "Init connect {0}", address);

		socketChannel.socket().connect(address, timeout);
		socketChannel.configureBlocking(false);

		logger.log(Level.FINEST, "Blocking set to false now {0}", address);

		synchronized (this.changeRequests) {
			changeRequests.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_READ));
		}
		selector.wakeup();
		return socketChannel;
	}

	public SocketChannel blockingConnect(InetSocketAddress address, int timeout) throws IOException {
		return initiateConnection(address, timeout);
	}

	public void send(SocketChannel socket, byte[] data) {
		logger.log(Level.FINEST, "Sending data {0} bytes on socket {1}", new Object[] {data.length, socket});

		synchronized (this.changeRequests) {
			this.changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			synchronized (this.pendingData) {
				List<ByteBuffer> queue = this.pendingData.get(socket);

				if(queue == null) {
					queue = new ArrayList<>();

					this.pendingData.put(socket, queue);
				}

				queue.add(ByteBuffer.wrap(data));
			}
		}

		logger.log(Level.FINEST, "Waking up selector thread");

		this.selector.wakeup();
	}

	// This will be our selector thread, only one thread for all sockets. If you
	// want to understand the overall design decisions read this first
	// http://rox-xmlrpc.sourceforge.net/niotut/
	class ProcessorTask implements Runnable {
		public ProcessorTask() {
		}

		public void read(SelectionKey selectionKey) {
			// read it.
			SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
			final NioTcpMessageChannel nioTcpMessageChannel = NioTcpMessageChannel.getMessageChannel(socketChannel);

			logger.log(Level.FINEST, "Got something on nioTcpMessageChannel {0} socket {1}",
					new Object[] {nioTcpMessageChannel, socketChannel});

			if(nioTcpMessageChannel == null) {
				logger.log(Level.FINEST, "Dead socketChannel {0} socket {1}:{2}",
						new Object[] {socketChannel, socketChannel.socket().getInetAddress(),
								socketChannel.socket().getPort()});

				selectionKey.cancel();

				// https://java.net/jira/browse/JSIP-475 remove the socket from the hash map
				pendingData.remove(socketChannel);

				return;
			}

			nioTcpMessageChannel.readChannel();
		}

		public void write(SelectionKey selectionKey) {
			SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

			final NioTcpMessageChannel nioTcpMessageChannel = NioTcpMessageChannel.getMessageChannel(socketChannel);

			logger.log(Level.FINEST, "Need to write something on nioTcpMessageChannel {0} socket {1}", new Object[] {
					nioTcpMessageChannel, socketChannel});

			if(nioTcpMessageChannel == null) {
				logger.log(Level.FINEST, "Dead socketChannel {0} socket {1}:{2}", new Object[] { socketChannel,
						socketChannel.socket().getInetAddress(), socketChannel.socket().getPort() });

				selectionKey.cancel();

				// https://java.net/jira/browse/JSIP-475 remove the socket from the hash map
				pendingData.remove(socketChannel);

				return;
			}

			synchronized(pendingData) {
				List<ByteBuffer> queue = pendingData.get(socketChannel);

				logger.log(Level.FINEST, "Queued items for writing {0}", queue.size());

				while(!queue.isEmpty()) {
					ByteBuffer buf = queue.get(0);

					try {
						socketChannel.write(buf);
					} catch(IOException e) {
						logger.log(Level.FINEST, "Dead socketChannel {0} socket {1}:{2}, error message: {3}",
								new Object[] {socketChannel, socketChannel.socket().getInetAddress(),
										socketChannel.socket().getPort(), e.getMessage()});

						nioTcpMessageChannel.close();

						/*
						 * Shall we perform a retry mechanism in case the remote host connection was closed due to
						 * a TCP RST ? https://java.net/jira/browse/JSIP-475 in the meanwhile remove the data from
						 * the hash map
						 */
						queue.remove(0);

						pendingData.remove(socketChannel);

						return;
					}

					int remain = buf.remaining();

					if(remain > 0) {
						// ... or the socket's buffer fills up
						logger.log(Level.FINEST, "Socket buffer filled and more is remaining: {0} remain: {1}",
								new Object[] {queue.size(), remain});

						break;
					}

					queue.remove(0);
				}

				if(queue.isEmpty()) {
					logger.log(Level.FINEST, "We wrote away all data. Setting READ interest."
							+ " Queue is emtpy now size = {0}", queue.size());

					selectionKey.interestOps(SelectionKey.OP_READ);
				}
			}

			logger.log(Level.FINEST, "Done writing");
		}

		public void connect(SelectionKey selectionKey) throws IOException {
			/*
			 * Ignoring the advice from http://rox-xmlrpc.sourceforge.net/niotut/ because it leads
			 * to spinning on my machine
			 */
			throw new IOException("We should use blocking connect, we must never reach here");
		}

		public void accept(SelectionKey selectionKey) throws IOException {
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
			SocketChannel client;

			client = serverSocketChannel.accept();

			if(sipStack.isTcpNoDelayEnabled) {
				client.setOption(StandardSocketOptions.TCP_NODELAY, true);
			}

			client.configureBlocking(false);

			logger.log(Level.FINEST, "got a new connection! {0}", client);

			// No need for MAX SOCKET CHANNELS check here because this can be configured at OS level
			createMessageChannel(NioTcpMessageProcessor.this, client);

			logger.log(Level.FINEST, "Adding to selector: {0}", client);

			client.register(selector, SelectionKey.OP_READ);
		}

		@Override
		public void run() {
			while(true) {
				logger.log(Level.FINEST, "Selector thread cycle begin...");

				synchronized (changeRequests) {
					for(ChangeRequest change : changeRequests) {
						logger.log(Level.FINEST, "ChangeRequest: {0}, selector: {1}", new Object[] {change, selector});

						try {
							switch(change.type) {
							case ChangeRequest.CHANGEOPS:
								SelectionKey key = change.socket.keyFor(selector);
								if(key == null || !key.isValid()) {
									continue;
								}

								key.interestOps(change.ops);

								logger.log(Level.FINEST, "Change opts: {0}, selector: {1} key: {2} blocking: {3}",
										new Object[] {change, selector, key, change.socket.isBlocking()});

								break;
							case ChangeRequest.REGISTER:
								try {
									logger.log(Level.FINEST, "NIO register: {0}, selector: {1}, blocking: {2}",
											new Object[] {change, selector, change.socket.isBlocking()});

									change.socket.register(selector, change.ops);
								} catch (ClosedChannelException e) {
									logger.log(Level.WARNING, "Socket closed before register ops: {0}", change.socket);
								}

								break;
							}
						} catch(Exception e) {
							logger.log(Level.SEVERE, "Problem setting changes", e);
						}
					}

					changeRequests.clear();
				}

				try {
					logger.log(Level.FINEST, "Before select");

					if (!selector.isOpen()) {
						logger.log(Level.INFO, "Selector is closed ");

						return;
					}

					selector.select();

					logger.log(Level.FINEST, "After select");
				} catch(IOException e) {
					logger.log(Level.SEVERE, "problem in select", e);

					break;
				} catch(CancelledKeyException cke) {
					logger.log(Level.INFO, "Looks like remote side closed a connection");
				}

				try {
					if(selector.selectedKeys() == null) {
						logger.log(Level.FINEST, "null selectedKeys ");

						continue;
					}

					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					while(it.hasNext()) {
						SelectionKey selectionKey = it.next();
						try {
							it.remove();

							logger.log(Level.FINEST, "We got selkey {0}", selectionKey);

							if(!selectionKey.isValid()) {
								logger.log(Level.FINEST, "Invalid key found {0}", selectionKey);
							} else if(selectionKey.isAcceptable()) {
								logger.log(Level.FINEST, "Accept {0}", selectionKey);

								accept(selectionKey);
							} else if(selectionKey.isReadable()) {
								logger.log(Level.FINEST, "Read {0}", selectionKey);

								read(selectionKey);
							} else if(selectionKey.isWritable()) {
								logger.log(Level.FINEST, "Write {0}", selectionKey);

								write(selectionKey);
							} else if(selectionKey.isConnectable()) {
								logger.log(Level.FINEST, "Connect {0}", selectionKey);

								connect(selectionKey);
							}
						} catch(Exception e) {
							logger.log(Level.SEVERE, "Problem processing selection key event", e);
						}
					}
				} catch(ClosedSelectorException ex) {
					logger.log(Level.INFO, "Selector is closed");

					return;
				} catch(Exception ex) {
					logger.log(Level.SEVERE, "Problem in the selector loop", ex);
				}
			}
		}
	}

	public NioTcpMessageChannel createMessageChannel(NioTcpMessageProcessor nioTcpMessageProcessor,
			SocketChannel client) throws IOException {
		return NioTcpMessageChannel.create(NioTcpMessageProcessor.this, client);
	}

	public NioTcpMessageProcessor(InetAddress ipAddress, SIPTransactionStack sipStack, int port) {
		super(ipAddress, port, "TCP", sipStack);
		nioHandler = new NIOHandler(sipStack, this);
	}

	@Override
	public MessageChannel createMessageChannel(HostPort targetHostPort) throws IOException {
		logger.log(Level.FINEST, "NioTcpMessageProcessor::createMessageChannel: {0}", targetHostPort);

		try {
			String key = MessageChannel.getKey(targetHostPort, transport);
			if (messageChannels.get(key) != null) {
				return this.messageChannels.get(key);
			} else {
				NioTcpMessageChannel retval = new NioTcpMessageChannel(targetHostPort.getInetAddress(),
						targetHostPort.getPort(), sipStack, this);

				synchronized(messageChannels) {
					this.messageChannels.put(key, retval);
				}

				retval.isCached = true;

				logger.log(Level.FINEST, "key {0}\ncreating: {1}", new Object[] {key, retval});

				selector.wakeup();

				return retval;
			}
		} finally {
			logger.log(Level.FINEST, "MessageChannel::createMessageChannel - exit");
		}
	}

	@Override
	public MessageChannel createMessageChannel(InetAddress targetHost, int port) throws IOException {
		String key = MessageChannel.getKey(targetHost, port, transport);
		if (messageChannels.get(key) != null) {
			return this.messageChannels.get(key);
		} else {
			NioTcpMessageChannel retval = new NioTcpMessageChannel(targetHost, port, sipStack, this);

			selector.wakeup();

			this.messageChannels.put(key, retval);

			retval.isCached = true;

			logger.log(Level.FINEST, "key {0}\nCreating: {1}", new Object[] {key, retval});

			return retval;
		}
	}

	// https://java.net/jira/browse/JSIP-475
	@Override
	protected synchronized void remove(ConnectionOrientedMessageChannel messageChannel) {
		logger.log(Level.FINEST, "{0} removing {1} from processor {2}:{3}/{4}",
				new Object[] {Thread.currentThread(), ((NioTcpMessageChannel) messageChannel).getSocketChannel(),
						getIpAddress(), getPort(), getTransport()});

		pendingData.remove(((NioTcpMessageChannel) messageChannel).getSocketChannel());

		super.remove(messageChannel);
	}

	@Override
	public int getDefaultTargetPort() {
		return 5060;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void start() throws IOException {
		selector = Selector.open();
		channel = ServerSocketChannel.open();
		channel.configureBlocking(false);

		InetSocketAddress isa = new InetSocketAddress(super.getIpAddress(), super.getPort());

		channel.socket().bind(isa);
		channel.register(selector, SelectionKey.OP_ACCEPT);

		selectorThread = new Thread(createProcessorTask());
		selectorThread.start();
		selectorThread.setName("NioSelector-" + getTransport() + '-' + getIpAddress().getHostAddress() + '/' + getPort());
	}

	protected ProcessorTask createProcessorTask() {
		return new ProcessorTask();
	}

	@Override
	public void stop() {
		try {
			nioHandler.stop();

			if (selector.isOpen()) {
				selector.close();
			}
		} catch(Exception ex) {
			logger.log(Level.SEVERE, "Problem closing channel", ex);
		}

		try {
			channel.close();
		} catch(Exception ex) {
			logger.log(Level.SEVERE, "Problem closing channel", ex);
		}
	}
}
