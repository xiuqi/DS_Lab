package ds.lab.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ds.lab.core.MessagePasser;
import ds.lab.entity.Message;
import ds.lab.entity.TimeStamp;
import ds.lab.entity.TimeStampedMessage;
import ds.lab.util.ConfigParser.Group;
import ds.lab.util.ConfigParser.Node;
import ds.lab.util.Constant;

public class TestApp {

    private JPanel mLeftPanel = null;

    private JPanel mRightPanel = null;

    private JFrame mAppFrame = null;

    private JTextField mKindText = null;

    private JTextField mSendText = null;

    private JButton mSendButton = null;

    private JButton mEnableLogButton = null;

    private JTextArea mReceivedText = null;

    private MessagePasser mMessagePasser = null;

    private HashMap<Node, Boolean> mSelectedNodeMap = null;

    private HashMap<Group, Boolean> mSelectedGroupMap = null;

    private JCheckBox mCrashSend = null;

    public TestApp(String localName, MessagePasser messagePasser,
            List<Node> nodeList, List<Group> groupList) {
        mMessagePasser = messagePasser;
        mSelectedNodeMap = new HashMap<Node, Boolean>();
        mSelectedGroupMap = new HashMap<Group, Boolean>();
        initUI(localName, nodeList, groupList);
    }

    private void initUI(String localName, List<Node> nodeList,
            List<Group> groupList) {
        initMainWindow(localName);
        initLeftPanel(nodeList, groupList);
        initRightPanel();
    }

    private void initMainWindow(String localName) {
        mAppFrame = new JFrame(TestApp.class.getSimpleName() + " " + localName);
        mAppFrame.setLayout(new BorderLayout());
        mAppFrame.setSize(720, 480);
        mAppFrame.setLocationRelativeTo(null);
        mAppFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initLeftPanel(final List<Node> nodeList,
            final List<Group> groupList) {
        mLeftPanel = new JPanel();
        mLeftPanel.setAlignmentY(1f);
        mLeftPanel.setLayout(new BoxLayout(mLeftPanel, BoxLayout.Y_AXIS));
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel nodeLabel = new JLabel(Constant.APP_NODES);
        mLeftPanel.add(nodeLabel);
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        for (final Node node : nodeList) {
            mSelectedNodeMap.put(node, false);
            if (node.getName().equals(Constant.LOGGER)) {
                continue;
            }
            final JTextField nodeField = new JTextField(Constant.APP_NAME + " "
                    + node.getName() + " " + Constant.APP_IP + " "
                    + node.getIp());
            nodeField.setEditable(false);
            mLeftPanel.add(nodeField);
            mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            nodeField.addMouseListener(new MouseListener() {

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    Color foregroundColor = nodeField.getForeground();
                    nodeField.setForeground(nodeField.getBackground());
                    nodeField.setBackground(foregroundColor);
                    mSelectedNodeMap.put(node, !mSelectedNodeMap.get(node));
                }
            });
        }
        JLabel groupLabel = new JLabel(Constant.APP_GROUPS);
        mLeftPanel.add(groupLabel);
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        for (final Group group : groupList) {
            mSelectedGroupMap.put(group, false);
            StringBuffer sb = new StringBuffer();
            sb.append(group.getName() + " -> ");
            final JTextField groupField = new JTextField();
            for (Node node : group.getMembers()) {
                sb.append(node.getName() + " ");
            }
            groupField.setText(sb.toString());
            groupField.setEditable(false);
            mLeftPanel.add(groupField);
            mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            groupField.addMouseListener(new MouseListener() {

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    Color foregroundColor = groupField.getForeground();
                    groupField.setForeground(groupField.getBackground());
                    groupField.setBackground(foregroundColor);
                    mSelectedGroupMap.put(group, !mSelectedGroupMap.get(group));
                }
            });
        }

        JLabel kindTextLabel = new JLabel(Constant.APP_MESSAGE_KIND);
        mLeftPanel.add(kindTextLabel);
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mKindText = new JTextField(Constant.APP_MESSAGE_KIND_DEFAULT);
        mLeftPanel.add(mKindText);
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel sendTextLabel = new JLabel(Constant.APP_SEND_MESSAGE);
        mLeftPanel.add(sendTextLabel);
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mSendText = new JTextField(Constant.APP_INPUT_HINT);
        mLeftPanel.add(mSendText);
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        mCrashSend = new JCheckBox();
        mCrashSend.setText(Constant.APP_CRASH_SEND);
        mLeftPanel.add(mCrashSend);
        mSendButton = new JButton(Constant.APP_SEND);
        mSendButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseClicked(MouseEvent e) {
                HashMap<Node, Boolean> sentNodeMap = new HashMap<Node, Boolean>();
                Iterator<Entry<Node, Boolean>> iter = mSelectedNodeMap
                        .entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<Node, Boolean> entry = iter.next();
                    if (entry.getValue()) {
                        TimeStampedMessage message = new TimeStampedMessage(
                                entry.getKey().getName(), mKindText.getText(),
                                mSendText.getText());
                        mMessagePasser.send(message);
                    }
                }
                Iterator<Entry<Group, Boolean>> it = mSelectedGroupMap
                        .entrySet().iterator();

