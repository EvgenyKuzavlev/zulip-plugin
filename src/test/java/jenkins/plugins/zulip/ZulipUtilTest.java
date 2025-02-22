package jenkins.plugins.zulip;

import hudson.model.Item;
import hudson.model.ItemGroup;
import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
public class ZulipUtilTest {

    @Mock
    private Jenkins jenkins;

    @Mock
    private DescriptorImpl descMock;

    @Mock
    private ItemGroup<?> rootItemGroupMock;

    @Mock
    private ItemAndItemGroup<?> parentItemGroupMock;

    @Mock
    private Item itemMock;

    @Before
    public void setUp() {
        // Mock Jenkins Instance
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
        PowerMockito.when(jenkins.getRootUrl()).thenReturn("http://JenkinsConfigUrl");
    }

    @Test
    public void testIsValueSet() {
        assertTrue(ZulipUtil.isValueSet("Test"));
        assertFalse(ZulipUtil.isValueSet(""));
        assertFalse(ZulipUtil.isValueSet(null));
    }

    @Test
    public void testGetDefaultValue() {
        assertEquals("Expect value", "Test", ZulipUtil.getDefaultValue("Test", "Default"));
        assertEquals("Expect default", "Default", ZulipUtil.getDefaultValue("", "Default"));
        assertEquals("Expect default", "Default", ZulipUtil.getDefaultValue(null, "Default"));
    }

    @Test
    public void testGetJenkinsUrl() {
        assertEquals("Expect Jenkins Configured Url", "http://JenkinsConfigUrl/", ZulipUtil.getJenkinsUrl(descMock));
        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        assertEquals("Expect Zulip config Url", "http://ZulipConfigUrl/", ZulipUtil.getJenkinsUrl(descMock));
    }

    @Test
    public void testDisplayObjectWithLink() {
        when(itemMock.getDisplayName()).thenReturn("MyJobName");

        // Default Jenkins root URL from the Jenkins class
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));

        // Custom Jenkins root URL from plugin config
        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        assertEquals("[MyJobName](http://ZulipConfigUrl/job/MyJob)", ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));

        // No Jenkins root URL at all
        PowerMockito.when(jenkins.getRootUrl()).thenReturn(null);
        when(descMock.getJenkinsUrl()).thenReturn(null);
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, "job/MyJob", descMock));
        assertEquals("MyJobName", ZulipUtil.displayObjectWithLink(itemMock, null, descMock));
    }

    @Test
    @SuppressWarnings("unchecked") // We need unchecked casts to ItemGroup (raw type) in order to mock methods that return ItemGroup<? extends Item>
    public void testDisplayJob() {
        when(itemMock.getDisplayName()).thenReturn("MyJobName");
        when(itemMock.getUrl()).thenReturn("job/MyJob");
        when(itemMock.getParent()).thenReturn((ItemGroup) parentItemGroupMock);
        // Jenkins is always at the root of the job tree
        when(parentItemGroupMock.getParent()).thenReturn((ItemGroup)jenkins);
        // Make sure Jenkins has a name, otherwise we might miss a bug where Jenkins is displayed unexpectedly.
        PowerMockito.when(jenkins.getDisplayName()).thenReturn("Jenkins");

        // Parent with a name => Display full path when requested
        when(parentItemGroupMock.getDisplayName()).thenReturn("ParentName");
        when(parentItemGroupMock.getUrl()).thenReturn("job/Parent");
        assertEquals("[ParentName](http://JenkinsConfigUrl/job/Parent) » [MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent without a URL => don't add a link
        when(parentItemGroupMock.getDisplayName()).thenReturn("ParentName");
        when(parentItemGroupMock.getUrl()).thenReturn(null);
        assertEquals("ParentName » [MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent with an empty name => display the job only
        // This can happen: see hudson.model.AbstractItem.getFullDisplayName
        when(parentItemGroupMock.getDisplayName()).thenReturn("");
        when(parentItemGroupMock.getUrl()).thenReturn("view/RootUrl");
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Parent without a name => display the job only
        // Not sure this can happen, but let's be safe
        when(parentItemGroupMock.getDisplayName()).thenReturn(null);
        when(parentItemGroupMock.getUrl()).thenReturn("view/RootUrl");
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://JenkinsConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // Custom Jenkins root URL from plugin config
        when(descMock.getJenkinsUrl()).thenReturn("http://ZulipConfigUrl/");
        when(parentItemGroupMock.getDisplayName()).thenReturn("ParentName");
        when(parentItemGroupMock.getUrl()).thenReturn("job/Parent");
        assertEquals("[ParentName](http://ZulipConfigUrl/job/Parent) » [MyJobName](http://ZulipConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("[MyJobName](http://ZulipConfigUrl/job/MyJob)", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));

        // No Jenkins root URL at all
        PowerMockito.when(jenkins.getRootUrl()).thenReturn(null);
        when(descMock.getJenkinsUrl()).thenReturn(null);
        when(parentItemGroupMock.getDisplayName()).thenReturn("ParentName");
        when(parentItemGroupMock.getUrl()).thenReturn("job/Parent");
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, true));
        assertEquals("ParentName » MyJobName", ZulipUtil.displayItem(itemMock, descMock, true, false));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, true));
        assertEquals("MyJobName", ZulipUtil.displayItem(itemMock, descMock, false, false));
    }

}
