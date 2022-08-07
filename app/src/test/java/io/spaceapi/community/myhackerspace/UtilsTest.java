package io.spaceapi.community.myhackerspace;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UtilsTest {
    @Test
    public void testJoinStrings() {
        Assert.assertEquals("foo / bar", Utils.joinStrings(" / ", "foo", "bar"));
        Assert.assertEquals("foo.bar.baz", Utils.joinStrings(".", "foo", "bar", "baz"));
        Assert.assertEquals("foo.baz", Utils.joinStrings(".", "foo", null, "baz"));
        Assert.assertEquals("foo.baz", Utils.joinStrings(".", null, "foo", null, "baz"));
        Assert.assertNull(Utils.joinStrings(" / ", null, null, null));
        Assert.assertNull(Utils.joinStrings(" / "));
    }
}
