package ds.lab.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import ds.lab.core.clock.ClockService;
import ds.lab.core.event.MessageHandler;
import ds.lab.entity.Message;
import ds.lab.entity.TimeStamp;
import ds.lab.entity.TimeStampedMessage;
import ds.lab.util.ConfigParser;
import ds.lab.util.Constant;

/**
 * Receiving thread
 */
class ReceivingThread extends Thread {

    private boolean mIsRunning = false;

    private BlockingQueue<Message> mReceiveQueue = null;

    private int mPort = -1;

    private ClockService mClockService = null;

    private MessageHandler mHandler = null;

    private HashMap<Message, Boolean> mCheckedDelayedMessage = null;

    private HashMap<Message, Boolean> mCheckedDuplciatedMessage = null;

    private BlockingQueue<Message> mDelayedReceivingQueue = null;

    private ConfigParser mConfigParser = null;

    private String mConfigFileName = null;

    /**
     * @param receiveQueue
     * @param configParser
     */
    public ReceivingThread(BlockingQueue<Message> receiveQueue,
            BlockingQueue<Message> delayedQueue, ConfigParser configParser,
            String configFilePath, MessageHandler handler, int port,
            ClockService clockService) {
        mReceiveQueue = receiveQueue;
        mPort = port;
        mConfigParser = configParser;
        mConfigFileName = configFilePath;
        mClockService = clockService;
        mHandler = handler;
        mDelayedReceivingQueue = delayedQueue;

        mCheckedDelayedMessage = new HashMap<Message, Boolean>();
        mCheckedDuplciatedMessage = new HashMap<Message, Boolean>();
    }

    public void stopServer() {
        mIsRunning = false;
    }

    @Override
    public void run() {
        mIsRunning = true;
        startServer(mPort);
    }

