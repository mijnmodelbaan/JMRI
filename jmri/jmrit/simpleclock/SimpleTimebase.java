// Timebase.java

package jmri.jmrit.simpleclock;

import java.util.Date;
import jmri.Timebase;
import jmri.Sensor;

/**
 * Provide basic Timebase implementation from system clock.
 * <P>
 * This implementation provides for the internal clock and for one hardware clock.
 * <P>
 * The setTimeValue member is the fast time when the clock
 * started.  The startAtTime member is the wall-clock time
 * when the clock was started.  Together, those can be used
 * to calculate the current fast time.
 * <P>
 * The pauseTime member is used to indicate that the
 * timebase was paused. If non-null, it indicates the
 * current fast time when the clock was paused.
 *
 * @author			Bob Jacobsen Copyright (C) 2004, 2007
 *                  Dave Duchamp - 2007 additions/revisions for handling one hardware clock
 * @version			$Revision: 1.7 $
 */
public class SimpleTimebase implements Timebase {

	public SimpleTimebase() {
		// set to start counting from now
		setTime(new Date());
		pauseTime = null;
		// initialize start/stop sensor for time running
		clockSensor = jmri.InstanceManager.sensorManagerInstance().provideSensor("ISClockRunning");
		if (clockSensor!=null) {
			try {				
				clockSensor.setKnownState(Sensor.ACTIVE);
			} catch (jmri.JmriException e) {
				log.warn("Exception setting ISClockRunning sensor ACTIVE: "+e);
			}
			clockSensor.addPropertyChangeListener(clockSensorListener =
					new java.beans.PropertyChangeListener() {
				public void propertyChange(java.beans.PropertyChangeEvent e) {
					clockSensorChanged();
				}
			});
		}		
	}
	
    // methods for getting and setting the current Fast Clock time
    public Date getTime() {
    	// is clock stopped?
    	if (pauseTime!=null) return new Date(pauseTime.getTime()); // to ensure not modified outside
    	// clock running
    	long elapsedMSec = (new Date()).getTime() - startAtTime.getTime();
    	long nowMSec = setTimeValue.getTime()+(long)(mFactor*(double)elapsedMSec);
    	return new Date(nowMSec);
    }    	
    public void setTime(Date d) {
    	startAtTime = new Date();	// set now
    	setTimeValue = new Date(d.getTime());   // to ensure not modified from outside
		if (synchronizeWithHardware || correctHardware || (!internalMaster)) {
			jmri.InstanceManager.clockControlInstance().setTime(d);
		}
    	if (pauseTime != null)
    		pauseTime = setTimeValue;  // if stopped, continue stopped at new time
    	handleAlarm();
    }

	// methods for starting and stopping the Fast Clock and returning status
    public void setRun(boolean run) {
    	if (run && pauseTime!=null) {
    		// starting of stopped clock
    		setTime(pauseTime);
			if (synchronizeWithHardware || correctHardware || (!internalMaster)) {
				jmri.InstanceManager.clockControlInstance().startHardwareClock(getTime());
			}
			pauseTime = null;
			if (clockSensor!=null) {
				try {				
					clockSensor.setKnownState(Sensor.ACTIVE);
				} catch (jmri.JmriException e) {
					log.warn("Exception setting ISClockRunning sensor ACTIVE: "+e);
				}
			}
    	} else if (!run && pauseTime == null) {
    		// stopping of running clock:
    		// Store time it was stopped, and stop it
    		pauseTime = getTime();
			if (synchronizeWithHardware || correctHardware || (!internalMaster)) {
				jmri.InstanceManager.clockControlInstance().stopHardwareClock();
			}
			if (clockSensor!=null) {
				try {				
					clockSensor.setKnownState(Sensor.INACTIVE);
				} catch (jmri.JmriException e) {
					log.warn("Exception setting ISClockRunning sensor INACTIVE: "+e);
				}
			}			    		
    	}
    	firePropertyChange("run", new Boolean(run), new Boolean(!run));
    	handleAlarm();
    }    
    public boolean getRun() { return pauseTime == null; }

