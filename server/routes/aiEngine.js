import express from 'express';
import { scoreAndOptimizePrompt } from '../services/promptScorer.js';
import { generateLyrics } from '../services/lyricsEngine.js';
import { explainStyle } from '../services/styleExplainer.js';
import { generateGuide } from '../services/guideGenerator.js';
import { analyzeAudioFile } from '../services/audioAnalyzer.js';

const router = express.Router();

/**
 * POST /api/explain-style
 * Body: { title, style, language }
 */
router.post('/explain-style', (req, res) => {
  try {
    const { title, style, language = 'vi' } = req.body;
    const result = explainStyle({ title, style, language });
    res.json(result);
  } catch (err) {
    console.error('[explain-style] Error:', err.message);
    res.status(500).json({ error: 'Failed to explain style' });
  }
});

/**
 * POST /api/generate-lyrics
 * Body: { topic, genre, mood, vocalType, structure, language }
 */
router.post('/generate-lyrics', (req, res) => {
  try {
    const { topic, genre, mood, vocalType, structure, language = 'vi' } = req.body;
    const result = generateLyrics({ topic, genre, mood, vocalType, structure, language });
    res.json(result);
  } catch (err) {
    console.error('[generate-lyrics] Error:', err.message);
    res.status(500).json({ error: 'Failed to generate lyrics' });
  }
});

/**
 * POST /api/optimize-prompt
 * Body: { prompt, language }
 */
router.post('/optimize-prompt', (req, res) => {
  try {
    const { prompt, language = 'vi' } = req.body;
    const result = scoreAndOptimizePrompt(prompt, language);
    res.json(result);
  } catch (err) {
    console.error('[optimize-prompt] Error:', err.message);
    res.status(500).json({ error: 'Failed to optimize prompt' });
  }
});

/**
 * POST /api/generate-guide
 * Body: { keyword, category, language }
 */
router.post('/generate-guide', (req, res) => {
  try {
    const { keyword, category, language = 'vi' } = req.body;
    const result = generateGuide({ keyword, category, language });
    res.json(result);
  } catch (err) {
    console.error('[generate-guide] Error:', err.message);
    res.status(500).json({ error: 'Failed to generate guide' });
  }
});

/**
 * POST /api/analyze-audio
 * Accepts audio upload or metadata
 */
router.post('/analyze-audio', express.raw({ type: '*/*', limit: '20mb' }), (req, res) => {
  try {
    const size = req.body ? req.body.length : 0;
    const result = analyzeAudioFile({ size });
    res.json(result);
  } catch (err) {
    console.error('[analyze-audio] Error:', err.message);
    res.status(500).json({ error: 'Failed to analyze audio' });
  }
});

export default router;
