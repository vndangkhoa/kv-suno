import express from 'express';
import { spawn } from 'child_process';

const router = express.Router();

/**
 * POST /api/decode-audio-file
 * Body: Raw audio binary (application/octet-stream)
 * Transcodes arbitrary audio stream into standard 16-bit 44.1kHz PCM WAV via ffmpeg
 */
router.post('/decode-audio-file', express.raw({ type: '*/*', limit: '50mb' }), (req, res) => {
  if (!req.body || req.body.length === 0) {
    return res.status(400).json({ error: 'Empty audio buffer' });
  }

  // Spawn ffmpeg to transcode stdin to WAV on stdout
  const ffmpeg = spawn('ffmpeg', [
    '-i', 'pipe:0',           // read from stdin
    '-f', 'wav',              // output format wav
    '-acodec', 'pcm_s16le',   // standard PCM 16-bit
    '-ar', '44100',           // 44.1 kHz sample rate
    '-ac', '2',               // stereo
    'pipe:1'                  // write to stdout
  ]);

  res.set({
    'Content-Type': 'audio/wav',
    'Access-Control-Allow-Origin': '*'
  });

  ffmpeg.stdout.pipe(res);

  ffmpeg.stderr.on('data', () => {
    // ffmpeg diagnostic stream, ignore in normal operation
  });

  ffmpeg.on('error', (err) => {
    console.error('[decode-audio-file] ffmpeg spawn error:', err);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Server audio transcoding failed' });
    }
  });

  // Write uploaded audio buffer to ffmpeg stdin
  ffmpeg.stdin.write(req.body);
  ffmpeg.stdin.end();
});

export default router;
