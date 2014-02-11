package ds.lab.util.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ds.lab.util.ConfigParser;
import ds.lab.util.ConfigParser.Group;
import ds.lab.util.ConfigParser.Node;

public class ConfigParserTest {

    @Test
    public void testGetGroups() {
        ConfigParser configParser = new ConfigParser("config.txt");
        // HashMap<String, List<HashMap<String, Object>>> list =
        // configParser.get
        List<Group> groups = configParser.getGroups();
        assertEquals(2, groups.size());
    }

    @Test
    public void testGroupName() {
        ConfigParser configParser = new ConfigParser("config.txt");
        List<Group> groups = configParser.getGroups();
        assertEquals(groups.get(0).getName(), "Group1");
        assertEquals(groups.get(1).getName(), "Group2");
    }

    @Test
    public void testGroupMembers() {
        ConfigParser configParser = new ConfigParser("config.txt");
        List<Group> groups = configParser.getGroups();
        List<Node> members1 = groups.get(0).getMembers();
        List<Node> members2 = groups.get(1).getMembers();
        assertEquals(members1.size(), 3);
        assertEquals(members2.size(), 3);
        assertEquals(members1.get(0).getName(), "alice");
        assertEquals(members1.get(1).getName(), "bob");
        assertEquals(members1.get(2).getName(), "charlie");
        assertEquals(members2.get(0).getName(), "daphnie");
        assertEquals(members2.get(1).getName(), "bob");
        assertEquals(members2.get(2).getName(), "charlie");
    }

    @Test
    public void testGetGroupByName() {
        ConfigParser configParser = new ConfigParser("config.txt");
        Group group = configParser.getGroupByName("Group1");
        assertEquals(group.getName(), "Group1");
    }
}