	// methods for setting and getting rate
    public void setRate(double factor) {
        if (factor < 0.1 || factor > 100) {
            log.error("rate of "+factor+" is out of reasonable range, set to 1");
            factor = 1;
        }
    	double oldFactor = mFactor;
    	Date now = getTime();
        // actually make the change
    	mFactor = factor;
		if (synchronizeWithHardware || correctHardware || (!internalMaster)) {
			jmri.InstanceManager.clockControlInstance().setRate(factor);
		}
        // make sure time is right with new rate
        setTime(now);
        // notify listeners
    	firePropertyChange("rate", new Double(factor), new Double(oldFactor));
    	handleAlarm();
    }
    public double getRate() { return mFactor; }
	
	public void setInternalMaster(boolean master, boolean update) {
		if (master!=internalMaster) {
			internalMaster = master;
			if (update) {
				jmri.InstanceManager.clockControlInstance().initializeHardwareClock(mFactor,
													getTime(), false);
			}
			if (internalMaster)
				masterName = "";
			else
				masterName = jmri.InstanceManager.clockControlInstance().getHardwareClockName();
		}
	}
	public boolean getInternalMaster() {return internalMaster;}
	
	public void setMasterName(String name) {masterName = name;}
	public String getMasterName() {return masterName;}
	
	public void setSynchronize(boolean synchronize, boolean update) {
		if (synchronizeWithHardware!=synchronize) {
			synchronizeWithHardware = synchronize;
			if (update) {
				jmri.InstanceManager.clockControlInstance().initializeHardwareClock(mFactor,
													getTime(), false);
			}
		}
	}
	public boolean getSynchronize() {return synchronizeWithHardware;}
	
	public void setCorrectHardware(boolean correct, boolean update) {
		if (correctHardware!=correct) {
			correctHardware = correct;
			if (update) {
				jmri.InstanceManager.clockControlInstance().initializeHardwareClock(mFactor,
												  getTime(), false);
			}
		}
	}
	public boolean getCorrectHardware() {return correctHardware;}
	
	public void setStartStopped(boolean stopped) {
		startStopped = stopped;
	}
	public boolean getStartStopped() {return startStopped;}
	
	public void setStartSetTime(boolean set, Date time) {
		startSetTime = set;
		startTime = time;
	}
	public boolean getStartSetTime() {return startSetTime;}
	public Date getStartTime() {return startTime;}
	
	public void setStartClockOption(int option) {startClockOption = option;}
	public int getStartClockOption() {return startClockOption;}
	// Note the following method should only be invoked at start up
	public void initializeClock () {
		switch (startClockOption) {
			case NIXIE_CLOCK:
				jmri.jmrit.nixieclock.NixieClockFrame f = 
										new jmri.jmrit.nixieclock.NixieClockFrame();
				f.setVisible(true);
				break;
			case ANALOG_CLOCK:
				jmri.jmrit.analogclock.AnalogClockFrame g = 
										new jmri.jmrit.analogclock.AnalogClockFrame();
				g.setVisible(true);			
				break;
			case LCD_CLOCK:
				jmri.jmrit.lcdclock.LcdClockFrame h = 
										new jmri.jmrit.lcdclock.LcdClockFrame();
				h.setVisible(true);			
				break;
		}
	}

	/**
	 * Method to initialize hardware clock at start up
	 * Note: This method is always called at start up after all options have been set. 
	 *		It should be ignored if there is no communication with a hardware clock.
	 */
	public void initializeHardwareClock() {
		if (synchronizeWithHardware || correctHardware || !internalMaster) {
			if (startStopped) {
				jmri.InstanceManager.clockControlInstance().initializeHardwareClock(0,
						getTime(),(!internalMaster && !startSetTime));
			}
			else {
				jmri.InstanceManager.clockControlInstance().initializeHardwareClock(mFactor,
						getTime(),(!internalMaster && !startSetTime));
			}
		}
	}
	
