import express from 'express';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';

import sunoRoutes from './routes/suno.js';
import sheetsRoutes from './routes/sheets.js';
import audioRoutes from './routes/audio.js';
import aiEngineRoutes from './routes/aiEngine.js';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// API Endpoints
app.use('/api', sunoRoutes);
app.use('/api', sheetsRoutes);
app.use('/api', audioRoutes);
app.use('/api', aiEngineRoutes);

// Static frontend assets
const publicPath = path.resolve(__dirname, '../public');
app.use(express.static(publicPath, {
  maxAge: 0,
  etag: true,
  setHeaders: (res, filePath) => {
    if (/\.(html|js|css)$/i.test(filePath)) {
      res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate');
    }
  }
}));

// SPA Catch-all fallback
app.get('*', (req, res) => {
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate');
  res.sendFile(path.join(publicPath, 'index.html'));
});

// Process-level resilience
process.on('uncaughtException', (err) => {
  console.error('[Process] Uncaught Exception:', err);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('[Process] Unhandled Rejection at:', promise, 'reason:', reason);
});

// Start server
const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(` Suno Studio Wiki Server running at: http://localhost:${PORT}`);
  console.log(` Mode: Autonomous (No AI Key Required)`);
  console.log(` Static Assets: ${publicPath}`);
  console.log(`=======================================================`);
});

server.keepAliveTimeout = 65000;
server.headersTimeout = 66000;
