import eslint from "@eslint/js";
import tseslint from "typescript-eslint";

export default tseslint.config(
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  {
    ignores: [
      "bin",
      "build",
      "dist",
      "node_modules",
      "webpack",
      "*.config.js",
      "*.config.cjs",
      "*.config.mjs",
    ],
  },
);
