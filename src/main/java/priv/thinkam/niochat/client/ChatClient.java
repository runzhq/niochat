package priv.thinkam.niochat.client;

import priv.thinkam.niochat.common.Constant;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * chat client
 *
 * @author yanganyu
 * @date 2018/11/7 15:04
 */
class ChatClient {
	/**
	 * server IP
	 */
	private static final String SERVER_IP = "127.0.0.1";
	private Selector selector;
	private SocketChannel socketChannel;
	private String sendMessagePrefix;
	private volatile boolean running = true;

	private ChatClientFrame chatClientFrame;

	private ChatClient() {
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		ChatClient chatClient = new ChatClient();
		ChatClientFrame chatClientFrame = new ChatClientFrame(chatClient);
		chatClient.setChatClientFrame(chatClientFrame);
		chatClient.start();
	}

	private void setChatClientFrame(ChatClientFrame chatClientFrame) {
		this.chatClientFrame = chatClientFrame;
	}

	/**
	 * client thread
	 *
	 * @author yanganyu
	 * @date 2018/11/7 15:48
	 */
	private void start() {
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		while (running) {
			try {
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				SelectionKey key;
				while (it.hasNext()) {
					key = it.next();
					it.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null) {
								key.channel().close();
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	private void handleInput(SelectionKey key) throws IOException {
		if (key.isValid()) {
			// 判断是否连接成功
			SocketChannel socketChannel = (SocketChannel) key.channel();
			if (key.isConnectable()) {
				if (socketChannel.finishConnect()) {
					socketChannel.register(selector, SelectionKey.OP_READ);
				} else {
					// 连接失败，进程退出
					System.exit(-1);
				}
			} else if (key.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = socketChannel.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, StandardCharsets.UTF_8);
					chatClientFrame.setTextAreaText(body);
				} else if (readBytes < 0) {
					// 对端链路关闭
					key.cancel();
					socketChannel.close();
				}

				socketChannel.register(selector, SelectionKey.OP_READ);
			}
		}
	}

	private void doConnect() throws IOException {
		if (!socketChannel.connect(new InetSocketAddress(SERVER_IP, Constant.SERVER_PORT))) {
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}
	}

	/**
	 * stop client
	 *
	 * @author yanganyu
	 * @date 2018/11/8 16:07
	 */
	void stop() {
		running = false;
		if (selector != null) {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * send message to server
	 *
	 * @author yanganyu
	 * @date 11/10/18 10:22 AM
	 */
	void sendMessageToServer(String text) {
		try {
			byte[] req = (sendMessagePrefix + text).getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
			writeBuffer.put(req);
			writeBuffer.flip();
			socketChannel.write(writeBuffer);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * set send message prefix
	 *
	 * @param sendMessagePrefix sendMessagePrefix
	 * @author yanganyu
	 * @date 11/10/18 10:30 AM
	 */
	void setSendMessagePrefix(String sendMessagePrefix) {
		this.sendMessagePrefix = sendMessagePrefix;
	}
}
