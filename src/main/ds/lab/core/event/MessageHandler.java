package ds.lab.core.event;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ds.lab.core.event.IEventListener.Status;
import ds.lab.entity.*;
import ds.lab.util.ConfigParser;
import ds.lab.util.ConfigParser.Group;
import ds.lab.util.ConfigParser.Node;

public class MessageHandler {

    private IEventListener mEventListener = null;

    private BlockingQueue<Message> mHoldbackQueue = null;

    private String mLocalName = null;

    private ConfigParser mConfigParser = null;

    private String mConfigFileName = null;

    /**
     * MessageHandler constructor with IEventListener
     * 
     * @param listener
     */
    public MessageHandler(IEventListener listener, String localName,
            ConfigParser configParser, String configFileName) {
        mEventListener = listener;
        mHoldbackQueue = new LinkedBlockingQueue<Message>();
        mLocalName = localName;
        mConfigParser = configParser;
        mConfigFileName = configFileName;
    }

    /**
     * Add message
     * 
     * @param message
     */
    public void addMessage(Message message, TimeStamp ts) {
        if (mHoldbackQueue != null) {
            mHoldbackQueue.add(message);
            // TODO
            // Should implement sync algorithm here
            processQueue(mHoldbackQueue, ts);

            // ***
            // I commented out here, do it in processQueue
            // mEventListener.onReceive(message, Status.SUCCESS);
        }
    }

    /**
     * Process queue
     * 
     * @param queue
     */
    private void processQueue(BlockingQueue<Message> queue, TimeStamp ts) {
        // TODO
        // Process received message
        // 1. If can be delivered, call IEventListener.onDeliver() with Status
        // 2. If the message is in processing, call IEventListener.onPending()
        // with Status
        // 3. If the message is received in ReceivingThread, call
        // IEventListener.onReceive() with Status

        // ***
        // This function assumes that all messages passed in
        // are multicast messages, other type of messages should
        // not be pushed into this queue

        // Get the most recent message, why use Queue here?
        Message curmes = (Message) queue.toArray()[queue.size() - 1];
        if(curmes.getDest().equals(curmes.getSource())){
        	this.mEventListener.onDeliver(curmes, Status.SUCCESS);
        	return;
        }
        //Check Timestamp type
        if(!(ts instanceof VectorTimeStamp)){
        	System.out.println("A casual ordered multicast must "
        			+ "use vector timestamp!");
        	return;
        }
        VectorTimeStamp curts = (VectorTimeStamp)ts;
        //Update the config file
        try {
            mConfigParser.checkUpdate(mConfigFileName);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Get the lists
        List<Group> glist= mConfigParser.getGroups();
        List<Node> hlist = mConfigParser.getConfiguration();
        //Find the index of src and dst host
        int srcindex=0,dstindex=0;
        while(!hlist.get(srcindex).getName().equals(curmes.getSource())){
        	srcindex++;
        	if(srcindex>hlist.size()-1){
        		System.out.println("Can't find src in the host list");
        		return;
        	}	
        }
        while(!hlist.get(dstindex).getName().equals(mLocalName)){
        	dstindex++;
        	if(dstindex>hlist.size()-1){
        		System.out.println("Can't find dst in the host list");
        		return;
        	}
        }
        
        int i=0;
        //Find the group in the grouplist
        while(!curmes.getGroup().equals(glist.get(i).getName())){
        	i++;
        	if(i>glist.size()-1){
        		System.out.println("Unknown multicast group.");
        		return;
        	}
        }
        List<Node> nlist = glist.get(i).getMembers();
        if (!inGroup(nlist,mLocalName)) {
            // The host is not in the group
        	int temp = ((List<Integer>)curts.getValue()).get(srcindex);
        	((List<Integer>)curts.getValue()).set(srcindex, ++temp);
        	return;
        }
        //The host is in the group
        boolean match = true;
        while(match){
        	match = false;
        	int len = queue.size();
        	for(int j=0;j<len;j++){
        		TimeStampedMessage tempmes = (TimeStampedMessage)queue.poll();
        		if(canBeDelivered((VectorTimeStamp)tempmes.getTimeStamp(),curts,srcindex,dstindex)){
        			this.mEventListener.onDeliver(tempmes, Status.SUCCESS);
        			int temp = ((List<Integer>)curts.getValue()).get(srcindex);
                	((List<Integer>)curts.getValue()).set(srcindex, ++temp);
                	match = true;
                	continue;
        		}
        		queue.add(tempmes);
        	}
        }
    }
    
    private boolean canBeDelivered(VectorTimeStamp mes, VectorTimeStamp host
    		, int srcIndex, int dstIndex){
    	List<Integer> mests = (List<Integer>)mes.getValue();
    	List<Integer> ts = (List<Integer>)host.getValue();
    	int len = mests.size();
    	for(int i=0;i<mests.size();i++){
    		if(i==dstIndex) continue;
    		if(i==srcIndex){
    			if(ts.get(i)+1!=mests.get(i)){
    				return false;
    			}
    			else continue;
    		}
    		if(ts.get(i)<mests.get(i))
    			return false;
    	}
    	return true;
    }
    private boolean inGroup(List<Node>nlist, String mLocalName){
    	for(int i=0;i<nlist.size();i++){
    		if(nlist.get(i).getName().equals(mLocalName))
    			return true;
    	}
    	return false;
    }
}
