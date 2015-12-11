// DCCppMonPane.java
package jmri.jmrix.dccpp.swing.mon;

import jmri.jmrix.dccpp.DCCppConstants;
import jmri.jmrix.dccpp.DCCppListener;
import jmri.jmrix.dccpp.DCCppMessage;
import jmri.jmrix.dccpp.DCCppReply;
import jmri.jmrix.dccpp.DCCppSystemConnectionMemo;
import jmri.jmrix.dccpp.DCCppTrafficController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Panel displaying (and logging) DCC++ messages derived from DCCppMonFrame.
 *
 * @author	Bob Jacobsen Copyright (C) 2002
 * @author      Paul Bender Copyright (C) 2004-2014
 * @author      Giorgio Terdina Copyright (C) 2007
 * @author      Mark Underwood Copyright (C) 2015
 * @version $Revision$
 */
public class DCCppMonPane extends jmri.jmrix.AbstractMonPane implements DCCppListener {

    final java.util.ResourceBundle rb
            = java.util.ResourceBundle.
            getBundle("jmri.jmrix.dccpp.swing.DCCppSwingBundle");

    protected DCCppTrafficController tc = null;
    protected DCCppSystemConnectionMemo memo = null;

    @Override
    public String getTitle() {
        return (rb.getString("DCCppMonFrameTitle"));
    }

    @Override
    public void initContext(Object context) {
        if (context instanceof DCCppSystemConnectionMemo) {
            memo = (DCCppSystemConnectionMemo) context;
            tc = memo.getDCCppTrafficController();
            // connect to the TrafficController
            tc.addDCCppListener(~0, this);
        }
    }

    /**
     * Initialize the data source.
     */
    @Override
    protected void init() {
    }

    @Override
    public void dispose() {
        // disconnect from the LnTrafficController
        tc.removeDCCppListener(~0, this);
        // and unwind swing
        super.dispose();
    }

    public synchronized void message(DCCppReply l) {				// receive a DCC++ message and log it
        // display the raw data if requested
	// Since DCC++ is text-based traffic, this is good enough for now.
	// TODO: Provide "beautified" output later.
        StringBuilder raw = new StringBuilder();
        if (rawCheckBox.isSelected()) {
            raw.append(l.toString());
        } 
	
	// Beautify and display
	String text = new String();

	switch(l.getOpCodeChar()) {
	case DCCppConstants.THROTTLE_REPLY:
	    text = "Throttle Reply: \n";
	    text += "\tRegister: " + l.getRegisterString() + "\n";
	    text += "\tSpeed: " + l.getSpeedString() + "\n";
	    text += "\tDirection: " + l.getDirectionString() + "\n";
	    break;
	case DCCppConstants.TURNOUT_REPLY:
	    text = "Turnout Reply: \n";
	    text += "\tT/O Number: " + l.getTOIDString()  + "\n";
	    text += "\tDirection: " + l.getTOStateString() + "\n";
	    break;
	case DCCppConstants.SENSOR_REPLY_H:
	    text = "Sensor Reply (Inactive): \n";
	    text += "\tSensor Number: " + l.getSensorNumString()  + "\n";
	    text += "\tState: INACTIVE\n";
	    break;
	case DCCppConstants.SENSOR_REPLY_L:
	    // Also covers the V1.0 version SENSOR_REPLY
            if (l.isSensorDefReply()) {
                text = "Sensor Def Reply: \n";
                text += "\tSensor Number: " + l.getSensorDefNumString() + "\n";
                text += "\tSensor Pin: " + l.getSensorDefPinString() + "\n";
                text += "\tSensor Pullup: " + l.getSensorDefPullupString() + "\n";
            } else {
                text = "Sensor Reply (Active): \n";
                text += "\tSensor Number: " + l.getSensorNumString()  + "\n";
                text += "\tState: ACTIVE\n";
            }
	    break;
        case DCCppConstants.OUTPUT_REPLY:
            text = "Output Reply: \n";
            text += "\tOutput Number: " + l.getOutputNumString() + "\n";
            text += "\tOutputState: " + l.getOutputCmdStateString() + "\n";
            break;
	case DCCppConstants.PROGRAM_REPLY:
	    text = "Program Reply: \n";
	    text += "\tCallback Num: " + l.getCallbackNumString()  + "\n";
	    text += "\tCallback Sub: " + l.getCallbackSubString()  + "\n";
	    text += "\tCV Value: " + l.getCVString()  + "\n";
	    break;
	case DCCppConstants.VERSION_REPLY:
	    text = "Base Station Status: \n";
            text += "\tVersion: " + l.getStatusVersionString() + "\n";
            text += "\tBuild: " + l.getStatusBuildDateString() + "\n";
	    break;
	case DCCppConstants.POWER_REPLY:
	    text = "Power Status: ";
	    text += ((char)(l.getElement(1) & 0x00FF) == '1' ? "ON" : "OFF");
	    break;
	case DCCppConstants.CURRENT_REPLY:
	    text = "Current: " + l.getCurrentString() + " / 1024";
	    break;
//	case DCCppConstants.LISTPACKET_REPLY:
//	    // TODO: Implement this fully
//	    text = "List Packet Reply...\n";
//	    break;
        case DCCppConstants.WRITE_EEPROM_REPLY:
            text = "Write EEPROM Reply...\n";
            // TODO: Don't use getValueString()
            text += "\tTurnouts: " + l.getValueString(1) + "\n";
            text += "\tSensors: " + l.getValueString(2) + "\n";
        case DCCppConstants.MEMORY_REPLY:
	    // TODO: Implement this fully
	    text = "Memory Reply...\n";
            text += "\tFree Memory: " + l.getFreeMemoryString() + "\n";
	    break;
        case DCCppConstants.COMM_TYPE_REPLY:
            text = "Comm Port: " + l.getValueString(2) + "\n";
            break;
	default:
	    text += "Unregonized reply: ";
	}

	// we use Llnmon to format, expect it to provide consistent \n after each line
	nextLine(text + "\n", new String(raw));
    }

