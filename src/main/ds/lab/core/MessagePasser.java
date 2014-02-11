package ds.lab.core;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import ds.lab.core.clock.ClockService;
import ds.lab.core.clock.ClockServiceFactory;
import ds.lab.core.event.IEventListener;
import ds.lab.core.event.MessageHandler;
import ds.lab.entity.Message;
import ds.lab.entity.TimeStampedMessage;
import ds.lab.util.ConfigParser;
import ds.lab.util.ConfigParser.Group;
import ds.lab.util.ConfigParser.Node;
import ds.lab.util.Constant;

public class MessagePasser {

    // Sending queue
    private BlockingQueue<Message> mSendQueue = null;

    // Receiving queue
    private BlockingQueue<Message> mReceiveQueue = null;

    // Delayed sending message queue
    private BlockingQueue<Message> mDelayedSendingQueue = null;

    // Delayed receiving message queue
    private BlockingQueue<Message> mDelayedReceivingQueue = null;

    // Configuration file name
    private String mConfigFileName = null;

    private String mLocalName = null;

    // Current sequence number that is added into sending message
    private int mCurrentSeqNum = 1;

    // Configuration parser with YAML format
    private ConfigParser mConfigParser = null;

    private SendingThread mSendingThread = null;

    private ReceivingThread mReceivingThread = null;

    // private HashMap<Message, Boolean> mCheckedDelayedMessage = null;
    //
    // private HashMap<Message, Boolean> mCheckedDuplciatedMessage = null;

    private Node mNode = null;

    private boolean mEnableLogger = false;

    private ClockService mClockService = null;

    private MessageHandler mMessageHandler = null;

    private static Logger mLogger = null;

    static {
        mLogger = Logger.getLogger(MessagePasser.class.getSimpleName());
        mLogger.setLevel(Level.ALL);
    }

    /**
     * Message passer constructor
     * 
     * @param configurationFilename
     * @param localName
     */
    public MessagePasser(String configurationFilename, String localName,
            int clockType) {
        mSendQueue = new LinkedBlockingQueue<Message>();
        mReceiveQueue = new LinkedBlockingQueue<Message>();
        mDelayedSendingQueue = new LinkedBlockingQueue<Message>();
        mDelayedReceivingQueue = new LinkedBlockingQueue<Message>();

        mConfigFileName = configurationFilename;
        mLocalName = localName;

        mConfigParser = new ConfigParser(configurationFilename);
        mNode = mConfigParser.getConfiguration(localName);

        List<Node> configurationList = mConfigParser.getConfiguration();
        int processIndex = configurationList.indexOf(mNode);
        int numProcesses = configurationList.size() - 1;
        mClockService = ClockServiceFactory.createClockService(clockType,
                processIndex, numProcesses);

        mMessageHandler = new MessageHandler(
                new MessageListener(mReceiveQueue), mLocalName, mConfigParser,
                mConfigFileName);

        mSendingThread = new SendingThread(mSendQueue, mDelayedSendingQueue,
                mConfigParser, mClockService);
        mReceivingThread = new ReceivingThread(mReceiveQueue,
                mDelayedReceivingQueue, mConfigParser, mConfigFileName,
                mMessageHandler, mNode.getPort(), mClockService);
        // mCheckedDelayedMessage = new HashMap<Message, Boolean>();
        // mCheckedDuplciatedMessage = new HashMap<Message, Boolean>();

    }

    public void start() {
        if (mSendingThread != null) {
            mSendingThread.start();
        }
        if (mReceivingThread != null) {
            mReceivingThread.start();
        }
    }

    /**
     * Send message
     * 
     * @param message
     */
    public void send(Message message) {
        mLogger.log(Level.INFO, "send message dest = " + message.getDest()
                + ", message source " + message.getSource());
        if (message != null
                && !message.getKind().equals(Constant.MULTICAST_CRASH)) {
            checkUpdate(mConfigFileName);
            message.setSequenceNumber(mCurrentSeqNum++);
            message.setSource(mLocalName);
            String action = mConfigParser.getSendAction(message);
            if (action == null) {
                // If action equals null
                // No matching rule
                // Normal case
                sendMessage(message);
                return;
            }
            if (action.equals(Constant.ACTION_DROP)) {
                // Drop message
                // Do nothing
            } else if (action.equals(Constant.ACTION_DELAY)) {
                sendDelayedMessage(message);
            } else if (action.equals(Constant.ACTION_DUP)) {
                sendDuplicatedMessage(message);
            } else {
                sendMessage(message);
            }
        }
    }

