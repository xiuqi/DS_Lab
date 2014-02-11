package ds.lab.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import ds.lab.entity.Message;

public class ConfigParser {
	List<Node> configurationList; // a list for configuration info of nodes
	List<Rule> sendRules; // a list for send rules
	List<Rule> receiveRules; // a list for receive rules
	List<Group> groups; // a list for groups
	byte[] mdbytes; // MD5

	public ConfigParser (String configurationFilename) {
		loadConfigurationFile(configurationFilename);
	}

	private void loadConfigurationFile (String configurationFilename) {
		Yaml yaml = new Yaml();

		try {
			mdbytes = getMD5(configurationFilename);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(configurationFilename))));
			HashMap<String, List<HashMap<String, Object>>> configurationFile = 
					(HashMap<String, List<HashMap<String, Object>>>) yaml.load(br);

			List<HashMap<String, Object>> list;
			// configuration
			configurationList = new ArrayList<Node>();
			list = configurationFile.get("configuration");    
			for (HashMap<String, Object> nodeInfo : list) {
				Node node = new Node((String)nodeInfo.get("name"), (String)nodeInfo.get("ip"), (Integer)nodeInfo.get("port"));
				configurationList.add(node);
			}
			
			// send rules
			sendRules = new ArrayList<Rule>();
			list = configurationFile.get("sendRules");  
			for (HashMap<String, Object> ruleInfo : list) {
				Rule rule = new Rule((String)ruleInfo.get("action"), (String)ruleInfo.get("src"), (String)ruleInfo.get("dest"), 
						(String)ruleInfo.get("kind"), (Integer)ruleInfo.get("seqNum"), (Boolean)ruleInfo.get("duplicate"));
				sendRules.add(rule);
			}
			// receive rules
			receiveRules  = new ArrayList<Rule>();
			list = configurationFile.get("receiveRules");  
			for (HashMap<String, Object> ruleInfo : list) {
				Rule rule = new Rule((String)ruleInfo.get("action"), (String)ruleInfo.get("src"), (String)ruleInfo.get("dest"), 
						(String)ruleInfo.get("kind"), (Integer)ruleInfo.get("seqNum"), (Boolean)ruleInfo.get("duplicate"));
				receiveRules.add(rule);
			}

            groups = new ArrayList<Group>();
            list = configurationFile.get("groups");
            for (HashMap<String, Object> groupInfo : list) {
                String groupName = (String) groupInfo.get("name");
                List<String> memberInfo = (List<String>) groupInfo.get("members");
                List<Node> memberList = new ArrayList<Node>();
                for (String member : memberInfo) {
                    memberList.add(getConfiguration(member));
                }
                groups.add(new Group(groupName, memberList));
            }

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// check if MD5 of configuration file changes, if so, re-load content of the configuration file
	public void checkUpdate (String configurationFilename) throws IOException, NoSuchAlgorithmException {
		byte[] newmdbytes = getMD5(configurationFilename);
		if (!Arrays.equals(mdbytes, newmdbytes)) {
			loadConfigurationFile(configurationFilename);
		}
	}

	public List<Node> getConfiguration() {
	    return configurationList;
	}

	public Node getConfiguration(String name) {
		for (Node node : configurationList) {
			if (name.equals(node.name))
				return node;
		}

		return null;
	}

	public List<Group> getGroups() {
	    return groups;
	}

    public Group getGroupByName(String groupName) {
        for (Group group : groups) {
            if (groupName.equals(group.name)) {
                return group;
            }
        }
        return null;
    }

	public Node getLoggerConfiguration() {
		return getConfiguration(Constant.LOGGER);
	}

	public String getSendAction(Message msg) {
		for (Rule rule : sendRules) {
			if (compareMessageRule(msg, rule))
				return rule.action;
		}
		return null;
	}

	public String getReceiveAction(Message msg) {
		for (Rule rule : receiveRules) {
			if (compareMessageRule(msg, rule))
				return rule.action;
		}
		return null;
	}

	private byte[] getMD5(String configurationFilename) throws NoSuchAlgorithmException, IOException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		FileInputStream fis = new FileInputStream(configurationFilename);

		byte[] dataBytes = new byte[1024];

		int nread = 0; 
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}
		fis.close();
		return  md.digest();
	}
	
	private boolean compareMessageRule (Message msg, Rule rule) {
		if (rule.src != null && !rule.src.equals(msg.getSource()) ||
				rule.dest != null && !rule.dest.equals(msg.getDest()) ||
				rule.kind != null && !rule.kind.equals(msg.getKind()) ||
				rule.seqNum != null && msg.getSequenceNumber() != rule.seqNum ||
				rule.duplicate != null && rule.duplicate != msg.isDuplicate())
			return false;
		else
			return true;    	
	}

	public static class Node {
		String name;
		String ip;
		Integer port;
		
		public Node (String name, String ip, Integer port) {
			this.name = name;
			this.ip = ip;
			this.port = port;
		}

		public String getName() {
		    return name;
		}

		public String getIp() {
		    return ip;
		}

		public Integer getPort() {
		    return port;
		}
	}
	
	public static class Rule {
		String action;
		String src;
		String dest;
		String kind;
		Integer seqNum;
		Boolean duplicate;
		
		public Rule (String action, String src, String dest, String kind, Integer seqNum, Boolean duplicate) {
			this.action = action;
			this.src = src;
			this.dest = dest;
			this.kind = kind;
			this.seqNum = seqNum;
			this.duplicate = duplicate;
		}
	}

    public static class Group {
        String name;
        List<Node> members;

        public Group(String name, List<Node> members) {
            this.name = name;
            this.members = members;
        }

        public String getName() {
            return name;
        }

        public List<Node> getMembers() {
            return members;
        }
    }
}
