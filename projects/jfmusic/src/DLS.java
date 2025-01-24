/**
 * Import DLS (Sound Bank)
 *
 * @author pquiring
 *
 * Created : Feb 22, 2014
 *
 * See : DlsBank.cpp from OpenMPT for more info.
 *
 */

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import javaforce.*;
import javaforce.media.*;

public class DLS {
  public String errmsg;
  public static boolean debug = false;  //lots of info

  private final static int IFFID_RIFF = 0x46464952;
  private final static int IFFID_WAVE = 0x45564157;
  private final static int IFFID_wave = 0x65766177;
  private final static int IFFID_XDLS = 0x534c4458;
  private final static int IFFID_DLS  = 0x20534C44;
  private final static int IFFID_MLS  = 0x20534C4D;
  private final static int IFFID_RMID = 0x44494D52;
  private final static int IFFID_colh = 0x686C6F63;
  private final static int IFFID_vers = 0x73726576;
  private final static int IFFID_msyn = 0x6E79736D;
  private final static int IFFID_lins = 0x736E696C;
  private final static int IFFID_ins  = 0x20736E69;
  private final static int IFFID_insh = 0x68736E69;
  private final static int IFFID_ptbl = 0x6C627470;
  private final static int IFFID_wvpl = 0x6C707677;
  private final static int IFFID_rgn  = 0x206E6772;
  private final static int IFFID_rgn2 = 0x326E6772;
  private final static int IFFID_rgnh = 0x686E6772;
  private final static int IFFID_wlnk = 0x6B6E6C77;
  private final static int IFFID_art1 = 0x31747261;
  private final static int IFFID_LIST = 0x5453494C;
  private final static int IFFID_INFO = 0x4F464E49;
  private final static int IFFID_wsmp = 0x706D7377;
  private final static int IFFID_INAM = 0x4D414E49;
  private final static int IFFID___X1 = 0x6e67726c;
  private final static int IFFID___X2 = 0x7472616c;
  private final static int IFFID_ICOP = 0x504f4349;

  public static class Region {
    int loopStart = -1;
    int loopLength = -1;
    int waveLink;
    int percEnv;
    int volume;		// 0..256
    int fuOptions;	// flags + key group
    int fineTune;	// 1..100
    int keyMin;
    int keyMax;
    int unityNote;
    int attenuation;
  }

  public static class Instrument {
    public String name;
    public int id, len, bank, instrument;
    public Region regions[];
    public short samples[];
    public int loopStart = -1;
    public int loopLength = -1;
  }

  private int waveFormsOffsets[];
  private byte waveFormPool[];
  public Instrument instruments[];
  public String version;

  private String getName(int id) {
    Field fields[] = this.getClass().getDeclaredFields();
    try {
      for(int a=0;a<fields.length;a++) {
        String name = fields[a].getName();
        if (!name.startsWith("IFFID_")) continue;
        int fid = fields[a].getInt(null);
        if (fid == id) return name;
      }
    } catch (Exception e) {

    }
    return null;
  }

