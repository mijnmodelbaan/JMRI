package jmri.util.swing;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Paul Bender Copyright (C) 2017	
 */
public class BeanSelectCreatePanelTest {

    @Test
    public void testCTor() {
        jmri.TurnoutManager manager = jmri.InstanceManager.getDefault(jmri.TurnoutManager.class);
        jmri.Turnout turnout = manager.provideTurnout("IT1");
        BeanSelectCreatePanel t = new BeanSelectCreatePanel(manager,turnout);
        Assert.assertNotNull("exists",t);
    }

    // The minimal setup for log4J
    @Before
    public void setUp() {
        jmri.util.JUnitUtil.setUp();
        jmri.util.JUnitUtil.initInternalTurnoutManager();
    }

    @After
    public void tearDown() {
        jmri.util.JUnitUtil.tearDown();
    }

    private final static Logger log = LoggerFactory.getLogger(BeanSelectCreatePanelTest.class.getName());

}