                boolean isGroupSent = false;
                TimeStamp timeStamp = null;
                while (it.hasNext()) {
                    Entry<Group, Boolean> entry = it.next();
                    boolean first = true;
                    if (entry.getValue()) {
                        if (first) {
                            timeStamp = mMessagePasser.getClockService()
                                    .generateTimeStamp();
                            System.out.println("TimeStamp = "
                                    + timeStamp.toString());
                        }
                        isGroupSent = true;
                        List<Node> memberList = entry.getKey().getMembers();
                        for (int i = 0; i < memberList.size(); i++) {
                            Node node = memberList.get(i);
                            TimeStampedMessage message = null;
                            if (mCrashSend.isSelected()
                                    && i == memberList.size() - 1) {
                                message = new TimeStampedMessage(
                                        node.getName(),
                                        Constant.MULTICAST_CRASH, mSendText
                                                .getText());
                            } else {
                                message = new TimeStampedMessage(
                                        node.getName(), mKindText.getText(),
                                        mSendText.getText());
                                message.setIsMulticast(true);
                            }
                            sentNodeMap.put(node, true);
                            message.setTimeStamp(timeStamp);
                            message.setGroup(entry.getKey().getName());
                            mMessagePasser.send(message);
                        }
                    }
                    first = false;
                }
                if (isGroupSent) {
                    for (Node node : nodeList) {
                        if (!sentNodeMap.containsKey(node)
                                && !node.getName().equals(Constant.LOGGER)) {
                            TimeStampedMessage message = new TimeStampedMessage(
                                    node.getName(), mKindText.getText(),
                                    mSendText.getText());
                            message.setTimeStamp(timeStamp);
                            message.setIsMulticast(true);
                            mMessagePasser.send(message);
                        }
                    }
                }
            }
        });
        mLeftPanel.add(mSendButton);
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mEnableLogButton = new JButton(Constant.APP_ENABLE_LOG);
        mEnableLogButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (mMessagePasser != null) {
                    if (mMessagePasser.getEnableLog()) {
                        mMessagePasser.setEnableLogger(false);
                        mEnableLogButton.setText(Constant.APP_ENABLE_LOG);
                    } else {
                        mMessagePasser.setEnableLogger(true);
                        mEnableLogButton.setText(Constant.APP_DISABLE_LOG);
                    }
                }
            }
        });
        mLeftPanel.add(mEnableLogButton);
        mLeftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mAppFrame.add(mLeftPanel, BorderLayout.WEST);
    }

    private void initRightPanel() {
        mRightPanel = new JPanel(new BorderLayout());
        mReceivedText = new JTextArea(Constant.APP_RECEIVED_MESSAGE);
        mReceivedText.setEditable(false);
        mRightPanel.add(mReceivedText);
        mAppFrame.add(mRightPanel, BorderLayout.CENTER);
    }

    public void display() {
        if (mAppFrame != null) {
            mAppFrame.setVisible(true);
        }
    }

    public void updateMessage(Message message) {
        if (message != null) {
            mReceivedText.append("\n" + Constant.APP_FROM + message.getSource()
                    + " " + Constant.APP_DATA + message.getData());
        }
    }

    private static String CONFIG_FILE_PATH = "config.txt";

    /**
     * @param args
     */
    public static void main(String[] args) {
        MessagePasser messagePasser = null;
        TestApp app = null;
        if (args != null) {
            String configFilePath = null;
            String localName = null;

            HashMap<String, Integer> clockMap = new HashMap<String, Integer>();
            clockMap.put("logical", Constant.LOGICAL_CLOCK);
            clockMap.put("vector", Constant.VECTOR_CLOCK);

            int clockType = 0;
            switch (args.length) {
            case 3:
                configFilePath = args[0];
                localName = args[1];
                clockType = clockMap.get(args[2]);
                break;
            case 2:
                configFilePath = args[0];
                localName = args[1];
                break;
            case 1:
                configFilePath = CONFIG_FILE_PATH;
                localName = args[0];
                break;
            default:
                System.out
                        .println("Usage: java TestApp [config_file_path] [local_name] [logical | vector]");
                return;
            }
            if (new File(configFilePath).exists()) {
                messagePasser = new MessagePasser(configFilePath, localName,
                        clockType);
                List<Node> nodeList = messagePasser.getNodeList();
                List<Group> groupList = messagePasser.getGroupList();
                messagePasser.start();
                app = new TestApp(localName, messagePasser, nodeList, groupList);
                app.display();
            } else {
                System.out.println("Config file is not found!");
            }
        } else {
            System.out
                    .println("Usage: java TestApp [config_file_path] [local_name]");
        }
        while (messagePasser != null) {
            Message message = messagePasser.receive();
            // System.out.println(message);
            app.updateMessage(message);
        }
    }
}
