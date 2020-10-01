package jfpbx.core;

import jfpbx.db.Database;
import jfpbx.db.ExtensionRow;
import java.io.*;

import javaforce.*;
import javaforce.voip.*;


/** Low-level plugin for handling INVITEs to voicemail. */

public class VoiceMail implements Plugin, DialChain, PBXEventHandler {
  public final static int pid = 10;  //priority
  public static enum State {
    VM_GREETING,

    VM_PASSWORD,
    VM_BAD_PASSWORD,

    VM_REC_MSG_BEEP,
    VM_REC_MSG,
    VM_REC_MSG_MENU,

    VM_REC_GREETING_BEEP,
    VM_REC_GREETING,
    VM_REC_GREETING_MENU,

    VM_MAIN_MENU,
    VM_PLAY_MSG,
    VM_GOODBYE
  }
  private PBXAPI api;

//interface Plugin

  public void init(PBXAPI api) {
    this.api = api;
    api.hookDialChain(this);
    JFLog.log("Voicemail plugin init");
  }

  public void uninit(PBXAPI api) {
    api.unhookDialChain(this);
    JFLog.log("Voicemail plugin uninit");
  }

  public void install(PBXAPI api) {
    //nothing to do
  }

  public void uninstall(PBXAPI api) {
    //nothing to do
  }

//interface DialChain
  public int getPriority() {return pid;}
  public int onInvite(CallDetailsPBX cd, boolean src) {
    //NOTE : there is no dst - it's a one-sided call
    if (!src) return -1;
    if (!cd.authorized) {
      if (!cd.anon) return -1;
    }
    ExtensionRow ext = Database.getExtension(cd.dialed);
    if (ext == null) return -1;  //an extension is not being dialed
    if (cd.invited) {
      //reINVITE (just get new codecs)
      api.log(cd, "VM : reINVITE");
      SDP.Stream astream = cd.src.sdp.getFirstAudioStream();
      cd.audioRelay.change_src(astream);
      cd.pbxsrc.sdp.getFirstAudioStream().codecs = cd.src.sdp.getFirstAudioStream().codecs;
      cd.sip.buildsdp(cd, cd.pbxsrc);
      api.reply(cd, 200, "OK", null, true, true);
      return pid;
    }
    if (!ext.voicemail) {
      api.log(cd, "VM : ext doesn't have voicemail enabled");
      return -1;  //extension doesn't have voicemail
    }
    cd.pbxsrc.to = cd.src.to.clone();
    cd.pbxsrc.from = cd.src.from.clone();
    if (cd.audioRelay == null) {
      cd.audioRelay = new RTPRelay();
      cd.audioRelay.init();
    }
    cd.audioRelay.init(cd, this, api);
    cd.pbxsrc.host = cd.src.host;
    cd.pbxsrc.sdp = new SDP();
    cd.pbxsrc.sdp.ip = api.getlocalhost(cd);
    SDP.Stream astream = cd.pbxsrc.sdp.addStream(SDP.Type.audio);
    astream.port = cd.audioRelay.getPort_src();
    astream.codecs = cd.src.sdp.getFirstAudioStream().codecs;
    cd.sip.buildsdp(cd, cd.pbxsrc);
    cd.invited = true;
    cd.connected = true;
    cd.pbxsrc.to = SIP.replacetag(cd.pbxsrc.to, SIP.generatetag());  //assign tag
    api.reply(cd, 200, "OK", null, true, true);
    start(cd);
    return pid;
  }

  public void onRinging(CallDetailsPBX cd, boolean src) {
  }

  public void onSuccess(CallDetailsPBX cd, boolean src) {
  }

  public void onCancel(CallDetailsPBX cd, boolean src) {
  }

  public void onError(CallDetailsPBX cd, int code, boolean src) {
  }

  public void onTrying(CallDetailsPBX cd, boolean src) {
  }

  public void onBye(CallDetailsPBX cd, boolean src) {
    if (!src) return;
    api.reply(cd, 200, "OK", null, false, true);
    if (cd.audioRelay != null) {
      cd.audioRelay.uninit();
      cd.audioRelay = null;
    }
  }

