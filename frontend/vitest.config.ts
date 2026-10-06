import path from "node:path";
import { defineConfig } from "vitest/config";

export default defineConfig({
  resolve: {
    // Same "@/..." imports as tsconfig.json, so components can be tested.
    alias: { "@": path.resolve(process.cwd(), "src") },
  },
  test: {
    environment: "jsdom",
    setupFiles: [
      "./vitest.setup.ts"
    ]
  }
});
