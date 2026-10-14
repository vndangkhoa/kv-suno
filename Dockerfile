FROM node:20-alpine

# Install ffmpeg for Suno .m4a audio stream extraction & HD Enhancer decoding
RUN apk add --no-cache ffmpeg

WORKDIR /app

# Install production dependencies
COPY package*.json ./
RUN npm ci --omit=dev || npm install --omit=dev

# Copy application source and static assets
COPY server ./server
COPY public ./public
COPY vietnamese-suno-creators.csv ./vietnamese-suno-creators.csv
COPY README.md CHANGELOG.md DESIGN.md PRODUCT.md ./

# Create persistent cache directory for extracted audio & metadata
RUN mkdir -p /app/.cache/audio && chmod -R 777 /app/.cache

EXPOSE 3000

ENV PORT=3000
ENV HOST=0.0.0.0
ENV NODE_ENV=production
ENV TZ=Asia/Ho_Chi_Minh

CMD ["node", "server/index.js"]