  public void onFeature(CallDetailsPBX cd, String cmd, String cmddata, boolean src) {
  }

//interface PBXEventHandler
  public void event(CallDetailsPBX cd, int type, char digit, boolean interrupted) {
    //type = DTMF digit or end of playSound()
    JFLog.log("event:type=" + type + ":digit=" + digit + ":vmstate=" + cd.vmstate);
    switch (type) {
      case PBXEventHandler.DIGIT:
        switch (cd.vmstate) {
          case VM_GREETING:
            if (digit == '*') {
              cd.vmstate = State.VM_PASSWORD;
              cd.vmstr = "";
              cd.audioRelay.playSound("vm-enter-password");
              cd.audioRelay.addSound("vm-pause");
            } else {
              cd.vmstate = State.VM_REC_MSG_BEEP;
              cd.audioRelay.playSound("vm-beep");
            }
            break;
          case VM_PASSWORD:
            JFLog.log("vm password digit=" + digit);
            switch (digit) {
              case '#':
                checkPassword(cd);
                break;
              case '*':
                //reset password
                cd.vmstr = "";
                cd.audioRelay.playSound("vm-pause");
                break;
              default:
                cd.vmstr += digit;
                if (cd.vmstr.length() > 32) {
                  checkPassword(cd);
                } else {
                  cd.audioRelay.playSound("vm-pause");  //play a long pause to cause a timeout
                }
                break;
            }
            break;
          case VM_REC_MSG_MENU:
            switch (digit) {
              case '1':
                cd.vmstate = State.VM_GOODBYE;
                cd.audioRelay.playSound("vm-goodbye");
                break;
              case '2':
                deleteRecording(cd);
                cd.vmstate = State.VM_REC_MSG_BEEP;
                cd.audioRelay.playSound("vm-beep");
                break;
            }
            break;
          case VM_REC_GREETING_MENU:
            switch (digit) {
              case '1':
                saveNewGreeting(cd);
                cd.vmstate = State.VM_MAIN_MENU;
                cd.audioRelay.playSound("vm-main-menu");
                cd.audioRelay.addSound("vm-pause");
                break;
              case '2':
                deleteRecording(cd);
                cd.vmstate = State.VM_REC_GREETING_BEEP;
                cd.audioRelay.playSound("vm-beep");
                break;
            }
            break;
          case VM_MAIN_MENU:
            cd.vmattempts = 0;
            switch (digit) {
              case '1':  //listen messages
                if (getMsgLists(cd)) {
                  playMsg(cd);
                }
                break;
              case '2':  //record greeting
                cd.vmstate = State.VM_REC_GREETING_BEEP;
                cd.audioRelay.playSound("vm-rec-greeting");
                cd.audioRelay.addSound("vm-beep");
                break;
              case '*':
                cd.vmstate = State.VM_GOODBYE;
                cd.audioRelay.playSound("vm-goodbye");
                break;
              default:
                cd.audioRelay.playSound("vm-incorrect");
                break;
            }
            break;
          case VM_PLAY_MSG:
            switch (digit) {
              case '1':  //replay message
                playMsg(cd);
                break;
              case '3':  //skip message
                nextMsg(cd);
                break;
              case '7':  //delete message
                deleteMsg(cd);
                nextMsg(cd);
                break;
              case '9':  //save message
                saveMsg(cd);
                nextMsg(cd);
              case '*':  //prev menu
                cd.vmstate = State.VM_MAIN_MENU;
                cd.audioRelay.playSound("vm-main-menu");
                cd.audioRelay.addSound("vm-pause");
                break;
            }
            break;
        }
        break;
      case PBXEventHandler.SOUND:
        switch (cd.vmstate) {
          case VM_GREETING:
            if (interrupted) break;
            cd.vmstate = State.VM_REC_MSG_BEEP;
            cd.audioRelay.playSound("vm-beep");
            break;
          case VM_REC_MSG_BEEP:
            cd.vmstate = State.VM_REC_MSG;
            api.log(cd, "Recording voicemail for ext " + cd.dialed);
            api.makePath(Paths.lib + "voicemail/" + cd.dialed);
            cd.vmrecfn = Paths.lib + "voicemail/" + cd.dialed + "/msg-new-" + System.currentTimeMillis();
            cd.audioRelay.recordSoundFull(cd.vmrecfn, 5 * 60);
            cd.vmattempts = 0;
            break;
          case VM_REC_MSG:
            if (cd.audioRelay.getRecordingLength() < 3) {
              deleteRecording(cd);
              cd.audioRelay.playSound("vm-msg");
              cd.audioRelay.addSound("vm-too-short");
              cd.vmstate = State.VM_REC_MSG_BEEP;
              cd.audioRelay.playSound("vm-beep");
            } else {
              cd.vmstate = State.VM_REC_MSG_MENU;
              cd.audioRelay.playSound("vm-rec-menu");
              cd.audioRelay.addSound("vm-pause");
            }
            break;
          case VM_REC_MSG_MENU:
          case VM_REC_GREETING_MENU:
            if (interrupted) break;
            cd.vmattempts++;
            if (cd.vmattempts > 3) {
              hangup(cd);
            } else {
              cd.audioRelay.playSound("vm-rec-menu");
              cd.audioRelay.addSound("vm-pause");
            }
            break;
          case VM_REC_GREETING_BEEP:
            cd.vmstate = State.VM_REC_GREETING;
            api.log(cd, "Recording voicemail greeting for ext " + cd.dialed);
            api.makePath(Paths.lib + "voicemail/" + cd.dialed);
            cd.vmrecfn = Paths.lib + "voicemail/" + cd.dialed + "/new-greeting";
            cd.audioRelay.recordSoundFull(cd.vmrecfn, 5 * 60);
            cd.vmattempts = 0;
            break;
          case VM_REC_GREETING:
            if (cd.audioRelay.getRecordingLength() < 5) {
              deleteRecording(cd);
              cd.audioRelay.playSound("vm-msg");
              cd.audioRelay.addSound("vm-too-short");
              cd.vmstate = State.VM_REC_GREETING_BEEP;
              cd.audioRelay.playSound("vm-beep");
            } else {
              cd.vmstate = State.VM_REC_GREETING_MENU;
              cd.audioRelay.playSound("vm-rec-menu");
              cd.audioRelay.addSound("vm-pause");
            }
            break;
          case VM_PASSWORD:
            if (interrupted) break;
            checkPassword(cd);
            break;
          case VM_BAD_PASSWORD:
            if (cd.vmattempts > 3) {
              cd.vmstate = State.VM_GOODBYE;
              cd.audioRelay.playSound("vm-goodbye");
            } else {
              cd.vmstate = State.VM_PASSWORD;
              cd.vmstr = "";
              cd.audioRelay.playSound("vm-enter-password");
            }
            break;
          case VM_MAIN_MENU:
            if (interrupted) break;
            cd.vmattempts++;
            if (cd.vmattempts > 3) {
              hangup(cd);
            } else {
              cd.audioRelay.playSound("vm-main-menu");
              cd.audioRelay.addSound("vm-pause");
            }
            break;
          case VM_PLAY_MSG:
            if (interrupted) break;
            cd.vmattempts++;
            if (cd.vmattempts > 3) {
              cd.vmstate = State.VM_MAIN_MENU;
              break;
            }
            cd.audioRelay.playSound("vm-msg-menu");
            cd.audioRelay.addSound("vm-pause");
            break;
          case VM_GOODBYE:
            hangup(cd);
            break;
        }
        break;
    }
  }

