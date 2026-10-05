var medias = new Map();

function media_set_source(media, src) {
  media.src = src;
  var table = document.getElementById(media.id + "s2");
  table.style.display = 'none';  //hide initial controls
  media.setAttribute('controls', '');  //show native controls
  media.play();
}

function media_set_live_source(media, codecs) {
  console.log("media_init:id=" + media.id);
  var ctx = {};
  ctx.media = media;
  ctx.mediaSource = null;
  ctx.streamBuffer = null;
  ctx.codecs = codecs;
  ctx.playing = false;
  ctx.toString = function() {return "media context:id=" + this.media.id;};
  console.log("media_init:ctx=" + ctx);
  medias.set(media.id, ctx);
  ctx.mediaSource = new MediaSource();
  media.src = URL.createObjectURL(ctx.mediaSource);
  var table = document.getElementById(media.id + "s2");
  table.style.display = 'none';  //hide initial controls
  media.setAttribute('controls', '');  //show native controls
  ctx.mediaSource.addEventListener('sourceopen', function(event) {
    URL.revokeObjectURL(media.src);
    console.log("media_init.sourceopen:ctx=" + ctx);
    if (!MediaSource.isTypeSupported(ctx.codecs)) {
      console.log("Error:codecs not supported:" + ctx.codecs);
      return;
    }
    ctx.streamBuffer = ctx.mediaSource.addSourceBuffer(ctx.codecs);
  });
}

//containers
var mkv = "video/x-matroska";  //capture works, playback does not
var mp4 = "video/mp4";
var webm = "video/webm";

//video codecs
var h264 = "avc1.42401e";
var h265 = "hvc1.1.6.L186.B0";
var h265vr = "hev1.1.6.L186.B0";  //variable resolution
var vp8 = "vp8";
var vp9 = "vp9";
var av1 = "av01.0.04M.08";

//audio codes
var opus = "opus";
var mp4a = "mp4a.40.2";  //AAC

//NOTE : not all container/codec combinations are supported

function media_set_capture(media, audio, video) {
  console.log("media_set_capture:id=" + media.id);
  var ctx = {};
  ctx.media = media;
  var table = document.getElementById(media.id + "s2");
  table.style.display = 'none';  //hide initial controls
  ctx.capture = navigator.mediaDevices.getUserMedia({'audio' : audio,'video': video});
  //{'audio' : true, 'video' : width:1024, height: 720, framerate: {ideal: 10, max:15}, facingMode: 'user' | 'environment'}
  ctx.capture.then((stream) => {
    var opts = {
      audioBitsPerSecond: 128000,
      videoBitsPerSecond: 2500000,
      mimeType: mp4 + ";codecs=" + h264 + "," + opus
    };
    ctx.recorder = new MediaRecorder(stream, opts);
    ctx.recorder.onstart = (event) => {console.log("recorder.start");};
    ctx.recorder.onerror = (event) => {console.log("recorder.error");};
    ctx.recorder.ondataavailable = (event) => {
      //console.log("data.length=" + event.data.size);
      ws.send(event.data);
    };
    ctx.media.srcObject = stream;
    ctx.media.play();
    ctx.recorder.start(100);
  });
  medias.set(media.id, ctx);
}

function media_uninit(media) {
  var ctx = medias.get(media.id);
  //TODO : stop/free resources
  medias.delete(media.id);
}

var HTMLMediaElement_HAVE_NOTHING = 0;
var HTMLMediaElement_HAVE_METADATA = 1;
var HTMLMediaElement_HAVE_CURRENT_DATA = 2;
var HTMLMediaElement_HAVE_FUTURE_DATA = 3;
var HTMLMediaElement_HAVE_ENOUGH_DATA = 4;

//MediaSource.readyState = "open", "closed", "ended"

function media_add_buffer(media) {
  var ctx = medias.get(media.id);
  console.log("media_add_buffer:ctx=" + ctx
    + ":byteLength=" + bindata.byteLength
    + ":media.readyState=" + ctx.media.readyState
    + ":mediasource.readyState=" + ctx.mediaSource.readyState
    + ":currentTime=" + ctx.media.currentTime);
  if (ctx.mediaSource.readyState !== 'open') {
    console.log("media_add_buffer:Error:MediaSource not open");
    return;
  }
  if (ctx.streamBuffer === null) {
    console.log("media_add_buffer:Error:StreamBuffer is null");
    return;
  }
  if (ctx.streamBuffer.updating) {
    console.log("media_add_buffer:Error:StreamBuffer is still updating");
    return;
  }
  ctx.streamBuffer.appendBuffer(bindata);
  if (!ctx.playing && ctx.media.readyState === HTMLMediaElement.HAVE_ENOUGH_DATA) {
    var bufs = media.buffered;
    var len = bufs.length;
    for(var i=0;i<len;i++) {
      console.log("buffered:" + bufs.start(i) + " to " + bufs.end(i));
    }
    ctx.media.play();
    ctx.playing = true;
  }
}

function media_play(media) {
  console.log("media_play");
  var ctx = {};
  ctx.media = media;
  var table = document.getElementById(media.id + "s2");
  table.style.display = 'none';  //hide initial controls
  ctx.media.play();
  medias.set(media.id, ctx);
}

function media_pause(media) {
  console.log("media_pause");
  media.pause();
}

function media_stop(media) {
  console.log("media_stop");
  media.stop();
}

function media_seek(media, time) {
  console.log("media_seek:" + time);
  media.currentTime = time;
}

function media_onplay(media) {
  var msg = {
    event: "onplay",
    id: media.id
  };
  ws.send(JSON.stringify(msg));
}

function media_onpause(media) {
  var msg = {
    event: "onpause",
    id: media.id
  };
  ws.send(JSON.stringify(msg));
}

function media_onstop(media) {
  var msg = {
    event: "onstop",
    id: media.id
  };
  ws.send(JSON.stringify(msg));
}
