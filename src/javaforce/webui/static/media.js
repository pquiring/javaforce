var medias = new Map();

function media_init(media, codecs) {
  console.log("media_init:id=" + media.id);
  var ctx = {};
  ctx.media = media;
  ctx.mediaSource = null;
  ctx.streamBuffer = null;
  ctx.codecs = codecs;
  ctx.buffered = 0;
  ctx.toString = function() {return "media context:id=" + this.media.id;};
  console.log("media_init:ctx=" + ctx);
  medias.set(media.id, ctx);
  ctx.mediaSource = new MediaSource();
  media.src = URL.createObjectURL(ctx.mediaSource);
  ctx.mediaSource.addEventListener('sourceopen', function(event) {
//    URL.revokeObjectURL(media.src);
    console.log("media_init.sourceopen:ctx=" + ctx);
    if (!MediaSource.isTypeSupported(ctx.codecs)) {
      console.log("Error:codecs not supported:" + ctx.codecs);
      return;
    }
    ctx.streamBuffer = ctx.mediaSource.addSourceBuffer(ctx.codecs);
  });
}

function media_uninit(media) {
  var ctx = medias.get(media.id);
  //TODO : stop/free resources
  medias.delete(media.id);
}

function media_add_buffer(media) {
  var ctx = medias.get(media.id);
  console.log("media_add_buffer:ctx=" + ctx + ":byteLength=" + bindata.byteLength + ":readyState=" + ctx.media.readyState);
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
  if (ctx.media.readyState !== 4) {
    ctx.buffered++;
    if (ctx.buffered === 5) {
      ctx.media.play();
      ctx.buffered = 0;
    }
  }
}