  public void samples(CallDetailsPBX cd, short sam[]) {
    System.arraycopy(sam, 0, RTPRelay.silence, 0, 160);
  }
  public void video(CallDetailsPBX cd, byte data[], int off, int len) {}

//private code
  protected void start(CallDetailsPBX cd) {
    String lang = "en";  //TODO : query for ext
    ExtensionRow ext = Database.getExtension(cd.dialed);
    cd.vmstate = State.VM_GREETING;
    cd.vmstr = "";
    cd.vmpass = ext.voicemailpass;
    cd.audioRelay.setLang(lang);
    cd.audioRelay.setRawMode(false);
    if (api.getExtension(cd.fromnumber) != null) {
      //NAT src
      cd.src.sdp.getFirstAudioStream().port = -1;
    }
    cd.audioRelay.start_src(cd.src.sdp.getFirstAudioStream());
    File file;
    String fn = Paths.lib + "voicemail/" + cd.dialed + "/greeting.wav";
    try {
      file = new File(fn);
      if (file.exists()) {
        cd.audioRelay.playSoundFull(fn);
      } else {
        cd.audioRelay.playSound("vm-greeting");
      }
    } catch (Exception e) {}
  }

  private void hangup(CallDetailsPBX cd) {
    cd.cmd = "BYE";
    cd.src.cseq++;
    api.issue(cd, null, false, true);
    if (cd.audioRelay != null) {
      cd.audioRelay.uninit();
      cd.audioRelay = null;
    }
  }

  private void checkPassword(CallDetailsPBX cd) {
    //check passcode
    cd.vmattempts++;
    if (!cd.vmstr.equals(cd.vmpass)) {
      cd.vmstate = State.VM_BAD_PASSWORD;
      cd.audioRelay.playSound("vm-incorrect");
    } else {
      cd.vmstate = State.VM_MAIN_MENU;
      cd.vmattempts = 0;
      cd.audioRelay.playSound("vm-main-menu");
      cd.audioRelay.addSound("vm-pause");
    }
  }

