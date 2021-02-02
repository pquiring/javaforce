package javaforce;

/** LLRP API read/write tags from LLRP compatible controllers.
 *
 * Uses the LLRP Toolkit (https://sourceforge.net/projects/llrp-toolkit/)
 * Javadoc : http://llrp.org/docs/javaapidoc/
 *
 * v1.0 - init version
 * v1.1 - added AccessSpec to read tags during inventory scan.
 * v1.2 - added write tag operation
 * v1.3 - added wordOffset to writeTag operation and fixed TargetTag spec
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

  public boolean active;

  public boolean connect(String ip) {
    this.ip = ip;
    llrp = new LLRPConnector(this, ip);
    try {
      llrp.connect();
      return true;
    } catch (Exception e) {
      llrp = null;
      JFLog.log(e);
      return false;
    }
  }

  public void disconnect() {
    if (llrp == null) return;
    llrp.disconnect();
  }

  public void ping() {
    if (llrp == null) return;
    try {
      //enable keepalive : some readers do not support it, but it will still "test" the connection
      {
        KEEPALIVE msg = new KEEPALIVE();
        llrp.send(msg);
        JF.sleep(delay);
      }
    } catch (Exception e) {
      JFLog.log(e);
      active = false;
    }
  }

  public void setEventsListener(LLRPEvent events) {
    this.events = events;
  }


  /** Starts reading RFID tags with inventory scans.
   *
   * Use stop() to stop scanning.
   *
   * @param powerLevel = power level for each antenna
   * @param readTags = read each tag during inventory scan (can return more bits that tag is configured to return during inventory scan)
   * @return scanning active
   */
  public boolean startInventoryScan(int[] powerLevel, boolean readTags) {
    if (llrp == null) return false;
    active = true;
    try {
      rospec = createROSpec(powerLevel);
      if (readTags) {
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
      //add RO (read operation) spec
      {
        ADD_ROSPEC msg = new ADD_ROSPEC();
        msg.setROSpec(rospec);
//        JFLog.log("add RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //add ACCESS spec
      if (readTags) {
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
      if (readTags) {
        ENABLE_ACCESSSPEC msg = new ENABLE_ACCESSSPEC();
        msg.setAccessSpecID(accessspec.getAccessSpecID());
//        JFLog.log("enable ACCESS spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      //start RO spec
      {
        START_ROSPEC msg = new START_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("start RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public void stop() {
    try {
      //disable RO spec
      {
        DISABLE_ROSPEC msg = new DISABLE_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("disable RO spec");
        llrp.send(msg);
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
   * @param powerLevel = power level for each antenna
   *
   */
  public boolean startWriteTag(short[] oldEPC, short[] newEPC, int[] powerLevel) {
    return startWriteTag(oldEPC, newEPC, powerLevel, 2);
  }

  /** Write RFID Tag(s).
   *
   * Use stop() to stop writing tags.
   *
   * @param oldEPC = old EPC code
   * @param newEPC = new EPC code
   * @param powerLevel = power level for each antenna
   * @param wordOffset = beginning of EPC memory bank to write newEPC (default = 2)
   */
  public boolean startWriteTag(short[] oldEPC, short[] newEPC, int[] powerLevel, int wordOffset) {
    if (llrp == null) return false;
    if (active) return false;
    try {
      rospec = createROSpec(powerLevel);
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
      {
        START_ROSPEC msg = new START_ROSPEC();
        msg.setROSpecID(rospec.getROSpecID());
//        JFLog.log("start RO spec");
        llrp.send(msg);
        JF.sleep(delay);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private int[] powerLevels;

  /** Retrieves power levels. */
  private int[] getPowerLevels() {
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
          JFLog.log("LLRP:Error:getPowerLevels():timeout");
          return null;
        }
      }
      return powerLevels;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private static ROSpec createROSpec(int[] powerLevel) {
    ROSpec rospec;
    ROBoundarySpec roboundaryspec;
    ROSpecStartTrigger rospecstarttrigger;
    ROSpecStopTrigger rospecstoptrigger;
    AISpec aispec;
    ROReportSpec roreportspec;
    rospecstarttrigger = new ROSpecStartTrigger();
    rospecstarttrigger.setROSpecStartTriggerType(new ROSpecStartTriggerType(ROSpecStartTriggerType.Null));  //Immediate ???
//    PeriodicTriggerValue period = new PeriodicTriggerValue();
//    period.setPeriod(new UnsignedInteger(1));
//    rospecstarttrigger.setPeriodicTriggerValue(period);
    rospecstoptrigger = new ROSpecStopTrigger();
    rospecstoptrigger.setROSpecStopTriggerType(new ROSpecStopTriggerType(ROSpecStopTriggerType.Null));
    rospecstoptrigger.setDurationTriggerValue(new UnsignedInteger(0));
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
    roreportspec.setTagReportContentSelector(selector);

    ArrayList<SpecParameter> specParameterList = new ArrayList<SpecParameter>();
    aispec = new AISpec();
    UnsignedShortArray IDs = new UnsignedShortArray();
    for(int a=0;a<powerLevel.length;a++) {
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

    for(int a=0;a<powerLevel.length;a++) {
      AntennaConfiguration antennaConfiguration = new AntennaConfiguration();
      antennaConfiguration.setAirProtocolInventoryCommandSettingsList(new ArrayList<AirProtocolInventoryCommandSettings>());
      antennaConfiguration.setAntennaID(new UnsignedShort(a+1));
      RFReceiver rfrec = new RFReceiver();
      rfrec.setReceiverSensitivity(new UnsignedShort(1));
      antennaConfiguration.setRFReceiver(rfrec);
      RFTransmitter rftrans = new RFTransmitter();
      rftrans.setHopTableID(new UnsignedShort(1));
      rftrans.setChannelIndex(new UnsignedShort(1));
      rftrans.setTransmitPower(new UnsignedShort(powerLevel[a]));
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

  private static AccessSpec createReadAccessSpec(UnsignedInteger rospecid) {
    AccessSpec as = new AccessSpec();
    TwoBitField mb = new TwoBitField();
    mb.set(1);  //0=private 1=EPC 2=TID 3=user

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

  private static String shortArrayToHexString(short[] epc) {
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<epc.length;a++) {
      sb.append(String.format("%04x", epc[a]));
    }
    return sb.toString();
  }

  private static AccessSpec createWriteAccessSpec(UnsignedInteger rospecid, short[] oldEPC, short[] newEPC, int offset) {
    AccessSpec as = new AccessSpec();
    TwoBitField mb = new TwoBitField();
    mb.set(1);  //0=private 1=EPC 2=TID 3=user
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

  public void messageReceived(LLRPMessage llrpm) {
    try {
      int idx;
      String epc_scan = null, epc_read = null;
      if (llrpm instanceof RO_ACCESS_REPORT) {
        RO_ACCESS_REPORT report = (RO_ACCESS_REPORT)llrpm;
        List<TagReportData> tags = report.getTagReportDataList();
        for(TagReportData tag : tags) {
          epc_scan = tag.getEPCParameter().toString();
          idx = epc_scan.lastIndexOf(':');
          if (idx > 0) {
            epc_scan = epc_scan.substring(idx+1);
          }
          epc_scan = epc_scan.replaceAll(" ", "").trim();
          List<AccessCommandOpSpecResult> list = tag.getAccessCommandOpSpecResultList();
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
        }
      }
      if (llrpm instanceof GET_READER_CAPABILITIES_RESPONSE) {
        GET_READER_CAPABILITIES_RESPONSE caps = (GET_READER_CAPABILITIES_RESPONSE)llrpm;
        List<TransmitPowerLevelTableEntry> list = caps.getRegulatoryCapabilities().getUHFBandCapabilities().getTransmitPowerLevelTableEntryList();
        int[] levels = new int[list.size()];
        int index = 0;
        for(TransmitPowerLevelTableEntry entry : list) {
          levels[index++] = entry.getTransmitPowerValue().intValue();
        }
        this.powerLevels = levels;
        return;
      }
      if (events != null && (epc_read != null || epc_scan != null)) {
        events.tagRead(epc_read != null && epc_read.length() > 0 ? epc_read : epc_scan);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void errorOccured(String string) {
    JFLog.log("error:" + string);
    active = false;
  }

  private static void usage() {
    System.out.println("usage : LLRP controller_ip cmd");
    System.out.println("where : cmd = READ | POWERLEVELS");
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      usage();
      return;
    }
    boolean active = true;
    String ctrl = args[0];
    String cmd = args[1];
    int[] powerLevels = new int[] {50};
    switch (cmd) {
      case "READ": {
        LLRP llrp = new LLRP();
        llrp.connect(ctrl);
        llrp.startInventoryScan(powerLevels, true);
        while (active) {
          JF.sleep(100);
        }
        break;
      }
      case "POWERLEVELS": {
        LLRP llrp = new LLRP();
        llrp.connect(ctrl);
        int[] levels = llrp.getPowerLevels();
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
        break;
      }
    }
  }
}