    /**
     * Start NIO server with specific port
     * 
     * @param port specific port when server is running
     */
    private void startServer(int port) {
        Selector selector = null;
        ServerSocketChannel serverSocketChannel = null;
        try {
            // Open selector
            selector = Selector.open();

            // Create a new server socket and set to non blocking mode
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);

            // Bind the server socket to the local host and port
            serverSocketChannel.socket().setReuseAddress(true);
            serverSocketChannel.socket().bind(new InetSocketAddress(port));

            // Register accepts on the server socket with the selector.
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (mIsRunning) {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey readyKey = it.next();
                    it.remove();

                    readData(selector, readyKey);
                }
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (selector != null) {
                    selector.close();
                }
                if (serverSocketChannel != null) {
                    serverSocketChannel.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void readData(Selector selector, SelectionKey readyKey) {
        if (readyKey != null) {
            if (readyKey.isAcceptable()) {
                ServerSocketChannel server = (ServerSocketChannel) readyKey
                        .channel();
                SocketChannel channel = null;
                try {
                    channel = server.accept();
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (readyKey.isReadable()) {
                SocketChannel channel = (SocketChannel) readyKey.channel();
                Message message = readData(channel);
                if (message != null) {
                    // Not put message into receive queue immediately
                    // Call message handler to process
                    if (message.isMulticast()) {
                        Message handleMessage = checkAction(message);
                        if (handleMessage != null) {
                            mHandler.addMessage(handleMessage,
                                    mClockService.getTimeStamp());
                        }
                    } else {
                        Message handleMessage = checkAction(message);
                        if (handleMessage != null) {
                            syncClock(handleMessage);
                            timeStampMessage(handleMessage);
                            mReceiveQueue.add(handleMessage);
                            trasferDelayedMessage(mReceiveQueue,
                                    mDelayedReceivingQueue);
                        }
                    }
                }
            }
        }
    }

    private Message readData(SocketChannel channel) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        Message message = null;
        try {
            byte[] bytes = null;
            int size = 0;
            while ((size = channel.read(buffer)) > 0) {
                buffer.flip();
                bytes = new byte[size];
                buffer.get(bytes);
                baos.write(bytes);
                buffer.clear();
            }
            bytes = baos.toByteArray();
            if (bytes.length > 0) {
                message = deserialize(bytes);
            } else {
                channel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return message;
    }

    private Message deserialize(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = null;
        Message message = null;
        try {
            ois = new ObjectInputStream(bais);
            message = (Message) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return message;
    }

    private void syncClock(Message message) {
        if (message != null && message instanceof TimeStampedMessage) {
            if (mClockService != null) {
                TimeStamp timeStamp = ((TimeStampedMessage) message)
                        .getTimeStamp();
                mClockService.synchClock(timeStamp);
            }
        }
    }

    private void timeStampMessage(Message message) {
        if (message != null && message instanceof TimeStampedMessage) {
            ((TimeStampedMessage) message).setTimeStamp(mClockService
                    .getTimeStamp());
        }
    }

    private Message checkAction(Message message) {
        checkUpdate(mConfigFileName);
        String action = mConfigParser.getReceiveAction(message);
        if (action == null) {
            // If action equals null
            // No matching rule
            // Normal case
            if (message.isMulticast()) {
                trasferDelayedMessage(mReceiveQueue, mDelayedReceivingQueue);
            }
            return message;
        }
        if (action.equals(Constant.ACTION_DROP)) {
            // Drop message
            return null;
        } else if (action.equals(Constant.ACTION_DELAY)) {
            if (isCheckedDelayedMessage(message)) {
                // If message is checked, then remove it from map
                mCheckedDelayedMessage.remove(message);
            } else {
                // Delay
                receiveDelayedMessage(message);
                return null;
            }
        } else if (action.equals(Constant.ACTION_DUP)) {
            // Duplicate
            if (isCheckedDuplicatedMessage(message)) {
                mCheckedDuplciatedMessage.remove(message);
            } else {
                receiveDuplicateMessage(message);
            }
            if (message.isMulticast()) {
                trasferDelayedMessage(mReceiveQueue, mDelayedReceivingQueue);
            }
        } else {
            if (message.isMulticast()) {
                trasferDelayedMessage(mReceiveQueue, mDelayedReceivingQueue);
            }
            return message;
        }
        return message;
    }

    /**
     * Receive delayed message
     * 
     * @param message add delayed message into delayed message queue
     */
    private void receiveDelayedMessage(Message message) {
        if (mCheckedDelayedMessage != null) {
            mCheckedDelayedMessage.put(message, true);
        }
        if (mReceiveQueue != null) {
            // If sending queue has messages, then put delayed message into it.
            // Otherwise, put delayed message into delayed message queue for
            // later sending
            if (mReceiveQueue.size() == 0) {
                mDelayedReceivingQueue.add(message);
            } else if (mReceiveQueue.size() > 0) {
                mReceiveQueue.add(message);
            }
        }
    }

    /**
     * Receive duplicated message
     * 
     * @param message duplicate message and receive
     */
    private void receiveDuplicateMessage(Message message) {
        if (mReceiveQueue != null) {
            Message dupMessage = duplicateMessage(message);
            if (mCheckedDuplciatedMessage != null) {
                mCheckedDuplciatedMessage.put(dupMessage, true);
            }
            mReceiveQueue.add(dupMessage);
        }
    }

    private void trasferDelayedMessage(BlockingQueue<Message> receiveQueue,
            BlockingQueue<Message> delayedReceivingQueue) {
        if (receiveQueue != null && delayedReceivingQueue != null) {
            int size = delayedReceivingQueue.size();
            for (int i = 0; i < size; ++i) {
                Message temp = delayedReceivingQueue.remove();
                if (!temp.isMulticast())
                    receiveQueue.add(temp);
                else
                    mHandler.addMessage(temp, mClockService.getTimeStamp());
            }
        }
    }

    private boolean isCheckedDelayedMessage(Message message) {
        return mCheckedDelayedMessage.containsKey(message)
                && mCheckedDelayedMessage.get(message);
    }

    private boolean isCheckedDuplicatedMessage(Message message) {
        return mCheckedDuplciatedMessage.containsKey(message)
                && mCheckedDuplciatedMessage.get(message);
    }

    private Message duplicateMessage(Message message) {
        if (message != null) {
            Message dupMsg = new Message(message.getDest(), message.getKind(),
                    message.getData());
            dupMsg.setSource(message.getSource());
            dupMsg.setSequenceNumber(message.getSequenceNumber());
            dupMsg.setDuplicate(true);
            return dupMsg;
        }
        return null;
    }

    /**
     * Check update for config file
     * 
     * @param configFileName config file name
     */
    private void checkUpdate(String configFileName) {
        try {
            mConfigParser.checkUpdate(mConfigFileName);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