  public boolean load(InputStream is) {
    JFLog.log("Import DLS...");
    int nInstruments = -1;
    int nWaveForms = -1;
    int nRegions = 0;  //total regions of all instruments (debug)
    errmsg = "";
    int cInstrument = -1;
    int cRegion = -1;
    int cbSize;
    try {
      byte data[] = new byte[32];
      //read RIFF header (12 bytes)
      is.read(data, 0, 12);
      if (LE.getuint32(data, 0) != IFFID_RIFF) throw new Exception("not a valid RIFF file)");
      int riffLength = LE.getuint32(data, 4);
      if (LE.getuint32(data, 8) != IFFID_DLS) throw new Exception("not a DLS file");
      int pos = 4;
      while (pos < riffLength) {
        //read chunk ID/length
        if (debug) JFLog.log("pos=" + pos);
        is.read(data, 0, 8);
        int id = LE.getuint32(data, 0);
        int chunkLength = LE.getuint32(data, 4);
        int chunkRead = 0;
        if (debug) JFLog.log("id=0x" + Integer.toString(id, 16) + ":" + getName(id) + ":" + chunkLength);
        pos += 8;
        switch (id) {
          case IFFID_colh:
            is.read(data, 0, 4);
            chunkRead = 4;
            nInstruments = LE.getuint32(data, 0);
            if (debug) JFLog.log("Found " + nInstruments + " instruments");
            instruments = new Instrument[nInstruments];
            for(int a=0;a<nInstruments;a++) {
              instruments[a] = new Instrument();
            }
            break;
          case IFFID_vers:
            is.read(data, 0, 4);
            chunkRead = 4;
            version = LE.getString(data, 0, 4);
            break;
          case IFFID_ptbl:
            is.read(data, 0, 8);
            cbSize = LE.getuint32(data, 0);
            nWaveForms = LE.getuint32(data, 4);
            if (debug) JFLog.log("waveForms=" + nWaveForms + ",total regions=" + nRegions);
            if (cbSize > 8) is.skip(cbSize - 8);
            waveFormsOffsets = new int[nWaveForms];
            byte offsets[] = new byte[nWaveForms * 4];
            is.read(offsets);
            for(int a=0;a<nWaveForms;a++) {
              int offset = LE.getuint32(offsets, a * 4);
              if (debug) JFLog.log("offset=" + offset);
              waveFormsOffsets[a] = offset;
            }
            chunkRead = cbSize + (nWaveForms * 4);
            break;
          case IFFID_LIST:
            //read list id (which is included in chunkLength)
            is.read(data, 0, 4);
            chunkRead = 4;
            int listid = LE.getuint32(data, 0);
            if (debug) JFLog.log("listid=0x" + Integer.toString(listid, 16) + ":" + getName(listid));
            switch(listid) {
              case IFFID___X1:  //regions list ???
              case IFFID___X2:  //art1 list ???
              case IFFID_rgn:  //region list
              case IFFID_lins:  //list of instruments
              case IFFID_INFO:  //more info
                chunkLength = chunkRead = 4;  //process sub-chunks
                break;
              case IFFID_ins:
                //instrument
                chunkLength = chunkRead = 4;  //process sub-chunks
                cInstrument++;
                cRegion = -1;
                if (debug) JFLog.log("next instrument");
                break;
              case IFFID_wvpl:
//              case IFFID_sdta:  //SF2
                //waveform pool
                waveFormPool = new byte[chunkLength - 4];  //-4 for listid (which is part of chunk)
                is.read(waveFormPool);
                if (debug) {
                  FileOutputStream fos = new FileOutputStream("wavepool.dat");
                  fos.write(waveFormPool);
                  fos.close();
                }
                chunkRead = chunkLength;
                break;
            }
            break;
          case IFFID_insh:
            //instrument header
            is.read(data, 0, 4 * 3);
            chunkRead += 4 * 3;
            instruments[cInstrument].regions = new Region[LE.getuint32(data, 0)];
            if (debug) JFLog.log("Found " + instruments[cInstrument].regions.length + " regions");
            nRegions += instruments[cInstrument].regions.length;
            for(int a=0;a<instruments[cInstrument].regions.length;a++) {
              instruments[cInstrument].regions[a] = new Region();
            }
            instruments[cInstrument].bank = LE.getuint32(data, 4);
            instruments[cInstrument].instrument = LE.getuint32(data, 12);
            break;
          case IFFID_rgnh:
            //region header
            is.read(data, 0, 6 * 2);
            chunkRead += 6 * 2;
            int rangeKeyLow = LE.getuint16(data, 0);
            int rangeKeyHigh = LE.getuint16(data, 2);
            int rangeVelocityLow = LE.getuint16(data, 4);
            int rangeVelocityHigh = LE.getuint16(data, 6);
            int fusOptions = LE.getuint16(data, 8);
            int usKeyGroup = LE.getuint16(data, 10);
            cRegion++;
            if (debug) JFLog.log("next region, range=" + rangeKeyLow + "," + rangeKeyHigh);
            instruments[cInstrument].regions[cRegion].keyMin = rangeKeyLow;
            instruments[cInstrument].regions[cRegion].keyMax = rangeKeyHigh;
            break;
          case IFFID_wlnk:
            //wave link
            is.read(data, 0, 12);
            chunkRead += 12;
            int _fusOptions = LE.getuint16(data, 0);
            int phaseGroup = LE.getuint16(data, 2);
            int channel = LE.getuint32(data, 4);
            int tableIndex = LE.getuint32(data, 8);
            if (debug) JFLog.log("link:" + phaseGroup + "," + channel + "," + tableIndex);
            instruments[cInstrument].regions[cRegion].waveLink = tableIndex;
            break;
          case IFFID_wsmp:
            //wave samples header
            is.read(data, 0, 20);
            chunkRead += 20;
            cbSize = LE.getuint32(data, 0);
            int unityNote = LE.getuint16(data, 4);
            int fineTune = LE.getuint16(data, 6);
            int attenuation = LE.getuint32(data, 8);
            int fuOptions = LE.getuint32(data, 12);
            int cSampleLoops = LE.getuint32(data, 16);
            if (cbSize > 20) {
              cbSize -= 20;  //already read 20 bytes
              is.skip(cbSize);
              chunkRead += cbSize;
            }
            instruments[cInstrument].regions[cRegion].unityNote = unityNote;
            instruments[cInstrument].regions[cRegion].fineTune = fineTune;
            instruments[cInstrument].regions[cRegion].attenuation = attenuation;
            instruments[cInstrument].regions[cRegion].fuOptions = fuOptions;
            if (debug) JFLog.log("cSampleLoops = " + cSampleLoops);
            if (cSampleLoops != 0) {
              //samples
              is.read(data, 0, 16);
              chunkRead += 16;
              cbSize = LE.getuint32(data, 0);
              int loopType = LE.getuint32(data, 4);
              int loopStart = LE.getuint32(data, 8);
              int loopLength = LE.getuint32(data, 12);
              if (debug) JFLog.log("Region Loop:" + loopStart + "," + loopLength + "," + loopType);
              instruments[cInstrument].regions[cRegion].loopStart = loopStart;
              instruments[cInstrument].regions[cRegion].loopLength = loopLength;
            }
            break;
          case IFFID_INAM:
            byte str[] = new byte[chunkLength];
            is.read(str);
            chunkRead += chunkLength;
            instruments[cInstrument].name = LE.getString(str, 0, chunkLength).trim();
            break;
        }
        if (chunkRead != chunkLength) is.skip(chunkLength - chunkRead);
        pos += chunkLength;
        //each chunk MUST start on a WORD boundry
        if ((chunkLength & 1) == 1) {
          is.skip(1);
          pos++;
        }
      }
      //ensure names are unique (not really needed unless you get by names)
      for(int a=0;a<instruments.length;a++) {
        Instrument i = instruments[a];
        //change duplicate names to something unique
        boolean dup;
        char ch = 'a';
        String name = i.name;
        do {
          dup = false;
          for(int b=0;b<instruments.length;b++) {
            if (b == a) continue;
            if (instruments[b].name.equals(i.name)) {
              i.name = name + ch;
              ch++;
              dup = true;
              break;
            }
          }
        } while (dup);
        if (debug) JFLog.log("Instrument:" + i.name);
      }
      JFLog.log("Import complete");
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public Instrument buildSamples(Instrument i, int region) {
    //each region is a wav file
    try {
      Region r = i.regions[region];
      i.loopStart = r.loopStart;
      i.loopLength = r.loopLength;
      int offset = waveFormsOffsets[r.waveLink];
      if (debug) JFLog.log("offset=" + offset);
      ByteArrayInputStream bais = new ByteArrayInputStream(waveFormPool);
      bais.skip(offset);
      byte chunk[] = new byte[4 * 3];
      bais.read(chunk);
      int id = LE.getuint32(chunk, 0);
      int chunkLength = LE.getuint32(chunk, 4);
      int subid = LE.getuint32(chunk, 8);
      if (debug) {
        JFLog.log("id=0x" + Integer.toString(id, 16) + ":" + getName(id) + ":" + chunkLength);
        JFLog.log("listid=0x" + Integer.toString(subid, 16) + ":" + getName(id));
      }
      if (id == IFFID_LIST && subid == IFFID_wave) {
        LE.setuint32(waveFormPool, offset, IFFID_RIFF);
        id = IFFID_RIFF;
        LE.setuint32(waveFormPool, offset + 8, IFFID_WAVE);
        subid = IFFID_WAVE;
      }
      if (id != IFFID_RIFF || subid != IFFID_WAVE) {
        throw new Exception("WAV not found");
      }
      bais.reset();
      bais.skip(offset);
      Wav wav = new Wav();
      if (!wav.load(bais)) {
        throw new Exception("WAV load failed");
      }
      wav.readAllSamples();
      i.samples = wav.getSamples16();
      return i;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public int getRegionsCount(String name) {
    for(int a=0;a<instruments.length;a++) {
      Instrument i = instruments[a];
      if (i.name.equals(name)) return i.regions.length;
    }
    JFLog.log("Instrument not found:" + name);
    return 0;
  }

  /** Returns the key range that a range should be used within.
   *
   *  @return int[] : [0]=min Key [1]=max Key
   */
  public Region getRegion(String name, int region) {
    for(int a=0;a<instruments.length;a++) {
      Instrument i = instruments[a];
      if (i.name.equals(name)) {
        Region r = i.regions[region];
        return r;
      }
    }
    JFLog.log("Instrument not found:" + name);
    return null;
  }

  //C-2=0x00 C-1=0x0c C0=0x18 C1=0x24 C2=0x30 ... C8=0x78 ... C9=0x7f

  private static String notes[] = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

  public static String getKeyName(int key) {
    int octave = key / 12;
    int note = key % 12;
    return notes[note] + octave;
  }

  public Instrument getInstrument(String name, int region) {
    for(int a=0;a<instruments.length;a++) {
      Instrument i = instruments[a];
      if (i.name.equals(name)) return buildSamples(i, region);
    }
    JFLog.log("Instrument not found:" + name);
    return null;
  }

  public String[] getInstrumentNames() {
    ArrayList<String> names = new ArrayList<String>();
    for(int a=0;a<instruments.length;a++) {
      Instrument i = instruments[a];
      names.add(i.name);
    }
    return names.toArray(new String[0]);
  }
}