  private boolean getMsgLists(CallDetailsPBX cd) {
    try {
      File file = new File(Paths.lib + "voicemail/" + cd.dialed + "/");
      cd.vmlistnew = file.list(new FilenameFilter() {
        public boolean accept(File dir, String fn) { return (fn.startsWith("msg-new-") && fn.endsWith(".wav")); }
      });
      if ((cd.vmlistnew != null) && (cd.vmlistnew.length == 0)) cd.vmlistnew = null;
      cd.vmlistold = file.list(new FilenameFilter() {
        public boolean accept(File dir, String fn) { return (fn.startsWith("msg-old-") && fn.endsWith(".wav")); }
      });
      if ((cd.vmlistold != null) && (cd.vmlistold.length == 0)) cd.vmlistold = null;
    } catch (Exception e) {
    }
    if ((cd.vmlistnew == null) && (cd.vmlistold == null)) {
      cd.vmstate = State.VM_MAIN_MENU;
      cd.audioRelay.playSound("vm-no-msgs");
      return false;
    } else if (cd.vmlistnew == null) {
      cd.vmnew = false;
    } else {
      cd.vmnew = true;
    }
    cd.vmstate = State.VM_PLAY_MSG;
    cd.vmpos = 0;
    return true;
  }

  private void playMsg(CallDetailsPBX cd) {
    cd.vmstate = State.VM_PLAY_MSG;
    if (cd.vmnew)
      cd.audioRelay.playSound("vm-new");
    else
      cd.audioRelay.playSound("vm-old");
    cd.audioRelay.addSound("vm-msg");
    cd.audioRelay.addNumber(cd.vmpos+1);
    if (cd.vmnew)
      cd.audioRelay.addSoundFull(Paths.lib + "voicemail/" + cd.dialed + "/" + cd.vmlistnew[cd.vmpos]);
    else
      cd.audioRelay.addSoundFull(Paths.lib + "voicemail/" + cd.dialed + "/" + cd.vmlistold[cd.vmpos]);
    cd.audioRelay.addSound("vm-msg-menu");
    cd.audioRelay.addSound("vm-pause");
  }

  private void nextMsg(CallDetailsPBX cd) {
    if (cd.vmnew) {
      renameMsg(cd);
      cd.vmpos++;
      if (cd.vmpos == cd.vmlistnew.length) {
        if (cd.vmlistold == null) {
          cd.vmpos = -1;
        } else {
          cd.vmpos = 0;
          cd.vmnew = false;
        }
      }
    } else {
      cd.vmpos++;
      if (cd.vmpos == cd.vmlistold.length) {
        cd.vmpos = -1;
      }
    }
    if (cd.vmpos == -1) {
      cd.vmstate = State.VM_MAIN_MENU;
      cd.audioRelay.playSound("vm-end-msgs");
      cd.audioRelay.addSound("vm-main-menu");
      cd.audioRelay.addSound("vm-pause");
    } else {
      cd.audioRelay.playSound("vm-next");
      cd.audioRelay.addSound("vm-msg");
      playMsg(cd);
    }
  }

  private void renameMsg(CallDetailsPBX cd) {
    //rename 'new' to 'old'
    String fnnew = cd.vmlistnew[cd.vmpos];
    String fnold = fnnew.replaceAll("new", "old");
    try {
      File fnew = new File(fnnew);
      File fold = new File(fnold);
      fnew.renameTo(fold);
    } catch (Exception e) {
      api.log(cd, "VM : Unable to rename 'new' msg to 'old' : " + fnnew);
    }
  }

  private void deleteMsg(CallDetailsPBX cd) {
    File file = null;
    try {
      file = new File(Paths.lib + "voicemail/" + cd.dialed + "/" + (cd.vmnew ? cd.vmlistnew[cd.vmpos] : cd.vmlistold[cd.vmpos]) );
      file.delete();
      cd.audioRelay.playSound("vm-msg");
      cd.audioRelay.addSound("vm-deleted");
    } catch (Exception e) {
      if (file != null) api.log(cd, "Failed to delete VM msg : " + file.toString() );
    }
  }

  private void saveMsg(CallDetailsPBX cd) {
    //TODO : for now msgs are never auto deleted
  }

  private void deleteRecording(CallDetailsPBX cd) {
    File file = null;
    try {
      file = new File( cd.vmrecfn + ".wav" );
      file.delete();
    } catch (Exception e) {
      if (file != null) api.log(cd, "Failed to delete VM msg : " + file.toString() );
    }
  }

  private void saveNewGreeting(CallDetailsPBX cd) {
    File file, newFile;
    try {
      file = new File(Paths.lib + "voicemail/" + cd.dialed + "/greeting.wav");
      file.delete();
      newFile = new File(cd.vmrecfn + ".wav");
      newFile.renameTo(file);
    } catch (Exception e) {
      api.log(cd, "Failed to save new VM greeting for ext " + cd.dialed );
    }
  }
}
