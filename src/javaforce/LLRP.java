package javaforce;

/** LLRP API read/write tags from LLRP compatible controllers.
 *
 * Uses the LLRP Toolkit: https://sourceforge.net/projects/llrp-toolkit
 *
 * Javadoc : http://llrp.org/docs/javaapidoc
 *
 * @author Peter Quiring
 */

import java.util.*;

import org.llrp.ltk.net.*;
import org.llrp.ltk.types.*;
import org.llrp.ltk.util.*;
import org.llrp.ltk.generated.messages.*;
import org.llrp.ltk.generated.parameters.*;
import org.llrp.ltk.generated.interfaces.*;
import org.llrp.ltk.generated.enumerations.*;

public class LLRP implements LLRPEndpoint {
  private static int delay = 10;

  private static int rospecid = 101;
  private static int inventoryparamid = 102;
  private static int accessid = 103;
  private static int opspecid = 104;

  private ROSpec rospec;
  private AccessSpec accessspec;
  private LLRPConnector llrp;
  private LLRPEvent events;
  private String ip;
  private int[] powerIndexes = new int[50];
  private int impinj_search_mode = -1;
  private int period = -1;
  private int duration = -1;
  private int gpi = -1;
  private int mode = 1002;
  private int sensitivity = 1;
  private int session = 1;
  private boolean enableAccessSpec = false;

  public static final int IMPINJ_SEARCH_MODE_SINGLE = 1;
  public static final int IMPINJ_SEARCH_MODE_DUAL = 2;
  public static final int IMPINJ_SEARCH_MODE_FOCUS = 3;
  public static final int IMPINJ_SEARCH_MODE_SINGLE_RESET = 5;
  public static final int IMPINJ_SEARCH_MODE_DUAL_B_TO_A = 6;

  public boolean active;
  public int log;
  public long lastMsg;

  public static boolean debug;

  /** Connects to LLRP Controller. */
  public boolean connect(String ip, int time_out_ms) {
    this.ip = ip;
    llrp = new LLRPConnector(this, ip);
    try {
      llrp.connect(time_out_ms);
      return true;
    } catch (Exception e) {
      try {
        llrp.disconnect();  //ensure it's disconnected
      } catch (Exception e2) {}
      llrp = null;
      if (debug) JFLog.log(log, e);
      return false;
    }
  }

  /** Connects to LLRP Controller. time out = 30 seconds. */
  public boolean connect(String ip) {
    return connect(ip, 30000);
  }

  /** Disconnects from LLRP Controller. */
  public void disconnect() {
    if (llrp == null) return;
    llrp.disconnect();
    llrp = null;
  }

  /** Pings LLRP Controller to keep connection alive. */
  public void ping() {
    if (llrp == null) return;
    try {
      long prevMsg = lastMsg;
      //some readers do not support it, but it will still "test" the connection
      {
        KEEPALIVE msg = new KEEPALIVE();
        llrp.send(msg);
        JF.sleep(delay);
      }
      int cnt = 0;
      while (prevMsg == lastMsg) {
        JF.sleep(delay);
        cnt++;
        if (cnt == 1000) {  //10 secs
          break;
        }
      }
      if (prevMsg == lastMsg) {
        throw new Exception("ping:no reply");
      }
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      active = false;
    }
  }

  /** Sets Event Listener to read tags. */
  public void setEventsListener(LLRPEvent events) {
    this.events = events;
  }

  /** Sets RSSI threshold.  Tags below this value are ignored.
   * @param value = RSSI threshold index (default = 1)
   */
  public void setRSSIThreshold(int value) {
    sensitivity = value;
  }

  /** Sets power indexes for each antenna (see getPowerLevels())
   *
   * Default = new int[] {50}
   *
   */
  public void setPowerIndexes(int[] powerIndexes) {
    this.powerIndexes = powerIndexes;
  }

  /** Set reader mode index (0-5,1000-1005)
   * See : https://support.impinj.com/hc/en-us/articles/360000046899-Reader-Modes-Made-Easy
   *
   * Default = 1002
   *
   * @param mode
   */
  public void setModeIndex(int mode) {
    this.mode = mode;
  }

  /** Sets reader session (default = 1)
   *
   * @param session (0-3)
   *
   */
  public void setSession(int session) {
    this.session = session;
  }

  /** Set period of start trigger and duration of stop trigger.
   *
   * @param period = period in ms (-1 = disabled)
   * @param duration = duration is ms
   */
  public void setPeriod(int period, int duration) {
    this.period = period;
    this.duration = duration;
  }

  /** Set GPI as start trigger.
   *
   * @param port = GPI port (1-65535)
   */
  public void setGPITrigger(int port) {
    gpi = port;
  }

  /** Set GPI as start trigger with duration.
   *
   * @param port = GPI port (1-65535)
   * @param duration = duration in ms
   */
  public void setGPITrigger(int port, int duration) {
    gpi = port;
    this.duration = duration;
  }

  /** Set GPO port to state.
   *
   * @param  port = GPO port (1-65535)
   * @param state = value to write
   */
  public void setGPOState(int port, boolean state) {
    SET_READER_CONFIG msg = new SET_READER_CONFIG();
    msg.setResetToFactoryDefault(new Bit(false));
    GPOWriteData write = new GPOWriteData();
    write.setGPOPortNumber(new UnsignedShort(port));
    write.setGPOData(new Bit(state));
    ArrayList<GPOWriteData> list = new ArrayList<GPOWriteData>();
    list.add(write);
    msg.setGPOWriteDataList(list);
    llrp.send(msg);
  }

  /** Include an Access Spec with RO Spec during inventory scan.
   *
   * Access Spec can provide deep EPC memory reads.
   */
  public void setEnableAccessSpec(boolean access) {
    this.enableAccessSpec = access;
  }

