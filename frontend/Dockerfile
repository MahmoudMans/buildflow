FROM node:24-alpine AS dependencies

WORKDIR /app

RUN corepack enable
RUN corepack prepare pnpm@10.17.1 --activate

COPY package.json pnpm-lock.yaml ./

RUN pnpm install --frozen-lockfile


FROM node:24-alpine AS build

WORKDIR /app

RUN corepack enable
RUN corepack prepare pnpm@10.17.1 --activate

COPY --from=dependencies /app/node_modules ./node_modules
COPY . .

ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1

ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}

RUN pnpm build


FROM node:24-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=build --chown=nextjs:nodejs /app/public ./public
COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

CMD ["node","server.js"]