    java.beans.PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    protected void firePropertyChange(String p, Object old, Object n) { 
    	pcs.firePropertyChange(p,old,n);
    }
	
	/**
	 * Handle a change in the clock running sensor
	 */
	private void clockSensorChanged() {
		if (clockSensor.getKnownState() == Sensor.ACTIVE) {
			// simply return if clock is already running
			if (pauseTime==null) return;
			setRun(true);
		}
		else {
			// simply return if clock is already stopped
			if (pauseTime!=null) return;
			setRun(false);
		}
	}	

    /**
     * Request a call-back when the bound Rate or Run property changes.
     * <P>
     * Not yet implemented.
     */
    public synchronized void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    /**
     * Remove a request for a call-back when a bound property changes.
     * <P>
     * Not yet implemented.
     */
    public synchronized void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    /**
     * Remove references to and from this object, so that it can
     * eventually be garbage-collected.
     * 
     */
    public void dispose() {}
	
	/**
	 * Timebase variables and options
	 */
	private double mFactor = 1.0;
	private Date startAtTime;
	private Date setTimeValue;
	private Date pauseTime;   // null value indicates clock is running
	private Sensor clockSensor = null;   // active when clock is running, inactive when stopped
	private java.beans.PropertyChangeListener clockSensorListener = null;
	private boolean internalMaster = true;     // false indicates a hardware clock is the master
	private String masterName = "";		// name of hardware time source, if not internal master
	private boolean synchronizeWithHardware = true;  // true indicates need to synchronize
	private boolean correctHardware = true;    // true indicates hardware correction requested
	private boolean startStopped = false;    // true indicates start up with clock stopped requested
	private boolean startSetTime = false;    // true indicates set fast clock to specified time at 
															//start up requested
	private Date startTime = new Date();	// specified time for setting fast clock at start up
	private int startClockOption = NONE;	// request start of a clock at start up

	javax.swing.Timer timer = null;
    java.beans.PropertyChangeSupport pcMinutes = new java.beans.PropertyChangeSupport(this);
	
	/**
	 * Start the minute alarm ticking, if it isnt
	 * already.
	 */
	void startAlarm() {
	    if (timer == null) handleAlarm();
	}
	
	int oldMinutes = 0;
	/**
	 * Handle an "alarm", which is used to count off minutes.
	 *<P>
	 * Listeners won't be notified if the minute value hasn't changed since the last time.
	 */	 
	void handleAlarm() {
	    // on first pass, set up the timer to call this routine
	    if (timer==null) {
            timer = new javax.swing.Timer(60*1000, new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        handleAlarm();
                    }
                });
        }

        timer.stop();
        Date date = getTime();
        int waitSeconds = 60-date.getSeconds();
	    int delay = (int)(waitSeconds*1000/mFactor)+100;  // make sure you miss the time transition
        timer.setInitialDelay(delay);
        timer.setRepeats(true);     // in case we run by
        timer.start();
        
        // and notify the others
        int minutes = date.getMinutes();
    	if (minutes!=oldMinutes) 
    	    pcMinutes.firePropertyChange("minutes", new Double(oldMinutes), new Double(minutes));

        oldMinutes = minutes;

    }

    /**
     * Request a call-back when the minutes place of the time changes.
     */
    public void addMinuteChangeListener(java.beans.PropertyChangeListener l) {
        pcMinutes.addPropertyChangeListener(l);
        startAlarm();
    }

    /**
     * Remove a request for call-back when the minutes place of the time changes.
     */
    public void removeMinuteChangeListener(java.beans.PropertyChangeListener l) {
        pcMinutes.removePropertyChangeListener(l);
    }


    static org.apache.log4j.Category log = org.apache.log4j.Category.getInstance(SimpleTimebase.class.getName());

}

/* @(#)Timebase.java */