    // listen for the messages to the Base Station
    @SuppressWarnings("fallthrough")
    public synchronized void message(DCCppMessage l) {
        // display the raw data if requested  
	// Since DCC++ is text-based traffic, this is good enough for now.
	// TODO: Provide "beautified" output later.
        StringBuilder raw = new StringBuilder("packet: ");
        if (rawCheckBox.isSelected()) {
            raw.append(l.toString());
        }

	// Beautify and display
	String text = new String();

	switch(l.getOpCodeChar()) {
	case DCCppConstants.THROTTLE_CMD:
	    text = "Throttle Cmd: ";
	    text += "\n\tRegister: " + l.getRegisterString();
	    text += "\n\tAddress: " + l.getAddressString();
	    text += "\n\tSpeed: " + l.getSpeedString();
	    text += "\n\t:Direction: " + l.getDirectionString();
	    text += "\n";
	    break;
	case DCCppConstants.FUNCTION_CMD:
	    text = "Function Cmd: ";
	    text += "\n\tAddress: " + l.getFuncAddressString();
	    text += "\n\tByte 1: " + l.getFuncByte1String();
	    text += "\n\tByte 2: " + l.getFuncByte2String();
	    text += "\n\t(No Reply Expected)\n";
	    break;
	case DCCppConstants.ACCESSORY_CMD:
	    text = "Accessory Decoder Cmd: ";
	    text += "\n\tAddress: " + l.getAccessoryAddrString();
	    text += "\n\tSubaddr: " + l.getAccessorySubString();
	    text += "\n\tState: " + l.getAccessoryStateString() + "\n";
	    break;
	case DCCppConstants.TURNOUT_CMD:
	    if (l.isTurnoutAddMessage()) {
		text = "Add Turnout: ";
		text += "\n\tT/O ID: " + l.getTOIDString();
		text += "\n\tAddress: " + l.getTOAddressString();
		text += "\n\tSubaddr: " + l.getTOSubAddressString();
		text += "\n";
	    } else if (l.isTurnoutDeleteMessage()) {
		text = "Delete Turnout: ";
		text += "\n\tT/O ID: " + l.getTOIDString();
		text += "\n";
	    } else if (l.isListTurnoutsMessage()) {
		text = "List Turnouts...\n";
	    } else {
		text = "Turnout Cmd: ";
		text += "\n\tT/O ID: " + l.getTOIDString();
		text += "\n\tState: " + l.getTOStateString();
		text += "\n";
	    }
	    break;
        case DCCppConstants.OUTPUT_CMD:
            text = "Output Cmd: ";
            text += "\n\tOutput ID: " + l.getOutputIDString();
            text += "\n\tState: " + l.getOutputStateString();
            text += "\n";
        case DCCppConstants.OUTPUT_DEF_CMD:
            if (l.isOutputAddMessage()) {
                text = "Add Output: ";
                text += "\n\tOutput ID: " + l.getOutputIDString();
                text += "\n\tPin: " + l.getOutputPinString();
                text += "\n";
            } else if (l.isOutputDeleteMessage()) {
                text = "Add Output: ";
                text += "\n\tOutput ID: " + l.getOutputIDString();
                text += "\n";
            } else if (l.isListOutputsMessage()) {
                text = "List Outputs...\n";
            }
	case DCCppConstants.SENSOR_CMD:
	    if (l.isSensorAddMessage()) {
		text = "Add Sensor: ";
		text += "\n\tSensor ID: " + l.getSensorIDString();
		text += "\n\tPin: " + l.getSensorPinString();
		text += "\n\tPullup: " + l.getSensorPullupString();
		text += "\n";
	    } else if (l.isSensorDeleteMessage()) {
		text = "Delete Sensor: ";
		text += "\n\tSensor ID: " + l.getSensorIDString();
		text += "\n";
	    } else if (l.isListSensorsMessage()) {
		text = "List Sensors...\n";
	    } else {
		text = "Unknown Sensor Cmd... \n";
	    }
	    break;
	case DCCppConstants.OPS_WRITE_CV_BYTE:
	    text = "Ops Write Byte Cmd: \n"; // <w cab cv val>
            text += "\tAddress: " + l.getOpsWriteAddrString() + "\n";
            text += "\tCV: " + l.getOpsWriteCVString() + "\n";
            text += "\tValue: " + l.getOpsWriteValueString() + "\n";
	    break;
	case DCCppConstants.OPS_WRITE_CV_BIT: // <b cab cv bit val>
	    text = "Ops Write Bit Cmd: \n";
            text += "\tAddress: " + l.getOpsWriteAddrString() + "\n";
            text += "\tCV: " + l.getOpsWriteCVString() + "\n";
            text += "\tBit: " + l.getOpsWriteBitString() + "\n";
            text += "\tValue: " + l.getOpsWriteValueString() + "\n";
	    break;
	case DCCppConstants.PROG_WRITE_CV_BYTE:
	    text = "Prog Write Byte Cmd: ";
	    text += "\n\tCV : " + l.getCVString();
	    text += "\n\tValue: " + l.getValueString();
	    text += "\n\tCallback Num: " + l.getCallbackNumString();
	    text += "\n\tCallback Sub: " + l.getCallbackSubString() + "\n";
	    break;
	    
	case DCCppConstants.PROG_WRITE_CV_BIT:
	    text = "Prog Write Bit Cmd: ";
	    text += "\n\tCV : " + l.getCVString();
	    text += "\n\tBit : " + l.getBitString();
	    text += "\n\tValue: " + l.getValueString();
	    text += "\n\tCallback Num: " + l.getCallbackNumString();
	    text += "\n\tCallback Sub: " + l.getCallbackSubString() + "\n";
	    break;
	case DCCppConstants.PROG_READ_CV:
	    text = "Prog Read Cmd: ";
	    text += "\n\tCV: " + l.getCVString();
	    text += "\n\tCallback Num: " + l.getCallbackNumString();
	    text += "\n\tCallback Sub: " + l.getCallbackSubString() + "\n";
	    break;
	case DCCppConstants.TRACK_POWER_ON:
	    text = "Track Power ON Cmd ";
	    break;
	case DCCppConstants.TRACK_POWER_OFF:
	    text = "Track Power OFF Cmd ";
	    break;
	case DCCppConstants.READ_TRACK_CURRENT:
	    text = "Read Track Current Cmd ";
	    break;
	case DCCppConstants.READ_CS_STATUS:
	    text = "Status Cmd ";
	    break;
	case DCCppConstants.WRITE_DCC_PACKET_MAIN:
            text = "Write DCC Packet Main Cmd: ";
            break;
	case DCCppConstants.WRITE_DCC_PACKET_PROG:
	    text = "Write DCC Packet Prog Cmd: ";
	    break;
	case DCCppConstants.GET_FREE_MEMORY:
	    text = "Get Free Memory Cmd: ";
	    break;
	case DCCppConstants.LIST_REGISTER_CONTENTS:
	    text = "List Register Contents Cmd: ";
	    break;
	default:
	    text = "Unknown Message: ";
	}
	
        // we use Llnmon to format, expect it to provide consistent \n after each line
        nextLine(text + "\n", new String(raw));

    }