    /**
     * Receive messages
     * 
     * @return message that is received
     */
    public Message receive() {
        Message m = receiveMessage();

        if (m != null && mLocalName != Constant.LOGGER) {
            if (getEnableLog()) {
                Message m2 = createLogMessage(m);
                if (m2 != null) {
                    mSendQueue.add(m2);
                }
            }
        }
        return m;
    }

    public Message receiveMessage() {
        Message message = null;
        if (mReceiveQueue != null) {
            try {
                message = mReceiveQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return message;
    }

    public List<Node> getNodeList() {
        if (mConfigParser != null) {
            return mConfigParser.getConfiguration();
        }
        return null;
    }

    public List<Group> getGroupList() {
        if (mConfigParser != null) {
            return mConfigParser.getGroups();
        }
        return null;
    }

    public boolean getEnableLog() {
        return mEnableLogger;
    }

    public void setEnableLogger(boolean flag) {
        mEnableLogger = flag;
    }

    public ClockService getClockService() {
        return mClockService;
    }

    /**
     * Send message
     * 
     * @param message add message into sending queue
     */
    private void sendMessage(Message message) {
        if (mSendQueue != null) {
            mSendQueue.add(message);
            if (getEnableLog()) {
                mSendQueue.add(createLogMessage(message));
            }
        }
    }

    /**
     * Send delayed message
     * 
     * @param message add message into delayed message queue
     */
    private void sendDelayedMessage(Message message) {
        mLogger.log(Level.INFO, "sendDelayedMessagreceiveDelayedMessagee");
        if (mSendQueue != null) {
            // If sending queue has messages, then put delayed message into it.
            // Otherwise, put delayed message into delayed message queue for
            // later sending
            if (mSendQueue.size() == 0) {
                mDelayedSendingQueue.add(message);
                if (getEnableLog()) {
                    mDelayedSendingQueue.add(createLogMessage(message));
                }
            } else if (mSendQueue.size() > 0) {
                mSendQueue.add(message);
                if (getEnableLog()) {
                    mSendQueue.add(createLogMessage(message));
                }
            }
        }
    }

    /**
     * Send duplicated message
     * 
     * @param message duplicate message and send
     */
    private void sendDuplicatedMessage(Message message) {
        mLogger.log(Level.INFO, "sendDelayedMessage");
        if (mSendQueue != null) {
            mSendQueue.add(message);
            Message duplicatedMessage = duplicateMessage(message);
            mSendQueue.add(duplicatedMessage);
            if (getEnableLog()) {
                mSendQueue.add(createLogMessage(message));
                mSendQueue.add(createLogMessage(duplicatedMessage));
            }
        }
    }

    /**
     * Duplicate message that needs to be sent
     * 
     * @param message the message that needs to be duplicated
     * @return duplicated message
     */
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

    private Message createLogMessage(Message message) {
        if (message != null && message instanceof TimeStampedMessage) {
            Node node = mConfigParser.getConfiguration(Constant.LOGGER);
            Message msg = new Message(node.getName(), Constant.LOGGER, message);
            msg.setSource(mLocalName);
            msg.setSequenceNumber(mCurrentSeqNum++);
            return msg;
        }
        return null;
    }

    private class MessageListener implements IEventListener {

        private BlockingQueue<Message> mReceiveQueue = null;

        public MessageListener(BlockingQueue<Message> queue) {
            mReceiveQueue = queue;
        }

        @Override
        public void onDeliver(Message message, Status status) {
            if (mReceiveQueue != null) {
                if (status.equals(Status.SUCCESS)) {
                    // mReceivingThread.syncClock(message);
                    // mReceivingThread.timeStampMessage(message);
                    mReceiveQueue.add(message);
                } else if (status.equals(Status.FAILURE)) {
                    // mReceivingThread.syncClock(message);
                    // mReceivingThread.timeStampMessage(message);
                }
            }
        }

        @Override
        public void onPending(Message message, Status status) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onReceive(Message message, Status status) {
            // TODO Auto-generated method stub

        }

    }
}