  /** Sets Impinj Search Mode (custom parameter)
   * @param mode = 1-6 (see IMPINJ_SEARCH_MODE_...)  (-1 = disabled)
   */
  public void setImpinjSearchMode(int mode) {
    impinj_search_mode = mode;
  }

  public void setLog(int log) {
    this.log = log;
  }

  /** Starts reading RFID tags with inventory scans.
   *
   * Use stop() to stop scanning.
   *
   * @return scanning active
   */
  public boolean startInventoryScan() {
    if (llrp == null) return false;
    active = true;
    try {
      rospec = createROSpec();
      if (enableAccessSpec) {
        accessspec = createReadAccessSpec(rospec.getROSpecID());
      } else {
        accessspec = null;
      }
      //reset reader
      {
        SET_READER_CONFIG msg = new SET_READER_CONFIG();
        msg.setResetToFactoryDefault(new Bit(true));
//        JFLog.log("reset_reader");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //delete all RO specs
      {
        DELETE_ROSPEC msg = new DELETE_ROSPEC();
        msg.setROSpecID(new UnsignedInteger(0));
//        JFLog.log("delete all RO specs");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //delete all Access Specs
      {
        DELETE_ACCESSSPEC msg = new DELETE_ACCESSSPEC();
        msg.setAccessSpecID(new UnsignedInteger(0));
//        JFLog.log("delete all ACCESS specs");
        llrp.send(msg);
        JF.sleep(delay);
      }
      {
        SET_READER_CONFIG msg = new SET_READER_CONFIG();
        msg.setResetToFactoryDefault(new Bit(false));
        if (gpi != -1) {
          ArrayList<GPIPortCurrentState> list = new ArrayList<GPIPortCurrentState>();
          for(int a=1;a<=4;a++) {
            GPIPortCurrentState state = new GPIPortCurrentState();
            state.setConfig(new Bit(true));
            state.setGPIPortNum(new UnsignedShort(a));
            state.setState(new GPIPortState(GPIPortState.Low));
            list.add(state);
          }
          msg.setGPIPortCurrentStateList(list);
        }
        ReaderEventNotificationSpec events = new ReaderEventNotificationSpec();
        List<EventNotificationState> eventsList = new ArrayList<EventNotificationState>();
        if (gpi != -1) {
          EventNotificationState gpi_events = new EventNotificationState();
          gpi_events.setEventType(new NotificationEventType(NotificationEventType.GPI_Event));
          gpi_events.setNotificationState(new Bit(true));
          eventsList.add(gpi_events);
        }
        EventNotificationState ro_events = new EventNotificationState();
        ro_events.setEventType(new NotificationEventType(NotificationEventType.ROSpec_Event));
        ro_events.setNotificationState(new Bit(true));
        eventsList.add(ro_events);
        events.setEventNotificationStateList(eventsList);
        msg.setReaderEventNotificationSpec(events);
//        JFLog.log("reset_reader");
        llrp.send(msg);
        JF.sleep(delay);
      }
      if (impinj_search_mode != -1) {
        //enable impinj extensions
        CUSTOM_MESSAGE msg = new CUSTOM_MESSAGE();
        msg.setVendorIdentifier(new UnsignedInteger(25882));
        msg.setMessageSubtype(new UnsignedByte(21));
        msg.setData(new BytesToEnd_HEX("00000000"));
        llrp.send(msg);
        JF.sleep(delay);
      }
      //add RO (read operation) spec
      {
        ADD_ROSPEC msg = new ADD_ROSPEC();
        msg.setROSpec(rospec);
//        JFLog.log("add RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //add ACCESS spec
      if (enableAccessSpec) {
        ADD_ACCESSSPEC msg = new ADD_ACCESSSPEC();
        msg.setAccessSpec(accessspec);
//        JFLog.log("add ACCESS spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //enable RO spec
      {
        ENABLE_ROSPEC msg = new ENABLE_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("enable RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //enable Access spec
      if (enableAccessSpec) {
        ENABLE_ACCESSSPEC msg = new ENABLE_ACCESSSPEC();
        msg.setAccessSpecID(accessspec.getAccessSpecID());
//        JFLog.log("enable ACCESS spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //start RO spec
      if (period == -1 && gpi == -1) {
        START_ROSPEC msg = new START_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("start RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      return true;
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      return false;
    }
  }

  /** Stop reading or writing tags. */
  public void stop() {
    if (llrp == null) return;
    try {
      //disable RO spec
      if (rospec != null) {
        DISABLE_ROSPEC msg = new DISABLE_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("disable RO spec");
        llrp.send(msg);
        rospec = null;
      }
      //delete RO spec
      {
        DELETE_ROSPEC msg = new DELETE_ROSPEC();
        msg.setROSpecID(new UnsignedInteger(0));
//        JFLog.log("delete RO spec");
        llrp.send(msg);
      }
      if (accessspec != null) {
        DISABLE_ACCESSSPEC msg = new DISABLE_ACCESSSPEC();
        msg.setAccessSpecID(accessspec.getROSpecID());
//        JFLog.log("disable RO spec");
        llrp.send(msg);
        accessspec = null;
      }
      {
        DELETE_ACCESSSPEC msg = new DELETE_ACCESSSPEC();
        msg.setAccessSpecID(new UnsignedInteger(0));
//        JFLog.log("delete Access spec");
        llrp.send(msg);
      }
      active = false;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Write RFID Tag(s).
   *
   * Use stop() to stop writing tags.
   *
   * @param oldEPC = old EPC code
   * @param newEPC = new EPC code
   * @param wordOffset = beginning of EPC memory bank to write newEPC (default = 2)
   */
  public boolean startWriteTag(short[] oldEPC, short[] newEPC, int wordOffset) {
    if (llrp == null) return false;
    if (active) return false;
    try {
      rospec = createROSpec();
      accessspec = createWriteAccessSpec(rospec.getROSpecID(), oldEPC, newEPC, wordOffset);
      //reset reader
      {
        SET_READER_CONFIG msg = new SET_READER_CONFIG();
        msg.setResetToFactoryDefault(new Bit(true));
//        JFLog.log("reset_reader");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //delete all RO specs
      {
        DELETE_ROSPEC msg = new DELETE_ROSPEC();
        msg.setROSpecID(new UnsignedInteger(0));
//        JFLog.log("delete all RO specs");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //delete all Access Specs
      {
        DELETE_ACCESSSPEC msg = new DELETE_ACCESSSPEC();
        msg.setAccessSpecID(new UnsignedInteger(0));
//        JFLog.log("delete all ACCESS specs");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //add RO (read operation) spec
      {
        ADD_ROSPEC msg = new ADD_ROSPEC();
        msg.setROSpec(rospec);
//        JFLog.log("add RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //add ACCESS spec
      {
        ADD_ACCESSSPEC msg = new ADD_ACCESSSPEC();
        msg.setAccessSpec(accessspec);
//        JFLog.log("add ACCESS spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //enable RO spec
      {
        ENABLE_ROSPEC msg = new ENABLE_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("enable RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //enable Access spec
      {
        ENABLE_ACCESSSPEC msg = new ENABLE_ACCESSSPEC();
        msg.setAccessSpecID(accessspec.getAccessSpecID());
//        JFLog.log("enable ACCESS spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //start RO spec
      if (period == -1 && gpi == -1) {
        START_ROSPEC msg = new START_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("start RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      return true;
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      return false;
    }
  }

  /** Write RFID Tag(s).
   *
   * Use stop() to stop writing tags.
   *
   * @param oldEPC = old EPC code
   * @param newEPC = new EPC code
   *
   */
  public boolean startWriteTag(short[] oldEPC, short[] newEPC) {
    return startWriteTag(oldEPC, newEPC, 2);
  }

  /** Kill RFID Tag.
   *
   * Use stop() to stop killing tag.
   *
   * @param targetEPC = target EPC code
   * @param password = kill password
   */
  public boolean startKillTag(short[] targetEPC, int password) {
    if (llrp == null) return false;
    if (active) return false;
    try {
      rospec = createROSpec();
      accessspec = createKillAccessSpec(rospec.getROSpecID(), targetEPC, password);
      //reset reader
      {
        SET_READER_CONFIG msg = new SET_READER_CONFIG();
        msg.setResetToFactoryDefault(new Bit(true));
//        JFLog.log("reset_reader");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //delete all RO specs
      {
        DELETE_ROSPEC msg = new DELETE_ROSPEC();
        msg.setROSpecID(new UnsignedInteger(0));
//        JFLog.log("delete all RO specs");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //delete all Access Specs
      {
        DELETE_ACCESSSPEC msg = new DELETE_ACCESSSPEC();
        msg.setAccessSpecID(new UnsignedInteger(0));
//        JFLog.log("delete all ACCESS specs");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //add RO (read operation) spec
      {
        ADD_ROSPEC msg = new ADD_ROSPEC();
        msg.setROSpec(rospec);
//        JFLog.log("add RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //add ACCESS spec
      {
        ADD_ACCESSSPEC msg = new ADD_ACCESSSPEC();
        msg.setAccessSpec(accessspec);
//        JFLog.log("add ACCESS spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //enable RO spec
      {
        ENABLE_ROSPEC msg = new ENABLE_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("enable RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //enable Access spec
      {
        ENABLE_ACCESSSPEC msg = new ENABLE_ACCESSSPEC();
        msg.setAccessSpecID(accessspec.getAccessSpecID());
//        JFLog.log("enable ACCESS spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //start RO spec
      if (period == -1 && gpi == -1) {
        START_ROSPEC msg = new START_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("start RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      return true;
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      return false;
    }
  }

  private int[] powerLevels;
  private boolean reading_power_levels;

  /** Retrieves power levels from LLRP Controller.
   * Power Levels are indexes into a table of dBm * 100 values.
   */
  public int[] getPowerLevels() {
    if (llrp == null) return null;
    if (active) return null;
    powerLevels = null;
    try {
      //reset reader
      {
        SET_READER_CONFIG msg = new SET_READER_CONFIG();
        msg.setResetToFactoryDefault(new Bit(true));
//        JFLog.log("reset_reader");
        llrp.send(msg);
        JF.sleep(delay);
      }
      reading_power_levels = true;
      {
        GET_READER_CAPABILITIES caps = new GET_READER_CAPABILITIES();
        caps.setRequestedData(new GetReaderCapabilitiesRequestedData(GetReaderCapabilitiesRequestedData.All));
//        JFLog.log("get_reader_caps");
        llrp.send(caps);
        JF.sleep(delay);
      }
      int max = 12;
      while (powerLevels == null) {
        JF.sleep(1000);
        max--;
        if (max == 0) {
          JFLog.log(log, "LLRP:Error:getPowerLevels():timeout");
          return null;
        }
      }
      reading_power_levels = false;
      return powerLevels;
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      return null;
    }
  }

  private int[] sensitivityLevels;
  private boolean reading_sensitivity_levels;

  /** Retrieves sensitivity levels from LLRP Controller.
   * Sensitivity Levels are indexes into a table of dB values.
   */
  public int[] getSensitivityLevels() {
    if (llrp == null) return null;
    if (active) return null;
    sensitivityLevels = null;
    try {
      //reset reader
      {
        SET_READER_CONFIG msg = new SET_READER_CONFIG();
        msg.setResetToFactoryDefault(new Bit(true));
//        JFLog.log("reset_reader");
        llrp.send(msg);
        JF.sleep(delay);
      }
      reading_sensitivity_levels = true;
      {
        GET_READER_CAPABILITIES caps = new GET_READER_CAPABILITIES();
        caps.setRequestedData(new GetReaderCapabilitiesRequestedData(GetReaderCapabilitiesRequestedData.All));
//        JFLog.log("get_reader_caps");
        llrp.send(caps);
        JF.sleep(delay);
      }
      int max = 12;
      while (sensitivityLevels == null) {
        JF.sleep(1000);
        max--;
        if (max == 0) {
          JFLog.log(log, "LLRP:Error:getSensitivityLevels():timeout");
          return null;
        }
      }
      reading_sensitivity_levels = false;
      return sensitivityLevels;
    } catch (Exception e) {
      if (debug) JFLog.log(log, e);
      return null;
    }
  }

  private ROSpec createROSpec() {
    ROSpec rospec;
    ROBoundarySpec roboundaryspec;
    ROSpecStartTrigger rospecstarttrigger;
    ROSpecStopTrigger rospecstoptrigger;
    AISpec aispec;
    ROReportSpec roreportspec;
    rospecstarttrigger = new ROSpecStartTrigger();
    if (period != -1) {
      rospecstarttrigger.setROSpecStartTriggerType(new ROSpecStartTriggerType(ROSpecStartTriggerType.Periodic));
      PeriodicTriggerValue periodValue = new PeriodicTriggerValue();
      periodValue.setPeriod(new UnsignedInteger(period));
      periodValue.setOffset(new UnsignedInteger(0));
      rospecstarttrigger.setPeriodicTriggerValue(periodValue);
    } else if (gpi != -1) {
      rospecstarttrigger.setROSpecStartTriggerType(new ROSpecStartTriggerType(ROSpecStartTriggerType.GPI));
      GPITriggerValue gpiValue = new GPITriggerValue();
      gpiValue.setGPIPortNum(new UnsignedShort(gpi));
      gpiValue.setGPIEvent(new Bit(false)); //false=trigger when beam is broken:true=trigger when beam is restored
      gpiValue.setTimeout(new UnsignedInteger(0));
      rospecstarttrigger.setGPITriggerValue(gpiValue);
    } else {
      rospecstarttrigger.setROSpecStartTriggerType(new ROSpecStartTriggerType(ROSpecStartTriggerType.Null));
    }
    rospecstoptrigger = new ROSpecStopTrigger();
    if (duration != -1) {
      rospecstoptrigger.setROSpecStopTriggerType(new ROSpecStopTriggerType(ROSpecStopTriggerType.Duration));
      rospecstoptrigger.setDurationTriggerValue(new UnsignedInteger(duration));
    } else {
      rospecstoptrigger.setROSpecStopTriggerType(new ROSpecStopTriggerType(ROSpecStopTriggerType.Null));
      rospecstoptrigger.setDurationTriggerValue(new UnsignedInteger(0));
    }
    roboundaryspec = new ROBoundarySpec();
    roboundaryspec.setROSpecStartTrigger(rospecstarttrigger);
    roboundaryspec.setROSpecStopTrigger(rospecstoptrigger);

    roreportspec = new ROReportSpec();
    roreportspec.setROReportTrigger(new ROReportTriggerType(ROReportTriggerType.Upon_N_Tags_Or_End_Of_ROSpec));
    roreportspec.setN(new UnsignedShort(1));

    TagReportContentSelector selector = new TagReportContentSelector();
    ArrayList<AirProtocolEPCMemorySelector> selectorList = new ArrayList<AirProtocolEPCMemorySelector>();
    C1G2EPCMemorySelector selectorEPC = new C1G2EPCMemorySelector();
    selectorEPC.setEnableCRC(new Bit(true));
    selectorEPC.setEnablePCBits(new Bit(true));
    selectorList.add(selectorEPC);
    selector.setAirProtocolEPCMemorySelectorList(selectorList);
    selector.setEnableROSpecID(new Bit(false));
    selector.setEnableSpecIndex(new Bit(false));
    selector.setEnableInventoryParameterSpecID(new Bit(false));
    selector.setEnableAntennaID(new Bit(true));
    selector.setEnableChannelIndex(new Bit(true));
    selector.setEnablePeakRSSI(new Bit(false));
    selector.setEnableFirstSeenTimestamp(new Bit(true));
    selector.setEnableLastSeenTimestamp(new Bit(false));
    selector.setEnableTagSeenCount(new Bit(false));
    selector.setEnableAccessSpecID(new Bit(true));
    selector.setEnablePeakRSSI(new Bit(true));
    roreportspec.setTagReportContentSelector(selector);

    ArrayList<SpecParameter> specParameterList = new ArrayList<SpecParameter>();
    aispec = new AISpec();
    UnsignedShortArray IDs = new UnsignedShortArray();
    for(int a=0;a<powerIndexes.length;a++) {
      IDs.add(new UnsignedShort(a+1));
    }
    aispec.setAntennaIDs(IDs);
    AISpecStopTrigger aispecstoptrigger = new AISpecStopTrigger();
    aispecstoptrigger.setAISpecStopTriggerType(new AISpecStopTriggerType(AISpecStopTriggerType.Null));
    aispecstoptrigger.setDurationTrigger(new UnsignedInteger(0));
    aispec.setAISpecStopTrigger(aispecstoptrigger);

    ArrayList<InventoryParameterSpec> inventoryParameterSpecList = new ArrayList<InventoryParameterSpec>();

    InventoryParameterSpec invspec = new InventoryParameterSpec();
    invspec.setInventoryParameterSpecID(new UnsignedShort(inventoryparamid));
    invspec.setProtocolID(new AirProtocols(AirProtocols.EPCGlobalClass1Gen2));
    ArrayList<AntennaConfiguration> antennaConfigurationList = new ArrayList<AntennaConfiguration>();

    ArrayList<AirProtocolInventoryCommandSettings> commands = new ArrayList<AirProtocolInventoryCommandSettings>();
    C1G2InventoryCommand command = new C1G2InventoryCommand();
    C1G2RFControl rfcontrol = new C1G2RFControl();
    rfcontrol.setModeIndex(new UnsignedShort(mode));
    rfcontrol.setTari(new UnsignedShort(0));
    command.setC1G2RFControl(rfcontrol);
    C1G2SingulationControl singulationcontrol = new C1G2SingulationControl();
    TwoBitField session_bits = new TwoBitField();
    //NOTE : TwoBitField stores bits in reverse order
    boolean session_bit_0 = (session & 0x01) == 0x01;
    boolean session_bit_1 = (session & 0x02) == 0x02;
    if (session_bit_0) {
      session_bits.set(1);  //set bit 1 (LSB)
    }
    if (session_bit_1) {
      session_bits.set(0);  //set bit 0 (MSB)
    }
    singulationcontrol.setSession(session_bits);
    singulationcontrol.setTagPopulation(new UnsignedShort(32));
    singulationcontrol.setTagTransitTime(new UnsignedInteger(0));
    command.setC1G2SingulationControl(singulationcontrol);
    command.setTagInventoryStateAware(new Bit(false));
    if (impinj_search_mode != -1) {
      ArrayList<Custom> customList = new ArrayList<Custom>();
      Custom search_mode = new Custom();
      search_mode.setVendorIdentifier(new UnsignedInteger(25882));
      search_mode.setParameterSubtype(new UnsignedInteger(23));
      search_mode.setData(new BytesToEnd_HEX(String.format("%04x", impinj_search_mode)));
      customList.add(search_mode);
      command.setCustomList(customList);
    }
    commands.add(command);

    for(int a=0;a<powerIndexes.length;a++) {
      AntennaConfiguration antennaConfiguration = new AntennaConfiguration();
      antennaConfiguration.setAirProtocolInventoryCommandSettingsList(commands);
      antennaConfiguration.setAntennaID(new UnsignedShort(a+1));
      RFReceiver rfrec = new RFReceiver();
      rfrec.setReceiverSensitivity(new UnsignedShort(sensitivity));
      antennaConfiguration.setRFReceiver(rfrec);
      RFTransmitter rftrans = new RFTransmitter();
      rftrans.setHopTableID(new UnsignedShort(1));
      rftrans.setChannelIndex(new UnsignedShort(1));
      rftrans.setTransmitPower(new UnsignedShort(powerIndexes[a]));
      antennaConfiguration.setRFTransmitter(rftrans);
      antennaConfigurationList.add(antennaConfiguration);
    }

    invspec.setAntennaConfigurationList(antennaConfigurationList);
    inventoryParameterSpecList.add(invspec);
    aispec.setInventoryParameterSpecList(inventoryParameterSpecList);

    specParameterList.add(aispec);

    rospec = new ROSpec();
    rospec.setROBoundarySpec(roboundaryspec);
    rospec.setROSpecID(new UnsignedInteger(rospecid));
    rospec.setROReportSpec(roreportspec);
    rospec.setSpecParameterList(specParameterList);
    rospec.setCurrentState(new ROSpecState(ROSpecState.Disabled));
    rospec.setPriority(new UnsignedByte(0));
    return rospec;
  }

  private AccessSpec createReadAccessSpec(UnsignedInteger rospecid) {
    AccessSpec as = new AccessSpec();
    TwoBitField mb = new TwoBitField();
    //NOTE : TwoBitField stores bits in reverse order
    mb.set(1);  //set bit 1 (LSB) (value : 0=private 1=EPC 2=TID 3=user)
    AccessCommand accessCommand = new AccessCommand();
    ArrayList<AccessCommandOpSpec> accessCommandOpSpecList = new ArrayList<AccessCommandOpSpec>();
    C1G2Read read = new C1G2Read();
    read.setAccessPassword(new UnsignedInteger(0));
    read.setMB(mb);
    read.setOpSpecID(new UnsignedShort(opspecid));
    read.setWordCount(new UnsignedShort(8));
    read.setWordPointer(new UnsignedShort(2));
    accessCommandOpSpecList.add(read);
    accessCommand.setAccessCommandOpSpecList(accessCommandOpSpecList);
    C1G2TagSpec tagSpec = new C1G2TagSpec();
    ArrayList<C1G2TargetTag> targetTagList = new ArrayList<C1G2TargetTag>();
    C1G2TargetTag tt = new C1G2TargetTag();
    tt.setMB(mb);
    tt.setMatch(new Bit(false));
    tt.setPointer(new UnsignedShort(2));
    BitArray_HEX mask = new BitArray_HEX(128);
    for(int a=0;a<128;a++) {
      mask.set(a);
    }
    tt.setTagMask(mask);
    BitArray_HEX data = new BitArray_HEX(128);
    for(int a=0;a<128;a++) {
      data.set(a);
    }
    tt.setTagData(data);
    targetTagList.add(tt);
    tagSpec.setC1G2TargetTagList(targetTagList);
    accessCommand.setAirProtocolTagSpec(tagSpec);
    as.setAccessCommand(accessCommand);

    AccessReportSpec accessReportSpec = new AccessReportSpec();
    accessReportSpec.setAccessReportTrigger(new AccessReportTriggerType(AccessReportTriggerType.Whenever_ROReport_Is_Generated));
    as.setAccessReportSpec(accessReportSpec);

    AccessSpecStopTrigger accessSpecStopTrigger = new AccessSpecStopTrigger();
    accessSpecStopTrigger.setAccessSpecStopTrigger(new AccessSpecStopTriggerType(AccessSpecStopTriggerType.Null));
    accessSpecStopTrigger.setOperationCountValue(new UnsignedShort(1));
    as.setAccessSpecStopTrigger(accessSpecStopTrigger);

    as.setAntennaID(new UnsignedShort(1));

    as.setCurrentState(new AccessSpecState(AccessSpecState.Disabled));

    AirProtocols airProtocols = new AirProtocols();
    airProtocols.set(AirProtocols.EPCGlobalClass1Gen2);
    as.setProtocolID(airProtocols);

    as.setROSpecID(rospecid);
    as.setAccessSpecID(new UnsignedInteger(accessid));
    return as;
  }

  private String shortArrayToHexString(short[] epc) {
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<epc.length;a++) {
      sb.append(String.format("%04x", epc[a]));
    }
    return sb.toString();
  }

  private AccessSpec createWriteAccessSpec(UnsignedInteger rospecid, short[] oldEPC, short[] newEPC, int offset) {
    AccessSpec as = new AccessSpec();
    TwoBitField mb = new TwoBitField();
    //NOTE : TwoBitField stores bits in reverse order
    mb.set(1);  //set bit 1 (LSB) (value:0=private 1=EPC 2=TID 3=user)
    int oldBits = oldEPC.length * 16;
    int newBits = newEPC.length * 16;
    if (oldBits != newBits) {
      JFLog.log("Warning:oldEPC.length != newEPC.length");
      //NOTE : this is okay and works!!!
      //     : this could happen if RFID PC bits are setup to only return 96 bits for a 128 bit tag
      //     : RFID_PC (word offset 1): 3400 = 96 bit   4400 = 128 bit tag
    }

    AccessCommand accessCommand = new AccessCommand();
    ArrayList<AccessCommandOpSpec> accessCommandOpSpecList = new ArrayList<AccessCommandOpSpec>();
    C1G2Write write = new C1G2Write();
    write.setAccessPassword(new UnsignedInteger(0));
    write.setMB(mb);
    write.setOpSpecID(new UnsignedShort(opspecid));
    write.setWordPointer(new UnsignedShort(offset));
    UnsignedShortArray_HEX newepchex = new UnsignedShortArray_HEX(newEPC.length);
    for(int a=0;a<newEPC.length;a++) {
      newepchex.set(a, new UnsignedShort(newEPC[a]));
    }
    write.setWriteData(newepchex);
    accessCommandOpSpecList.add(write);
    accessCommand.setAccessCommandOpSpecList(accessCommandOpSpecList);
    C1G2TagSpec tagSpec = new C1G2TagSpec();
    ArrayList<C1G2TargetTag> targetTagList = new ArrayList<C1G2TargetTag>();
    C1G2TargetTag tt = new C1G2TargetTag();
    tt.setMB(mb);
    tt.setMatch(new Bit(true));
    tt.setPointer(new UnsignedShort(2 * 16));  //pointer is in BITS not WORDS
    BitArray_HEX mask = new BitArray_HEX(oldBits);
    for(int a=0;a<oldBits;a++) {
      mask.set(a);
    }
    tt.setTagMask(mask);
    BitArray_HEX data = new BitArray_HEX(shortArrayToHexString(oldEPC));
    tt.setTagData(data);
    targetTagList.add(tt);
    tagSpec.setC1G2TargetTagList(targetTagList);
    accessCommand.setAirProtocolTagSpec(tagSpec);
    as.setAccessCommand(accessCommand);

    AccessReportSpec accessReportSpec = new AccessReportSpec();
    accessReportSpec.setAccessReportTrigger(new AccessReportTriggerType(AccessReportTriggerType.Whenever_ROReport_Is_Generated));
    as.setAccessReportSpec(accessReportSpec);

    AccessSpecStopTrigger accessSpecStopTrigger = new AccessSpecStopTrigger();
    accessSpecStopTrigger.setAccessSpecStopTrigger(new AccessSpecStopTriggerType(AccessSpecStopTriggerType.Null));
    accessSpecStopTrigger.setOperationCountValue(new UnsignedShort(1));
    as.setAccessSpecStopTrigger(accessSpecStopTrigger);

    as.setAntennaID(new UnsignedShort(1));

    as.setCurrentState(new AccessSpecState(AccessSpecState.Disabled));

    AirProtocols airProtocols = new AirProtocols();
    airProtocols.set(AirProtocols.EPCGlobalClass1Gen2);
    as.setProtocolID(airProtocols);

    as.setROSpecID(rospecid);
    as.setAccessSpecID(new UnsignedInteger(accessid));
    return as;
  }

  private AccessSpec createKillAccessSpec(UnsignedInteger rospecid, short[] targetEPC, int password) {
    AccessSpec as = new AccessSpec();
    TwoBitField mb = new TwoBitField();
    //NOTE : TwoBitField stores bits in reverse order
    mb.set(1);  //set bit 1 (LSB) (value:0=private 1=EPC 2=TID 3=user)
    int oldBits = targetEPC.length * 16;

    AccessCommand accessCommand = new AccessCommand();
    ArrayList<AccessCommandOpSpec> accessCommandOpSpecList = new ArrayList<AccessCommandOpSpec>();
    C1G2Kill kill = new C1G2Kill();
    kill.setOpSpecID(new UnsignedShort(opspecid));
    kill.setKillPassword(new UnsignedInteger(password));
    accessCommandOpSpecList.add(kill);
    accessCommand.setAccessCommandOpSpecList(accessCommandOpSpecList);
    C1G2TagSpec tagSpec = new C1G2TagSpec();
    ArrayList<C1G2TargetTag> targetTagList = new ArrayList<C1G2TargetTag>();
    C1G2TargetTag tt = new C1G2TargetTag();
    tt.setMB(mb);
    tt.setMatch(new Bit(true));
    tt.setPointer(new UnsignedShort(2 * 16));  //pointer is in BITS not WORDS
    BitArray_HEX mask = new BitArray_HEX(oldBits);
    for(int a=0;a<oldBits;a++) {
      mask.set(a);
    }
    tt.setTagMask(mask);
    BitArray_HEX data = new BitArray_HEX(shortArrayToHexString(targetEPC));
    tt.setTagData(data);
    targetTagList.add(tt);
    tagSpec.setC1G2TargetTagList(targetTagList);
    accessCommand.setAirProtocolTagSpec(tagSpec);
    as.setAccessCommand(accessCommand);

    AccessReportSpec accessReportSpec = new AccessReportSpec();
    accessReportSpec.setAccessReportTrigger(new AccessReportTriggerType(AccessReportTriggerType.Whenever_ROReport_Is_Generated));
    as.setAccessReportSpec(accessReportSpec);

    AccessSpecStopTrigger accessSpecStopTrigger = new AccessSpecStopTrigger();
    accessSpecStopTrigger.setAccessSpecStopTrigger(new AccessSpecStopTriggerType(AccessSpecStopTriggerType.Null));
    accessSpecStopTrigger.setOperationCountValue(new UnsignedShort(1));
    as.setAccessSpecStopTrigger(accessSpecStopTrigger);

    as.setAntennaID(new UnsignedShort(1));

    as.setCurrentState(new AccessSpecState(AccessSpecState.Disabled));

    AirProtocols airProtocols = new AirProtocols();
    airProtocols.set(AirProtocols.EPCGlobalClass1Gen2);
    as.setProtocolID(airProtocols);

    as.setROSpecID(rospecid);
    as.setAccessSpecID(new UnsignedInteger(accessid));
    return as;
  }

  public void messageReceived(LLRPMessage llrpm) {
    lastMsg = System.currentTimeMillis();
    if (debug) {
      JFLog.log("LLRP:messageReceived:" + ip + ":" + llrpm);
    }
    try {
      if (llrpm instanceof RO_ACCESS_REPORT) {
        int idx;
        RO_ACCESS_REPORT report = (RO_ACCESS_REPORT)llrpm;
        List<TagReportData> tags = report.getTagReportDataList();
        for(TagReportData tag : tags) {
          String epc_scan = null;
          String epc_read = null;
          int tag_rssi = 0;
          PeakRSSI peak_rssi = tag.getPeakRSSI();
          if (peak_rssi != null) {
            tag_rssi = peak_rssi.getPeakRSSI().intValue();
          }
          epc_scan = tag.getEPCParameter().toString();
          idx = epc_scan.lastIndexOf(':');
          if (idx > 0) {
            epc_scan = epc_scan.substring(idx+1);
          }
          epc_scan = epc_scan.replaceAll(" ", "").trim();
          List<AccessCommandOpSpecResult> list = tag.getAccessCommandOpSpecResultList();
          if (list.size() == 0) {
            if (events != null) {
              events.tagRead(null, 0);
            }
          }
          for(AccessCommandOpSpecResult res : list) {
            if (res instanceof C1G2ReadOpSpecResult) {
              C1G2ReadOpSpecResult readop = (C1G2ReadOpSpecResult)res;
              epc_read = readop.getReadData().toString();
              idx = epc_read.lastIndexOf(':');
              if (idx > 0) {
                epc_read = epc_read.substring(idx+1);
              }
              epc_read = epc_read.replaceAll(" ", "").trim();
            }
          }
          if (events != null && (epc_read != null || epc_scan != null)) {
            String epc = epc_read != null && epc_read.length() > 0 ? epc_read : epc_scan;
            if (debug) {
              JFLog.log(log, "[" + System.currentTimeMillis() + "] EPC=" + epc + ":RSSI=" + tag_rssi);
            }
            events.tagRead(epc, tag_rssi);
          }
        }
      }
      else if (llrpm instanceof GET_READER_CAPABILITIES_RESPONSE) {
        GET_READER_CAPABILITIES_RESPONSE caps = (GET_READER_CAPABILITIES_RESPONSE)llrpm;
        if (reading_power_levels) {
          List<TransmitPowerLevelTableEntry> list = caps.getRegulatoryCapabilities().getUHFBandCapabilities().getTransmitPowerLevelTableEntryList();
          int[] levels = new int[list.size() + 1];
          int index = 1;
          for(TransmitPowerLevelTableEntry entry : list) {
            levels[index++] = entry.getTransmitPowerValue().intValue();
          }
          this.powerLevels = levels;
        }
        if (reading_sensitivity_levels) {
          List<ReceiveSensitivityTableEntry> list = caps.getGeneralDeviceCapabilities().getReceiveSensitivityTableEntryList();
          int[] levels = new int[list.size() + 1];
          int index = 1;
          for(ReceiveSensitivityTableEntry entry : list) {
            levels[index++] = entry.getReceiveSensitivityValue().toShort();
          }
          this.sensitivityLevels = levels;
        }
      }
      else if (llrpm instanceof READER_EVENT_NOTIFICATION) {
        READER_EVENT_NOTIFICATION event = (READER_EVENT_NOTIFICATION)llrpm;
        ReaderEventNotificationData data = event.getReaderEventNotificationData();
        int type = 0;
        if (data.getGPIEvent() != null) type = 248;
        if (data.getROSpecEvent() != null) type = 249;
        switch (type) {
          case 248: {  //GPIEVent
            GPIEvent gpiEvent = data.getGPIEvent();
            events.gpiEvent(gpiEvent.getGPIPortNumber().toShort(), gpiEvent.getGPIEvent().toBoolean());
            break;
          }
          case 249: {  //ROSpec
            ROSpecEvent roEvent = data.getROSpecEvent();
            ROSpecEventType roEventType = roEvent.getEventType();
            int roType = roEventType.toInteger();
            switch (roType) {
              case 0: events.readStarted(); break;
              case 1: events.readEnded(); break;
            }
          }
        }
      }
      else if (llrpm instanceof SET_READER_CONFIG_RESPONSE) {
        //TODO
      }
      else if (llrpm instanceof DELETE_ROSPEC_RESPONSE) {
        //TODO
      }
      else if (llrpm instanceof DELETE_ACCESSSPEC_RESPONSE) {
        //TODO
      }
      else if (llrpm instanceof ADD_ROSPEC_RESPONSE) {
        //TODO
      }
      else if (llrpm instanceof ENABLE_ROSPEC_RESPONSE) {
        //TODO
      }
      else if (llrpm instanceof START_ROSPEC_RESPONSE) {
        //TODO
      }
      else if (llrpm instanceof DISABLE_ROSPEC_RESPONSE) {
        //TODO
      }
      else if (llrpm instanceof ERROR_MESSAGE) {
        ERROR_MESSAGE msg = (ERROR_MESSAGE)llrpm;
        //TODO : handle error message
      }
      else {
        JFLog.log("LLRP:Unknown message:" + llrpm);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void errorOccured(String msg) {
    JFLog.log(log, "LLRP:Error:" + ip + ":" + msg);
    active = false;
  }

  private static void usage() {
    System.out.println("usage : LLRP controller_ip cmd [args]");
    System.out.println("where : cmd = read | powerlevels | rssilevels | ping");
    System.out.println("      : read [power=p1[,p2[,p3[,p4]]]] [rssi=threshold] [period=ms] [gpi=port] [duration=ms]");
    System.out.println("      : powerlevels");
    System.out.println("      : rssilevels");
    System.out.println("      : ping");
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      usage();
      return;
    }
    boolean active = true;
    String ctrl = args[0];
    String cmd = args[1];
    int[] powerLevels = new int[] {80, 80, 80, 80};
    int rssi = 0;
    int period = 0;
    int duration = 0;
    int gpi = 0;
    switch (cmd) {
      case "read": {
        for(int a=2;a<args.length;a++) {
          String arg = args[a];
          int idx = arg.indexOf('=');
          if (idx == -1) continue;
          String key = arg.substring(0, idx);
          String value = arg.substring(idx + 1);
          switch (key) {
            case "power":
              String[] lvls = value.split("[,]");
              powerLevels = new int[lvls.length];
              for(int b=0;b<lvls.length;b++) {
                powerLevels[b] = Integer.valueOf(lvls[b]);
              }
              break;
            case "rssi":
              rssi = Integer.valueOf(value);
              break;
            case "period":
              period = Integer.valueOf(value);
              if (duration == 0) {
                duration = 500;
              }
              break;
            case "duration":
              duration = Integer.valueOf(value);
              break;
            case "gpi":
              gpi = Integer.valueOf(value);
              break;
          }
        }
        LLRP llrp = new LLRP();
        llrp.debug = true;
//        llrp.setImpinjSearchMode(IMPINJ_SEARCH_MODE_FOCUS);
        if (period != 0) {
          llrp.setPeriod(period, duration);
        }
        if (gpi != 0) {
          if (duration != 0) {
            llrp.setGPITrigger(gpi, duration);
          } else {
            llrp.setGPITrigger(gpi);
          }
        }
        if (rssi != 0) {
          llrp.setRSSIThreshold(rssi);
        }
        llrp.setEventsListener(new LLRPEvent() {
          public void tagRead(String epc, int rssi) {
            System.out.println("EPC=" + epc + ":RSSI=" + rssi);
          }
          public void gpiEvent(int port, boolean event) {
            System.out.println("GPI:" + port + ":" + event);
          }
          public void readStarted() {
            System.out.println("readStarted");
          }
          public void readEnded() {
            System.out.println("readEnded");
          }
        });
        llrp.setPowerIndexes(powerLevels);
        llrp.connect(ctrl);
        llrp.startInventoryScan();
        while (active) {
          JF.sleep(100);
        }
        llrp.disconnect();
        break;
      }
      case "powerlevels": {
        LLRP llrp = new LLRP();
        llrp.debug = true;
        llrp.connect(ctrl);
        int[] levels = llrp.getPowerLevels();
        llrp.stop();
        if (levels == null) {
          System.out.println("Error:getPowerLevels()==null");
          break;
        }
        System.out.println("# Power Levels = " + levels.length);
        int index = 0;
        for(int level : levels) {
          System.out.println("PowerLevel:index=" + index + ":level=" + levels[index]);
          index++;
        }
        llrp.disconnect();
        break;
      }
      case "rssilevels": {
        LLRP llrp = new LLRP();
        llrp.debug = true;
        llrp.connect(ctrl);
        int[] levels = llrp.getSensitivityLevels();
        llrp.stop();
        if (levels == null) {
          System.out.println("Error:getSensitivityLevels()==null");
          break;
        }
        System.out.println("# Sensitivty Levels = " + levels.length);
        int index = 0;
        for(int level : levels) {
          System.out.println("SensitivtyLevel:index=" + index + ":level=" + levels[index]);
          index++;
        }
        llrp.disconnect();
        break;
      }
      case "ping": {
        LLRP llrp = new LLRP();
        llrp.debug = true;
        llrp.connect(ctrl);
        llrp.ping();
        llrp.disconnect();
        break;
      }
      default: {
        usage();
        break;
      }
    }
    System.out.println("Done");
  }
}