    // Handle a timeout notification
    public void notifyTimeout(DCCppMessage msg) {
        if (log.isDebugEnabled()) {
            log.debug("Notified of timeout on message" + msg.toString());
        }
    }

    /**
     * We need to calculate the locomotive address when doing the translations
     * back to text. XPressNet Messages will have these as two elements, which
     * need to get translated back into a single address by reversing the
     * formulas used to calculate them in the first place.
     */
    /* NOT USED
    private int calcLocoAddress(int AH, int AL) {
        if (AH == 0x00) {
            // if AH is 0, this is a short address
            return (AL);
        } else {
            // This must be a long address
            int address = 0;
            address = ((AH * 256) & 0xFF00);
            address += (AL & 0xFF);
            address -= 0xC000;
            return (address);
        }
    }
    */

    /**
     * Nested class to create one of these using old-style defaults
     */
    static public class Default extends jmri.util.swing.JmriNamedPaneAction {

        public Default() {
            super(java.util.ResourceBundle.
                    getBundle("jmri.jmrix.dccpp.swing.DCCppSwingBundle").
                    getString("DCCppMonFrameTitle"), DCCppMonPane.class.getName());
            setContext(jmri.InstanceManager.
                    getDefault(DCCppSystemConnectionMemo.class));
        }
    }

    static Logger log = LoggerFactory.getLogger(DCCppMonPane.class.getName());

}